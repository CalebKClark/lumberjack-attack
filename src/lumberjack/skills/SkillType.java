package lumberjack.skills;

/**
 * Core player skills. Add new entries here as gameplay systems ship.
 */
public enum SkillType {

    FORAGING("Foraging"),
    COMBAT("Combat"),
    FISHING("Fishing");

    private final String displayName;

    SkillType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SkillType fromId(String id) {
        return SkillType.valueOf(id.trim().toUpperCase());
    }

    public String getId() {
        return name().toLowerCase();
    }
}
