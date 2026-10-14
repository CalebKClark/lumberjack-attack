using System;
using Godot;

namespace LumberjackAttack.World;

/// <summary>
/// Java map rotation quarters (0–3 clockwise). Stored beside the TileMap — not as
/// alternative tile ids (those broke some cells / cabin door collision).
/// </summary>
public static class TileRotation
{
	public static float Degrees(int rotationQuarters) => (rotationQuarters & 3) * 90f;

	public static void SetCell(TileMapLayer layer, Vector2I cell, int sourceId)
	{
		if (sourceId < 0 || layer.TileSet == null || !layer.TileSet.HasSource(sourceId))
		{
			return;
		}

		layer.SetCell(cell, sourceId, Vector2I.Zero);
	}
}

/// <summary>Per-map rotation grid matching tile dimensions.</summary>
public sealed class RotationGrid
{
	public int Width { get; }
	public int Height { get; }
	public int[] Quarters { get; }

	public RotationGrid(int width, int height, int[]? quarters = null)
	{
		Width = width;
		Height = height;
		Quarters = quarters ?? new int[width * height];
	}

	public int Get(int col, int row)
	{
		if (col < 0 || row < 0 || col >= Width || row >= Height)
		{
			return 0;
		}

		return Quarters[row * Width + col] & 3;
	}

	public void Set(int col, int row, int quarters)
	{
		if (col < 0 || row < 0 || col >= Width || row >= Height)
		{
			return;
		}

		Quarters[row * Width + col] = quarters & 3;
	}

	public RotationGrid Clone()
	{
		var copy = new int[Quarters.Length];
		Array.Copy(Quarters, copy, Quarters.Length);
		return new RotationGrid(Width, Height, copy);
	}
}
