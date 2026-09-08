package lumberjack.collection;

/**
 * One discoverable entry in the Collections tab.
 */
public final class CollectionEntryDefinition {

    private final CollectionCategory category;
    private final String entryId;
    private final String displayName;
    private final DiscoveryType discoveryType;
    private final String discoveryId;
    private final String tooltip;
    private final int sortOrder;

    public CollectionEntryDefinition(
            CollectionCategory category,
            String entryId,
            String displayName,
            DiscoveryType discoveryType,
            String discoveryId,
            String tooltip,
            int sortOrder
    ) {
        this.category = category;
        this.entryId = entryId;
        this.displayName = displayName;
        this.discoveryType = discoveryType;
        this.discoveryId = discoveryId;
        this.tooltip = tooltip == null ? "" : tooltip;
        this.sortOrder = sortOrder;
    }

    public CollectionCategory getCategory() {
        return category;
    }

    public String getEntryId() {
        return entryId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public DiscoveryType getDiscoveryType() {
        return discoveryType;
    }

    public String getDiscoveryId() {
        return discoveryId;
    }

    public String getTooltip() {
        return tooltip;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean usesFishStats() {
        return category == CollectionCategory.FISH;
    }
}
