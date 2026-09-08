package lumberjack.ui.hud;

import java.awt.Rectangle;

/**
 * Grid layout math for arbitrary slot grids (chests, machine panels, etc.).
 */
public final class SlotGridLayout {

    private SlotGridLayout() {
    }

    public static int getGridWidth(int columns) {
        return columns * HudTheme.SLOT_SIZE + (columns - 1) * HudTheme.SLOT_GAP;
    }

    public static int getGridHeight(int rows) {
        return rows * HudTheme.SLOT_SIZE + (rows - 1) * HudTheme.SLOT_GAP;
    }

    public static int getGridStartX(Rectangle content, int columns) {
        int gridWidth = getGridWidth(columns);
        return content.x + (content.width - gridWidth) / 2;
    }

    public static int getGridStartY(Rectangle content, int rows) {
        int gridHeight = getGridHeight(rows);
        return content.y + (content.height - gridHeight) / 2;
    }

    public static Rectangle getSlotBounds(int slotIndex, Rectangle content, int columns, int slotCount) {
        int rows = slotCount / columns;
        int row = slotIndex / columns;
        int col = slotIndex % columns;

        int x = getGridStartX(content, columns) + col * (HudTheme.SLOT_SIZE + HudTheme.SLOT_GAP);
        int y = getGridStartY(content, rows) + row * (HudTheme.SLOT_SIZE + HudTheme.SLOT_GAP);

        return new Rectangle(x, y, HudTheme.SLOT_SIZE, HudTheme.SLOT_SIZE);
    }

    public static int getSlotAtPoint(int x, int y, Rectangle content, int columns, int slotCount) {
        for (int slot = 0; slot < slotCount; slot++) {
            if (getSlotBounds(slot, content, columns, slotCount).contains(x, y)) {
                return slot;
            }
        }
        return -1;
    }
}
