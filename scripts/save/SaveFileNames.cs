using System.Text.RegularExpressions;

namespace LumberjackAttack.Save;

/// <summary>Converts player-facing save titles into safe file names (Java SaveFileNames).</summary>
public static class SaveFileNames
{
	private static readonly Regex NonAlnum = new("[^a-z0-9]+", RegexOptions.Compiled);

	public static string ToFileName(string saveTitle)
	{
		var slug = NonAlnum.Replace((saveTitle ?? "").Trim().ToLowerInvariant(), "_")
			.Trim('_');

		if (string.IsNullOrEmpty(slug))
		{
			slug = "untitled";
		}

		if (slug.Length > 48)
		{
			slug = slug[..48];
		}

		return slug + ".json";
	}
}
