using Godot;
using LumberjackAttack.Core;
using LumberjackAttack.Entities;
using LumberjackAttack.Settings;
using LumberjackAttack.World;
using LumberjackAttack.World.Placeable;

namespace LumberjackAttack.Ui;

/// <summary>Draws collision/debug hitboxes when DisplaySettings.DebugEnabled (Java GdxEntityRenderer).</summary>
public partial class DebugHitboxOverlay : Node2D
{
	private static readonly Color PlayerColor = new(0.2f, 1f, 0.35f, 0.45f);
	private static readonly Color DropColor = new(1f, 0.9f, 0.2f, 0.45f);
	private static readonly Color PlaceableColor = new(0.3f, 0.7f, 1f, 0.45f);
	private static readonly Color BuildingColor = new(1f, 0.45f, 0.1f, 0.4f);
	private static readonly Color DoorColor = new(0.8f, 0.2f, 1f, 0.4f);
	private static readonly Color EdgeColor = new(1f, 1f, 1f, 0.85f);
	private const float Edge = 1f;

	private DisplaySettings _display = null!;
	private PlayerController _player = null!;
	private LootLayer _loot = null!;
	private PlaceableLayer _placeables = null!;
	private System.Func<string> _mapId = null!;
	private System.Func<CabinBuilding?> _cabin = null!;

	public void Configure(
		DisplaySettings display,
		PlayerController player,
		LootLayer loot,
		PlaceableLayer placeables,
		System.Func<string> mapId,
		System.Func<CabinBuilding?> cabin
	)
	{
		_display = display;
		_player = player;
		_loot = loot;
		_placeables = placeables;
		_mapId = mapId;
		_cabin = cabin;
		ZIndex = 100;
	}

	public override void _Process(double delta)
	{
		QueueRedraw();
	}

	public override void _Draw()
	{
		if (_display is not { DebugEnabled: true } || _player == null)
		{
			return;
		}

		var halfW = GameConfig.PlayerHitboxWidth / 2f;
		var hitH = GameConfig.PlayerHitboxHeight;
		DrawHitbox(
			_player.GlobalPosition.X - halfW,
			_player.GlobalPosition.Y - hitH,
			GameConfig.PlayerHitboxWidth,
			hitH,
			PlayerColor
		);

		foreach (var child in _loot.GetChildren())
		{
			if (child is not DroppedLoot drop)
			{
				continue;
			}

			var size = GameConfig.DropItemSize;
			DrawHitbox(drop.Center.X - size / 2f, drop.Center.Y - size / 2f, size, size, DropColor);
		}

		foreach (var o in _placeables.ExportPlaced())
		{
			if (o.MapId != _mapId())
			{
				continue;
			}

			if (BedPlaceable.IsBed(o.ItemId))
			{
				DrawHitbox(
					o.Col * GameConfig.TileSize,
					BedPlaceable.HeadRow(o.Row) * GameConfig.TileSize,
					BedPlaceable.SpriteWidth,
					BedPlaceable.SpriteHeight,
					PlaceableColor
				);
			}
			else
			{
				var pad = (GameConfig.TileSize - GameConfig.PlaceableHitboxSize) / 2f;
				DrawHitbox(
					o.Col * GameConfig.TileSize + pad,
					o.Row * GameConfig.TileSize + pad,
					GameConfig.PlaceableHitboxSize,
					GameConfig.PlaceableHitboxSize,
					PlaceableColor
				);
			}
		}

		var cabin = _cabin();
		if (cabin != null && _mapId() == "homestead")
		{
			DrawHitboxRect(cabin.BaseCollisionRect, BuildingColor);
			DrawHitboxRect(cabin.DoorRect, DoorColor);
		}
	}

	private void DrawHitboxRect(Rect2 rect, Color color) =>
		DrawHitbox(rect.Position.X, rect.Position.Y, rect.Size.X, rect.Size.Y, color);

	private void DrawHitbox(float x, float y, float w, float h, Color fill)
	{
		DrawRect(new Rect2(x, y, w, h), fill);
		DrawRect(new Rect2(x, y, w, Edge), EdgeColor);
		DrawRect(new Rect2(x, y + h - Edge, w, Edge), EdgeColor);
		DrawRect(new Rect2(x, y, Edge, h), EdgeColor);
		DrawRect(new Rect2(x + w - Edge, y, Edge, h), EdgeColor);
	}
}
