using Godot;
using Godot.Collections;

namespace LumberjackAttack.Ui;

/// <summary>
/// Project UI fonts (SIL Open Font License — free for published games).
/// <list type="bullet">
/// <item><b>Body</b> — Nunito: rounded, readable menus / HUD</item>
/// <item><b>Display</b> — Alfa Slab One: chunky titles &amp; big declarations</item>
/// <item><b>Tooltip</b> — Source Sans 3: compact item tips</item>
/// </list>
/// Custom pixel font kept at <c>res://assets/fonts/custom_pixel/</c> for later.
/// </summary>
public static class GameFont
{
	public const string BodyFontPath = "res://assets/fonts/ui/Nunito-Variable.ttf";
	public const string DisplayFontPath = "res://assets/fonts/ui/AlfaSlabOne-Regular.ttf";
	public const string TooltipFontPath = "res://assets/fonts/ui/SourceSans3-Variable.ttf";

	/// <summary>Archived custom bitmap font (not active).</summary>
	public const string CustomPixelFontPath = "res://assets/fonts/custom_pixel/lumberjack_font.fnt";

	public const int BaseFontSize = 14;
	public const int TitleFontSize = 36;
	public const int SectionFontSize = 22;
	public const int TooltipNameSize = 15;
	public const int TooltipBodySize = 12;

	private static Font? _body;
	private static Font? _display;
	private static Font? _tooltip;
	private static Theme? _theme;

	public static Font GetFont() => GetBodyFont();

	public static Font GetBodyFont() => _body ??= LoadFont(BodyFontPath);

	public static Font GetDisplayFont() => _display ??= LoadFont(DisplayFontPath);

	public static Font GetTooltipFont() => _tooltip ??= LoadFont(TooltipFontPath);

	public static Theme GetTheme()
	{
		if (_theme != null)
		{
			return _theme;
		}

		var body = GetBodyFont();
		var display = GetDisplayFont();
		var theme = new Theme
		{
			DefaultFont = body,
			DefaultFontSize = BaseFontSize,
		};

		var text = Colors.White;
		var muted = new Color(0.88f, 0.9f, 0.93f);
		var outline = Colors.Black;
		const int outlineSize = 2;

		string[] types =
		[
			"Label",
			"Button",
			"LinkButton",
			"LineEdit",
			"TextEdit",
			"CodeEdit",
			"RichTextLabel",
			"CheckBox",
			"CheckButton",
			"OptionButton",
			"PopupMenu",
			"MenuBar",
			"TabBar",
			"TabContainer",
			"ItemList",
			"Tree",
			"Window",
			"AcceptDialog",
			"ConfirmationDialog",
			"FileDialog",
			"TooltipLabel",
		];

		foreach (var type in types)
		{
			theme.SetFont("font", type, body);
			theme.SetFontSize("font_size", type, BaseFontSize);
			theme.SetColor("font_color", type, text);
			theme.SetColor("font_outline_color", type, outline);
			theme.SetConstant("outline_size", type, outlineSize);
		}

		string[] buttonTypes = ["Button", "CheckBox", "CheckButton", "OptionButton", "LinkButton"];
		foreach (var type in buttonTypes)
		{
			theme.SetColor("font_hover_color", type, text);
			theme.SetColor("font_pressed_color", type, muted);
			theme.SetColor("font_disabled_color", type, new Color(0.55f, 0.55f, 0.58f));
			theme.SetColor("font_focus_color", type, text);
		}

		theme.SetFont("title_font", "Window", display);
		theme.SetFontSize("title_font_size", "Window", SectionFontSize);
		theme.SetColor("title_color", "Window", text);
		theme.SetFont("normal_font", "RichTextLabel", body);
		theme.SetFont("bold_font", "RichTextLabel", body);
		theme.SetFont("italics_font", "RichTextLabel", body);
		theme.SetFont("bold_italics_font", "RichTextLabel", body);
		theme.SetFont("mono_font", "RichTextLabel", body);

		_theme = theme;
		return _theme;
	}

	public static void Apply(Control control) => control.Theme = GetTheme();

	public static void Apply(Window window) => window.Theme = GetTheme();

	public static void ApplyToCanvasLayer(CanvasLayer layer)
	{
		var theme = GetTheme();
		layer.ChildEnteredTree += node =>
		{
			if (node is Control control && control.GetParent() == layer)
			{
				control.Theme ??= theme;
			}
		};

		foreach (var child in layer.GetChildren())
		{
			if (child is Control control)
			{
				control.Theme ??= theme;
			}
		}
	}

	/// <summary>Chunky display face for titles and big callouts.</summary>
	public static void ApplyDisplay(Label label, int fontSize = TitleFontSize)
	{
		label.AddThemeFontOverride("font", GetDisplayFont());
		label.AddThemeFontSizeOverride("font_size", fontSize);
	}

	/// <summary>Compact face for item / world tooltips.</summary>
	public static void ApplyTooltip(Label label, int fontSize)
	{
		label.AddThemeFontOverride("font", GetTooltipFont());
		label.AddThemeFontSizeOverride("font_size", fontSize);
	}

	private static Font LoadFont(string path)
	{
		if (!ResourceLoader.Exists(path))
		{
			GD.PrintErr($"UI font missing: {path}");
			return ThemeDB.FallbackFont;
		}

		var loaded = GD.Load<FontFile>(path);
		if (loaded == null)
		{
			GD.PrintErr($"Failed to load UI font: {path}");
			return ThemeDB.FallbackFont;
		}

		var font = (FontFile)loaded.Duplicate();
		font.Antialiasing = TextServer.FontAntialiasing.Gray;
		font.Fallbacks = new Array<Font> { ThemeDB.FallbackFont };
		return font;
	}
}
