package lumberjack.interaction;

import lumberjack.combat.ReachUtils;
import lumberjack.core.GameConfig;
import lumberjack.entity.Player;
import lumberjack.game.GameSession;
import lumberjack.inventory.Inventory;
import lumberjack.item.ItemDefinition;
import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;
import lumberjack.world.map.TileMap;
import lumberjack.world.placeable.BedPlaceable;
import lumberjack.world.placeable.PlacedObjectKey;
import lumberjack.world.tile.TileDefinition;

import java.awt.Rectangle;

/**
 * Handles placing placeable items onto valid tiles.
 */
public final class PlaceService {

    public boolean tryPlaceAtWorldPosition(GameSession session, int worldX, int worldY) {
        Inventory inventory = session.getInventory();
        ItemRegistry itemRegistry = session.getItemRegistry();

        if (!isHoldingPlaceable(inventory, itemRegistry)) {
            return false;
        }

        TileMap map = session.getCurrentMap();
        int col = map.getColumnAtWorldX(worldX);
        int row = map.getRowAtWorldY(worldY);

        if (!map.isInBounds(col, row)) {
            return false;
        }

        ItemStack held = inventory.getSelectedHotbarStack();
        String itemId = held.getItemId();

        if (BedPlaceable.isBed(itemId)) {
            return tryPlaceBed(session, map, col, row, itemId);
        }

        TileDefinition tile = session.getTileRegistry().get(map.getTileIdAt(col, row));
        if (!tile.isPlaceable()) {
            return false;
        }

        String mapId = session.getCurrentMapId();
        if (session.getPlacedObjectManager().hasAt(mapId, col, row)) {
            return false;
        }

        Player player = session.getPlayer();
        int tileCenterX = map.getTileCenterX(col);
        int tileCenterY = map.getTileCenterY(row);
        if (!ReachUtils.isWithinReach(player, tileCenterX, tileCenterY)) {
            return false;
        }

        if (player.getBounds().intersects(placementHitbox(col, row))) {
            return false;
        }

        consumeOneFromSelectedHotbar(inventory);
        session.getPlacedObjectManager().place(mapId, itemId, col, row);
        registerPlacedStorage(session, mapId, col, row, itemId);
        return true;
    }

    private boolean tryPlaceBed(GameSession session, TileMap map, int footCol, int footRow, String itemId) {
        if (!BedPlaceable.canPlaceAt(map, session.getTileRegistry(), footCol, footRow)) {
            return false;
        }

        String mapId = session.getCurrentMapId();
        int headRow = BedPlaceable.headRow(footRow);
        if (session.getPlacedObjectManager().hasAt(mapId, footCol, footRow)
                || session.getPlacedObjectManager().hasAt(mapId, footCol, headRow)) {
            return false;
        }

        Player player = session.getPlayer();
        int tileCenterX = map.getTileCenterX(footCol);
        int tileCenterY = (map.getTileCenterY(footRow) + map.getTileCenterY(headRow)) / 2;
        if (!ReachUtils.isWithinReach(player, tileCenterX, tileCenterY)) {
            return false;
        }

        if (player.getBounds().intersects(bedFootprintBounds(footCol, footRow))) {
            return false;
        }

        consumeOneFromSelectedHotbar(session.getInventory());
        session.getPlacedObjectManager().place(mapId, itemId, footCol, footRow);
        return true;
    }

    private static Rectangle placementHitbox(int col, int row) {
        int size = GameConfig.PLACEABLE_HITBOX_SIZE;
        int pad = (GameConfig.TILE_SIZE - size) / 2;
        return new Rectangle(
                col * GameConfig.TILE_SIZE + pad,
                row * GameConfig.TILE_SIZE + pad,
                size,
                size
        );
    }

    private static Rectangle bedFootprintBounds(int footCol, int footRow) {
        return new Rectangle(
                footCol * GameConfig.TILE_SIZE,
                BedPlaceable.headRow(footRow) * GameConfig.TILE_SIZE,
                BedPlaceable.SPRITE_WIDTH,
                BedPlaceable.SPRITE_HEIGHT
        );
    }

    private void registerPlacedStorage(GameSession session, String mapId, int col, int row, String itemId) {
        PlacedObjectKey key = new PlacedObjectKey(mapId, col, row);
        if (lumberjack.world.placeable.WoodChipperMachine.MACHINE_ITEM_ID.equals(itemId)) {
            session.getWoodChipperManager().getOrCreate(key);
        } else if (lumberjack.world.placeable.ChestManager.CHEST_ITEM_ID.equals(itemId)) {
            session.getChestManager().getOrCreate(key);
        }
    }

    private boolean isHoldingPlaceable(Inventory inventory, ItemRegistry itemRegistry) {
        ItemStack held = inventory.getSelectedHotbarStack();
        if (held.isEmpty()) {
            return false;
        }

        ItemDefinition item = itemRegistry.get(held.getItemId());
        return item.isPlaceable();
    }

    private void consumeOneFromSelectedHotbar(Inventory inventory) {
        int slotIndex = inventory.getSelectedHotbarSlot();
        ItemStack held = inventory.getSlot(slotIndex);
        if (held.isEmpty()) {
            return;
        }

        if (held.getQuantity() <= 1) {
            inventory.setSlot(slotIndex, ItemStack.empty());
        } else {
            held.setQuantity(held.getQuantity() - 1);
        }
    }
}
