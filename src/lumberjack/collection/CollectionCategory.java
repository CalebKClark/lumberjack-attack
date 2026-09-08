package lumberjack.collection;

/**
 * Collection groupings shown in the Collections tab.
 */
public enum CollectionCategory {

    WOOD("Wood"),
    COMBAT("Combat"),
    FISH("Fish");

    private final String displayName;

    CollectionCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CollectionCategory fromId(String id) {
        return CollectionCategory.valueOf(id.trim().toUpperCase());
    }
}
