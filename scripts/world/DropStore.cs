using System.Collections.Generic;

namespace LumberjackAttack.World;

/// <summary>Ground loot kept per map while the player is elsewhere.</summary>
public sealed class DropStore
{
	private readonly Dictionary<string, List<StoredDrop>> _byMap = new();

	public void SetMapDrops(string mapId, List<StoredDrop> drops) =>
		_byMap[mapId] = drops;

	public List<StoredDrop> TakeMapDrops(string mapId)
	{
		if (_byMap.TryGetValue(mapId, out var list))
		{
			_byMap.Remove(mapId);
			return list;
		}

		return new List<StoredDrop>();
	}

	public void Clear() => _byMap.Clear();

	public IEnumerable<KeyValuePair<string, List<StoredDrop>>> All => _byMap;

	public void PutAll(IEnumerable<(string MapId, string ItemId, int Qty, float X, float Y)> drops)
	{
		_byMap.Clear();
		foreach (var d in drops)
		{
			if (!_byMap.TryGetValue(d.MapId, out var list))
			{
				list = new List<StoredDrop>();
				_byMap[d.MapId] = list;
			}

			list.Add(new StoredDrop(d.ItemId, d.Qty, d.X, d.Y));
		}
	}
}

public readonly struct StoredDrop
{
	public string ItemId { get; }
	public int Quantity { get; }
	public float X { get; }
	public float Y { get; }

	public StoredDrop(string itemId, int quantity, float x, float y)
	{
		ItemId = itemId;
		Quantity = quantity;
		X = x;
		Y = y;
	}
}
