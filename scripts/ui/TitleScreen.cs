using Godot;
using LumberjackAttack.Core;
using LumberjackAttack.Save;
using LumberjackAttack.Settings;

namespace LumberjackAttack.Ui;

/// <summary>Title: New Game (named), Load Game, Quit — Java MainMenuScreen slice.</summary>
public partial class TitleScreen : Control
{
	private enum View
	{
		Home,
		Load,
		NewGame,
		Settings,
	}

	private View _view = View.Home;
	private VBoxContainer _homeBox = null!;
	private VBoxContainer _newBox = null!;
	private VBoxContainer _loadBox = null!;
	private LineEdit _nameEdit = null!;
	private VBoxContainer _saveList = null!;
	private Label _status = null!;
	private ConfirmationDialog _overwriteDialog = null!;
	private ConfirmationDialog _deleteDialog = null!;
	private SettingsPanel _settings = null!;
	private DisplaySettings _displaySettings = null!;
	private InputSettings _inputSettings = null!;
	private string _pendingTitle = "";
	private SaveSummary? _pendingDelete;

	public override void _Ready()
	{
		_displaySettings = DisplaySettings.Load();
		_inputSettings = InputSettings.Load();
		_displaySettings.ApplyWindowMode();

		GameFont.Apply(this);

		SetAnchorsPreset(LayoutPreset.FullRect);
		MouseFilter = MouseFilterEnum.Stop;

		var bg = new ColorRect
		{
			Color = new Color(0.07f, 0.08f, 0.1f, 1f),
		};
		bg.SetAnchorsPreset(LayoutPreset.FullRect);
		AddChild(bg);

		var center = new CenterContainer();
		center.SetAnchorsPreset(LayoutPreset.FullRect);
		AddChild(center);

		var root = new VBoxContainer
		{
			CustomMinimumSize = new Vector2(420, 0),
		};
		root.AddThemeConstantOverride("separation", 12);
		center.AddChild(root);

		var title = new Label
		{
			Text = "Lumberjack Attack",
			HorizontalAlignment = HorizontalAlignment.Center,
		};
		GameFont.ApplyDisplay(title, GameFont.TitleFontSize);
		title.AddThemeColorOverride("font_color", Colors.White);
		title.AddThemeColorOverride("font_outline_color", Colors.Black);
		title.AddThemeConstantOverride("outline_size", 4);
		root.AddChild(title);

		var sub = new Label
		{
			Text = "Godot port",
			HorizontalAlignment = HorizontalAlignment.Center,
		};
		sub.AddThemeFontSizeOverride("font_size", 14);
		sub.AddThemeColorOverride("font_color", new Color(0.9f, 0.92f, 0.95f));
		sub.AddThemeColorOverride("font_outline_color", Colors.Black);
		sub.AddThemeConstantOverride("outline_size", 3);
		root.AddChild(sub);

		_status = new Label
		{
			HorizontalAlignment = HorizontalAlignment.Center,
			AutowrapMode = TextServer.AutowrapMode.WordSmart,
			CustomMinimumSize = new Vector2(400, 24),
		};
		_status.AddThemeFontSizeOverride("font_size", 13);
		_status.AddThemeColorOverride("font_color", new Color(1f, 0.88f, 0.4f));
		_status.AddThemeColorOverride("font_outline_color", Colors.Black);
		_status.AddThemeConstantOverride("outline_size", 3);
		root.AddChild(_status);

		_homeBox = new VBoxContainer();
		_homeBox.AddThemeConstantOverride("separation", 8);
		root.AddChild(_homeBox);
		_homeBox.AddChild(MakeMenuButton("New Game", () => ShowNewGame()));
		_homeBox.AddChild(MakeMenuButton("Load Game", () => ShowLoad()));
		_homeBox.AddChild(MakeMenuButton("Settings", ShowSettings));
		if (GameConfig.EnableWorldEdit)
		{
			_homeBox.AddChild(MakeMenuButton("World Edit", OpenWorldEdit));
		}

		_homeBox.AddChild(MakeMenuButton("Quit", () => GetTree().Quit()));

		_newBox = new VBoxContainer();
		_newBox.AddThemeConstantOverride("separation", 8);
		_newBox.Visible = false;
		root.AddChild(_newBox);

		_newBox.AddChild(new Label { Text = "Name your save" });
		_nameEdit = new LineEdit
		{
			PlaceholderText = "e.g. Homestead Run",
			MaxLength = 48,
			CustomMinimumSize = new Vector2(400, 36),
		};
		_nameEdit.TextSubmitted += _ => TryBeginNewGame();
		_newBox.AddChild(_nameEdit);

		var newRow = new HBoxContainer();
		newRow.AddThemeConstantOverride("separation", 8);
		_newBox.AddChild(newRow);
		newRow.AddChild(MakeMenuButton("Start", TryBeginNewGame, 180));
		newRow.AddChild(MakeMenuButton("Back", ShowHome, 180));

		_loadBox = new VBoxContainer();
		_loadBox.AddThemeConstantOverride("separation", 8);
		_loadBox.Visible = false;
		root.AddChild(_loadBox);

		_loadBox.AddChild(new Label { Text = "Load Game" });
		var scroll = new ScrollContainer
		{
			CustomMinimumSize = new Vector2(400, 280),
			HorizontalScrollMode = ScrollContainer.ScrollMode.Disabled,
		};
		_loadBox.AddChild(scroll);
		_saveList = new VBoxContainer();
		_saveList.AddThemeConstantOverride("separation", 6);
		_saveList.SizeFlagsHorizontal = SizeFlags.ExpandFill;
		scroll.AddChild(_saveList);
		_loadBox.AddChild(MakeMenuButton("Back", ShowHome));

		_overwriteDialog = new ConfirmationDialog
		{
			Title = "Overwrite save?",
			DialogText = "A save with that name already exists. Overwrite it?",
			OkButtonText = "Overwrite",
			CancelButtonText = "Cancel",
		};
		_overwriteDialog.Confirmed += OnOverwriteConfirmed;
		AddChild(_overwriteDialog);
		GameFont.Apply(_overwriteDialog);
		MenuChrome.ApplyDialog(_overwriteDialog);

		_deleteDialog = new ConfirmationDialog
		{
			Title = "Delete save?",
			DialogText = "Delete this save? This cannot be undone.",
			OkButtonText = "Delete",
			CancelButtonText = "Cancel",
		};
		_deleteDialog.Confirmed += OnDeleteConfirmed;
		AddChild(_deleteDialog);
		GameFont.Apply(_deleteDialog);
		MenuChrome.ApplyDialog(_deleteDialog);

		_settings = new SettingsPanel();
		AddChild(_settings);
		_settings.Configure(_displaySettings, _inputSettings, onClosed: ShowHome);

		ShowHome();
	}

	private void ShowHome()
	{
		_view = View.Home;
		_homeBox.Visible = true;
		_newBox.Visible = false;
		_loadBox.Visible = false;
		_settings.Visible = false;
		_status.Text = "";
	}

	private void ShowSettings()
	{
		_view = View.Settings;
		_homeBox.Visible = false;
		_newBox.Visible = false;
		_loadBox.Visible = false;
		_status.Text = "";
		_settings.Open();
	}

	private void OpenWorldEdit() =>
		GetTree().ChangeSceneToFile("res://scenes/WorldEdit.tscn");

	private void ShowNewGame()
	{
		_view = View.NewGame;
		_homeBox.Visible = false;
		_newBox.Visible = true;
		_loadBox.Visible = false;
		_settings.Visible = false;
		_status.Text = "";
		_nameEdit.Text = "";
		_nameEdit.GrabFocus();
	}

	private void ShowLoad()
	{
		_view = View.Load;
		_homeBox.Visible = false;
		_newBox.Visible = false;
		_loadBox.Visible = true;
		_settings.Visible = false;
		RebuildSaveList();
	}

	private void RebuildSaveList()
	{
		foreach (var child in _saveList.GetChildren())
		{
			_saveList.RemoveChild(child);
			child.Free();
		}

		var saves = SaveManager.ListSummaries();
		if (saves.Count == 0)
		{
			_status.Text = "No saves found.";
			_saveList.AddChild(new Label { Text = "— empty —" });
			return;
		}

		_status.Text = "";
		foreach (var summary in saves)
		{
			var row = new HBoxContainer();
			row.AddThemeConstantOverride("separation", 6);
			_saveList.AddChild(row);

			var loadBtn = new Button
			{
				Text = summary.ButtonLabel,
				CustomMinimumSize = new Vector2(320, 40),
				Alignment = HorizontalAlignment.Left,
				Disabled = !summary.Loadable,
				FocusMode = FocusModeEnum.None,
				SizeFlagsHorizontal = SizeFlags.ExpandFill,
			};
			var captured = summary;
			loadBtn.Pressed += () => LaunchLoad(captured);
			row.AddChild(loadBtn);

			var delBtn = new Button
			{
				Text = "Del",
				CustomMinimumSize = new Vector2(56, 40),
				FocusMode = FocusModeEnum.None,
			};
			delBtn.Pressed += () => PromptDelete(captured);
			row.AddChild(delBtn);
		}
	}

	private void TryBeginNewGame()
	{
		var title = _nameEdit.Text.Trim();
		if (string.IsNullOrEmpty(title))
		{
			_status.Text = "Enter a save name.";
			return;
		}

		_pendingTitle = title;
		if (SaveManager.ExistsByTitle(title))
		{
			_overwriteDialog.DialogText =
				$"A save named \"{title}\" already exists.\nOverwrite it?";
			_overwriteDialog.PopupCentered();
			return;
		}

		LaunchNewGame(title);
	}

	private void OnOverwriteConfirmed()
	{
		if (string.IsNullOrEmpty(_pendingTitle))
		{
			return;
		}

		SaveManager.Delete(SaveFileNames.ToFileName(_pendingTitle));
		LaunchNewGame(_pendingTitle);
		_pendingTitle = "";
	}

	private void LaunchNewGame(string title)
	{
		GameLaunch.RequestNewGame(title);
		GetTree().ChangeSceneToFile("res://scenes/Main.tscn");
	}

	private void LaunchLoad(SaveSummary summary)
	{
		if (!summary.Loadable)
		{
			_status.Text = "That save is corrupt.";
			return;
		}

		GameLaunch.RequestLoad(summary.FileName);
		GetTree().ChangeSceneToFile("res://scenes/Main.tscn");
	}

	private void PromptDelete(SaveSummary summary)
	{
		_pendingDelete = summary;
		_deleteDialog.DialogText =
			$"Delete save \"{summary.SaveTitle}\"?\nThis cannot be undone.";
		_deleteDialog.PopupCentered();
	}

	private void OnDeleteConfirmed()
	{
		if (_pendingDelete == null)
		{
			return;
		}

		SaveManager.Delete(_pendingDelete.FileName);
		_status.Text = $"Deleted \"{_pendingDelete.SaveTitle}\".";
		_pendingDelete = null;
		RebuildSaveList();
	}

	private static Button MakeMenuButton(string text, System.Action onPressed, float width = 400)
	{
		var btn = new Button
		{
			Text = text,
			CustomMinimumSize = new Vector2(width, 48),
			FocusMode = FocusModeEnum.None,
		};
		MenuChrome.ApplyFramedButton(btn);
		btn.Pressed += onPressed;
		return btn;
	}

	public override void _UnhandledInput(InputEvent @event)
	{
		if (@event is not InputEventKey { Pressed: true, Echo: false } key)
		{
			return;
		}

		if (key.Keycode != Key.F11)
		{
			return;
		}

		_displaySettings.ToggleFullscreen();
		_displaySettings.Save();
		if (_settings.Visible)
		{
			_settings.Open(); // refresh labels
		}

		GetViewport().SetInputAsHandled();
	}
}
