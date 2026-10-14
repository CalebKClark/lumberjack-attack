using System.Collections.Generic;
using Godot;
using LumberjackAttack.Save;

namespace LumberjackAttack.World;

public sealed class TreeRegrowthManager
{
	private readonly Dictionary<string, List<PendingRegrowth>> _byMap = new();

	public void Schedule(string mapId, int col, int row, int tileId, double regrowthMs)
	{
		if (!_byMap.TryGetValue(mapId, out var list))
		{
			list = new List<PendingRegrowth>();
			_byMap[mapId] = list;
		}

		list.Add(new PendingRegrowth(col, row, tileId, regrowthMs));
	}

	public void ClearAll() => _byMap.Clear();

	public void ClearMap(string mapId) => _byMap.Remove(mapId);

	public List<RegrowthDto> ExportAll()
	{
		var result = new List<RegrowthDto>();
		foreach (var (mapId, list) in _byMap)
		{
			foreach (var p in list)
			{
				result.Add(new RegrowthDto
				{
					MapId = mapId,
					Col = p.Col,
					Row = p.Row,
					TileId = p.TileId,
					RemainingMs = p.RemainingMs,
				});
			}
		}

		return result;
	}

	public void ImportAll(IEnumerable<RegrowthDto> entries)
	{
		_byMap.Clear();
		foreach (var e in entries)
		{
			Schedule(e.MapId, e.Col, e.Row, e.TileId, e.RemainingMs);
		}
	}

	/// <summary>Ensure regenerating spots show as stumps (Java applyStumpTiles).</summary>
	public void ApplyStumpTiles(string mapId, TileMapLayer ground, OverlayLayer? overlays, TileVisualLayer? visuals)
	{
		if (!_byMap.TryGetValue(mapId, out var list))
		{
			return;
		}

		foreach (var p in list)
		{
			var cell = new Vector2I(p.Col, p.Row);
			var stumpId = TallTreeSprites.StumpTileIdForTree(p.TileId);
			TileRotation.SetCell(ground, cell, stumpId);
			overlays?.RefreshCell(ground, cell);
			visuals?.RefreshCell(ground, cell);
		}
	}

	public void Update(
		string mapId,
		TileMapLayer ground,
		double elapsedMs,
		TallTreeLayer? trees,
		OverlayLayer? overlays,
		TileVisualLayer? visuals
	)
	{
		if (!_byMap.TryGetValue(mapId, out var list) || list.Count == 0)
		{
			return;
		}

		for (var i = list.Count - 1; i >= 0; i--)
		{
			var pending = list[i];
			pending.RemainingMs -= elapsedMs;
			list[i] = pending;
			if (pending.RemainingMs > 0)
			{
				continue;
			}

			var cell = new Vector2I(pending.Col, pending.Row);
			var current = ground.GetCellSourceId(cell);
			var stumpId = TallTreeSprites.StumpTileIdForTree(pending.TileId);
			if (current == stumpId || current == 0 || TallTreeSprites.IsTreeStump(current))
			{
				TileRotation.SetCell(ground, cell, pending.TileId);
				trees?.RefreshCell(ground, cell);
				overlays?.RefreshCell(ground, cell);
				visuals?.RefreshCell(ground, cell);
			}

			list.RemoveAt(i);
		}
	}

	private struct PendingRegrowth
	{
		public int Col;
		public int Row;
		public int TileId;
		public double RemainingMs;

		public PendingRegrowth(int col, int row, int tileId, double remainingMs)
		{
			Col = col;
			Row = row;
			TileId = tileId;
			RemainingMs = remainingMs;
		}
	}
}
