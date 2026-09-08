package lumberjack.gdx;

import java.awt.Rectangle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.core.GameConfig;
import lumberjack.inventory.Inventory;
import lumberjack.inventory.ItemContainer;
import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;
import lumberjack.ui.hud.InventoryLayout;
import lumberjack.ui.hud.SlotGridLayout;
import lumberjack.ui.hud.StorageOverlayLayout;

/**
 * Native LibGDX chest / storage overlay.
 */
public final class GdxStorageOverlay {

    private static final Color DIM = new Color(0f, 0f, 0f, 0.45f);
    private static final Color PANEL_BACKGROUND = new Color(0.09f, 0.08f, 0.06f, 0.92f);
    private static final Color PANEL_BORDER = new Color(0.47f, 0.35f, 0.24f, 1f);
    private static final Color CONTAINER_SECTION_BG = new Color(0.13f, 0.10f, 0.08f, 0.94f);
    private static final Color INVENTORY_SECTION_BG = new Color(0.11f, 0.09f, 0.08f, 0.94f);
    private static final Color DIVIDER_COLOR = new Color(0.47f, 0.35f, 0.24f, 0.7f);
    private static final Color TITLE_TEXT = new Color(0.94f, 0.86f, 0.71f, 1f);
    private static final Color SECTION_LABEL = new Color(0.78f, 0.71f, 0.59f, 1f);

    private final GdxItemSlotDrawer slots = new GdxItemSlotDrawer();

    public void draw(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            String containerTitle,
            ItemContainer container,
            Inventory inventory,
            ItemRegistry itemRegistry,
            int screenWidth,
            int screenHeight,
            Integer hoveredContainerSlot,
            Integer hoveredInventorySlot
    ) {
        GdxBatchUtils.drawSolid(batch, textures, DIM, 0, 0, screenWidth, screenHeight);

        Rectangle panel = StorageOverlayLayout.getPanelBounds(screenWidth, screenHeight);
        Rectangle containerSection = StorageOverlayLayout.getContainerSectionBounds(panel);
        Rectangle containerContent = StorageOverlayLayout.getContainerContentBounds(panel);
        Rectangle inventorySection = StorageOverlayLayout.getInventorySectionBounds(panel);
        Rectangle inventoryContent = StorageOverlayLayout.getInventoryContentBounds(panel);
        Rectangle divider = StorageOverlayLayout.getDividerBounds(panel);

        drawRect(batch, textures, PANEL_BACKGROUND, panel, screenHeight);
        drawBorder(batch, textures, PANEL_BORDER, panel, screenHeight);
        drawRect(batch, textures, CONTAINER_SECTION_BG, containerSection, screenHeight);
        drawRect(batch, textures, DIVIDER_COLOR, divider, screenHeight);
        drawRect(batch, textures, INVENTORY_SECTION_BG, inventorySection, screenHeight);

        slots.drawCenteredLabel(
                batch,
                font,
                containerTitle,
                TITLE_TEXT,
                containerSection.x + containerSection.width / 2f,
                GdxUiCoords.bottom(containerSection, screenHeight) + containerSection.height - 10
        );
        slots.drawCenteredLabel(
                batch,
                font,
                "Your Inventory",
                SECTION_LABEL,
                inventorySection.x + inventorySection.width / 2f,
                GdxUiCoords.bottom(inventorySection, screenHeight) + inventorySection.height - 10
        );

        int columns = ItemContainer.CHEST_COLUMNS;
        for (int slot = 0; slot < container.getSlotCount(); slot++) {
            Rectangle bounds = SlotGridLayout.getSlotBounds(slot, containerContent, columns, container.getSlotCount());
            slots.drawTopDownSlot(
                    batch,
                    font,
                    textures,
                    itemRegistry,
                    container.getSlot(slot),
                    bounds,
                    screenHeight,
                    hoveredContainerSlot != null && hoveredContainerSlot == slot
            );
        }

        for (int slot = 0; slot < GameConfig.INVENTORY_SLOTS; slot++) {
            Rectangle bounds = InventoryLayout.getSlotBounds(slot, inventoryContent);
            slots.drawTopDownSlot(
                    batch,
                    font,
                    textures,
                    itemRegistry,
                    inventory.getSlot(slot),
                    bounds,
                    screenHeight,
                    hoveredInventorySlot != null && hoveredInventorySlot == slot
            );
        }

        if (hoveredInventorySlot != null && hoveredInventorySlot >= 0) {
            ItemStack stack = inventory.getSlot(hoveredInventorySlot);
            if (!stack.isEmpty()) {
                slots.drawTooltipForItem(
                        batch,
                        font,
                        textures,
                        itemRegistry.get(stack.getItemId()),
                        InventoryLayout.getSlotBounds(hoveredInventorySlot, inventoryContent),
                        screenWidth,
                        screenHeight
                );
            }
            return;
        }

        if (hoveredContainerSlot != null && hoveredContainerSlot >= 0) {
            ItemStack stack = container.getSlot(hoveredContainerSlot);
            if (!stack.isEmpty()) {
                slots.drawTooltipForItem(
                        batch,
                        font,
                        textures,
                        itemRegistry.get(stack.getItemId()),
                        SlotGridLayout.getSlotBounds(
                                hoveredContainerSlot,
                                containerContent,
                                columns,
                                container.getSlotCount()
                        ),
                        screenWidth,
                        screenHeight
                );
            }
        }
    }

    private static void drawRect(
            SpriteBatch batch,
            GdxTextureCache textures,
            Color color,
            Rectangle topDown,
            int screenHeight
    ) {
        GdxBatchUtils.drawSolid(
                batch,
                textures,
                color,
                topDown.x,
                GdxUiCoords.bottom(topDown, screenHeight),
                topDown.width,
                topDown.height
        );
    }

    private static void drawBorder(
            SpriteBatch batch,
            GdxTextureCache textures,
            Color color,
            Rectangle topDown,
            int screenHeight
    ) {
        float left = topDown.x;
        float bottom = GdxUiCoords.bottom(topDown, screenHeight);
        GdxBatchUtils.drawSolid(batch, textures, color, left, bottom, topDown.width, 2);
        GdxBatchUtils.drawSolid(batch, textures, color, left, bottom + topDown.height - 2, topDown.width, 2);
        GdxBatchUtils.drawSolid(batch, textures, color, left, bottom, 2, topDown.height);
        GdxBatchUtils.drawSolid(batch, textures, color, left + topDown.width - 2, bottom, 2, topDown.height);
    }
}
