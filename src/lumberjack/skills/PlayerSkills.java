package lumberjack.skills;

import java.util.EnumMap;
import java.util.Map;

import lumberjack.save.SaveData;
import lumberjack.skills.SkillProgressListener;
import lumberjack.skills.SkillType;
import lumberjack.skills.SkillXpTable;

/**
 * Tracks XP and levels for all core skills.
 */
public final class PlayerSkills {

    private final Map<SkillType, Integer> totalXpBySkill = new EnumMap<>(SkillType.class);
    private SkillProgressListener progressListener;

    public PlayerSkills() {
        reset();
    }

    public void reset() {
        totalXpBySkill.clear();
        for (SkillType skillType : SkillType.values()) {
            totalXpBySkill.put(skillType, 0);
        }
    }

    public int getTotalXp(SkillType skillType) {
        return totalXpBySkill.getOrDefault(skillType, 0);
    }

    public int getLevel(SkillType skillType) {
        return SkillXpTable.resolveLevel(getTotalXp(skillType));
    }

    public void setProgressListener(SkillProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public int getXpToNextLevel(SkillType skillType) {
        return SkillXpTable.xpToNextLevel(getTotalXp(skillType));
    }

    public void grantXp(SkillType skillType, int amount) {
        if (amount <= 0 || skillType == null) {
            return;
        }

        if (getLevel(skillType) >= SkillXpTable.getMaxLevel()) {
            return;
        }

        int previousLevel = getLevel(skillType);
        totalXpBySkill.merge(skillType, amount, Integer::sum);
        int newLevel = getLevel(skillType);

        if (progressListener != null) {
            progressListener.onXpGained(skillType, amount);
            if (newLevel > previousLevel) {
                progressListener.onSkillLevelUp(skillType, newLevel);
            }
        }
    }

    public void exportTo(SaveData data) {
        data.getSkillEntries().clear();
        for (SkillType skillType : SkillType.values()) {
            data.getSkillEntries().add(new SaveData.SkillEntry(skillType.getId(), getTotalXp(skillType)));
        }
    }

    public void importFrom(SaveData data) {
        reset();
        for (SaveData.SkillEntry entry : data.getSkillEntries()) {
            SkillType skillType = SkillType.fromId(entry.getSkillId());
            totalXpBySkill.put(skillType, Math.max(0, entry.getTotalXp()));
        }
    }
}
