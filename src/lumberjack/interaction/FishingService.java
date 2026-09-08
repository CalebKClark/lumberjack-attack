package lumberjack.interaction;

import lumberjack.combat.ReachUtils;
import lumberjack.entity.Player;
import lumberjack.game.GameSession;
import lumberjack.inventory.Inventory;
import lumberjack.item.ItemDefinition;
import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;
import lumberjack.world.map.TileMap;

/**
 * Handles casting a fishing line onto water tiles.
 */
public final class FishingService {

    public boolean tryCastAtWorldPosition(GameSession session, int worldX, int worldY) {
        if (!session.getFishingManager().isBobberIdle()) {
            return false;
        }

        if (!isHoldingRod(session.getInventory(), session.getItemRegistry())) {
            return false;
        }

        TileMap map = session.getCurrentMap();
        int col = map.getColumnAtWorldX(worldX);
        int row = map.getRowAtWorldY(worldY);

        if (!map.isInBounds(col, row)) {
            return false;
        }

        if (!session.getTileRegistry().get(map.getTileIdAt(col, row)).isFishable()) {
            return false;
        }

        Player player = session.getPlayer();
        int tileCenterX = map.getTileCenterX(col);
        int tileCenterY = map.getTileCenterY(row);

        if (!ReachUtils.isWithinReach(player, tileCenterX, tileCenterY)) {
            return false;
        }

        if (!session.trySpendCastEnergy()) {
            return true; // click handled; blocked by fatigue
        }

        player.faceToward(tileCenterX, tileCenterY);
        player.triggerRodCast();

        session.getFishingManager().beginPendingCast(tileCenterX, tileCenterY);
        return true;
    }

    private boolean isHoldingRod(Inventory inventory, ItemRegistry itemRegistry) {
        ItemStack held = inventory.getSelectedHotbarStack();
        if (held.isEmpty()) {
            return false;
        }

        ItemDefinition item = itemRegistry.get(held.getItemId());
        return item.isFishingRod();
    }
}
