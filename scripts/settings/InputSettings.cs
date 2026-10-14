using System;
using System.Collections.Generic;
using Godot;

namespace LumberjackAttack.Settings;

/// <summary>Key bindings (Java InputSettings), persisted under user://settings/.</summary>
public sealed class InputSettings
{
	private const string Path = "user://settings/input.cfg";
	private readonly Dictionary<GameAction, Key> _bindings = new();

	public InputSettings()
	{
		ResetToDefaults();
	}

	public static InputSettings Load()
	{
		var settings = new InputSettings();
		var cfg = new ConfigFile();
		if (cfg.Load(Path) != Error.Ok)
		{
			return settings;
		}

		foreach (GameAction action in Enum.GetValues(typeof(GameAction)))
		{
			var raw = (long)cfg.GetValue("input", action.ToString(), (long)action.DefaultKey());
			settings._bindings[action] = (Key)raw;
		}

		return settings;
	}

	public void Save()
	{
		DirAccess.MakeDirRecursiveAbsolute(ProjectSettings.GlobalizePath("user://settings"));
		var cfg = new ConfigFile();
		foreach (var (action, key) in _bindings)
		{
			cfg.SetValue("input", action.ToString(), (long)key);
		}

		cfg.Save(Path);
	}

	public Key GetBinding(GameAction action) =>
		_bindings.TryGetValue(action, out var key) ? key : action.DefaultKey();

	public void SetBinding(GameAction action, Key key) => _bindings[action] = key;

	public GameAction? FindActionForKey(Key key)
	{
		foreach (var (action, bound) in _bindings)
		{
			if (bound == key)
			{
				return action;
			}
		}

		return null;
	}

	public void ResetToDefaults()
	{
		_bindings.Clear();
		foreach (GameAction action in Enum.GetValues(typeof(GameAction)))
		{
			_bindings[action] = action.DefaultKey();
		}
	}

	public static string DisplayName(Key key)
	{
		if (key == Key.None)
		{
			return "Unbound";
		}

		var name = key.ToString();
		if (name.StartsWith("Key") && name.Length == 4)
		{
			return name[3].ToString();
		}

		return name;
	}
}
