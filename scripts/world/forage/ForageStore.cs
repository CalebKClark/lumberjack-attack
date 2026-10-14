using System.Collections.Generic;

namespace LumberjackAttack.World.Forage;

/// <summary>Foragables kept per map while the player is elsewhere.</summary>
public sealed class ForageStore
{
	private readonly Dictionary<string, List<ForageInstance>> _byMap = new();

	public bool Has(string mapId) => _byMap.ContainsKey(mapId);

	public bool HasAny => _byMap.Count > 0;

	public void SetMapForages(string mapId, List<ForageInstance> forages) =>
		_byMap[mapId] = forages;

	public List<ForageInstance> TakeMapForages(string mapId)
	{
		if (_byMap.TryGetValue(mapId, out var list))
		{
			_byMap.Remove(mapId);
			return list;
		}

		return new List<ForageInstance>();
	}

	/// <summary>Copy of stored forages without removing them from the store.</summary>
	public List<ForageInstance> PeekMapForages(string mapId) =>
		_byMap.TryGetValue(mapId, out var list)
			? new List<ForageInstance>(list)
			: new List<ForageInstance>();

	public void Clear() => _byMap.Clear();

	public IEnumerable<KeyValuePair<string, List<ForageInstance>>> All => _byMap;

	public void PutAll(IEnumerable<(string MapId, ForageInstance Forage)> entries)
	{
		_byMap.Clear();
		foreach (var (mapId, forage) in entries)
		{
			if (!_byMap.TryGetValue(mapId, out var list))
			{
				list = new List<ForageInstance>();
				_byMap[mapId] = list;
			}

			list.Add(forage);
		}
	}
}
