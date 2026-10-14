using System.Collections.Generic;

namespace LumberjackAttack.Save;

public sealed class SaveGameDto
{
	public int Version { get; set; } = 1;
	public string Title { get; set; } = "Untitled";
	public long SavedAtUnixMs { get; set; }
	public string MapId { get; set; } = "homestead";
	public float PlayerX { get; set; }
	public float PlayerY { get; set; }
	public double Energy { get; set; }
	public double Health { get; set; } = -1;
	public int Day { get; set; } = 1;
	public int Hour { get; set; }
	public int Minute { get; set; }
	public int SelectedHotbar { get; set; }
	public List<InventorySlotDto> Inventory { get; set; } = new();
	public List<MapDto> Maps { get; set; } = new();
	public List<DropDto> Drops { get; set; } = new();
	public List<ForageDto> Forages { get; set; } = new();
	public List<RegrowthDto> Regrowths { get; set; } = new();
	public List<PlacedObjectDto> PlacedObjects { get; set; } = new();
	public List<WoodChipperDto> WoodChippers { get; set; } = new();
	public List<ChestSlotDto> ChestSlots { get; set; } = new();
	public List<string> DiscoveredItems { get; set; } = new();
	public long WoodChips { get; set; }
	/// <summary>Pickup magnet radius in tiles. 0 or missing means use default on load.</summary>
	public double PickupMagnetTiles { get; set; }
	public List<SkillDto> Skills { get; set; } = new();
}

public sealed class SkillDto
{
	public string SkillId { get; set; } = "";
	public int TotalXp { get; set; }
}

public sealed class ChestSlotDto
{
	public string MapId { get; set; } = "";
	public int Col { get; set; }
	public int Row { get; set; }
	public int Slot { get; set; }
	public string ItemId { get; set; } = "";
	public int Qty { get; set; }
}

public sealed class PlacedObjectDto
{
	public string MapId { get; set; } = "";
	public string ItemId { get; set; } = "";
	public int Col { get; set; }
	public int Row { get; set; }
}

public sealed class WoodChipperDto
{
	public string MapId { get; set; } = "";
	public int Col { get; set; }
	public int Row { get; set; }
	public string InputItemId { get; set; } = "";
	public int InputQty { get; set; }
	public string OutputItemId { get; set; } = "";
	public int OutputQty { get; set; }
	public double ProgressMs { get; set; }
	public bool Processing { get; set; }
}

public sealed class InventorySlotDto
{
	public int Slot { get; set; }
	public string ItemId { get; set; } = "";
	public int Qty { get; set; }
}

public sealed class MapDto
{
	public string MapId { get; set; } = "";
	public int Width { get; set; }
	public int Height { get; set; }
	public List<int> Tiles { get; set; } = new();
	public List<int> Rotations { get; set; } = new();
}

public sealed class DropDto
{
	public string MapId { get; set; } = "";
	public string ItemId { get; set; } = "";
	public int Qty { get; set; }
	public float X { get; set; }
	public float Y { get; set; }
}

public sealed class ForageDto
{
	public string MapId { get; set; } = "";
	public string ItemId { get; set; } = "";
	public int Col { get; set; }
	public int Row { get; set; }
	public int OffsetX { get; set; }
	public int OffsetY { get; set; }
	public int RotationDegrees { get; set; }
}

public sealed class RegrowthDto
{
	public string MapId { get; set; } = "";
	public int Col { get; set; }
	public int Row { get; set; }
	public int TileId { get; set; }
	public double RemainingMs { get; set; }
}
