package lumberjack.ui.hud;

import lumberjack.game.GameSession;

/**
 * Shared left/right click routing for inventory slots in overlay UIs.
 */
public final class InventoryUiClicks {

    private InventoryUiClicks() {
    }

    public static void handlePlayerInventorySlot(GameSession session, int slotIndex, boolean rightClick) {
        if (rightClick) {
            session.getInventoryInteraction().handleRightClick(
                    session.getInventory(),
                    session.getItemRegistry(),
                    slotIndex
            );
        } else {
            session.getInventoryInteraction().handleLeftClick(
                    session.getInventory(),
                    session.getItemRegistry(),
                    slotIndex
            );
        }
    }
}
