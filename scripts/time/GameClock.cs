using System;
using System.Collections.Generic;
using System.Globalization;
using Godot;
using LumberjackAttack.Core;

namespace LumberjackAttack.Time;

public enum DayPhase
{
	Day,
	Night,
}

public sealed class DayCycleConfig
{
	public int DayStartHour { get; }
	public int NightStartHour { get; }
	public int StartingDay { get; }
	public int StartingHour { get; }
	public int StartingMinute { get; }

	public DayCycleConfig(string path = "res://data/day_cycle.csv")
	{
		var values = Load(path);
		DayStartHour = ParseHour(values, "day_start_hour", 6);
		NightStartHour = ParseHour(values, "night_start_hour", 20);
		StartingDay = ParseInt(values, "starting_day", 1);
		StartingHour = ParseHour(values, "starting_hour", 6);
		StartingMinute = ParseInt(values, "starting_minute", 0);
	}

	public DayPhase GetPhase(int hour, int minute)
	{
		var minutes = hour * 60 + minute;
		var dayStart = DayStartHour * 60;
		var nightStart = NightStartHour * 60;
		if (dayStart < nightStart)
		{
			return minutes >= dayStart && minutes < nightStart ? DayPhase.Day : DayPhase.Night;
		}

		return minutes >= dayStart || minutes < nightStart ? DayPhase.Day : DayPhase.Night;
	}

	private static Dictionary<string, string> Load(string path)
	{
		var map = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
		using var file = Godot.FileAccess.Open(path, Godot.FileAccess.ModeFlags.Read);
		if (file == null)
		{
			return map;
		}

		file.GetLine();
		while (!file.EofReached())
		{
			var line = file.GetLine();
			if (string.IsNullOrWhiteSpace(line))
			{
				continue;
			}

			var p = line.Split(',');
			if (p.Length < 2)
			{
				continue;
			}

			map[p[0].Trim()] = p[1].Trim();
		}

		return map;
	}

	private static int ParseHour(Dictionary<string, string> values, string key, int fallback)
	{
		if (!values.TryGetValue(key, out var raw))
		{
			return fallback;
		}

		return int.TryParse(raw, NumberStyles.Integer, CultureInfo.InvariantCulture, out var n)
			? Math.Clamp(n, 0, 23)
			: fallback;
	}

	private static int ParseInt(Dictionary<string, string> values, string key, int fallback)
	{
		if (!values.TryGetValue(key, out var raw))
		{
			return fallback;
		}

		return int.TryParse(raw, NumberStyles.Integer, CultureInfo.InvariantCulture, out var n) ? n : fallback;
	}
}

public sealed class GameClock
{
	private readonly DayCycleConfig _config;
	private double _minuteAccumulatorMs;

	public GameClock(DayCycleConfig? config = null)
	{
		_config = config ?? new DayCycleConfig();
		Day = _config.StartingDay;
		Hour = _config.StartingHour;
		Minute = _config.StartingMinute;
		Phase = _config.GetPhase(Hour, Minute);
	}

	/// <summary>Fired when <see cref="Day"/> increments (midnight wrap or sleep).</summary>
	public event Action? DayAdvanced;

	public int Day { get; private set; }
	public int Hour { get; private set; }
	public int Minute { get; private set; }
	public DayPhase Phase { get; private set; }

	public void Update(double elapsedMs)
	{
		if (elapsedMs <= 0)
		{
			return;
		}

		_minuteAccumulatorMs += elapsedMs;
		while (_minuteAccumulatorMs >= GameConfig.RealMsPerGameMinute)
		{
			_minuteAccumulatorMs -= GameConfig.RealMsPerGameMinute;
			AdvanceOneMinute();
		}
	}

	public int GetMinutesUntilNextMorning()
	{
		var untilMidnight = (GameConfig.HoursPerDay - Hour) * GameConfig.MinutesPerHour - Minute;
		var fromMidnight = _config.DayStartHour * GameConfig.MinutesPerHour;
		return untilMidnight + fromMidnight;
	}

	public int AdvanceToNextMorning()
	{
		var skipped = GetMinutesUntilNextMorning();
		Day++;
		Hour = _config.DayStartHour;
		Minute = 0;
		_minuteAccumulatorMs = 0;
		Phase = _config.GetPhase(Hour, Minute);
		DayAdvanced?.Invoke();
		return skipped;
	}

	public string FormatHudText()
	{
		var period = Hour >= 12 ? "PM" : "AM";
		var displayHour = Hour % 12;
		if (displayHour == 0)
		{
			displayHour = 12;
		}

		var phaseName = Phase == DayPhase.Day ? "Day" : "Night";
		return $"Day {Day} · {displayHour}:{Minute:00} {period} · {phaseName}";
	}

	public void SetTime(int day, int hour, int minute)
	{
		Day = Math.Max(1, day);
		Hour = Math.Clamp(hour, 0, GameConfig.HoursPerDay - 1);
		Minute = Math.Clamp(minute, 0, GameConfig.MinutesPerHour - 1);
		_minuteAccumulatorMs = 0;
		Phase = _config.GetPhase(Hour, Minute);
	}

	private void AdvanceOneMinute()
	{
		Minute++;
		if (Minute >= GameConfig.MinutesPerHour)
		{
			Minute = 0;
			Hour++;
			if (Hour >= GameConfig.HoursPerDay)
			{
				Hour = 0;
				Day++;
				Phase = _config.GetPhase(Hour, Minute);
				DayAdvanced?.Invoke();
				return;
			}
		}

		Phase = _config.GetPhase(Hour, Minute);
	}
}
