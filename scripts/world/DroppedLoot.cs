using System;
using Godot;
using LumberjackAttack.Core;

namespace LumberjackAttack.World;

/// <summary>Ground loot with canopy rain → scatter → magnet pickup (simplified Java DroppedItem).</summary>
public partial class DroppedLoot : Node2D
{
	private enum Phase
	{
		Delay,
		Fall,
		Scatter,
		Settled,
	}

	private Sprite2D _sprite = null!;
	private Phase _phase;
	private string _itemId = "";
	private int _quantity = 1;

	private Vector2 _pos;
	private Vector2 _fallFrom;
	private Vector2 _fallTo;
	private Vector2 _scatterFrom;
	private Vector2 _scatterTo;
	private double _appearDelayMs;
	private double _appearElapsedMs;
	private double _fallDurationMs;
	private double _fallElapsedMs;
	private double _scatterDurationMs;
	private double _scatterElapsedMs;
	private float _bounceOffsetY;

	public string ItemId => _itemId;
	public int Quantity => _quantity;
	public bool IsSettled => _phase == Phase.Settled;
	public Vector2 Center => _pos;

	public static DroppedLoot Settled(string itemId, int quantity, float centerX, float centerY)
	{
		var drop = new DroppedLoot();
		drop._itemId = itemId;
		drop._quantity = quantity;
		drop._pos = new Vector2(centerX, centerY);
		drop._fallFrom = drop._pos;
		drop._fallTo = drop._pos;
		drop._scatterFrom = drop._pos;
		drop._scatterTo = drop._pos;
		drop._phase = Phase.Settled;
		drop._bounceOffsetY = 0;
		return drop;
	}

	/// <summary>Bounce-scatter from a point (Java DroppedItem scatter=true).</summary>
	public static DroppedLoot ScatterFrom(string itemId, int quantity, float centerX, float centerY)
	{
		var drop = new DroppedLoot();
		drop._itemId = itemId;
		drop._quantity = Math.Max(1, quantity);
		drop._pos = new Vector2(centerX, centerY);
		drop._fallFrom = drop._pos;
		drop._fallTo = drop._pos;
		drop.BeginScatter();
		return drop;
	}

	public static DroppedLoot CanopyRain(
		string itemId,
		float startCenterX,
		float startCenterY,
		float landCenterX,
		float landCenterY,
		double appearDelayMs,
		double fallDurationMs
	)
	{
		var drop = new DroppedLoot();
		drop.SetupRain(itemId, 1, startCenterX, startCenterY, landCenterX, landCenterY, appearDelayMs, fallDurationMs);
		return drop;
	}

	public override void _Ready()
	{
		_sprite = new Sprite2D
		{
			Centered = true,
			TextureFilter = CanvasItem.TextureFilterEnum.Nearest,
			Texture = GD.Load<Texture2D>($"res://assets/sprites/items/{_itemId}.png"),
			Scale = new Vector2(
				GameConfig.DropItemSize / 16f,
				GameConfig.DropItemSize / 16f
			),
		};
		AddChild(_sprite);
		Visible = _phase != Phase.Delay;
		SyncVisual();
	}

	public void Tick(double elapsedMs)
	{
		if (_phase == Phase.Delay)
		{
			_appearElapsedMs += elapsedMs;
			if (_appearElapsedMs < _appearDelayMs)
			{
				return;
			}

			_phase = Phase.Fall;
			Visible = true;
		}

		if (_phase == Phase.Fall)
		{
			_fallElapsedMs += elapsedMs;
			var t = Math.Min(1.0, _fallElapsedMs / Math.Max(1.0, _fallDurationMs));
			var ease = t * t;
			_pos = _fallFrom.Lerp(_fallTo, (float)ease);
			_bounceOffsetY = 0;
			SyncVisual();
			if (t >= 1.0)
			{
				_pos = _fallTo;
				BeginScatter();
			}

			return;
		}

		if (_phase == Phase.Scatter)
		{
			_scatterElapsedMs += elapsedMs;
			var t = _scatterDurationMs <= 0 ? 1.0 : Math.Min(1.0, _scatterElapsedMs / _scatterDurationMs);
			var ease = 1.0 - Math.Pow(1.0 - t, 3.0);
			_pos = _scatterFrom.Lerp(_scatterTo, (float)ease);
			var bounceWave = Math.Sin(t * Math.PI * GameConfig.DropBounceCount);
			_bounceOffsetY = (float)(Math.Abs(bounceWave) * GameConfig.DropBounceHeightPx * (1.0 - t));
			if (t >= 1.0)
			{
				_phase = Phase.Settled;
				_pos = _scatterTo;
				_bounceOffsetY = 0;
			}

			SyncVisual();
		}
	}

	/// <returns>True when close enough to collect.</returns>
	public bool UpdateMagnet(
		Vector2 playerFeetCenter,
		float playerHalfW,
		float playerHitH,
		double elapsedMs,
		float magnetRadiusPx
	)
	{
		if (_phase != Phase.Settled)
		{
			return false;
		}

		var magnetRadius = magnetRadiusPx;
		var dropHalf = GameConfig.DropItemSize / 2f;
		var playerLeft = playerFeetCenter.X - playerHalfW;
		var playerTop = playerFeetCenter.Y - playerHitH;
		var dropLeft = _pos.X - dropHalf;
		var dropTop = _pos.Y - dropHalf;

		var gap = HitboxGap(
			playerLeft, playerTop, playerHalfW * 2f, playerHitH,
			dropLeft, dropTop, GameConfig.DropItemSize, GameConfig.DropItemSize
		);
		if (gap > magnetRadius)
		{
			return false;
		}

		var target = new Vector2(playerFeetCenter.X, playerFeetCenter.Y - playerHitH / 2f);
		var toPlayer = target - _pos;
		var dist = toPlayer.Length();
		if (dist > 0.001f)
		{
			var step = (float)(GameConfig.DropMagnetSpeedPxPerSec * elapsedMs / 1000.0);
			_pos += toPlayer / dist * Math.Min(step, dist);
			SyncVisual();
		}

		gap = HitboxGap(
			playerLeft, playerTop, playerHalfW * 2f, playerHitH,
			_pos.X - dropHalf, _pos.Y - dropHalf, GameConfig.DropItemSize, GameConfig.DropItemSize
		);
		return gap <= GameConfig.DropCollectDistancePx;
	}

	private void SetupRain(
		string itemId,
		int quantity,
		float startCenterX,
		float startCenterY,
		float landCenterX,
		float landCenterY,
		double appearDelayMs,
		double fallDurationMs
	)
	{
		_itemId = itemId;
		_quantity = quantity;
		_pos = new Vector2(startCenterX, startCenterY);
		_fallFrom = _pos;
		_fallTo = new Vector2(landCenterX, landCenterY);
		_appearDelayMs = Math.Max(0, appearDelayMs);
		_fallDurationMs = Math.Max(1, fallDurationMs);
		_phase = _appearDelayMs > 0 ? Phase.Delay : Phase.Fall;
	}

	private void BeginScatter()
	{
		_scatterFrom = _pos;
		var angle = Random.Shared.NextDouble() * Math.PI * 2.0;
		var distTiles = GameConfig.DropScatterMinTiles
			+ Random.Shared.NextDouble() * (GameConfig.DropScatterMaxTiles - GameConfig.DropScatterMinTiles);
		var dist = (float)(distTiles * GameConfig.TileSize);
		_scatterTo = _scatterFrom + new Vector2((float)Math.Cos(angle) * dist, (float)Math.Sin(angle) * dist);
		_scatterDurationMs = GameConfig.DropScatterDurationMs;
		_scatterElapsedMs = 0;
		_phase = Phase.Scatter;
	}

	private void SyncVisual()
	{
		Position = new Vector2(_pos.X, _pos.Y - _bounceOffsetY);
	}

	private static double HitboxGap(
		float ax, float ay, float aw, float ah,
		float bx, float by, float bw, float bh
	)
	{
		var dx = Math.Max(0, Math.Max(ax - (bx + bw), bx - (ax + aw)));
		var dy = Math.Max(0, Math.Max(ay - (by + bh), by - (ay + ah)));
		return Math.Sqrt(dx * dx + dy * dy);
	}
}
