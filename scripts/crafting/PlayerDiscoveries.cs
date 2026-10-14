using System.Collections.Generic;
using System.Linq;
using LumberjackAttack.Collection;

namespace LumberjackAttack.Crafting;

/// <summary>
/// Tracks discovered collection entries (Java PlayerCollections).
/// Recipe unlocks use HasDiscoveredItem — true when a matching item entry is unlocked.
/// </summary>
public sealed class PlayerDiscoveries
{
	private readonly IReadOnlyList<CollectionEntryDef> _catalog;
	private readonly HashSet<string> _discoveredEntryIds = new();

	public PlayerDiscoveries(IReadOnlyList<CollectionEntryDef> catalog)
	{
		_catalog = catalog;
	}

	public IReadOnlyList<CollectionEntryDef> Catalog => _catalog;

	public void Reset() => _discoveredEntryIds.Clear();

	public bool IsEntryDiscovered(string entryId) =>
		!string.IsNullOrEmpty(entryId) && _discoveredEntryIds.Contains(entryId);

	/// <summary>True if any collection entry for this item id is discovered (recipe unlocks).</summary>
	public bool HasDiscovered(string itemId)
	{
		if (string.IsNullOrEmpty(itemId))
		{
			return false;
		}

		foreach (var entry in _catalog)
		{
			if (entry.DiscoveryType == DiscoveryType.Item
				&& entry.DiscoveryId == itemId
				&& _discoveredEntryIds.Contains(entry.EntryId))
			{
				return true;
			}
		}

		// Legacy saves stored raw item ids before collection entries existed.
		return _discoveredEntryIds.Contains(itemId);
	}

	/// <summary>Discovers collection entries for an acquired item. Returns newly unlocked display names.</summary>
	public List<string> OnItemAcquired(string itemId)
	{
		var newly = new List<string>();
		if (string.IsNullOrWhiteSpace(itemId))
		{
			return newly;
		}

		foreach (var entry in _catalog)
		{
			if (entry.DiscoveryType != DiscoveryType.Item || entry.DiscoveryId != itemId)
			{
				continue;
			}

			if (_discoveredEntryIds.Add(entry.EntryId))
			{
				newly.Add(entry.DisplayName);
			}
		}

		return newly;
	}

	/// <summary>Discovers collection entries for a defeated enemy.</summary>
	public List<string> OnEnemyDefeated(string enemyId)
	{
		var newly = new List<string>();
		if (string.IsNullOrWhiteSpace(enemyId))
		{
			return newly;
		}

		foreach (var entry in _catalog)
		{
			if (entry.DiscoveryType != DiscoveryType.Enemy || entry.DiscoveryId != enemyId)
			{
				continue;
			}

			if (_discoveredEntryIds.Add(entry.EntryId))
			{
				newly.Add(entry.DisplayName);
			}
		}

		return newly;
	}

	public int GetDiscoveredCount(CollectionCategory category) =>
		_catalog.Count(e => e.Category == category && _discoveredEntryIds.Contains(e.EntryId));

	public int GetTotalCount(CollectionCategory category) =>
		_catalog.Count(e => e.Category == category);

	public IEnumerable<CollectionEntryDef> GetEntries(CollectionCategory category) =>
		_catalog.Where(e => e.Category == category);

	public List<string> Export() => _discoveredEntryIds.OrderBy(x => x).ToList();

	public void Import(IEnumerable<string>? ids)
	{
		Reset();
		if (ids == null)
		{
			return;
		}

		foreach (var raw in ids)
		{
			if (string.IsNullOrWhiteSpace(raw))
			{
				continue;
			}

			var id = raw.Trim();
			if (_catalog.Any(e => e.EntryId == id))
			{
				_discoveredEntryIds.Add(id);
				continue;
			}

			// Migrate legacy item-id saves into matching collection entries.
			foreach (var entry in _catalog)
			{
				if (entry.DiscoveryType == DiscoveryType.Item && entry.DiscoveryId == id)
				{
					_discoveredEntryIds.Add(entry.EntryId);
				}
			}

			// Keep raw id so HasDiscovered still works if no catalog entry exists.
			_discoveredEntryIds.Add(id);
		}
	}
}
