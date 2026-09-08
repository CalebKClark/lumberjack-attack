package lumberjack.world.tile;

import java.awt.Color;

/**
 * Immutable definition of a tile type loaded from content data.
 * Rendering uses placeholder colors until sprite sheets exist.
 */
public final class TileDefinition {

    private final int id;
    private final String name;
    private final boolean solid;
    private final Color debugColor;
    private final boolean choppable;
    private final String dropItemId;
    private final int regrowthGameMinutes;
    private final boolean bed;
    private final boolean placeable;
    private final boolean fishable;
    private final int foragingXp;
    private final int toughness;
    private final int dropQuantityMin;
    private final int dropQuantityMax;

    public TileDefinition(
            int id,
            String name,
            boolean solid,
            Color debugColor,
            boolean choppable,
            String dropItemId,
            int regrowthGameMinutes,
            boolean bed,
            boolean placeable,
            boolean fishable,
            int foragingXp,
            int toughness,
            int dropQuantityMin,
            int dropQuantityMax
    ) {
        this.id = id;
        this.name = name;
        this.solid = solid;
        this.debugColor = debugColor;
        this.choppable = choppable;
        this.dropItemId = dropItemId;
        this.regrowthGameMinutes = regrowthGameMinutes;
        this.bed = bed;
        this.placeable = placeable;
        this.fishable = fishable;
        this.foragingXp = foragingXp;
        this.toughness = Math.max(0, toughness);
        this.dropQuantityMin = Math.max(0, dropQuantityMin);
        this.dropQuantityMax = Math.max(this.dropQuantityMin, dropQuantityMax);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isSolid() {
        return solid;
    }

    public Color getDebugColor() {
        return debugColor;
    }

    public boolean isChoppable() {
        return choppable;
    }

    /**
     * Item id dropped when this tile is chopped (from items.csv).
     */
    public String getDropItemId() {
        return dropItemId;
    }

    /** In-game minutes before this tile regrows after being chopped; 0 means no regrowth. */
    public int getRegrowthGameMinutes() {
        return regrowthGameMinutes;
    }

    public boolean isBed() {
        return bed;
    }

    public boolean isPlaceable() {
        return placeable;
    }

    public boolean isFishable() {
        return fishable;
    }

    public int getForagingXp() {
        return foragingXp;
    }

    /** Hits (at 1 tree-damage) needed to fell; 0/1 means single-hit. */
    public int getToughness() {
        return toughness;
    }

    public int getDropQuantityMin() {
        return dropQuantityMin;
    }

    public int getDropQuantityMax() {
        return dropQuantityMax;
    }

    /** Random drop count for a felled tile; defaults to 1 when unset. */
    public int rollDropQuantity() {
        if (dropQuantityMax <= 0) {
            return 1;
        }
        int min = Math.max(1, dropQuantityMin);
        if (dropQuantityMax <= min) {
            return min;
        }
        return min + (int) (Math.random() * (dropQuantityMax - min + 1));
    }
}
