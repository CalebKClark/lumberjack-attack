package lumberjack.interaction;

import lumberjack.combat.ReachUtils;
import lumberjack.core.GameConfig;
import lumberjack.entity.PlacedObject;
import lumberjack.entity.Player;
import lumberjack.game.GameSession;
import lumberjack.inventory.Inventory;
import lumberjack.inventory.ItemContainer;
import lumberjack.item.ItemDefinition;
import lumberjack.item.ItemStack;
import lumberjack.world.map.TileMap;
import lumberjack.world.placeable.BedPlaceable;
import lumberjack.world.placeable.ChestManager;
import lumberjack.world.placeable.PlacedObjectKey;
import lumberjack.world.placeable.WoodChipperMachine;

/**
 * Axe-breaks placed objects (chest, wood chipper) using the same swing lock /
 * hit-on-finish rules as tree chopping. Containers must be empty first.
 */
public final class PlaceableBreakService {

    private PendingBreak pendingBreak;

    public boolean tryBreakAtWorldPosition(GameSession session, int worldX, int worldY) {
        Player player = session.getPlayer();
        if (player.isActionLocked() || pendingBreak != null) {
            return false;
        }

        if (!isHoldingAxe(session.getInventory(), session.getItemRegistry())) {
            return false;
        }

        String mapId = session.getCurrentMapId();
        PlacedObject placed = session.getPlacedObjectManager().findAtWorldPosition(mapId, worldX, worldY);
        if (placed == null) {
            return false;
        }

        if (!ReachUtils.isWithinReach(player, placed)) {
            return false;
        }

        PlacedObjectKey key = new PlacedObjectKey(mapId, placed.getCol(), placed.getRow());
        if (!isBreakableWhenEmpty(session, key, placed.getItemId())) {
            return false;
        }

        if (!session.trySpendActionEnergy()) {
            return true;
        }

        player.faceToward(
                placed.getX() + placed.getWidth() / 2,
                placed.getY() + placed.getHeight() / 2
        );
        player.triggerAxeSwing();
        pendingBreak = new PendingBreak(mapId, placed.getCol(), placed.getRow(), placed.getItemId());
        return true;
    }

    public void onAxeSwingCompleted(GameSession session, boolean swingCompleted) {
        if (!swingCompleted || pendingBreak == null) {
            return;
        }

        PendingBreak pending = pendingBreak;
        pendingBreak = null;
        applyBreak(session, pending);
    }

    private void applyBreak(GameSession session, PendingBreak pending) {
        if (!pending.mapId.equals(session.getCurrentMapId())) {
            return;
        }

        PlacedObjectKey key = new PlacedObjectKey(pending.mapId, pending.col, pending.row);
        if (!session.getPlacedObjectManager().hasAt(pending.mapId, pending.col, pending.row)) {
            return;
        }
        if (!isBreakableWhenEmpty(session, key, pending.itemId)) {
            return;
        }

        if (key.equals(session.getActiveChestKey())) {
            session.closeChest();
        }
        if (key.equals(session.getActiveWoodChipperKey())) {
            session.closeWoodChipper();
        }

        PlacedObject removed = session.getPlacedObjectManager().removeAt(pending.mapId, pending.col, pending.row);
        session.getChestManager().remove(key);
        session.getWoodChipperManager().remove(key);
        if (removed == null) {
            return;
        }

        // World-edit / seeded beds leave bed tiles under the placeable — restore cabin floor.
        if (BedPlaceable.isBed(pending.itemId)) {
            clearBedTilesUnderPlaceable(session.getCurrentMap(), pending.col, pending.row);
        }

        session.getDroppedItems().spawn(
                pending.itemId,
                1,
                removed.getX() + removed.getWidth() / 2,
                removed.getY() + removed.getHeight() / 2
        );
    }

    private static void clearBedTilesUnderPlaceable(TileMap map, int footCol, int footRow) {
        int headRow = BedPlaceable.headRow(footRow);
        clearBedTileIfPresent(map, footCol, footRow);
        clearBedTileIfPresent(map, footCol, headRow);
    }

    private static void clearBedTileIfPresent(TileMap map, int col, int row) {
        if (!map.isInBounds(col, row)) {
            return;
        }
        if (BedPlaceable.isBedTile(map.getTileIdAt(col, row))) {
            map.setTileIdAt(col, row, GameConfig.CABIN_FLOOR_TILE_ID);
        }
    }

    private boolean isBreakableWhenEmpty(GameSession session, PlacedObjectKey key, String itemId) {
        if (ChestManager.CHEST_ITEM_ID.equals(itemId)) {
            ItemContainer chest = session.getChestManager().get(key);
            return chest == null || chest.isEmpty();
        }
        if (WoodChipperMachine.MACHINE_ITEM_ID.equals(itemId)) {
            WoodChipperMachine machine = session.getWoodChipperManager().get(key);
            return machine == null || machine.isEmpty();
        }
        return true;
    }

    private boolean isHoldingAxe(Inventory inventory, lumberjack.item.ItemRegistry itemRegistry) {
        ItemStack held = inventory.getSelectedHotbarStack();
        if (held.isEmpty()) {
            return false;
        }
        ItemDefinition item = itemRegistry.get(held.getItemId());
        return item.isAxe();
    }

    private record PendingBreak(String mapId, int col, int row, String itemId) {
    }
}
