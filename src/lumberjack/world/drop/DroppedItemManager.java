package lumberjack.world.drop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lumberjack.entity.DroppedItem;
import lumberjack.entity.Player;
import lumberjack.entity.PlayerAttributes;
import lumberjack.inventory.Inventory;
import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;
import lumberjack.core.GameConfig;
import lumberjack.save.SaveData;

/**
 * Tracks ground loot per map. Switching maps keeps other maps' drops intact.
 */
public final class DroppedItemManager {

    private String activeMapId;
    private final Map<String, List<DroppedItem>> dropsByMap = new HashMap<>();

    public void setActiveMap(String mapId) {
        if (mapId == null || mapId.isBlank()) {
            throw new IllegalArgumentException("Active drop map id is required");
        }
        this.activeMapId = mapId;
    }

    public String getActiveMapId() {
        return activeMapId;
    }

    private List<DroppedItem> activeDrops() {
        if (activeMapId == null) {
            throw new IllegalStateException("Active drop map has not been set");
        }
        return dropsByMap.computeIfAbsent(activeMapId, id -> new ArrayList<>());
    }

    public void spawn(String itemId, int quantity, int worldCenterX, int worldCenterY) {
        activeDrops().add(new DroppedItem(ItemStack.of(itemId, quantity), worldCenterX, worldCenterY, true));
    }

    /**
     * Spawns many single-item pieces that rain from the canopy and land at stump height.
     */
    public void spawnCanopyRain(
            String itemId,
            int quantity,
            float treeLeft,
            float treeTop,
            float treeWidth,
            float treeHeight,
            float landCenterY
    ) {
        int count = Math.max(1, quantity);
        float stumpOverlap = Math.min(treeHeight - 1f, GameConfig.PINE_TREE_STUMP_OVERLAP_PX);
        float canopyHeight = Math.max(8f, treeHeight - stumpOverlap);
        float stumpLeft = treeLeft + (treeWidth - GameConfig.TILE_SIZE) / 2f;
        List<DroppedItem> drops = activeDrops();

        for (int i = 0; i < count; i++) {
            float startX = treeLeft + (float) (Math.random() * treeWidth);
            float startY = treeTop + (float) (Math.random() * canopyHeight);
            float landX = stumpLeft + (float) (Math.random() * GameConfig.TILE_SIZE);
            double delay = Math.random() * GameConfig.TREE_RAIN_MAX_DELAY_MS;
            double fallMs = GameConfig.TREE_RAIN_FALL_MIN_MS
                    + Math.random() * (GameConfig.TREE_RAIN_FALL_MAX_MS - GameConfig.TREE_RAIN_FALL_MIN_MS);
            drops.add(DroppedItem.canopyRain(
                    ItemStack.of(itemId, 1),
                    startX,
                    startY,
                    landX,
                    landCenterY,
                    delay,
                    fallMs
            ));
        }
    }

    /** Clears loot on every map (load / new game). */
    public void clear() {
        dropsByMap.clear();
    }

    public void restoreDrop(String mapId, String itemId, int quantity, int centerX, int centerY) {
        String resolvedMapId = (mapId == null || mapId.isBlank()) ? activeMapId : mapId;
        if (resolvedMapId == null || resolvedMapId.isBlank()) {
            throw new IllegalStateException("Cannot restore drop without a map id");
        }
        dropsByMap
                .computeIfAbsent(resolvedMapId, id -> new ArrayList<>())
                .add(new DroppedItem(ItemStack.of(itemId, quantity), centerX, centerY, false));
    }

    public List<SaveData.DropEntry> exportDrops() {
        List<SaveData.DropEntry> entries = new ArrayList<>();

        for (Map.Entry<String, List<DroppedItem>> mapEntry : dropsByMap.entrySet()) {
            String mapId = mapEntry.getKey();
            for (DroppedItem drop : mapEntry.getValue()) {
                if (drop.getStack().isEmpty()) {
                    continue;
                }
                entries.add(new SaveData.DropEntry(
                        mapId,
                        drop.getStack().getItemId(),
                        drop.getStack().getQuantity(),
                        Math.round(drop.getCenterX()),
                        Math.round(drop.getCenterY())
                ));
            }
        }

        return entries;
    }

    public void update(
            Player player,
            PlayerAttributes attributes,
            Inventory inventory,
            ItemRegistry itemRegistry,
            double elapsedMs
    ) {
        double magnetRadiusPx = attributes.getPickupMagnetPixels();
        List<DroppedItem> drops = activeDrops();

        Iterator<DroppedItem> iterator = drops.iterator();
        while (iterator.hasNext()) {
            DroppedItem drop = iterator.next();
            drop.update(elapsedMs);

            boolean collectReady = drop.updateMagnet(
                    player.getX(),
                    player.getY(),
                    player.getWidth(),
                    player.getHeight(),
                    magnetRadiusPx,
                    elapsedMs
            );
            if (collectReady && drop.tryCollect(inventory, itemRegistry)) {
                iterator.remove();
            }
        }
    }

    /** Drops on the currently active map (for rendering). */
    public List<DroppedItem> getDrops() {
        if (activeMapId == null) {
            return List.of();
        }
        return List.copyOf(activeDrops());
    }
}
