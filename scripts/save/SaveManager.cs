using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;
using Godot;

namespace LumberjackAttack.Save;

/// <summary>Named JSON saves under user://saves/ (atomic temp → rename).</summary>
public static class SaveManager
{
	private static readonly JsonSerializerOptions JsonOptions = new()
	{
		WriteIndented = true,
		PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
		PropertyNameCaseInsensitive = true,
	};

	public static string SavesDir => "user://saves";

	public static string SlotPath(string fileName) => $"{SavesDir}/{fileName}";

	public static bool Exists(string fileName) =>
		!string.IsNullOrEmpty(fileName) && Godot.FileAccess.FileExists(SlotPath(fileName));

	public static bool ExistsByTitle(string saveTitle) =>
		Exists(SaveFileNames.ToFileName(saveTitle));

	public static void EnsureSaveDir()
	{
		DirAccess.MakeDirRecursiveAbsolute(ProjectSettings.GlobalizePath(SavesDir));
	}

	public static List<SaveSummary> ListSummaries()
	{
		EnsureSaveDir();
		var list = new List<SaveSummary>();
		using var dir = DirAccess.Open(SavesDir);
		if (dir == null)
		{
			return list;
		}

		dir.ListDirBegin();
		while (true)
		{
			var name = dir.GetNext();
			if (string.IsNullOrEmpty(name))
			{
				break;
			}

			if (dir.CurrentIsDir() || !name.EndsWith(".json", StringComparison.OrdinalIgnoreCase))
			{
				continue;
			}

			if (name.EndsWith(".tmp", StringComparison.OrdinalIgnoreCase))
			{
				continue;
			}

			try
			{
				var dto = Read(name);
				list.Add(dto == null ? SaveSummary.Corrupt(name) : SaveSummary.FromDto(name, dto));
			}
			catch (Exception)
			{
				list.Add(SaveSummary.Corrupt(name));
			}
		}

		dir.ListDirEnd();
		return list.OrderByDescending(s => s.SavedAtUnixMs).ToList();
	}

	public static void Write(SaveGameDto data, string fileName)
	{
		if (string.IsNullOrWhiteSpace(fileName))
		{
			throw new ArgumentException("Save file name required.", nameof(fileName));
		}

		EnsureSaveDir();
		data.SavedAtUnixMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
		data.Version = Math.Max(1, data.Version);
		var json = JsonSerializer.Serialize(data, JsonOptions);
		var path = SlotPath(fileName);
		var tmp = path + ".tmp";

		using (var file = Godot.FileAccess.Open(tmp, Godot.FileAccess.ModeFlags.Write))
		{
			if (file == null)
			{
				throw new InvalidOperationException($"Cannot write save temp: {tmp}");
			}

			file.StoreString(json);
		}

		DirAccess.RemoveAbsolute(ProjectSettings.GlobalizePath(path));
		var err = DirAccess.RenameAbsolute(
			ProjectSettings.GlobalizePath(tmp),
			ProjectSettings.GlobalizePath(path)
		);
		if (err != Error.Ok)
		{
			using var file = Godot.FileAccess.Open(path, Godot.FileAccess.ModeFlags.Write);
			if (file == null)
			{
				throw new InvalidOperationException($"Cannot write save: {path}");
			}

			file.StoreString(json);
		}
	}

	public static SaveGameDto? Read(string fileName)
	{
		var path = SlotPath(fileName);
		if (!Godot.FileAccess.FileExists(path))
		{
			return null;
		}

		using var file = Godot.FileAccess.Open(path, Godot.FileAccess.ModeFlags.Read);
		if (file == null)
		{
			return null;
		}

		var json = file.GetAsText();
		return JsonSerializer.Deserialize<SaveGameDto>(json, JsonOptions);
	}

	public static void Delete(string fileName)
	{
		if (string.IsNullOrWhiteSpace(fileName))
		{
			return;
		}

		var path = ProjectSettings.GlobalizePath(SlotPath(fileName));
		if (Godot.FileAccess.FileExists(SlotPath(fileName)))
		{
			DirAccess.RemoveAbsolute(path);
		}

		var tmpRel = SlotPath(fileName) + ".tmp";
		if (Godot.FileAccess.FileExists(tmpRel))
		{
			DirAccess.RemoveAbsolute(ProjectSettings.GlobalizePath(tmpRel));
		}
	}
}
