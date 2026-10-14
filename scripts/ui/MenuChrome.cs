using Godot;

namespace LumberjackAttack.Ui;

/// <summary>
/// Wood-plank menu chrome built from menu_bg (16×16) + menu_corner/edge (8×8) 9-slice.
/// Padding border is always half the background tile (8 / 16, or 16 / 32 when using x2).
/// </summary>
public static class MenuChrome
{
	private const string AtlasPath = "res://assets/sprites/ui/menu_panel_9slice_x2.png";
	private const string AtlasPathSmall = "res://assets/sprites/ui/menu_panel_9slice.png";

	/// <summary>Border thickness in the x2 atlas (8px padding × 2).</summary>
	private const int BorderX2 = 16;

	/// <summary>Border thickness in the 1× atlas (matches 8px padding art).</summary>
	private const int BorderX1 = 8;

	private static StyleBoxTexture? _panel;
	private static StyleBoxTexture? _button;
	private static StyleBoxTexture? _buttonHover;
	private static StyleBoxTexture? _buttonActive;
	private static StyleBoxTexture? _tab;
	private static StyleBoxTexture? _tabActive;

	public static StyleBoxTexture PanelStyle() =>
		_panel ??= MakeStyle(AtlasPath, BorderX2, content: 18, drawCenter: true);

	public static StyleBoxTexture ButtonStyle() =>
		_button ??= MakeStyle(AtlasPath, BorderX2, content: 12, drawCenter: true);

	public static StyleBoxTexture ButtonHoverStyle() =>
		_buttonHover ??= MakeStyle(AtlasPath, BorderX2, content: 12, drawCenter: true, modulate: new Color(1.12f, 1.08f, 1.0f));

	public static StyleBoxTexture ButtonActiveStyle() =>
		_buttonActive ??= MakeStyle(AtlasPath, BorderX2, content: 12, drawCenter: true, modulate: new Color(1.2f, 1.15f, 0.85f));

	public static StyleBoxTexture TabStyle() =>
		_tab ??= MakeStyle(AtlasPathSmall, BorderX1, content: 8, drawCenter: true);

	public static StyleBoxTexture TabActiveStyle() =>
		_tabActive ??= MakeStyle(AtlasPathSmall, BorderX1, content: 8, drawCenter: true, modulate: new Color(1.25f, 1.18f, 0.9f));

	public static void ApplyPanel(PanelContainer panel)
	{
		panel.AddThemeStyleboxOverride("panel", PanelStyle());
	}

	public static void ApplyFramedButton(Button button, bool active = false)
	{
		var normal = active ? ButtonActiveStyle() : ButtonStyle();
		button.AddThemeStyleboxOverride("normal", normal);
		button.AddThemeStyleboxOverride("hover", ButtonHoverStyle());
		button.AddThemeStyleboxOverride("pressed", ButtonActiveStyle());
		button.AddThemeStyleboxOverride("disabled", ButtonStyle());
		button.AddThemeStyleboxOverride("focus", normal);
	}

	public static void ApplyTabButton(Button button, bool active)
	{
		var normal = active ? TabActiveStyle() : TabStyle();
		button.AddThemeStyleboxOverride("normal", normal);
		button.AddThemeStyleboxOverride("hover", active ? TabActiveStyle() : TabStyle());
		button.AddThemeStyleboxOverride("pressed", TabActiveStyle());
		button.AddThemeStyleboxOverride("focus", normal);
		button.AddThemeColorOverride("font_color", active ? Colors.White : new Color(0.88f, 0.88f, 0.9f));
	}

	/// <summary>Apply wood panel to AcceptDialog / ConfirmationDialog chrome.</summary>
	public static void ApplyDialog(Window dialog)
	{
		dialog.AddThemeStyleboxOverride("panel", PanelStyle());
		dialog.AddThemeStyleboxOverride("embedded_border", PanelStyle());
		Callable.From(() => StyleDialogButtons(dialog)).CallDeferred();
		if (dialog is AcceptDialog accept)
		{
			accept.AboutToPopup += () => StyleDialogButtons(dialog);
		}
	}

	private static void StyleDialogButtons(Window dialog)
	{
		foreach (var child in dialog.FindChildren("*", "Button", recursive: true))
		{
			if (child is Button btn)
			{
				ApplyFramedButton(btn);
			}
		}
	}

	private static StyleBoxTexture MakeStyle(
		string atlasPath,
		int border,
		int content,
		bool drawCenter,
		Color? modulate = null
	)
	{
		var tex = GD.Load<Texture2D>(atlasPath);
		var style = new StyleBoxTexture
		{
			Texture = tex,
			DrawCenter = drawCenter,
			AxisStretchHorizontal = StyleBoxTexture.AxisStretchMode.Tile,
			AxisStretchVertical = StyleBoxTexture.AxisStretchMode.Tile,
			TextureMarginLeft = border,
			TextureMarginTop = border,
			TextureMarginRight = border,
			TextureMarginBottom = border,
			ContentMarginLeft = content,
			ContentMarginTop = content,
			ContentMarginRight = content,
			ContentMarginBottom = content,
		};
		if (modulate is { } m)
		{
			style.ModulateColor = m;
		}

		return style;
	}
}
