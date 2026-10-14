using System;
using System.Collections.Generic;
using Godot;
using LumberjackAttack.Core;
using LumberjackAttack.World;

namespace LumberjackAttack.Entities;

/// <summary>
/// Top-down WASD mover with feet hitbox, solid-tile collision, walk cycles, and axe swings.
/// </summary>
public partial class PlayerController : CharacterBody2D
{
	private static readonly int[] WalkSequenceSide = { 0, 1, 0, 2 };
	private static readonly int[] WalkSequenceVertical = { 0, 1, 2 };

	private TileMapLayer? _ground;
	private IReadOnlyDictionary<int, TileDef>? _catalog;
	private Func<Vector2, float, float, bool>? _buildingBlocker;
	private Sprite2D _sprite = null!;
	private Camera2D _camera = null!;
	private string _facingKey = "down";
	private double _animMs;
	private int _animIndex;
	private bool _axeSwinging;
	private double _axeMs;
	private int _axeFrame;
	private Action<bool>? _axeCallback;
	private Func<double>? _speedMultiplier;
	private Func<bool>? _keepRodLock;
	private Func<bool>? _movementBlocked;
	private bool _rodCasting;
	private bool _rodHolding;
	private double _rodMs;
	private int _rodFrame;
	private Action? _rodHoldCallback;

	public bool IsActionLocked => _axeSwinging || _rodCasting || _rodHolding;

	public void Configure(
		TileMapLayer ground,
		IReadOnlyDictionary<int, TileDef> catalog,
		Func<Vector2, float, float, bool>? buildingBlocker = null,
		Func<double>? speedMultiplier = null,
		Func<bool>? keepRodLock = null,
		Func<bool>? movementBlocked = null
	)
	{
		_ground = ground;
		_catalog = catalog;
		_buildingBlocker = buildingBlocker;
		_speedMultiplier = speedMultiplier;
		_keepRodLock = keepRodLock;
		_movementBlocked = movementBlocked;
	}

	public void ResetCameraZoom(float? zoomOverride = null)
	{
		var z = zoomOverride ?? GameConfig.CameraZoom;
		_camera.Zoom = new Vector2(z, z);
	}

	public void ApplyCameraZoom(float zoom)
	{
		_camera.Zoom = new Vector2(zoom, zoom);
	}

	/// <summary>
	/// Clamp the camera to map pixel bounds (Java Camera.centerOnClamped / Stardew-style).
	/// If the map is smaller than the view, Godot centers on the bounds automatically.
	/// </summary>
	public void SetCameraMapBounds(Rect2I usedCells)
	{
		if (usedCells.Size == Vector2I.Zero)
		{
			_camera.LimitEnabled = false;
			return;
		}

		var left = usedCells.Position.X * GameConfig.TileSize;
		var top = usedCells.Position.Y * GameConfig.TileSize;
		var right = (usedCells.Position.X + usedCells.Size.X) * GameConfig.TileSize;
		var bottom = (usedCells.Position.Y + usedCells.Size.Y) * GameConfig.TileSize;

		_camera.LimitLeft = left;
		_camera.LimitTop = top;
		_camera.LimitRight = right;
		_camera.LimitBottom = bottom;
		_camera.LimitEnabled = true;
		_camera.LimitSmoothed = true;
	}

	/// <summary>Hard-snap camera after map teleports so smoothing doesn't pan across the old world.</summary>
	public void SnapCamera()
	{
		_camera.ResetSmoothing();
		_camera.ForceUpdateScroll();
	}

	public void FaceToward(Vector2 worldTarget)
	{
		var center = GlobalPosition - new Vector2(0, GameConfig.PlayerHitboxHeight / 2f);
		var delta = worldTarget - center;
		if (Mathf.Abs(delta.Y) >= Mathf.Abs(delta.X))
		{
			_facingKey = delta.Y < 0 ? "up" : "down";
		}
		else
		{
			_facingKey = delta.X < 0 ? "left" : "right";
		}
	}

	public void TriggerAxeSwing(Action<bool> onCompleted)
	{
		if (IsActionLocked)
		{
			onCompleted(false);
			return;
		}

		_axeSwinging = true;
		_axeMs = 0;
		_axeFrame = 0;
		_axeCallback = onCompleted;
		_animMs = 0;
		_animIndex = 0;
		ApplyAxeFrame();
	}

	public void TriggerRodCast(Action onHoldReached)
	{
		if (IsActionLocked)
		{
			return;
		}

		_rodCasting = true;
		_rodHolding = false;
		_rodMs = 0;
		_rodFrame = 0;
		_rodHoldCallback = onHoldReached;
		ApplyRodFrame();
	}

	public void EndRodPose()
	{
		_rodCasting = false;
		_rodHolding = false;
		_rodFrame = 0;
		_rodMs = 0;
		_rodHoldCallback = null;
		SetIdleTexture();
	}

	/// <summary>Clear axe/rod locks (needed after F9 load mid-swing / mid-cast).</summary>
	public void CancelAllActions()
	{
		if (_axeSwinging)
		{
			var cb = _axeCallback;
			_axeCallback = null;
			_axeSwinging = false;
			_axeFrame = 0;
			_axeMs = 0;
			cb?.Invoke(false);
		}

		EndRodPose();
	}

	public override void _Ready()
	{
		CollisionLayer = 0;
		CollisionMask = 0;
		YSortEnabled = false;

		var shape = new CollisionShape2D
		{
			Position = new Vector2(0, -GameConfig.PlayerHitboxHeight / 2f),
		};
		shape.Shape = new RectangleShape2D
		{
			Size = new Vector2(GameConfig.PlayerHitboxWidth, GameConfig.PlayerHitboxHeight),
		};
		AddChild(shape);

		_sprite = new Sprite2D
		{
			Centered = true,
			Position = new Vector2(0, -GameConfig.PlayerSpriteHeight / 2f),
			TextureFilter = CanvasItem.TextureFilterEnum.Nearest,
		};
		AddChild(_sprite);
		SetIdleTexture();

		_camera = new Camera2D
		{
			PositionSmoothingEnabled = true,
			PositionSmoothingSpeed = 10f,
			LimitSmoothed = true,
			Zoom = new Vector2(GameConfig.CameraZoom, GameConfig.CameraZoom),
		};
		AddChild(_camera);
		_camera.MakeCurrent();
	}

	public override void _PhysicsProcess(double delta)
	{
		if (_ground == null || _catalog == null)
		{
			return;
		}

		if (_axeSwinging)
		{
			AdvanceAxeAnim(delta);
			return;
		}

		if (_rodCasting)
		{
			AdvanceRodCast(delta);
			return;
		}

		if (_movementBlocked?.Invoke() == true)
		{
			Velocity = Vector2.Zero;
			if (_rodHolding)
			{
				ApplyRodFrame();
			}

			return;
		}

		var wasd = Vector2.Zero;
		if (Input.IsKeyPressed(Key.A) || Input.IsKeyPressed(Key.Left)) wasd.X -= 1;
		if (Input.IsKeyPressed(Key.D) || Input.IsKeyPressed(Key.Right)) wasd.X += 1;
		if (Input.IsKeyPressed(Key.W) || Input.IsKeyPressed(Key.Up)) wasd.Y -= 1;
		if (Input.IsKeyPressed(Key.S) || Input.IsKeyPressed(Key.Down)) wasd.Y += 1;

		// Walking cancels a stuck fishing pose — unless fishing still owns the line/minigame.
		if (_rodHolding && wasd != Vector2.Zero)
		{
			if (_keepRodLock?.Invoke() == true)
			{
				ApplyRodFrame();
				return;
			}

			EndRodPose();
		}

		if (_rodHolding && wasd == Vector2.Zero)
		{
			ApplyRodFrame();
			return;
		}

		if (wasd != Vector2.Zero)
		{
			var input = wasd.Normalized();
			UpdateFacing(input);
			var mult = (float)(_speedMultiplier?.Invoke() ?? 1.0);
			var distance = GameConfig.PlayerSpeedPixelsPerSecond * mult * (float)delta;
			TryMove(new Vector2(input.X * distance, 0));
			TryMove(new Vector2(0, input.Y * distance));
			AdvanceWalkAnim(delta);
		}
		else
		{
			_animMs = 0;
			_animIndex = 0;
			SetIdleTexture();
		}
	}

	private void AdvanceRodCast(double delta)
	{
		_rodMs += delta * 1000.0;
		if (_rodMs < GameConfig.PlayerRodFrameMs)
		{
			return;
		}

		_rodMs = 0;
		_rodFrame++;
		if (_rodFrame >= GameConfig.PlayerRodFrames)
		{
			_rodCasting = false;
			_rodHolding = true;
			_rodFrame = GameConfig.PlayerRodFrames - 1;
			ApplyRodFrame();
			var cb = _rodHoldCallback;
			_rodHoldCallback = null;
			cb?.Invoke();
			return;
		}

		ApplyRodFrame();
	}

	private void ApplyRodFrame()
	{
		var frame = Math.Clamp(_rodFrame, 0, GameConfig.PlayerRodFrames - 1);
		_sprite.Texture = GD.Load<Texture2D>($"res://assets/sprites/player/rod_{_facingKey}_{frame}.png");
		var topPad = GameConfig.PlayerAxeTopPadPx;
		var bodyH = GameConfig.PlayerSpriteHeight + topPad;
		_sprite.Position = new Vector2(0, -bodyH / 2f + topPad / 2f);
		if (_facingKey is "left" or "right")
		{
			var side = GameConfig.PlayerAxeSideExtendPx / 2f;
			_sprite.Position = new Vector2(_facingKey == "left" ? -side : side, _sprite.Position.Y);
		}
	}

	private void AdvanceAxeAnim(double delta)
	{
		_axeMs += delta * 1000.0;
		if (_axeMs >= GameConfig.PlayerAxeFrameMs)
		{
			_axeMs = 0;
			_axeFrame++;
			if (_axeFrame >= GameConfig.PlayerAxeFrames)
			{
				FinishAxeSwing(true);
				return;
			}

			ApplyAxeFrame();
		}
	}

	private void FinishAxeSwing(bool completed)
	{
		_axeSwinging = false;
		_axeFrame = 0;
		_axeMs = 0;
		SetIdleTexture();
		var cb = _axeCallback;
		_axeCallback = null;
		cb?.Invoke(completed);
	}

	private void ApplyAxeFrame()
	{
		_sprite.Texture = GD.Load<Texture2D>($"res://assets/sprites/player/axe_{_facingKey}_{_axeFrame}.png");
		// Axe sheets include top pad (and side reach on left/right).
		var topPad = GameConfig.PlayerAxeTopPadPx;
		var bodyH = GameConfig.PlayerSpriteHeight + topPad;
		_sprite.Position = new Vector2(0, -bodyH / 2f + topPad / 2f);
		if (_facingKey is "left" or "right")
		{
			var side = GameConfig.PlayerAxeSideExtendPx / 2f;
			_sprite.Position = new Vector2(_facingKey == "left" ? -side : side, _sprite.Position.Y);
		}
	}

	private void UpdateFacing(Vector2 input)
	{
		if (Mathf.Abs(input.X) > Mathf.Abs(input.Y))
		{
			_facingKey = input.X < 0 ? "left" : "right";
		}
		else
		{
			_facingKey = input.Y < 0 ? "up" : "down";
		}
	}

	private void AdvanceWalkAnim(double delta)
	{
		_animMs += delta * 1000.0;
		if (_animMs >= GameConfig.PlayerWalkFrameMs)
		{
			_animMs = 0;
			var sequence = _facingKey is "left" or "right" ? WalkSequenceSide : WalkSequenceVertical;
			_animIndex = (_animIndex + 1) % sequence.Length;
		}

		ApplyWalkFrame();
	}

	private void ApplyWalkFrame()
	{
		var sequence = _facingKey is "left" or "right" ? WalkSequenceSide : WalkSequenceVertical;
		var frame = sequence[_animIndex];
		_sprite.Texture = GD.Load<Texture2D>($"res://assets/sprites/player/walk_{_facingKey}_{frame}.png");
		_sprite.Position = new Vector2(0, -GameConfig.PlayerSpriteHeight / 2f);
	}

	private void SetIdleTexture()
	{
		_sprite.Texture = GD.Load<Texture2D>($"res://assets/sprites/player/idle_{_facingKey}_0.png");
		_sprite.Position = new Vector2(0, -GameConfig.PlayerSpriteHeight / 2f);
	}

	private void TryMove(Vector2 delta)
	{
		if (delta == Vector2.Zero)
		{
			return;
		}

		var next = GlobalPosition + delta;
		if (!HitsSolid(next))
		{
			GlobalPosition = next;
		}
	}

	private bool HitsSolid(Vector2 feetCenter)
	{
		var halfW = GameConfig.PlayerHitboxWidth / 2f;
		var height = GameConfig.PlayerHitboxHeight;
		if (_buildingBlocker != null && _buildingBlocker(feetCenter, halfW, height))
		{
			return true;
		}

		var corners = new[]
		{
			feetCenter + new Vector2(-halfW + 0.1f, -height + 0.1f),
			feetCenter + new Vector2(halfW - 0.1f, -height + 0.1f),
			feetCenter + new Vector2(-halfW + 0.1f, -0.1f),
			feetCenter + new Vector2(halfW - 0.1f, -0.1f),
		};

		foreach (var corner in corners)
		{
			if (IsSolidWorldPoint(corner))
			{
				return true;
			}
		}

		return false;
	}

	private bool IsSolidWorldPoint(Vector2 world)
	{
		var local = _ground!.ToLocal(world);
		var cell = _ground.LocalToMap(local);
		var sourceId = _ground.GetCellSourceId(cell);
		if (sourceId < 0)
		{
			// Outside the painted map = solid. Inside holes (e.g. grass id 0 failed to apply) = walkable.
			var used = _ground.GetUsedRect();
			return !used.HasArea() || !used.HasPoint(cell);
		}

		return _catalog!.TryGetValue(sourceId, out var def) && def.Solid;
	}
}
