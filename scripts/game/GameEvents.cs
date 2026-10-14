using System;
using LumberjackAttack.Skills;

namespace LumberjackAttack.Game;

/// <summary>
/// Central extension hooks for future systems (Steam achievements, analytics, quests).
/// Subscribe from a platform layer — gameplay code raises these; it does not depend on Steam.
/// </summary>
public static class GameEvents
{
	public static event Action<string, int>? ItemsPickedUp;
	public static event Action<string>? CollectionUnlocked;
	public static event Action<string>? RecipeUnlocked;
	public static event Action<SkillType, int>? SkillLevelUp;
	public static event Action? PlayerSlept;
	public static event Action? PlayerDied;
	public static event Action<string>? MapEntered;
	public static event Action<int>? DayAdvanced;

	public static void RaiseItemsPickedUp(string itemId, int qty) =>
		ItemsPickedUp?.Invoke(itemId, qty);

	public static void RaiseCollectionUnlocked(string displayName) =>
		CollectionUnlocked?.Invoke(displayName);

	public static void RaiseRecipeUnlocked(string recipeName) =>
		RecipeUnlocked?.Invoke(recipeName);

	public static void RaiseSkillLevelUp(SkillType skill, int level) =>
		SkillLevelUp?.Invoke(skill, level);

	public static void RaisePlayerSlept() => PlayerSlept?.Invoke();

	public static void RaisePlayerDied() => PlayerDied?.Invoke();

	public static void RaiseMapEntered(string mapId) => MapEntered?.Invoke(mapId);

	public static void RaiseDayAdvanced(int day) => DayAdvanced?.Invoke(day);
}
