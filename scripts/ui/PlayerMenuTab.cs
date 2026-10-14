namespace LumberjackAttack.Ui;

/// <summary>Tabs in the player menu opened with Inventory (E) — Java PlayerMenuTab.</summary>
public enum PlayerMenuTab
{
	Inventory,
	Skills,
	Crafting,
	Stats,
	Collections,
}

public static class PlayerMenuTabExtensions
{
	public static string Label(this PlayerMenuTab tab) => tab switch
	{
		PlayerMenuTab.Inventory => "Inventory",
		PlayerMenuTab.Skills => "Skills",
		PlayerMenuTab.Crafting => "Crafting",
		PlayerMenuTab.Stats => "Stats & Misc",
		PlayerMenuTab.Collections => "Collections",
		_ => tab.ToString(),
	};
}
