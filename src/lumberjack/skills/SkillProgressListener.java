package lumberjack.skills;

/**
 * Receives XP gains and level-ups for popup notifications.
 */
public interface SkillProgressListener {

    void onXpGained(SkillType skillType, int amount);

    void onSkillLevelUp(SkillType skillType, int newLevel);
}
