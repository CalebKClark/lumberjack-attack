package lumberjack.ui.hud;

import java.awt.Rectangle;

import lumberjack.core.GameConfig;

/**
 * Shared layout math for inventory grids inside the player menu content area.
 */
public final class InventoryLayout {

    private InventoryLayout() {
    }

    public static int getColumns() {
        return GameConfig.HOTBAR_SLOTS;
    }

    public static int getRows() {
        return GameConfig.INVENTORY_SLOTS / getColumns();
    }

    public static int getGridWidth() {
        return getColumns() * HudTheme.SLOT_SIZE + (getColumns() - 1) * HudTheme.SLOT_GAP;
    }

    public static int getGridHeight() {
        return getRows() * HudTheme.SLOT_SIZE + (getRows() - 1) * HudTheme.SLOT_GAP;
    }

    public static int getGridStartX(Rectangle content) {
        return content.x + (content.width - getGridWidth()) / 2;
    }

    public static int getGridStartY(Rectangle content) {
        return content.y + (content.height - getGridHeight()) / 2;
    }

    public static Rectangle getSlotBounds(int slotIndex, Rectangle content) {
        int columns = getColumns();
        int row = slotIndex / columns;
        int col = slotIndex % columns;

        int x = getGridStartX(content) + col * (HudTheme.SLOT_SIZE + HudTheme.SLOT_GAP);
        int y = getGridStartY(content) + row * (HudTheme.SLOT_SIZE + HudTheme.SLOT_GAP);

        return new Rectangle(x, y, HudTheme.SLOT_SIZE, HudTheme.SLOT_SIZE);
    }

    /**
     * @return slot index 0-35, or -1 if the point is not over a slot
     */
    public static int getSlotAtPoint(int x, int y, Rectangle content) {
        for (int slot = 0; slot < GameConfig.INVENTORY_SLOTS; slot++) {
            if (getSlotBounds(slot, content).contains(x, y)) {
                return slot;
            }
        }

        return -1;
    }
}
