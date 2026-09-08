package lumberjack.skills;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import lumberjack.core.GameConfig;

/**
 * Loads cumulative XP thresholds per level from /skills/skill_xp.csv.
 * Level 1 starts at 0 total XP; each row defines total XP required to reach that level.
 */
public final class SkillXpTable {

    private static final String DEFAULT_PATH = "/skills/skill_xp.csv";
    private static final SkillXpTable DEFAULT = new SkillXpTable();

    private final int[] totalXpByLevel;

    public SkillXpTable() {
        this(DEFAULT_PATH);
    }

    public SkillXpTable(String resourcePath) {
        totalXpByLevel = new int[GameConfig.SKILL_MAX_LEVEL + 1];
        load(resourcePath);
        validate();
    }

    public static int getMaxLevel() {
        return DEFAULT.getMaxLevelInternal();
    }

    public static int totalXpRequiredForLevel(int level) {
        return DEFAULT.totalXpRequiredForLevelInternal(level);
    }

    public static int resolveLevel(int totalXp) {
        return DEFAULT.resolveLevelInternal(totalXp);
    }

    public static int xpToNextLevel(int totalXp) {
        return DEFAULT.xpToNextLevelInternal(totalXp);
    }

    private int getMaxLevelInternal() {
        return GameConfig.SKILL_MAX_LEVEL;
    }

    private int totalXpRequiredForLevelInternal(int level) {
        if (level <= 1) {
            return 0;
        }
        if (level > getMaxLevelInternal()) {
            return totalXpByLevel[getMaxLevelInternal()];
        }
        return totalXpByLevel[level];
    }

    private int resolveLevelInternal(int totalXp) {
        int level = 1;
        for (int candidate = 2; candidate <= getMaxLevelInternal(); candidate++) {
            if (totalXp >= totalXpRequiredForLevelInternal(candidate)) {
                level = candidate;
            } else {
                break;
            }
        }
        return level;
    }

    private int xpToNextLevelInternal(int totalXp) {
        int level = resolveLevelInternal(totalXp);
        if (level >= getMaxLevelInternal()) {
            return 0;
        }
        return totalXpRequiredForLevelInternal(level + 1) - totalXp;
    }

    private void load(String resourcePath) {
        Arrays.fill(totalXpByLevel, 0);

        try (InputStream input = SkillXpTable.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Skill XP data not found: " + resourcePath);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                reader.readLine(); // header

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }

                    String[] parts = line.split(",");
                    int level = Integer.parseInt(parts[0].trim());
                    int totalXp = Integer.parseInt(parts[1].trim());

                    if (level < 1 || level > getMaxLevelInternal()) {
                        throw new IllegalStateException("Skill XP level out of range: " + level);
                    }

                    totalXpByLevel[level] = totalXp;
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load skill XP data: " + resourcePath, exception);
        }
    }

    private void validate() {
        if (totalXpByLevel[1] != 0) {
            throw new IllegalStateException("Skill level 1 must start at 0 total XP.");
        }

        for (int level = 2; level <= getMaxLevelInternal(); level++) {
            if (totalXpByLevel[level] <= totalXpByLevel[level - 1]) {
                throw new IllegalStateException(
                        "Skill XP thresholds must increase: level " + (level - 1)
                                + " -> " + level
                );
            }
        }
    }
}
