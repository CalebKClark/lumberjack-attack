package lumberjack.skills;

/**
 * Reward text shown when hovering a level node in the skill snake.
 */
public final class SkillLevelDefinition {

    private final SkillType skillType;
    private final int level;
    private final String reward;

    public SkillLevelDefinition(SkillType skillType, int level, String reward) {
        this.skillType = skillType;
        this.level = level;
        this.reward = reward;
    }

    public SkillType getSkillType() {
        return skillType;
    }

    public int getLevel() {
        return level;
    }

    public String getReward() {
        return reward;
    }
}
