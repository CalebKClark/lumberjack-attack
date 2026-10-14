using System.Globalization;

namespace LumberjackAttack.World;

/// <summary>
/// Encodes tile id, optional rotation, and optional transfer marker for .map files.
/// <list type="bullet">
/// <item><c>12</c> — tile 12 at 0°</item>
/// <item><c>12:1</c> — 90° CW (<c>:2</c>=180°, <c>:3</c>=270°)</item>
/// <item><c>12t</c> / <c>12:1t</c> — same with editor-only transfer marker</item>
/// </list>
/// </summary>
public static class MapCellCodec
{
	public static int ParseTileId(string token)
	{
		var body = StripMarker(token);
		var colon = body.IndexOf(':');
		return int.Parse(colon < 0 ? body : body[..colon], CultureInfo.InvariantCulture);
	}

	public static int ParseRotation(string token)
	{
		var body = StripMarker(token);
		var colon = body.IndexOf(':');
		if (colon < 0 || colon == body.Length - 1)
		{
			return 0;
		}

		return int.Parse(body[(colon + 1)..], CultureInfo.InvariantCulture) & 3;
	}

	public static bool ParseTransitionMarker(string token) =>
		!string.IsNullOrEmpty(token) && (token[^1] is 't' or 'T');

	public static string Format(int tileId, int rotation, bool transitionMarker = false)
	{
		var rot = rotation & 3;
		var body = rot == 0
			? tileId.ToString(CultureInfo.InvariantCulture)
			: $"{tileId}:{rot}";
		return transitionMarker ? body + "t" : body;
	}

	private static string StripMarker(string token) =>
		ParseTransitionMarker(token) ? token[..^1] : token;
}
