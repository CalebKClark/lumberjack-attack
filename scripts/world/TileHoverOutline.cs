using Godot;
using LumberjackAttack.Core;

namespace LumberjackAttack.World;

/// <summary>Red tile outline under the cursor (Java TILE_HOVER_OUTLINE).</summary>
public partial class TileHoverOutline : Node2D
{
	private static readonly Color OutlineColor = new(1f, 0.15f, 0.15f, 0.55f);
	private const float Edge = 1f;

	private Vector2I? _primary;
	private Vector2I? _secondary;

	public void Clear()
	{
		if (_primary == null && _secondary == null)
		{
			return;
		}

		_primary = null;
		_secondary = null;
		QueueRedraw();
	}

	public void SetCells(Vector2I? primary, Vector2I? secondary = null)
	{
		if (_primary == primary && _secondary == secondary)
		{
			return;
		}

		_primary = primary;
		_secondary = secondary;
		QueueRedraw();
	}

	public override void _Draw()
	{
		if (_primary is { } a)
		{
			DrawCellOutline(a);
		}

		if (_secondary is { } b)
		{
			DrawCellOutline(b);
		}
	}

	private void DrawCellOutline(Vector2I cell)
	{
		var x = cell.X * GameConfig.TileSize;
		var y = cell.Y * GameConfig.TileSize;
		var size = GameConfig.TileSize;
		DrawRect(new Rect2(x, y, size, Edge), OutlineColor);
		DrawRect(new Rect2(x, y + size - Edge, size, Edge), OutlineColor);
		DrawRect(new Rect2(x, y, Edge, size), OutlineColor);
		DrawRect(new Rect2(x + size - Edge, y, Edge, size), OutlineColor);
	}
}
