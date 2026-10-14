namespace LumberjackAttack.World.Forage;

/// <summary>One grounded foragable on a map tile (no collision; RMB collect).</summary>
public readonly struct ForageInstance
{
	public string ItemId { get; }
	public int Col { get; }
	public int Row { get; }
	/// <summary>Pixel offset inside the tile (top-left of sprite AABB).</summary>
	public int OffsetX { get; }
	public int OffsetY { get; }
	/// <summary>0, 90, 180, or 270.</summary>
	public int RotationDegrees { get; }

	public ForageInstance(string itemId, int col, int row, int offsetX, int offsetY, int rotationDegrees)
	{
		ItemId = itemId;
		Col = col;
		Row = row;
		OffsetX = offsetX;
		OffsetY = offsetY;
		RotationDegrees = rotationDegrees;
	}
}
