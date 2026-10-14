using System;
using System.Collections.Generic;
using Godot;
using LumberjackAttack.Core;
using LumberjackAttack.Entities;
using LumberjackAttack.Inventory;
using LumberjackAttack.Items;
using LumberjackAttack.Skills;
using LumberjackAttack.Time;
using LumberjackAttack.World;

namespace LumberjackAttack.Fishing;

public enum BobberPhase
{
	Idle,
	Waiting,
	Flashing,
}

/// <summary>
/// Cast → wait → flash-click → underwater minigame → grant catch (Java FishingManager).
/// </summary>
public partial class FishingService : Node2D
{
	private readonly TileMapLayer _ground;
	private readonly IReadOnlyDictionary<int, TileDef> _catalog;
	private readonly IReadOnlyDictionary<string, ItemDef> _items;
	private readonly PlayerInventory _inventory;
	private readonly PlayerEnergyState _energy;
	private readonly PlayerSkills _skills;
	private readonly IReadOnlyList<FishSpawnDef> _fishTable;
	private readonly Action _onInventoryChanged;
	private readonly Func<PlayerController> _player;

	private Polygon2D _bobber = null!;
	private FishingMinigameView _view = null!;
	private BobberPhase _phase = BobberPhase.Idle;
	private bool _busy;
	private double _waitMs;
	private double _flashMs;
	private double _flashToggleMs;
	private bool _flashVisible = true;
	private double _resultMs;
	private int _rodFishingSpeed;
	private FishingMinigame? _minigame;

	public bool IsBusy => _busy;
	public bool IsFlashing => _phase == BobberPhase.Flashing;
	public bool IsMinigameOpen => _minigame != null && !_minigame.Finished;
	public bool BlocksWorldInput => _busy && _phase != BobberPhase.Flashing;

	public FishingService(
		TileMapLayer ground,
		IReadOnlyDictionary<int, TileDef> catalog,
		IReadOnlyDictionary<string, ItemDef> items,
		PlayerInventory inventory,
		PlayerEnergyState energy,
		PlayerSkills skills,
		IReadOnlyList<FishSpawnDef> fishTable,
		Func<PlayerController> player,
		Action onInventoryChanged
	)
	{
		_ground = ground;
		_catalog = catalog;
		_items = items;
		_inventory = inventory;
		_energy = energy;
		_skills = skills;
		_fishTable = fishTable;
		_player = player;
		_onInventoryChanged = onInventoryChanged;
	}

	public override void _Ready()
	{
		_bobber = new Polygon2D
		{
			Polygon = new Vector2[]
			{
				new(-2, -2), new(2, -2), new(2, 2), new(-2, 2),
			},
			Color = new Color(1f, 0.86f, 0.24f),
			Visible = false,
			ZIndex = 8,
		};
		AddChild(_bobber);

		_view = new FishingMinigameView();
		_view.Configure(_items);
		AddChild(_view);
	}

	public bool TryStartCast(PlayerController player, Vector2 worldClick, out string toast)
	{
		toast = "";
		if (_busy || player.IsActionLocked)
		{
			return false;
		}

		var rod = _inventory.GetHeldRod();
		if (rod == null)
		{
			return false;
		}

		var cell = _ground.LocalToMap(_ground.ToLocal(worldClick));
		var tileId = _ground.GetCellSourceId(cell);
		if (tileId < 0 || !_catalog.TryGetValue(tileId, out var tile) || !tile.Fishable)
		{
			return false;
		}

		var tileCenter = new Vector2(
			cell.X * GameConfig.TileSize + GameConfig.TileSize / 2f,
			cell.Y * GameConfig.TileSize + GameConfig.TileSize / 2f
		);
		var playerCenter = player.GlobalPosition - new Vector2(0, GameConfig.PlayerHitboxHeight / 2f);
		if (playerCenter.DistanceTo(tileCenter) > GameConfig.ChopReachDistance)
		{
			return false;
		}

		if (!_energy.TrySpend(GameConfig.FishingCastEnergyCost))
		{
			toast = "Too tired";
			return true;
		}

		_busy = true;
		_phase = BobberPhase.Idle;
		_resultMs = 0;
		_minigame = null;
		_rodFishingSpeed = rod.FishingSpeed;
		player.FaceToward(tileCenter);
		player.TriggerRodCast(() =>
		{
			_bobber.GlobalPosition = tileCenter;
			_bobber.Visible = true;
			_bobber.Color = new Color(1f, 0.86f, 0.24f);
			_waitMs = RandomBiteDelayMs();
			_phase = BobberPhase.Waiting;
			_flashVisible = true;
		});
		return true;
	}

	/// <summary>Click during FLASHING to start the underwater minigame.</summary>
	public bool TryHookBite()
	{
		if (_phase != BobberPhase.Flashing || _minigame != null)
		{
			return false;
		}

		_phase = BobberPhase.Idle;
		_flashMs = 0;
		_bobber.Visible = false;
		_minigame = new FishingMinigame(_fishTable);
		_resultMs = 0;
		_view.Open(_minigame);
		return true;
	}

	public void Cancel()
	{
		_busy = false;
		_phase = BobberPhase.Idle;
		_waitMs = 0;
		_flashMs = 0;
		_resultMs = 0;
		_minigame = null;
		_bobber.Visible = false;
		_bobber.Color = new Color(1f, 0.86f, 0.24f);
		_view.Close();
		_player()?.CancelAllActions();
	}

	public override void _PhysicsProcess(double delta)
	{
		if (!_busy)
		{
			return;
		}

		var elapsedMs = delta * 1000.0;

		if (_minigame != null)
		{
			UpdateMinigame(elapsedMs);
			return;
		}

		if (_phase == BobberPhase.Waiting)
		{
			_waitMs -= elapsedMs;
			if (_waitMs > 0)
			{
				return;
			}

			_phase = BobberPhase.Flashing;
			_flashMs = GameConfig.FishingBiteFlashMs;
			_flashToggleMs = 0;
			_flashVisible = true;
			UpdateBobberFlashColor();
			return;
		}

		if (_phase == BobberPhase.Flashing)
		{
			_flashMs -= elapsedMs;
			_flashToggleMs -= elapsedMs;
			if (_flashToggleMs <= 0)
			{
				_flashVisible = !_flashVisible;
				_flashToggleMs = GameConfig.FishingFlashToggleMs;
				UpdateBobberFlashColor();
			}

			if (_flashMs <= 0)
			{
				// Missed bite — wait for another nibble (line stays out).
				_phase = BobberPhase.Waiting;
				_waitMs = RandomBiteDelayMs();
				_flashMs = 0;
				_flashVisible = true;
				_bobber.Color = new Color(1f, 0.86f, 0.24f);
			}
		}
	}

	public override void _Process(double delta)
	{
		if (_minigame == null || _minigame.Finished)
		{
			return;
		}

		_minigame.SetMoveLeft(Input.IsKeyPressed(Key.A) || Input.IsKeyPressed(Key.Left));
		_minigame.SetMoveRight(Input.IsKeyPressed(Key.D) || Input.IsKeyPressed(Key.Right));
	}

	private void UpdateMinigame(double elapsedMs)
	{
		if (_minigame == null)
		{
			return;
		}

		if (!_minigame.Finished)
		{
			_minigame.Update(elapsedMs);
			_view.Redraw();
			return;
		}

		_view.Redraw();
		if (_resultMs <= 0)
		{
			_resultMs = GameConfig.FishingResultDisplayMs;
		}

		_resultMs -= elapsedMs;
		if (_resultMs > 0)
		{
			return;
		}

		FinishMinigame();
	}

	private void FinishMinigame()
	{
		var caught = _minigame?.CaughtItemId;
		_minigame = null;
		_resultMs = 0;
		_phase = BobberPhase.Idle;
		_view.Close();
		_bobber.Visible = false;

		if (!string.IsNullOrEmpty(caught))
		{
			_inventory.AddItem(caught, 1);
			if (_items.TryGetValue(caught, out var def) && def.FishingXp > 0)
			{
				_skills.GrantXp(SkillType.Fishing, def.FishingXp);
			}

			_onInventoryChanged();
			GD.Print($"Caught {caught}");
		}

		_busy = false;
		_player()?.EndRodPose();
	}

	private void UpdateBobberFlashColor()
	{
		_bobber.Color = _flashVisible
			? Colors.White
			: new Color(1f, 0.86f, 0.24f);
	}

	private double RandomBiteDelayMs()
	{
		var speed = Math.Clamp(_rodFishingSpeed, 0, GameConfig.FishingSpeedCap);
		var maxMs = Math.Max(GameConfig.FishingBiteMinMs, GameConfig.FishingBiteMaxMs - speed * 25.0);
		return GameConfig.FishingBiteMinMs
			+ Random.Shared.NextDouble() * (maxMs - GameConfig.FishingBiteMinMs);
	}
}
