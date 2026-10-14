using Godot;

namespace LumberjackAttack.World;

/// <summary>
/// Depth-sorts children by foot Y via ZIndex (reliable for CharacterBody2D + Node2D mix).
/// Matches Java drawYSorted: lower on screen (higher Y) draws on top.
/// </summary>
public partial class WorldSortRoot : Node2D
{
	public override void _Ready()
	{
		YSortEnabled = false;
		ZIndex = 0;
	}

	public override void _Process(double delta)
	{
		foreach (var child in GetChildren())
		{
			if (child is Node2D node)
			{
				node.ZIndex = (int)node.GlobalPosition.Y;
			}
		}
	}
}
