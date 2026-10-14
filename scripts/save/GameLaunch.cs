namespace LumberjackAttack.Save;

/// <summary>Passes new/load choice from Title → Main across scene changes.</summary>
public static class GameLaunch
{
	public enum Mode
	{
		NewGame,
		Load,
	}

	public static Mode? Pending { get; private set; }
	public static string SaveTitle { get; private set; } = "";
	public static string FileName { get; private set; } = "";

	public static void RequestNewGame(string saveTitle)
	{
		Pending = Mode.NewGame;
		SaveTitle = saveTitle.Trim();
		FileName = SaveFileNames.ToFileName(SaveTitle);
	}

	public static void RequestLoad(string fileName)
	{
		Pending = Mode.Load;
		FileName = fileName;
		SaveTitle = "";
	}

	public static void Clear()
	{
		Pending = null;
		SaveTitle = "";
		FileName = "";
	}
}
