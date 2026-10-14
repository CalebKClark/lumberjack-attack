namespace LumberjackAttack.Settings;

/// <summary>Bindable actions shown in Settings (Java GameAction).</summary>
public enum GameAction
{
	Hotbar1,
	Hotbar2,
	Hotbar3,
	Hotbar4,
	Hotbar5,
	Hotbar6,
	Hotbar7,
	Hotbar8,
	Hotbar9,
	Inventory,
}

public static class GameActionExtensions
{
	public static string Label(this GameAction action) => action switch
	{
		GameAction.Hotbar1 => "Hotbar Slot 1",
		GameAction.Hotbar2 => "Hotbar Slot 2",
		GameAction.Hotbar3 => "Hotbar Slot 3",
		GameAction.Hotbar4 => "Hotbar Slot 4",
		GameAction.Hotbar5 => "Hotbar Slot 5",
		GameAction.Hotbar6 => "Hotbar Slot 6",
		GameAction.Hotbar7 => "Hotbar Slot 7",
		GameAction.Hotbar8 => "Hotbar Slot 8",
		GameAction.Hotbar9 => "Hotbar Slot 9",
		GameAction.Inventory => "Inventory",
		_ => action.ToString(),
	};

	public static int HotbarIndex(this GameAction action) => action switch
	{
		GameAction.Hotbar1 => 0,
		GameAction.Hotbar2 => 1,
		GameAction.Hotbar3 => 2,
		GameAction.Hotbar4 => 3,
		GameAction.Hotbar5 => 4,
		GameAction.Hotbar6 => 5,
		GameAction.Hotbar7 => 6,
		GameAction.Hotbar8 => 7,
		GameAction.Hotbar9 => 8,
		_ => -1,
	};

	public static Godot.Key DefaultKey(this GameAction action) => action switch
	{
		GameAction.Hotbar1 => Godot.Key.Key1,
		GameAction.Hotbar2 => Godot.Key.Key2,
		GameAction.Hotbar3 => Godot.Key.Key3,
		GameAction.Hotbar4 => Godot.Key.Key4,
		GameAction.Hotbar5 => Godot.Key.Key5,
		GameAction.Hotbar6 => Godot.Key.Key6,
		GameAction.Hotbar7 => Godot.Key.Key7,
		GameAction.Hotbar8 => Godot.Key.Key8,
		GameAction.Hotbar9 => Godot.Key.Key9,
		GameAction.Inventory => Godot.Key.E,
		_ => Godot.Key.None,
	};
}
