using System;
using System.Collections.Generic;
using System.Linq;
using Godot;
using LumberjackAttack.Combat;
using LumberjackAttack.Collection;
using LumberjackAttack.Core;
using LumberjackAttack.Crafting;
using LumberjackAttack.Economy;
using LumberjackAttack.Entities;
using LumberjackAttack.Fishing;
using LumberjackAttack.Interaction;
using LumberjackAttack.Inventory;
using LumberjackAttack.Items;
using LumberjackAttack.Save;
using LumberjackAttack.Settings;
using LumberjackAttack.Skills;
using LumberjackAttack.Time;
using LumberjackAttack.Ui;
using LumberjackAttack.World;
using LumberjackAttack.World.Forage;
using LumberjackAttack.World.Placeable;

namespace LumberjackAttack;

/// <summary>Bootstraps gameplay + save/load (F5 save, F4 load, sleep autosave, 120s autosave).</summary>
public partial class Main : Node2D
{
	private const double TransitionCooldownSec = 0.45;

	private IReadOnlyDictionary<int, TileDef> _catalog = null!;
	private IReadOnlyDictionary<string, ItemDef> _items = null!;
	private TileSet _tileSet = null!;
	private IReadOnlyList<MapTransition> _transitions = null!;
	private TileMapLayer _ground = null!;
	private TileVisualLayer _tileVisuals = null!;
	private TallTreeLayer _trees = null!;
	private OverlayLayer _overlays = null!;
	private LootLayer _loot = null!;
	private ForageLayer _forage = null!;
	private PlaceableLayer _placeables = null!;
	private PlaceService _place = null!;
	private PlaceableBreakService _breakPlaceables = null!;
	private PlayerController _player = null!;
	private ChopService _chop = null!;
	private FishingService _fishing = null!;
	private SleepService _sleep = null!;
	private DeathService _death = null!;
	private PlayerInventory _inventory = null!;
	private GameClock _clock = null!;
	private PlayerEnergyState _energy = null!;
	private PlayerHealthState _health = null!;
	private EatService _eat = null!;
	private CraftingService _crafting = null!;
	private PlayerDiscoveries _discoveries = null!;
	private PlayerBank _bank = null!;
	private PlayerSkills _skills = null!;
	private GameHud _hud = null!;
	private PauseMenu _pause = null!;
	private DisplaySettings _displaySettings = null!;
	private InputSettings _inputSettings = null!;
	private PlayerAttributes _playerAttributes = null!;
	private IReadOnlyList<FishSpawnDef> _fishTable = null!;
	private DebugHitboxOverlay _debugOverlay = null!;
	private CanvasModulate _lighting = null!;
	private Node2D _worldSort = null!;
	private TileHoverOutline _tileHover = null!;
	private CabinBuilding? _cabin;
	private bool _wasInCabinDoor;
	private readonly MapStateStore _mapStore = new();
	private readonly DropStore _dropStore = new();
	private readonly ForageStore _forageStore = new();
	private IReadOnlyList<ForageDef> _forageTable = null!;
	private RotationGrid _rotations = new(0, 0);
	private string _mapId = "homestead";
	private string _saveFileName = "untitled.json";
	private string _saveTitle = "Untitled";
	private double _cooldown;
	private double _autoSaveMs;
	private double _cabinExitGraceMs;

	public override void _Ready()
	{
		YSortEnabled = false;

		_displaySettings = DisplaySettings.Load();
		_inputSettings = InputSettings.Load();
		_displaySettings.ApplyWindowMode();

		_catalog = TileCatalog.Load();
		_items = ItemCatalog.Load();
		_tileSet = WorldTileSetBuilder.Build(_catalog);
		_transitions = TransitionCatalog.Load();
		_inventory = new PlayerInventory(_items);
		_inventory.ApplyStarterLoadout();
		_clock = new GameClock();
		_energy = new PlayerEnergyState();
		_health = new PlayerHealthState();
		_discoveries = new PlayerDiscoveries(CollectionCatalog.Load());
		_crafting = new CraftingService(RecipeCatalog.Load(), _items);
		_bank = new PlayerBank();
		_skills = new PlayerSkills();
		_playerAttributes = new PlayerAttributes();
		_fishTable = FishCatalog.Load();
		_forageTable = ForageCatalog.Load();
		_inventory.ItemAcquired = OnItemAcquired;

		_ground = new TileMapLayer { Name = "Ground", TileSet = _tileSet, ZIndex = -10, Visible = false };
		AddChild(_ground);

		_tileHover = new TileHoverOutline { Name = "TileHover", ZIndex = 40 };
		AddChild(_tileHover);

		_tileVisuals = new TileVisualLayer { Name = "TileVisuals", ZIndex = -9 };
		AddChild(_tileVisuals);
		_tileVisuals.Configure(_catalog);

		// Player + trees + cabin: depth by foot Y. Placeables stay below (Java drawPlaceables first).
		_worldSort = new WorldSortRoot { Name = "WorldSort" };
		AddChild(_worldSort);

		_trees = new TallTreeLayer { Name = "Trees" };
		AddChild(_trees);
		_trees.SetSortParent(_worldSort);

		_overlays = new OverlayLayer { Name = "Overlays", ZIndex = -5 };
		AddChild(_overlays);
		_overlays.Configure(_catalog);

		_placeables = new PlaceableLayer { Name = "Placeables", ZIndex = -1 };
		AddChild(_placeables);
		_placeables.Configure(_items, id => _items.TryGetValue(id, out var def) ? def.MaxStack : 999);

		_loot = new LootLayer { Name = "Loot", ZIndex = 5 };
		AddChild(_loot);

		_forage = new ForageLayer { Name = "Forage", ZIndex = 2 };
		AddChild(_forage);

		_player = new PlayerController { Name = "Player" };
		_worldSort.AddChild(_player);
		_player.Configure(
			_ground,
			_catalog,
			IsBlockedByBuilding,
			() => _energy.MoveSpeedMultiplier,
			() => _fishing.IsBusy,
			IsMovementBlocked
		);
		_trees.Configure(() => (
			_player.GlobalPosition,
			GameConfig.PlayerHitboxWidth / 2f,
			GameConfig.PlayerHitboxHeight
		));

		_chop = new ChopService(_ground, _catalog, _trees, _overlays, _tileVisuals, _loot, _inventory, _energy, _skills, () => _mapId);
		_place = new PlaceService(_ground, _catalog, _items, _inventory, _placeables, () => _mapId);
		_breakPlaceables = new PlaceableBreakService(
			_ground,
			_inventory,
			_energy,
			_placeables,
			_loot,
			_overlays,
			_tileVisuals,
			() => _mapId,
			() =>
			{
				_hud.CloseChest();
				_hud.CloseChipper();
			}
		);
		_fishing = new FishingService(
			_ground,
			_catalog,
			_items,
			_inventory,
			_energy,
			_skills,
			_fishTable,
			() => _player,
			() => _hud.Refresh()
		);
		AddChild(_fishing);
		_sleep = new SleepService(_ground, _catalog, _clock, _energy, _placeables, () => _mapId, this, OnSlept);
		_death = new DeathService(
			_health,
			_energy,
			() => LoadMap(GameConfig.RespawnMap, GameConfig.RespawnBedCol, GameConfig.RespawnBedRow),
			() =>
			{
				_hud.ShowToast("You died — woke up in the cabin");
				Game.GameEvents.RaisePlayerDied();
				_hud.Refresh();
			}
		);

		_lighting = new CanvasModulate { Color = Colors.White };
		AddChild(_lighting);

		_hud = new GameHud();
		AddChild(_hud);
		_hud.Configure(
			_inventory,
			_items,
			_clock,
			_energy,
			_crafting,
			_discoveries,
			_bank,
			_skills,
			_health,
			_inputSettings,
			TryOpenHoveredPlaceable,
			_fishTable,
			SyncVitalsFromGear
		);
		_inventory.ItemsAdded = (itemId, qty) =>
		{
			_hud.NotifyItemsAdded(itemId, qty);
			Game.GameEvents.RaiseItemsPickedUp(itemId, qty);
		};
		_skills.XpGained = (_, amount) => _hud.Feed.NotifyXp(amount);
		_eat = new EatService(_inventory, _items, _energy, _health, this, () => _hud.Refresh());
		_crafting.SyncNotifiedUnlocks(_discoveries);
		_skills.LevelUp = (skill, level) =>
		{
			_hud.Feed.NotifyLevelUp(skill.DisplayName(), level);
			Game.GameEvents.RaiseSkillLevelUp(skill, level);
			_hud.Refresh();
		};

		_debugOverlay = new DebugHitboxOverlay { Name = "DebugHitboxes" };
		AddChild(_debugOverlay);
		_debugOverlay.Configure(
			_displaySettings,
			_player,
			_loot,
			_placeables,
			() => _mapId,
			() => _cabin
		);

		_pause = new PauseMenu();
		AddChild(_pause);
		_pause.Configure(
			_displaySettings,
			_inputSettings,
			onContinue: () => { },
			onSave: () =>
			{
				SaveGame(manual: true);
			},
			onSaveAndExit: () =>
			{
				SaveGame(manual: false);
				_pause.Close();
				GetTree().ChangeSceneToFile("res://scenes/Title.tscn");
			},
			onSettingsChanged: ApplyDisplaySettings
		);

		GetNodeOrNull<Label>("BootLabel")?.QueueFree();
		GetNodeOrNull<Camera2D>("Camera2D")?.QueueFree();

		ApplyDisplaySettings();

		_clock.DayAdvanced += OnDayAdvanced;
		_autoSaveMs = GameConfig.AutoSaveIntervalMs;

		if (GameLaunch.Pending == GameLaunch.Mode.NewGame)
		{
			_saveTitle = string.IsNullOrWhiteSpace(GameLaunch.SaveTitle)
				? "Untitled"
				: GameLaunch.SaveTitle;
			_saveFileName = string.IsNullOrWhiteSpace(GameLaunch.FileName)
				? SaveFileNames.ToFileName(_saveTitle)
				: GameLaunch.FileName;
			GameLaunch.Clear();
			BeginFreshGame(writeSave: true);
			_hud.ShowToast($"New game: {_saveTitle}  ·  F5=save  Esc=pause");
		}
		else if (GameLaunch.Pending == GameLaunch.Mode.Load)
		{
			_saveFileName = GameLaunch.FileName;
			GameLaunch.Clear();
			try
			{
				LoadGame(_saveFileName);
				_hud.ShowToast($"Loaded: {_saveTitle}  ·  F5=save  Esc=pause");
			}
			catch (Exception ex)
			{
				GD.PushError($"Load failed: {ex.Message}");
				GameLaunch.Clear();
				GetTree().ChangeSceneToFile("res://scenes/Title.tscn");
				return;
			}
		}
		else
		{
			// Opened Main directly (editor) — fall back to homestead without a named save.
			_saveTitle = "Dev";
			_saveFileName = "dev.json";
			BeginFreshGame(writeSave: false);
			_hud.ShowToast("Dev start — use Title scene for named saves");
		}

		_hud.Refresh();
	}

	public override void _UnhandledInput(InputEvent @event)
	{
		if (@event is InputEventKey { Pressed: true, Echo: false } key)
		{
			if (key.Keycode == Key.Escape && _eat.IsPromptOpen)
			{
				_eat.Cancel();
				GetViewport().SetInputAsHandled();
				return;
			}

			if (key.Keycode == Key.Escape && _fishing.IsBusy)
			{
				_fishing.Cancel();
				_hud.ShowToast("Fishing cancelled");
				GetViewport().SetInputAsHandled();
				return;
			}

			if (key.Keycode == Key.F5)
			{
				SaveGame(manual: true);
				GetViewport().SetInputAsHandled();
				return;
			}

			if (key.Keycode == Key.F11)
			{
				_displaySettings.ToggleFullscreen();
				_displaySettings.Save();
				GetViewport().SetInputAsHandled();
				return;
			}

			// F4 reloads the active named save from disk.
			if (key.Keycode == Key.F4)
			{
				if (SaveManager.Exists(_saveFileName))
				{
					try
					{
						LoadGame(_saveFileName);
						_hud.ShowToast($"Reloaded: {_saveTitle}");
					}
					catch (Exception ex)
					{
						_hud.ShowToast($"Load failed: {ex.Message}");
						GD.PushError(ex.ToString());
					}
				}
				else
				{
					_hud.ShowToast("No save file yet — press F5 first");
				}

				GetViewport().SetInputAsHandled();
				return;
			}

			// Esc opens pause menu (Continue / Settings / Save / Save and Exit).
			if (key.Keycode == Key.Escape)
			{
				if (_pause.IsOpen)
				{
					return;
				}

				if (_hud.IsPlayerMenuOpen || _hud.IsBackpackOpen || _hud.IsCraftOpen || _hud.IsSkillsOpen
					|| _hud.IsCollectionsOpen || _hud.IsChestOpen || _hud.IsChipperOpen)
				{
					return;
				}

				_pause.Open(_displaySettings, _inputSettings);
				GetViewport().SetInputAsHandled();
				return;
			}

			if (key.Keycode == Key.F6)
			{
				SaveGame(manual: false);
				GetTree().ChangeSceneToFile("res://scenes/Title.tscn");
				GetViewport().SetInputAsHandled();
				return;
			}

			// Temporary ABC for death until combat deals damage.
			if (key.Keycode == Key.F7)
			{
				_health.TakeDamage(_health.MaxHealth);
				_hud.Refresh();
				GetViewport().SetInputAsHandled();
				return;
			}

			if (!_pause.IsOpen && !_hud.IsPlayerMenuOpen && !_hud.IsChestOpen && !_hud.IsChipperOpen
				&& !_fishing.IsMinigameOpen && !_eat.IsPromptOpen)
			{
				if (key.Keycode is Key.Minus or Key.KpSubtract)
				{
					_displaySettings.SetZoomPercent(_displaySettings.ZoomPercent - 10);
					_displaySettings.Save();
					ApplyDisplaySettings();
					GetViewport().SetInputAsHandled();
					return;
				}

				if (key.Keycode is Key.Equal or Key.KpAdd)
				{
					_displaySettings.SetZoomPercent(_displaySettings.ZoomPercent + 10);
					_displaySettings.Save();
					ApplyDisplaySettings();
					GetViewport().SetInputAsHandled();
					return;
				}
			}
		}

		if (@event is InputEventMouseButton { Pressed: true, ButtonIndex: MouseButton.Right })
		{
			if (_cooldown > 0 || _pause.IsOpen || _hud.IsPlayerMenuOpen || _hud.IsBackpackOpen || _hud.IsCraftOpen
				|| _hud.IsSkillsOpen || _hud.IsCollectionsOpen || _hud.IsChestOpen || _hud.IsChipperOpen
				|| _hud.IsPointerOverUi || _fishing.BlocksWorldInput || _eat.IsPromptOpen)
			{
				return;
			}

			var placeWorld = _ground.GetGlobalMousePosition();
			if (_eat.TryBeginOnClick())
			{
				GetViewport().SetInputAsHandled();
				return;
			}

			if (TryCollectForageAt(placeWorld))
			{
				GetViewport().SetInputAsHandled();
				return;
			}

			if (_place.TryPlaceAt(_player, placeWorld, out var placeToast))
			{
				if (!string.IsNullOrEmpty(placeToast))
				{
					_hud.ShowToast(placeToast);
				}

				_hud.Refresh();
				GetViewport().SetInputAsHandled();
			}

			return;
		}

		if (@event is not InputEventMouseButton { Pressed: true, ButtonIndex: MouseButton.Left })
		{
			return;
		}

		// Hook bite even while rod-locked / fishing busy.
		if (_fishing.IsFlashing && _fishing.TryHookBite())
		{
			GetViewport().SetInputAsHandled();
			return;
		}

		if (_cooldown > 0 || _pause.IsOpen || _hud.IsPlayerMenuOpen || _hud.IsBackpackOpen || _hud.IsCraftOpen
			|| _hud.IsSkillsOpen || _hud.IsCollectionsOpen || _hud.IsChestOpen || _hud.IsChipperOpen
			|| _hud.IsPointerOverUi || _fishing.BlocksWorldInput || _eat.IsPromptOpen)
		{
			return;
		}

		var world = _ground.GetGlobalMousePosition();

		if (_breakPlaceables.TryBreakAt(_player, world, out var breakToast))
		{
			if (!string.IsNullOrEmpty(breakToast))
			{
				_hud.ShowToast(breakToast);
			}

			_hud.Refresh();
			GetViewport().SetInputAsHandled();
			return;
		}

		if (_inventory.GetHeldRod() != null && _fishing.TryStartCast(_player, world, out var castToast))
		{
			if (!string.IsNullOrEmpty(castToast))
			{
				_hud.ShowToast(castToast);
			}

			_hud.Refresh();
			GetViewport().SetInputAsHandled();
			return;
		}

		if (_chop.TryStartChop(_player, world, out var chopToast))
		{
			if (!string.IsNullOrEmpty(chopToast))
			{
				_hud.ShowToast(chopToast);
			}

			_hud.Refresh();
			GetViewport().SetInputAsHandled();
		}
	}

	/// <summary>Inventory key while hovering a chest/chipper opens it (Java PlaceableAccessService).</summary>
	private bool TryOpenHoveredPlaceable()
	{
		if (_cooldown > 0 || _pause.IsOpen || _fishing.BlocksWorldInput || _eat.IsPromptOpen)
		{
			return false;
		}

		var world = _ground.GetGlobalMousePosition();
		if (_place.TryOpenChest(_player, world, out var chest, out var chestToast))
		{
			if (chest != null)
			{
				_hud.OpenChest(chest);
			}

			if (!string.IsNullOrEmpty(chestToast))
			{
				_hud.ShowToast(chestToast);
			}

			_hud.Refresh();
			return true;
		}

		if (_place.TryOpenChipper(_player, world, out var chipper, out var chipToast))
		{
			if (chipper != null)
			{
				_hud.OpenChipper(chipper);
			}

			if (!string.IsNullOrEmpty(chipToast))
			{
				_hud.ShowToast(chipToast);
			}

			_hud.Refresh();
			return true;
		}

		return false;
	}

	public override void _PhysicsProcess(double delta)
	{
		var elapsedMs = delta * 1000.0;
		UpdateTileHover();

		// Esc pause uses GetTree().Paused. Soft-pause: inventory + chest (not chipper).
		var advanceWorld = ShouldAdvanceWorldTime();
		if (!advanceWorld && !_hud.IsChipperOpen)
		{
			return;
		}

		_clock.Update(elapsedMs);
		_chop.Regrowth.Update(_mapId, _ground, elapsedMs, _trees, _overlays, _tileVisuals);
		_placeables.UpdateAll(elapsedMs);
		UpdateLighting();

		_autoSaveMs -= elapsedMs;
		if (_autoSaveMs <= 0)
		{
			_autoSaveMs = GameConfig.AutoSaveIntervalMs;
			SaveGame(manual: false);
		}

		var inventoryDirty = false;
		_loot.TickAndCollect(
			_player.GlobalPosition,
			GameConfig.PlayerHitboxWidth / 2f,
			GameConfig.PlayerHitboxHeight,
			elapsedMs,
			_playerAttributes.PickupMagnetPixels,
			(itemId, qty) =>
			{
				_inventory.AddItem(itemId, qty);
				inventoryDirty = true;
			}
		);
		if (inventoryDirty)
		{
			DepositCurrencyFromInventory();
			_hud.Refresh();
		}

		var center = _player.GlobalPosition - new Vector2(0, GameConfig.PlayerHitboxHeight / 2f);
		_sleep.Update(center);
		_death.CheckAndRespawn();

		if (_cabinExitGraceMs > 0)
		{
			_cabinExitGraceMs -= elapsedMs;
		}

		if (_cooldown > 0)
		{
			_cooldown -= delta;
			return;
		}

		TryBuildingDoor();
		TryTileTransition();
	}

	/// <summary>
	/// World clock/regrowth/machines pause for inventory + chest (Java shouldAdvanceWorldTime).
	/// Chipper stays live so processing continues. Esc pause freezes the tree separately.
	/// </summary>
	private bool ShouldAdvanceWorldTime() =>
		!_hud.IsPlayerMenuOpen
		&& !_hud.IsChestOpen
		&& !_fishing.IsMinigameOpen
		&& !_eat.IsPromptOpen;

	private bool IsMovementBlocked() =>
		_hud.IsPlayerMenuOpen
		|| _hud.IsChestOpen
		|| _hud.IsChipperOpen
		|| _eat.IsPromptOpen
		|| _fishing.IsMinigameOpen;

	private void OnSlept(int skippedMinutes)
	{
		var skippedMs = skippedMinutes * (double)GameConfig.RealMsPerGameMinute;
		_chop.Regrowth.Update(_mapId, _ground, skippedMs, _trees, _overlays, _tileVisuals);
		_placeables.UpdateAll(skippedMs);
		_hud.Refresh();
		SaveGame(manual: false);
		_hud.ShowToast($"Slept (+{skippedMinutes}m) — game saved");
		Game.GameEvents.RaisePlayerSlept();
		GD.Print($"Slept — skipped {skippedMinutes} game minutes");
	}

	/// <summary>
	/// New day: keep existing forages, roll empty grass on every world map (1.5% each).
	/// Sleeping N days → N additive rolls so piles can build up until a seasonal reset.
	/// </summary>
	private void OnDayAdvanced()
	{
		if (!string.IsNullOrEmpty(_mapId) && _ground.GetUsedRect().Size != Vector2I.Zero)
		{
			_mapStore.CaptureFromLayer(_mapId, _ground, _rotations);
			_forageStore.SetMapForages(_mapId, _forage.Export());
		}

		EnsureWorldMapsCached();
		RollForagesAdditiveForAllMaps();
		_forage.Restore(_forageStore.TakeMapForages(_mapId));
		Game.GameEvents.RaiseDayAdvanced(_clock.Day);
		GD.Print($"Day {_clock.Day} — foragables rolled (additive)");
	}

	/// <summary>Load any <c>res://data/*.map</c> grids not yet in the session cache.</summary>
	private void EnsureWorldMapsCached()
	{
		foreach (var mapId in MapFileLoader.ListMapIds())
		{
			if (_mapStore.Has(mapId))
			{
				continue;
			}

			try
			{
				var grid = MapFileLoader.LoadGrid($"res://data/{mapId}.map");
				_mapStore.Put(mapId, grid.Width, grid.Height, grid.Tiles, grid.Rotations);
			}
			catch (Exception ex)
			{
				GD.PrintErr($"Failed to cache map '{mapId}' for forage rolls: {ex.Message}");
			}
		}
	}

	private void RollForagesAdditiveForAllMaps()
	{
		foreach (var (mapId, grid) in _mapStore.All)
		{
			var existing = _forageStore.PeekMapForages(mapId);
			_forageStore.SetMapForages(
				mapId,
				ForageSpawner.RollDayAdditive(grid, _forageTable, existing)
			);
		}
	}

	private void RestoreOrRollForages(string mapId)
	{
		_forage.Restore(
			_forageStore.Has(mapId)
				? _forageStore.TakeMapForages(mapId)
				: new List<ForageInstance>()
		);
	}

	private bool TryCollectForageAt(Vector2 world)
	{
		var cell = _ground.LocalToMap(_ground.ToLocal(world));
		if (!_forage.HasAt(cell.X, cell.Y))
		{
			return false;
		}

		var tileCenter = new Vector2(
			cell.X * GameConfig.TileSize + GameConfig.TileSize / 2f,
			cell.Y * GameConfig.TileSize + GameConfig.TileSize / 2f
		);
		var playerCenter = _player.GlobalPosition - new Vector2(0, GameConfig.PlayerHitboxHeight / 2f);
		if (playerCenter.DistanceTo(tileCenter) > GameConfig.ChopReachDistance)
		{
			_hud.ShowToast("Too far");
			return true;
		}

		if (!_forage.TryCollectAt(cell.X, cell.Y, out var itemId))
		{
			return false;
		}

		_inventory.AddItem(itemId, 1);
		DepositCurrencyFromInventory();
		_hud.Refresh();
		return true;
	}

	private void UpdateLighting()
	{
		_lighting.Color = _clock.Phase == DayPhase.Night
			? new Color(0.45f, 0.5f, 0.7f)
			: Colors.White;
	}

	private void UpdateTileHover()
	{
		if (_tileHover == null)
		{
			return;
		}

		if (!_displaySettings.ShowTileHover
			|| _pause.IsOpen
			|| _hud.IsPlayerMenuOpen
			|| _hud.IsChestOpen
			|| _hud.IsChipperOpen
			|| _hud.IsPointerOverUi
			|| _fishing.BlocksWorldInput
			|| _eat.IsPromptOpen)
		{
			_tileHover.Clear();
			return;
		}

		var used = _ground.GetUsedRect();
		if (used.Size == Vector2I.Zero)
		{
			_tileHover.Clear();
			return;
		}

		var cell = _ground.LocalToMap(_ground.ToLocal(_ground.GetGlobalMousePosition()));
		if (!used.HasPoint(cell))
		{
			_tileHover.Clear();
			return;
		}

		Vector2I? secondary = null;
		var held = _inventory.GetSelectedHotbarStack();
		if (!held.IsEmpty && held.ItemId == "bed")
		{
			var head = new Vector2I(cell.X, cell.Y - 1);
			if (used.HasPoint(head))
			{
				secondary = head;
			}
		}

		_tileHover.SetCells(cell, secondary);
	}

	private void LoadMap(string mapId, int spawnCol, int spawnRow)
	{
		SwitchToMap(mapId, setPlayer: true, spawnCol, spawnRow, applyTransitionCooldown: true);
	}

	private void SwitchToMap(
		string mapId,
		bool setPlayer,
		int spawnCol = 0,
		int spawnRow = 0,
		bool applyTransitionCooldown = true
	)
	{
		// Persist outgoing map + loot before leaving.
		if (!string.IsNullOrEmpty(_mapId) && _ground.GetUsedRect().Size != Vector2I.Zero)
		{
			_mapStore.CaptureFromLayer(_mapId, _ground, _rotations);
			_dropStore.SetMapDrops(_mapId, _loot.ExportSettled());
			_forageStore.SetMapForages(_mapId, _forage.Export());
		}

		_mapId = mapId;
		_chop.CancelPending();
		_breakPlaceables.CancelPending();
		_fishing.Cancel();
		_eat.Cancel();
		_player.CancelAllActions();
		_hud.CloseChest();
		_hud.CloseChipper();
		_cabin?.QueueFree();
		_cabin = null;
		_loot.Clear();
		_forage.Clear();

		if (_mapStore.Has(mapId))
		{
			_rotations = _mapStore.ApplyToLayer(mapId, _ground);
		}
		else
		{
			_rotations = MapFileLoader.ApplyToLayer(_ground, $"res://data/{mapId}.map");
			_mapStore.CaptureFromLayer(mapId, _ground, _rotations);
		}

		_tileVisuals.SetRotations(_rotations);
		_overlays.SetRotations(_rotations);
		_chop.Regrowth.ApplyStumpTiles(mapId, _ground, _overlays, _tileVisuals);
		_tileVisuals.Rebuild(_ground);
		_trees.Rebuild(_ground);
		_overlays.Rebuild(_ground);
		_placeables.SetActiveMap(mapId);

		_loot.RestoreSettled(_dropStore.TakeMapDrops(mapId));
		RestoreOrRollForages(mapId);

		if (mapId == "homestead")
		{
			_cabin = new CabinBuilding();
			_worldSort.AddChild(_cabin);
		}

		_player.SetCameraMapBounds(_ground.GetUsedRect());

		if (setPlayer)
		{
			_player.GlobalPosition = new Vector2(
				spawnCol * GameConfig.TileSize + GameConfig.TileSize / 2f,
				spawnRow * GameConfig.TileSize + GameConfig.TileSize - 1f
			);
			_player.ResetCameraZoom(_displaySettings.GetCameraZoom());
			_player.SnapCamera();
		}
		else
		{
			_player.SnapCamera();
		}

		_cooldown = applyTransitionCooldown ? TransitionCooldownSec : 0;
		_hud.Refresh();
		Game.GameEvents.RaiseMapEntered(mapId);
		GD.Print($"Map loaded: {mapId}");
	}

	private void SaveGame(bool manual)
	{
		_mapStore.CaptureFromLayer(_mapId, _ground, _rotations);
		_dropStore.SetMapDrops(_mapId, _loot.ExportSettled());
		_forageStore.SetMapForages(_mapId, _forage.Export());

		var dto = new SaveGameDto
		{
			Title = _saveTitle,
			MapId = _mapId,
			PlayerX = _player.GlobalPosition.X,
			PlayerY = _player.GlobalPosition.Y,
			Energy = _energy.CurrentEnergy,
			Health = _health.CurrentHealth,
			Day = _clock.Day,
			Hour = _clock.Hour,
			Minute = _clock.Minute,
			SelectedHotbar = _inventory.SelectedHotbarSlot,
			Inventory = _inventory.ExportSlots(),
			Regrowths = _chop.Regrowth.ExportAll(),
			PlacedObjects = _placeables.ExportPlaced(),
			WoodChippers = _placeables.ExportChippers(),
			ChestSlots = _placeables.ExportChests(),
			DiscoveredItems = _discoveries.Export(),
			WoodChips = _bank.WoodChips,
			PickupMagnetTiles = _playerAttributes.PickupMagnetTiles,
			Skills = _skills.Export(),
		};

		foreach (var (id, grid) in _mapStore.All)
		{
			dto.Maps.Add(new MapDto
			{
				MapId = id,
				Width = grid.Width,
				Height = grid.Height,
				Tiles = grid.Tiles.ToList(),
				Rotations = grid.Rotations.ToList(),
			});
		}

		foreach (var (mapId, drops) in _dropStore.All)
		{
			foreach (var d in drops)
			{
				dto.Drops.Add(new DropDto
				{
					MapId = mapId,
					ItemId = d.ItemId,
					Qty = d.Quantity,
					X = d.X,
					Y = d.Y,
				});
			}
		}

		foreach (var (mapId, forages) in _forageStore.All)
		{
			foreach (var f in forages)
			{
				dto.Forages.Add(new ForageDto
				{
					MapId = mapId,
					ItemId = f.ItemId,
					Col = f.Col,
					Row = f.Row,
					OffsetX = f.OffsetX,
					OffsetY = f.OffsetY,
					RotationDegrees = f.RotationDegrees,
				});
			}
		}

		SaveManager.Write(dto, _saveFileName);
		_autoSaveMs = GameConfig.AutoSaveIntervalMs;
		if (manual)
		{
			_hud.Feed.NotifyGameSaved();
		}

		GD.Print(manual ? $"Manual save OK ({_saveFileName})" : $"Autosave OK ({_saveFileName})");
	}

	private void BeginFreshGame(bool writeSave)
	{
		_fishing.Cancel();
		_chop.CancelPending();
		_breakPlaceables.CancelPending();
		_eat.Cancel();
		_player.CancelAllActions();
		_hud.CloseChest();
		_hud.CloseChipper();
		_cooldown = 0;
		GetTree().Paused = false;

		_mapStore.Clear();
		_dropStore.Clear();
		_forageStore.Clear();
		_chop.Regrowth.ClearAll();
		_placeables.ClearAll();
		SeedStartingCabinBedIfNeeded();

		_inventory.SuppressNotifications = true;
		_inventory.ApplyStarterLoadout();
		_inventory.SuppressNotifications = false;
		_clock.SetTime(day: 1, hour: 6, minute: 0);
		_energy.SetCurrentEnergy(_energy.MaxEnergy);
		_health.ResetToFull();
		_discoveries.Reset();
		_crafting.ResetNotifiedUnlocks();
		_crafting.SyncNotifiedUnlocks(_discoveries);
		_bank.Reset();
		_skills.Reset();
		_playerAttributes.Reset();

		_mapId = "";
		LoadMap(GameConfig.StartingMap, GameConfig.StartingSpawnCol, GameConfig.StartingSpawnRow);

		// Day-1 morning forages on the whole world (same roll every later day uses).
		if (!string.IsNullOrEmpty(_mapId) && _ground.GetUsedRect().Size != Vector2I.Zero)
		{
			_mapStore.CaptureFromLayer(_mapId, _ground, _rotations);
			_forageStore.SetMapForages(_mapId, _forage.Export());
		}

		EnsureWorldMapsCached();
		RollForagesAdditiveForAllMaps();
		_forage.Restore(_forageStore.TakeMapForages(_mapId));

		if (writeSave)
		{
			SaveGame(manual: false);
		}

		GD.Print($"Started fresh game — {_saveTitle} ({_saveFileName})");
	}

	private void LoadGame(string fileName)
	{
		var dto = SaveManager.Read(fileName) ?? throw new InvalidOperationException("Save missing");
		_saveFileName = fileName;
		_saveTitle = string.IsNullOrWhiteSpace(dto.Title) ? "Untitled" : dto.Title.Trim();
		_fishing.Cancel();
		_chop.CancelPending();
		_breakPlaceables.CancelPending();
		_eat.Cancel();
		_player.CancelAllActions();
		_cooldown = 0;
		GetTree().Paused = false;
		_hud.CloseChest();
		_hud.CloseChipper();

		_mapStore.Clear();
		_dropStore.Clear();
		_forageStore.Clear();
		_chop.Regrowth.ClearAll();
		_placeables.ClearAll();

		foreach (var map in dto.Maps)
		{
			_mapStore.Put(map.MapId, map.Width, map.Height, map.Tiles, map.Rotations);
		}

		_dropStore.PutAll(dto.Drops.Select(d => (d.MapId, d.ItemId, d.Qty, d.X, d.Y)));
		_forageStore.PutAll(
			(dto.Forages ?? new()).Select(f => (
				f.MapId,
				new ForageInstance(f.ItemId, f.Col, f.Row, f.OffsetX, f.OffsetY, f.RotationDegrees)
			))
		);
		_chop.Regrowth.ImportAll(dto.Regrowths);
		_placeables.ImportAll(
			dto.PlacedObjects ?? new(),
			dto.WoodChippers ?? new(),
			dto.ChestSlots ?? new()
		);
		SeedStartingCabinBedIfNeeded();

		_inventory.ImportSlots(dto.Inventory, dto.SelectedHotbar);
		_clock.SetTime(dto.Day, dto.Hour, dto.Minute);
		_energy.SetCurrentEnergy(dto.Energy < 0 ? _energy.MaxEnergy : dto.Energy);
		_health.SetCurrentHealth(dto.Health < 0 ? _health.MaxHealth : dto.Health);
		_discoveries.Import(dto.DiscoveredItems);
		_crafting.SyncNotifiedUnlocks(_discoveries);
		_bank.SetWoodChips(dto.WoodChips);
		_playerAttributes.SetPickupMagnetTiles(
			dto.PickupMagnetTiles > 0 ? dto.PickupMagnetTiles : GameConfig.DefaultPickupMagnetTiles
		);
		_skills.Import(dto.Skills);
		var strayChips = _inventory.RemoveAllOfItem(PlayerBank.WoodChipsId);
		if (strayChips > 0)
		{
			_bank.DepositWoodChips(strayChips);
		}

		_mapId = ""; // skip capture of outgoing
		SwitchToMap(dto.MapId, setPlayer: false, applyTransitionCooldown: false);
		_player.GlobalPosition = new Vector2(dto.PlayerX, dto.PlayerY);
		_player.ResetCameraZoom(_displaySettings.GetCameraZoom());
		_player.SnapCamera();
		_player.CancelAllActions();
		_player.ProcessMode = ProcessModeEnum.Inherit;
		CallDeferred(nameof(DeferredLoadUnlock));
		_hud.Refresh();
		UpdateLighting();
		_autoSaveMs = GameConfig.AutoSaveIntervalMs;
		GD.Print($"Loaded save {_saveTitle} on {dto.MapId} @ ({dto.PlayerX:0.##},{dto.PlayerY:0.##})");
	}

	private void ApplyDisplaySettings()
	{
		_player?.ApplyCameraZoom(_displaySettings.GetCameraZoom());
		_player?.SnapCamera();
		_hud?.Refresh();
	}

	/// <summary>Keep live HP cap aligned with held gear (EffectiveCombatStats).</summary>
	private void SyncVitalsFromGear()
	{
		var combat = EffectiveCombatStats.Resolve(_inventory, _items);
		_health.SetMaxHealth(combat.MaxHealth);
	}

	/// <summary>
	/// Java GameSession.seedStartingCabinBed — cabin bed tiles alone aren't breakable;
	/// a placeable bed is seeded on the foot tile so axe-break / move works.
	/// </summary>
	private void SeedStartingCabinBedIfNeeded()
	{
		if (_placeables.HasAt(GameConfig.RespawnMap, GameConfig.RespawnBedCol, GameConfig.RespawnBedRow))
		{
			return;
		}

		// After a break, tiles are cabin floor — don't respawn a free bed on load.
		if (_mapStore.Has(GameConfig.RespawnMap)
			&& _mapStore.GetTileId(GameConfig.RespawnMap, GameConfig.RespawnBedCol, GameConfig.RespawnBedRow)
				!= GameConfig.BedTileId)
		{
			return;
		}

		_placeables.Place(
			GameConfig.RespawnMap,
			BedPlaceable.ItemId,
			GameConfig.RespawnBedCol,
			GameConfig.RespawnBedRow
		);
	}

	private void OnItemAcquired(string itemId)
	{
		var newly = _discoveries.OnItemAcquired(itemId);
		if (newly.Count == 0)
		{
			return;
		}

		foreach (var name in newly)
		{
			_hud.Feed.NotifyCollection(name);
			Game.GameEvents.RaiseCollectionUnlocked(name);
		}

		var unlocked = _crafting.CheckForNewUnlocks(_discoveries);
		foreach (var name in unlocked)
		{
			_hud.Feed.NotifyRecipeUnlocked(name);
			Game.GameEvents.RaiseRecipeUnlocked(name);
		}

		_hud.Refresh();
	}

	private void DepositCurrencyFromInventory()
	{
		var chips = _inventory.RemoveAllOfItem(PlayerBank.WoodChipsId);
		if (chips > 0)
		{
			_bank.DepositWoodChips(chips);
		}
	}

	private void DeferredLoadUnlock()
	{
		GetTree().Paused = false;
		_fishing.Cancel();
		_player.CancelAllActions();
		_player.ProcessMode = ProcessModeEnum.Inherit;
		_cooldown = 0;
	}

	private void TryBuildingDoor()
	{
		if (_cabin == null || _mapId != "homestead")
		{
			_wasInCabinDoor = false;
			return;
		}

		// After exiting cabin, ignore door until player steps off then back on.
		if (_cabinExitGraceMs > 0)
		{
			_wasInCabinDoor = true;
			return;
		}

		var inDoor = _cabin.IsPlayerInDoor(_player.GlobalPosition);
		if (!inDoor)
		{
			_wasInCabinDoor = false;
			return;
		}

		// Edge trigger — standing in the door after load must not soft-lock / re-enter.
		if (_wasInCabinDoor)
		{
			return;
		}

		_wasInCabinDoor = true;
		LoadMap(CabinBuilding.TargetMapId, CabinBuilding.TargetSpawnCol, CabinBuilding.TargetSpawnRow);
	}

	private void TryTileTransition()
	{
		var center = _player.GlobalPosition - new Vector2(0, GameConfig.PlayerHitboxHeight / 2f);
		var cell = _ground.LocalToMap(_ground.ToLocal(center));
		foreach (var t in _transitions.Where(t => t.SourceMapId == _mapId && t.Col == cell.X && t.Row == cell.Y))
		{
			if (UsesInteriorExitDepth(_mapId) && !IsInteriorExitReady(t))
			{
				return;
			}

			if (_mapId == GameConfig.StartingMap)
			{
				_cabinExitGraceMs = 600;
				_wasInCabinDoor = true;
			}

			LoadMap(t.TargetMapId, t.SpawnCol, t.SpawnRow);
			return;
		}
	}

	private static bool UsesInteriorExitDepth(string mapId) =>
		mapId is GameConfig.StartingMap
			or GameConfig.SkeletonDungeonEntranceMap
			or GameConfig.CaveMap;

	private bool IsInteriorExitReady(MapTransition t)
	{
		// Walk most of the way down the exit tile before leaving (Java INTERIOR_EXIT_TRIGGER_DEPTH).
		var tileTop = t.Row * GameConfig.TileSize;
		var triggerY = tileTop + GameConfig.TileSize * 0.75;
		var centerY = _player.GlobalPosition.Y - GameConfig.PlayerHitboxHeight / 2f;
		return centerY >= triggerY;
	}

	private bool IsCabinExitReady(MapTransition t) => IsInteriorExitReady(t);

	private bool IsBlockedByBuilding(Vector2 feetCenter, float halfW, float hitH) =>
		(_cabin != null && _cabin.Blocks(feetCenter, halfW, hitH))
		|| _placeables.CollidesWith(_mapId, feetCenter, halfW, hitH);
}
