package lumberjack.skills;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads per-level reward descriptions from /skills/skill_levels.csv.
 */
public final class SkillLevelRegistry {

    private static final String DEFAULT_PATH = "/skills/skill_levels.csv";

    private final Map<SkillType, Map<Integer, SkillLevelDefinition>> levelsBySkill = new EnumMap<>(SkillType.class);

    public SkillLevelRegistry() {
        this(DEFAULT_PATH);
    }

    public SkillLevelRegistry(String resourcePath) {
        for (SkillType skillType : SkillType.values()) {
            levelsBySkill.put(skillType, new HashMap<>());
        }
        load(resourcePath);
        fillMissingDefaults();
    }

    public SkillLevelDefinition get(SkillType skillType, int level) {
        return levelsBySkill.get(skillType).get(level);
    }

    public String getRewardText(SkillType skillType, int level) {
        SkillLevelDefinition definition = get(skillType, level);
        if (definition == null) {
            return defaultReward(skillType, level);
        }
        return definition.getReward();
    }

    private void load(String resourcePath) {
        try (InputStream input = SkillLevelRegistry.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Skill level data not found: " + resourcePath);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                reader.readLine(); // header

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }

                    String[] parts = line.split(",", 3);
                    SkillType skillType = SkillType.fromId(parts[0].trim());
                    int level = Integer.parseInt(parts[1].trim());
                    String reward = parts[2].trim();

                    levelsBySkill.get(skillType).put(level, new SkillLevelDefinition(skillType, level, reward));
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load skill level data: " + resourcePath, exception);
        }
    }

    private void fillMissingDefaults() {
        for (SkillType skillType : SkillType.values()) {
            Map<Integer, SkillLevelDefinition> levels = levelsBySkill.get(skillType);
            for (int level = 1; level <= SkillXpTable.getMaxLevel(); level++) {
                if (!levels.containsKey(level)) {
                    levels.put(level, new SkillLevelDefinition(
                            skillType,
                            level,
                            defaultReward(skillType, level)
                    ));
                }
            }
        }
    }

    private String defaultReward(SkillType skillType, int level) {
        if (level % 5 == 0) {
            return "Major " + skillType.getDisplayName().toLowerCase() + " milestone";
        }
        return "Minor " + skillType.getDisplayName().toLowerCase() + " improvement";
    }
}
