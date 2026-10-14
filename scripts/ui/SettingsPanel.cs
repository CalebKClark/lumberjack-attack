using System;
using Godot;
using LumberjackAttack.Settings;

namespace LumberjackAttack.Ui;

/// <summary>Shared Settings UI for title + pause (zoom, debug, keybinds).</summary>
public partial class SettingsPanel : Control
{
	private DisplaySettings _display = null!;
	private InputSettings _input = null!;
	private Label _zoomLabel = null!;
	private Button _debugBtn = null!;
	private Button _fullscreenBtn = null!;
	private Button _tileHoverBtn = null!;
	private Label _status = null!;
	private VBoxContainer _bindList = null!;
	private GameAction? _listening;
	private Action? _onClosed;
	private Action? _onChanged;

	public bool IsListening => _listening != null;

	public void Configure(DisplaySettings display, InputSettings input, Action? onClosed = null, Action? onChanged = null)
	{
		_display = display;
		_input = input;
		_onClosed = onClosed;
		_onChanged = onChanged;
	}

	public override void _Ready()
	{
		SetAnchorsPreset(LayoutPreset.FullRect);
		MouseFilter = MouseFilterEnum.Stop;
		Visible = false;

		var dim = new ColorRect { Color = new Color(0.1f, 0.1f, 0.12f, 0.94f) };
		dim.SetAnchorsPreset(LayoutPreset.FullRect);
		AddChild(dim);

		var center = new CenterContainer();
		center.SetAnchorsPreset(LayoutPreset.FullRect);
		AddChild(center);

		var panel = new PanelContainer { CustomMinimumSize = new Vector2(520, 520) };
		MenuChrome.ApplyPanel(panel);
		center.AddChild(panel);

		var vbox = new VBoxContainer();
		vbox.AddThemeConstantOverride("separation", 10);
		panel.AddChild(vbox);

		var title = new Label
		{
			Text = "Settings",
			HorizontalAlignment = HorizontalAlignment.Center,
		};
		GameFont.ApplyDisplay(title, GameFont.SectionFontSize);
		vbox.AddChild(title);

		var zoomRow = new HBoxContainer();
		zoomRow.AddThemeConstantOverride("separation", 10);
		vbox.AddChild(zoomRow);
		zoomRow.AddChild(new Label { Text = "Zoom", CustomMinimumSize = new Vector2(80, 0) });
		var zoomMinus = new Button { Text = "−", CustomMinimumSize = new Vector2(44, 36), FocusMode = FocusModeEnum.None };
		zoomMinus.Pressed += () =>
		{
			_display.SetZoomPercent(_display.ZoomPercent - 10);
			PersistAndRefresh();
		};
		zoomRow.AddChild(zoomMinus);
		var zoomPlus = new Button { Text = "+", CustomMinimumSize = new Vector2(44, 36), FocusMode = FocusModeEnum.None };
		zoomPlus.Pressed += () =>
		{
			_display.SetZoomPercent(_display.ZoomPercent + 10);
			PersistAndRefresh();
		};
		zoomRow.AddChild(zoomPlus);
		_zoomLabel = new Label { CustomMinimumSize = new Vector2(80, 0) };
		zoomRow.AddChild(_zoomLabel);

		_fullscreenBtn = new Button
		{
			CustomMinimumSize = new Vector2(220, 36),
			FocusMode = FocusModeEnum.None,
		};
		_fullscreenBtn.Pressed += () =>
		{
			_display.ToggleFullscreen();
			_status.Text = _display.Fullscreen ? "Fullscreen on (F11)." : "Windowed (F11).";
			PersistAndRefresh();
		};
		vbox.AddChild(_fullscreenBtn);

		_debugBtn = new Button
		{
			CustomMinimumSize = new Vector2(220, 36),
			FocusMode = FocusModeEnum.None,
		};
		_debugBtn.Pressed += () =>
		{
			_display.ToggleDebug();
			_status.Text = _display.DebugEnabled ? "Debug enabled." : "Debug disabled.";
			PersistAndRefresh();
		};
		vbox.AddChild(_debugBtn);

		_tileHoverBtn = new Button
		{
			CustomMinimumSize = new Vector2(220, 36),
			FocusMode = FocusModeEnum.None,
		};
		_tileHoverBtn.Pressed += () =>
		{
			_display.ToggleShowTileHover();
			_status.Text = _display.ShowTileHover ? "Tile hover outline on." : "Tile hover outline off.";
			PersistAndRefresh();
		};
		vbox.AddChild(_tileHoverBtn);

		vbox.AddChild(new Label { Text = "Controls" });

		var scroll = new ScrollContainer
		{
			CustomMinimumSize = new Vector2(480, 280),
			HorizontalScrollMode = ScrollContainer.ScrollMode.Disabled,
			SizeFlagsVertical = SizeFlags.ExpandFill,
		};
		vbox.AddChild(scroll);
		_bindList = new VBoxContainer();
		_bindList.AddThemeConstantOverride("separation", 4);
		scroll.AddChild(_bindList);

		var footer = new HBoxContainer();
		footer.AddThemeConstantOverride("separation", 12);
		vbox.AddChild(footer);
		var reset = new Button
		{
			Text = "Reset Controls",
			CustomMinimumSize = new Vector2(180, 40),
			FocusMode = FocusModeEnum.None,
		};
		reset.Pressed += () =>
		{
			_input.ResetToDefaults();
			_status.Text = "Controls reset to defaults.";
			PersistAndRefresh();
		};
		footer.AddChild(reset);
		var back = new Button
		{
			Text = "Back",
			CustomMinimumSize = new Vector2(120, 40),
			FocusMode = FocusModeEnum.None,
		};
		back.Pressed += Close;
		footer.AddChild(back);

		_status = new Label
		{
			HorizontalAlignment = HorizontalAlignment.Center,
			AutowrapMode = TextServer.AutowrapMode.WordSmart,
			CustomMinimumSize = new Vector2(480, 24),
		};
		_status.AddThemeFontSizeOverride("font_size", 12);
		_status.AddThemeColorOverride("font_color", new Color(1f, 0.85f, 0.35f));
		vbox.AddChild(_status);
	}

	public void Open()
	{
		_listening = null;
		_status.Text = "";
		Visible = true;
		Refresh();
	}

	public void Close()
	{
		_listening = null;
		Visible = false;
		_display.Save();
		_input.Save();
		_onClosed?.Invoke();
	}

	public override void _UnhandledInput(InputEvent @event)
	{
		if (!Visible || _listening is not { } action)
		{
			return;
		}

		if (@event is not InputEventKey { Pressed: true, Echo: false } key)
		{
			return;
		}

		if (key.Keycode == Key.Escape)
		{
			_listening = null;
			_status.Text = "Rebind cancelled.";
			Refresh();
			GetViewport().SetInputAsHandled();
			return;
		}

		var existing = _input.FindActionForKey(key.Keycode);
		if (existing is { } other && other != action)
		{
			_input.SetBinding(other, _input.GetBinding(action));
		}

		_input.SetBinding(action, key.Keycode);
		_status.Text = $"{action.Label()} → {InputSettings.DisplayName(key.Keycode)}";
		_listening = null;
		PersistAndRefresh();
		GetViewport().SetInputAsHandled();
	}

	private void PersistAndRefresh()
	{
		_display.Save();
		_input.Save();
		Refresh();
		_onChanged?.Invoke();
	}

	private void Refresh()
	{
		if (_zoomLabel == null)
		{
			return;
		}

		_zoomLabel.Text = $"{_display.ZoomPercent}%";
		_fullscreenBtn.Text = _display.Fullscreen ? "Fullscreen: ON" : "Fullscreen: OFF";
		_debugBtn.Text = _display.DebugEnabled ? "Enable Debug: ON" : "Enable Debug: OFF";
		_tileHoverBtn.Text = _display.ShowTileHover ? "Tile Hover Outline: ON" : "Tile Hover Outline: OFF";

		foreach (var child in _bindList.GetChildren())
		{
			_bindList.RemoveChild(child);
			child.Free();
		}

		foreach (GameAction action in Enum.GetValues(typeof(GameAction)))
		{
			var row = new HBoxContainer();
			row.AddThemeConstantOverride("separation", 8);
			_bindList.AddChild(row);

			var label = new Label
			{
				Text = action.Label(),
				CustomMinimumSize = new Vector2(160, 0),
				SizeFlagsHorizontal = SizeFlags.ExpandFill,
			};
			row.AddChild(label);

			var captured = action;
			var listening = _listening == action;
			var btn = new Button
			{
				Text = listening ? "…" : InputSettings.DisplayName(_input.GetBinding(action)),
				CustomMinimumSize = new Vector2(120, 32),
				FocusMode = FocusModeEnum.None,
			};
			btn.Pressed += () =>
			{
				_listening = captured;
				_status.Text = $"Press a key for {captured.Label()} (Esc to cancel)";
				Refresh();
			};
			row.AddChild(btn);
		}
	}
}
