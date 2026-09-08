package lumberjack.ui.hud;

import java.awt.Rectangle;

import lumberjack.core.GameConfig;

/**
 * Layout helpers for the bottom hotbar.
 */
public final class HotbarLayout {

    private HotbarLayout() {
    }

    public static Rectangle getSlotBounds(int slotIndex, int screenWidth, int screenHeight) {
        int slotCount = GameConfig.HOTBAR_SLOTS;
        int barWidth = slotCount * HudTheme.SLOT_SIZE + (slotCount - 1) * HudTheme.SLOT_GAP;
        int startX = (screenWidth - barWidth) / 2;
        int y = screenHeight - HudTheme.SLOT_SIZE - HudTheme.HOTBAR_BOTTOM_MARGIN;
        int x = startX + slotIndex * (HudTheme.SLOT_SIZE + HudTheme.SLOT_GAP);
        return new Rectangle(x, y, HudTheme.SLOT_SIZE, HudTheme.SLOT_SIZE);
    }

    public static int getSlotAtPoint(int x, int y, int screenWidth, int screenHeight) {
        for (int slot = 0; slot < GameConfig.HOTBAR_SLOTS; slot++) {
            if (getSlotBounds(slot, screenWidth, screenHeight).contains(x, y)) {
                return slot;
            }
        }
        return -1;
    }
}
