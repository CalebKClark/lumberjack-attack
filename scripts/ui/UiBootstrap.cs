using Godot;

namespace LumberjackAttack.Ui;

/// <summary>Installs the game UI theme (custom pixel font) on the root viewport.</summary>
public partial class UiBootstrap : Node
{
	public override void _EnterTree()
	{
		var theme = GameFont.GetTheme();
		var root = GetTree()?.Root;
		if (root != null)
		{
			root.Theme = theme;
		}
	}

	public override void _Ready()
	{
		// Ensure late after scene change still inherits; re-assert on root.
		var root = GetTree()?.Root;
		if (root != null)
		{
			root.Theme = GameFont.GetTheme();
		}
	}
}
