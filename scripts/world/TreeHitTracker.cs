using System;
using System.Collections.Generic;

namespace LumberjackAttack.World;

/// <summary>Per-tree remaining toughness until the tree falls.</summary>
public sealed class TreeHitTracker
{
	private readonly Dictionary<string, int> _remaining = new();

	public int ApplyHit(string mapId, int col, int row, int maxToughness, int treeDamage)
	{
		var key = Key(mapId, col, row);
		if (!_remaining.TryGetValue(key, out var remaining))
		{
			remaining = Math.Max(1, maxToughness);
		}

		remaining = Math.Max(0, remaining - Math.Max(1, treeDamage));
		if (remaining <= 0)
		{
			_remaining.Remove(key);
			return 0;
		}

		_remaining[key] = remaining;
		return remaining;
	}

	public void Clear(string mapId, int col, int row) => _remaining.Remove(Key(mapId, col, row));

	public void ClearMap(string mapId)
	{
		var prefix = mapId + ":";
		var remove = new List<string>();
		foreach (var key in _remaining.Keys)
		{
			if (key.StartsWith(prefix, StringComparison.Ordinal))
			{
				remove.Add(key);
			}
		}

		foreach (var key in remove)
		{
			_remaining.Remove(key);
		}
	}

	private static string Key(string mapId, int col, int row) => $"{mapId}:{col}:{row}";
}
