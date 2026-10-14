using Godot;
using LumberjackAttack.Core;

namespace LumberjackAttack.World;

/// <summary>Homestead cabin visual + door trigger (ported from Java BuildingManager).</summary>
public partial class CabinBuilding : Node2D
{
	public const string TargetMapId = "cabin_interior";
	public const int TargetSpawnCol = 21;
	public const int TargetSpawnRow = 22;

	private Rect2 _doorRect;
	private Rect2 _baseRect;
	private float _doorGapLeft;
	private float _doorGapRight;

	public override void _Ready()
	{
		const int footprintCol = 14;
		const int footprintBottomRow = 10;
		const int spriteW = 80;
		const int spriteH = 96;

		var worldX = footprintCol * GameConfig.TileSize;
		var sortY = (footprintBottomRow + 1) * GameConfig.TileSize;
		var worldY = sortY - spriteH;

		// Node Y = building foot so WorldSort depth-matches player / trees.
		Position = new Vector2(worldX, sortY);

		var sprite = new Sprite2D
		{
			Texture = GD.Load<Texture2D>("res://assets/sprites/buildings/cabin.png"),
			Centered = false,
			TextureFilter = CanvasItem.TextureFilterEnum.Nearest,
			Position = new Vector2(0, -spriteH),
		};
		AddChild(sprite);

		var baseHeight = 2 * GameConfig.TileSize;
		var baseY = worldY + spriteH - baseHeight;
		_baseRect = new Rect2(worldX, baseY, spriteW, baseHeight);

		var doorGapW = GameConfig.TileSize;
		var doorGapX = worldX + (spriteW - doorGapW) / 2f;
		_doorGapLeft = doorGapX;
		_doorGapRight = doorGapX + doorGapW;
		_doorRect = new Rect2(doorGapX, worldY + spriteH - 24, doorGapW, 36);
	}

	public bool IsPlayerInDoor(Vector2 feetCenter) => _doorRect.HasPoint(feetCenter);

	public Rect2 BaseCollisionRect => _baseRect;
	public Rect2 DoorRect => _doorRect;

	public bool Blocks(Vector2 feetCenter, float halfW, float hitH)
	{
		var box = new Rect2(feetCenter.X - halfW, feetCenter.Y - hitH, halfW * 2f, hitH);
		if (!_baseRect.Intersects(box))
		{
			return false;
		}

		// Door gap is walkable (same idea as Java).
		var midX = feetCenter.X;
		if (midX >= _doorGapLeft && midX <= _doorGapRight)
		{
			return false;
		}

		return true;
	}
}
