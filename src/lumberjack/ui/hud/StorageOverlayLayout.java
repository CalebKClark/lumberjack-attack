package lumberjack.ui.hud;

import java.awt.Rectangle;

import lumberjack.inventory.ItemContainer;
import lumberjack.inventory.Inventory;

/**
 * Layout for storage overlays: container grid on top, player inventory below.
 */
public final class StorageOverlayLayout {

    public static final int PANEL_PADDING = 32;
    public static final int SECTION_GAP = 28;
    public static final int DIVIDER_HEIGHT = 2;
    public static final int SECTION_HEADER = 36;

    private StorageOverlayLayout() {
    }

    public static int getPanelWidth() {
        return Math.max(
                SlotGridLayout.getGridWidth(ItemContainer.CHEST_COLUMNS) + PANEL_PADDING * 2,
                InventoryLayout.getGridWidth() + PANEL_PADDING * 2
        );
    }

    public static Rectangle getPanelBounds(int screenWidth, int screenHeight) {
        int width = Math.min(getPanelWidth(), screenWidth - HudTheme.INVENTORY_PANEL_PADDING * 2);
        int chestRows = ItemContainer.CHEST_SLOT_COUNT / ItemContainer.CHEST_COLUMNS;
        int chestSectionHeight = SECTION_HEADER + SlotGridLayout.getGridHeight(chestRows);
        int inventoryHeight = SECTION_HEADER + InventoryLayout.getGridHeight();
        int height = PANEL_PADDING * 2 + chestSectionHeight + SECTION_GAP + DIVIDER_HEIGHT
                + SECTION_GAP + inventoryHeight;
        height = Math.min(height, screenHeight - HudTheme.INVENTORY_PANEL_PADDING * 2);
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height) / 2;
        return new Rectangle(x, y, width, height);
    }

    public static Rectangle getContainerSectionBounds(Rectangle panel) {
        int chestRows = ItemContainer.CHEST_SLOT_COUNT / ItemContainer.CHEST_COLUMNS;
        int height = SECTION_HEADER + SlotGridLayout.getGridHeight(chestRows);
        return new Rectangle(
                panel.x + PANEL_PADDING,
                panel.y + PANEL_PADDING,
                panel.width - PANEL_PADDING * 2,
                height
        );
    }

    public static Rectangle getContainerContentBounds(Rectangle panel) {
        Rectangle section = getContainerSectionBounds(panel);
        int y = section.y + SECTION_HEADER;
        int height = section.height - SECTION_HEADER;
        return new Rectangle(section.x, y, section.width, height);
    }

    public static Rectangle getDividerBounds(Rectangle panel) {
        Rectangle containerSection = getContainerSectionBounds(panel);
        int y = containerSection.y + containerSection.height + SECTION_GAP / 2;
        return new Rectangle(panel.x + PANEL_PADDING, y, panel.width - PANEL_PADDING * 2, DIVIDER_HEIGHT);
    }

    public static Rectangle getInventorySectionBounds(Rectangle panel) {
        Rectangle divider = getDividerBounds(panel);
        int y = divider.y + DIVIDER_HEIGHT + SECTION_GAP / 2;
        int height = panel.y + panel.height - PANEL_PADDING - y;
        return new Rectangle(panel.x + PANEL_PADDING, y, panel.width - PANEL_PADDING * 2, height);
    }

    public static Rectangle getInventoryContentBounds(Rectangle panel) {
        Rectangle section = getInventorySectionBounds(panel);
        int y = section.y + SECTION_HEADER;
        int height = section.height - SECTION_HEADER;
        return new Rectangle(section.x, y, section.width, height);
    }

    public static int getContainerSlotAtPoint(int x, int y, Rectangle panel) {
        Rectangle content = getContainerContentBounds(panel);
        if (!content.contains(x, y)) {
            return -1;
        }
        return SlotGridLayout.getSlotAtPoint(
                x,
                y,
                content,
                ItemContainer.CHEST_COLUMNS,
                ItemContainer.CHEST_SLOT_COUNT
        );
    }

    public static int getInventorySlotAtPoint(int x, int y, Rectangle panel) {
        Rectangle content = getInventoryContentBounds(panel);
        if (!content.contains(x, y)) {
            return -1;
        }
        return InventoryLayout.getSlotAtPoint(x, y, content);
    }
}
