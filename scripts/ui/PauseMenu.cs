using System;
using Godot;

namespace LumberjackAttack.Ui;

/// <summary>In-game Esc menu: Continue, Settings, Save, Save and Exit (Java pause menu).</summary>
public partial class PauseMenu : CanvasLayer
{
	private Control _root = null!;
	private Control _menuBox = null!;
	private SettingsPanel _settings = null!;
	private Action? _onContinue;
	private Action? _onSave;
	private Action? _onSaveAndExit;
	private Action? _onSettingsChanged;
	private Settings.DisplaySettings? _display;

	public bool IsOpen { get; private set; }
	public bool IsSettingsOpen => _settings is { Visible: true };

	public void Configure(
		Settings.DisplaySettings display,
		Settings.InputSettings input,
		Action onContinue,
		Action onSave,
		Action onSaveAndExit,
		Action? onSettingsChanged = null
	)
	{
		_onContinue = onContinue;
		_onSave = onSave;
		_onSaveAndExit = onSaveAndExit;
		_onSettingsChanged = onSettingsChanged;

		if (_settings != null)
		{
			_settings.Configure(display, input, onClosed: ShowMenu, onChanged: () => _onSettingsChanged?.Invoke());
		}
	}

	public override void _Ready()
	{
		Layer = 60;
		ProcessMode = ProcessModeEnum.Always;
		GameFont.ApplyToCanvasLayer(this);

		_root = new Control { Visible = false };
		_root.SetAnchorsPreset(Control.LayoutPreset.FullRect);
		_root.MouseFilter = Control.MouseFilterEnum.Stop;
		AddChild(_root);

		var dim = new ColorRect { Color = new Color(0.12f, 0.12f, 0.12f, 0.92f) };
		dim.SetAnchorsPreset(Control.LayoutPreset.FullRect);
		_root.AddChild(dim);

		var center = new CenterContainer();
		center.SetAnchorsPreset(Control.LayoutPreset.FullRect);
		_root.AddChild(center);

		_menuBox = new VBoxContainer { CustomMinimumSize = new Vector2(320, 0) };
		_menuBox.AddThemeConstantOverride("separation", 14);
		center.AddChild(_menuBox);

		_menuBox.AddChild(MakeButton("Continue", () =>
		{
			Close();
			_onContinue?.Invoke();
		}));
		_menuBox.AddChild(MakeButton("Settings", ShowSettings));
		_menuBox.AddChild(MakeButton("Save", () => _onSave?.Invoke()));
		_menuBox.AddChild(MakeButton("Save and Exit", () => _onSaveAndExit?.Invoke()));

		_settings = new SettingsPanel();
		_root.AddChild(_settings);
	}

	public void Open(Settings.DisplaySettings display, Settings.InputSettings input)
	{
		_display = display;
		_settings.Configure(display, input, onClosed: ShowMenu, onChanged: () => _onSettingsChanged?.Invoke());
		IsOpen = true;
		_root.Visible = true;
		ShowMenu();
		GetTree().Paused = true;
	}

	public void Close()
	{
		IsOpen = false;
		_root.Visible = false;
		_settings.Visible = false;
		GetTree().Paused = false;
	}

	private void ShowMenu()
	{
		_menuBox.Visible = true;
		_settings.Visible = false;
	}

	private void ShowSettings()
	{
		_menuBox.Visible = false;
		_settings.Open();
	}

	public override void _UnhandledInput(InputEvent @event)
	{
		if (!IsOpen || @event is not InputEventKey { Pressed: true, Echo: false } key)
		{
			return;
		}

		if (key.Keycode == Key.F11)
		{
			_display?.ToggleFullscreen();
			_display?.Save();
			GetViewport().SetInputAsHandled();
			return;
		}

		if (key.Keycode != Key.Escape)
		{
			return;
		}

		if (_settings.IsListening)
		{
			return; // SettingsPanel handles Esc cancel for rebind.
		}

		if (_settings.Visible)
		{
			_settings.Close();
			GetViewport().SetInputAsHandled();
			return;
		}

		Close();
		_onContinue?.Invoke();
		GetViewport().SetInputAsHandled();
	}

	private static Button MakeButton(string text, Action onPressed)
	{
		var btn = new Button
		{
			Text = text,
			CustomMinimumSize = new Vector2(320, 56),
			FocusMode = Control.FocusModeEnum.None,
		};
		MenuChrome.ApplyFramedButton(btn);
		btn.Pressed += onPressed;
		return btn;
	}
}
