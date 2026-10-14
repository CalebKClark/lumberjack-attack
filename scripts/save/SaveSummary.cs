using System;

namespace LumberjackAttack.Save;

/// <summary>One save row for the title / load list (Java SaveSummary).</summary>
public sealed class SaveSummary
{
	public string FileName { get; init; } = "";
	public string SaveTitle { get; init; } = "Untitled";
	public string DetailText { get; init; } = "";
	public long SavedAtUnixMs { get; init; }
	public bool Loadable { get; init; } = true;

	public string ButtonLabel => $"{SaveTitle} — {DetailText}";

	public static SaveSummary FromDto(string fileName, SaveGameDto data)
	{
		var title = string.IsNullOrWhiteSpace(data.Title) ? "Untitled" : data.Title.Trim();
		var period = data.Hour >= 12 ? "PM" : "AM";
		var displayHour = data.Hour % 12;
		if (displayHour == 0)
		{
			displayHour = 12;
		}

		var detail = $"Day {data.Day} · {displayHour}:{data.Minute:D2} {period}";
		return new SaveSummary
		{
			FileName = fileName,
			SaveTitle = title,
			DetailText = detail,
			SavedAtUnixMs = data.SavedAtUnixMs,
			Loadable = true,
		};
	}

	public static SaveSummary Corrupt(string fileName) => new()
	{
		FileName = fileName,
		SaveTitle = fileName,
		DetailText = "Corrupt save",
		SavedAtUnixMs = 0,
		Loadable = false,
	};
}
