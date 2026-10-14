using System;
using Godot;

namespace LumberjackAttack.Settings;

/// <summary>Zoom, fullscreen, debug — persisted under user://settings/.</summary>
public sealed class DisplaySettings
{
	public const int MinZoomPercent = 0;
	public const int MaxZoomPercent = 100;
	public const int DefaultZoomPercent = 85;
	public const double MaxViewZoomScale = 12.0;

	private const string Path = "user://settings/display.cfg";

	public int ZoomPercent { get; private set; } = DefaultZoomPercent;
	public bool DebugEnabled { get; private set; }
	public bool Fullscreen { get; private set; }
	public bool ShowTileHover { get; private set; } = true;

	public static DisplaySettings Load()
	{
		var settings = new DisplaySettings();
		var cfg = new ConfigFile();
		if (cfg.Load(Path) != Error.Ok)
		{
			return settings;
		}

		settings.SetZoomPercent((int)cfg.GetValue("display", "zoom_percent", DefaultZoomPercent));
		settings.DebugEnabled = (bool)cfg.GetValue("display", "enable_debug", false);
		settings.Fullscreen = (bool)cfg.GetValue("display", "fullscreen", false);
		settings.ShowTileHover = (bool)cfg.GetValue("display", "show_tile_hover", true);
		return settings;
	}

	public void Save()
	{
		DirAccess.MakeDirRecursiveAbsolute(ProjectSettings.GlobalizePath("user://settings"));
		var cfg = new ConfigFile();
		cfg.SetValue("display", "zoom_percent", ZoomPercent);
		cfg.SetValue("display", "enable_debug", DebugEnabled);
		cfg.SetValue("display", "fullscreen", Fullscreen);
		cfg.SetValue("display", "show_tile_hover", ShowTileHover);
		cfg.Save(Path);
	}

	public void ToggleShowTileHover() => ShowTileHover = !ShowTileHover;

	public void SetShowTileHover(bool show) => ShowTileHover = show;

	public void SetZoomPercent(int percent) =>
		ZoomPercent = Math.Clamp(percent, MinZoomPercent, MaxZoomPercent);

	public void ToggleDebug() => DebugEnabled = !DebugEnabled;

	public void SetFullscreen(bool fullscreen)
	{
		Fullscreen = fullscreen;
		ApplyWindowMode();
	}

	public void ToggleFullscreen() => SetFullscreen(!Fullscreen);

	/// <summary>Apply saved window mode to the OS window (call on boot + when toggled).</summary>
	public void ApplyWindowMode()
	{
		DisplayServer.WindowSetMode(
			Fullscreen
				? DisplayServer.WindowMode.Fullscreen
				: DisplayServer.WindowMode.Windowed
		);
	}

	public void ResetToDefaults()
	{
		ZoomPercent = DefaultZoomPercent;
		DebugEnabled = false;
		// Leave fullscreen as-is on control reset — only zoom/debug reset here.
	}

	/// <summary>World scale multiplier: 0% = 1×, 100% = MaxViewZoomScale.</summary>
	public double GetZoomScale()
	{
		var zoomT = ZoomPercent / (double)MaxZoomPercent;
		var minVisibleFraction = 1.0 / MaxViewZoomScale;
		var visibleFraction = 1.0 - zoomT * (1.0 - minVisibleFraction);
		return 1.0 / visibleFraction;
	}

	/// <summary>Godot Camera2D zoom — same curve as Java DisplaySettings.getZoomScale().</summary>
	public float GetCameraZoom() => (float)GetZoomScale();
}
