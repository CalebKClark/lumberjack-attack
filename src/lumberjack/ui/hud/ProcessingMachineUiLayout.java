package lumberjack.ui.hud;

import java.awt.Rectangle;

/**
 * Shared layout for processing-machine overlays (wood chipper and future machines).
 * Split into a machine section on top and a player inventory section below.
 */
public final class ProcessingMachineUiLayout {

    public static final int PANEL_PADDING = 32;
    public static final int SECTION_GAP = 28;
    public static final int DIVIDER_HEIGHT = 2;
    public static final int INVENTORY_SECTION_HEADER = 36;
    public static final int MACHINE_ROW_HEIGHT = HudTheme.SLOT_SIZE + 24;
    public static final int ARROW_WIDTH = 88;
    public static final int ARROW_HEIGHT = 24;
    public static final int SLOT_GAP = 24;

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    private ProcessingMachineUiLayout() {
    }

    public static int getPanelWidth() {
        return InventoryLayout.getGridWidth() + PANEL_PADDING * 2;
    }

    public static Rectangle getPanelBounds(int screenWidth, int screenHeight) {
        int width = Math.min(getPanelWidth(), screenWidth - HudTheme.INVENTORY_PANEL_PADDING * 2);
        int machineSectionHeight = HudTheme.INVENTORY_TITLE_HEIGHT + MACHINE_ROW_HEIGHT;
        int inventoryHeight = INVENTORY_SECTION_HEADER + InventoryLayout.getGridHeight();
        int height = PANEL_PADDING * 2 + machineSectionHeight + SECTION_GAP + DIVIDER_HEIGHT
                + SECTION_GAP + inventoryHeight;
        height = Math.min(height, screenHeight - HudTheme.INVENTORY_PANEL_PADDING * 2);
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height) / 2;
        return new Rectangle(x, y, width, height);
    }

    public static Rectangle getMachineSectionBounds(Rectangle panel) {
        int height = HudTheme.INVENTORY_TITLE_HEIGHT + MACHINE_ROW_HEIGHT;
        return new Rectangle(
                panel.x + PANEL_PADDING,
                panel.y + PANEL_PADDING,
                panel.width - PANEL_PADDING * 2,
                height
        );
    }

    public static Rectangle getDividerBounds(Rectangle panel) {
        Rectangle machineSection = getMachineSectionBounds(panel);
        int y = machineSection.y + machineSection.height + SECTION_GAP / 2;
        return new Rectangle(panel.x + PANEL_PADDING, y, panel.width - PANEL_PADDING * 2, DIVIDER_HEIGHT);
    }

    public static Rectangle getInventorySectionBounds(Rectangle panel) {
        Rectangle divider = getDividerBounds(panel);
        int y = divider.y + DIVIDER_HEIGHT + SECTION_GAP / 2;
        int height = panel.y + panel.height - PANEL_PADDING - y;
        return new Rectangle(panel.x + PANEL_PADDING, y, panel.width - PANEL_PADDING * 2, height);
    }

    public static Rectangle getMachineRowBounds(Rectangle panel) {
        Rectangle section = getMachineSectionBounds(panel);
        int rowWidth = HudTheme.SLOT_SIZE * 2 + SLOT_GAP * 2 + ARROW_WIDTH;
        int x = section.x + (section.width - rowWidth) / 2;
        int y = section.y + HudTheme.INVENTORY_TITLE_HEIGHT;
        return new Rectangle(x, y, rowWidth, MACHINE_ROW_HEIGHT);
    }

    public static Rectangle getInputSlotBounds(Rectangle panel) {
        Rectangle row = getMachineRowBounds(panel);
        return new Rectangle(
                row.x,
                row.y + (row.height - HudTheme.SLOT_SIZE) / 2,
                HudTheme.SLOT_SIZE,
                HudTheme.SLOT_SIZE
        );
    }

    public static Rectangle getOutputSlotBounds(Rectangle panel) {
        Rectangle row = getMachineRowBounds(panel);
        int x = row.x + row.width - HudTheme.SLOT_SIZE;
        return new Rectangle(
                x,
                row.y + (row.height - HudTheme.SLOT_SIZE) / 2,
                HudTheme.SLOT_SIZE,
                HudTheme.SLOT_SIZE
        );
    }

    public static Rectangle getProgressBounds(Rectangle panel) {
        Rectangle row = getMachineRowBounds(panel);
        int x = row.x + HudTheme.SLOT_SIZE + SLOT_GAP;
        int y = row.y + (row.height - ARROW_HEIGHT) / 2;
        return new Rectangle(x, y, ARROW_WIDTH, ARROW_HEIGHT);
    }

    public static Rectangle getInventoryContentBounds(Rectangle panel) {
        Rectangle section = getInventorySectionBounds(panel);
        int y = section.y + INVENTORY_SECTION_HEADER;
        int height = section.height - INVENTORY_SECTION_HEADER;
        return new Rectangle(section.x, y, section.width, height);
    }

    public static int getMachineSlotAtPoint(int x, int y, Rectangle panel) {
        Rectangle machineSection = getMachineSectionBounds(panel);
        if (!machineSection.contains(x, y)) {
            return -1;
        }
        if (getInputSlotBounds(panel).contains(x, y)) {
            return INPUT_SLOT;
        }
        if (getOutputSlotBounds(panel).contains(x, y)) {
            return OUTPUT_SLOT;
        }
        return -1;
    }

    public static int getInventorySlotAtPoint(int x, int y, Rectangle panel) {
        Rectangle inventoryContent = getInventoryContentBounds(panel);
        if (!inventoryContent.contains(x, y)) {
            return -1;
        }
        return InventoryLayout.getSlotAtPoint(x, y, inventoryContent);
    }
}
