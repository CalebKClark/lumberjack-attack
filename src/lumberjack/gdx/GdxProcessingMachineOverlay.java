package lumberjack.gdx;

import java.awt.Rectangle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.core.GameConfig;
import lumberjack.inventory.Inventory;
import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;
import lumberjack.ui.hud.InventoryLayout;
import lumberjack.ui.hud.ProcessingMachineUiLayout;

/**
 * Native LibGDX wood-chipper / processing-machine overlay.
 */
public final class GdxProcessingMachineOverlay {

    private static final Color DIM = new Color(0f, 0f, 0f, 0.45f);
    private static final Color PANEL_BACKGROUND = new Color(0.09f, 0.08f, 0.06f, 0.92f);
    private static final Color PANEL_BORDER = new Color(0.47f, 0.35f, 0.24f, 1f);
    private static final Color MACHINE_SECTION_BG = new Color(0.13f, 0.10f, 0.08f, 0.94f);
    private static final Color INVENTORY_SECTION_BG = new Color(0.11f, 0.09f, 0.08f, 0.94f);
    private static final Color DIVIDER_COLOR = new Color(0.47f, 0.35f, 0.24f, 0.7f);
    private static final Color TITLE_TEXT = new Color(0.94f, 0.86f, 0.71f, 1f);
    private static final Color SECTION_LABEL = new Color(0.78f, 0.71f, 0.59f, 1f);
    private static final Color ARROW_TRACK = new Color(0.24f, 0.20f, 0.16f, 1f);
    private static final Color ARROW_FILL = new Color(0.71f, 0.51f, 0.27f, 1f);
    private static final Color ARROW_BORDER = new Color(0.39f, 0.29f, 0.18f, 1f);
    private static final Color HINT_TEXT = new Color(0.71f, 0.82f, 0.63f, 1f);

    private final GdxItemSlotDrawer slots = new GdxItemSlotDrawer();
    private final GlyphLayout layout = new GlyphLayout();

    public void draw(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            String machineTitle,
            ItemStack inputStack,
            ItemStack outputStack,
            double progressRatio,
            boolean processing,
            String outputSlotHint,
            ItemRegistry itemRegistry,
            Inventory inventory,
            int screenWidth,
            int screenHeight,
            Integer hoveredMachineSlot,
            Integer hoveredInventorySlot
    ) {
        GdxBatchUtils.drawSolid(batch, textures, DIM, 0, 0, screenWidth, screenHeight);

        Rectangle panel = ProcessingMachineUiLayout.getPanelBounds(screenWidth, screenHeight);
        Rectangle machineSection = ProcessingMachineUiLayout.getMachineSectionBounds(panel);
        Rectangle inventorySection = ProcessingMachineUiLayout.getInventorySectionBounds(panel);
        Rectangle inventoryContent = ProcessingMachineUiLayout.getInventoryContentBounds(panel);
        Rectangle divider = ProcessingMachineUiLayout.getDividerBounds(panel);

        drawRect(batch, textures, PANEL_BACKGROUND, panel, screenHeight);
        drawRectBorder(batch, textures, PANEL_BORDER, panel, screenHeight);
        drawRect(batch, textures, MACHINE_SECTION_BG, machineSection, screenHeight);
        drawRect(batch, textures, DIVIDER_COLOR, divider, screenHeight);
        drawRect(batch, textures, INVENTORY_SECTION_BG, inventorySection, screenHeight);

        float titleBaseline = GdxUiCoords.bottom(machineSection, screenHeight) + machineSection.height - 12;
        slots.drawCenteredLabel(
                batch,
                font,
                machineTitle,
                TITLE_TEXT,
                machineSection.x + machineSection.width / 2f,
                titleBaseline
        );

        Rectangle inputBounds = ProcessingMachineUiLayout.getInputSlotBounds(panel);
        Rectangle outputBounds = ProcessingMachineUiLayout.getOutputSlotBounds(panel);
        Rectangle progressBounds = ProcessingMachineUiLayout.getProgressBounds(panel);

        slots.drawTopDownSlot(
                batch,
                font,
                textures,
                itemRegistry,
                inputStack,
                inputBounds,
                screenHeight,
                hoveredMachineSlot != null && hoveredMachineSlot == ProcessingMachineUiLayout.INPUT_SLOT
        );
        slots.drawTopDownSlot(
                batch,
                font,
                textures,
                itemRegistry,
                outputStack,
                outputBounds,
                screenHeight,
                hoveredMachineSlot != null && hoveredMachineSlot == ProcessingMachineUiLayout.OUTPUT_SLOT
        );
        drawProgressArrow(batch, textures, progressBounds, screenHeight, progressRatio, processing);

        float invLabelBaseline = GdxUiCoords.bottom(inventorySection, screenHeight) + inventorySection.height - 10;
        slots.drawCenteredLabel(
                batch,
                font,
                "Your Inventory",
                SECTION_LABEL,
                inventorySection.x + inventorySection.width / 2f,
                invLabelBaseline
        );

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

        drawTooltips(
                batch,
                font,
                textures,
                itemRegistry,
                inventory,
                inputStack,
                outputStack,
                outputSlotHint,
                panel,
                inventoryContent,
                hoveredMachineSlot,
                hoveredInventorySlot,
                screenWidth,
                screenHeight
        );
    }

    private void drawTooltips(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            ItemRegistry itemRegistry,
            Inventory inventory,
            ItemStack inputStack,
            ItemStack outputStack,
            String outputSlotHint,
            Rectangle panel,
            Rectangle inventoryContent,
            Integer hoveredMachineSlot,
            Integer hoveredInventorySlot,
            int screenWidth,
            int screenHeight
    ) {
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

        if (hoveredMachineSlot == null) {
            return;
        }

        if (hoveredMachineSlot == ProcessingMachineUiLayout.INPUT_SLOT && !inputStack.isEmpty()) {
            slots.drawTooltipForItem(
                    batch,
                    font,
                    textures,
                    itemRegistry.get(inputStack.getItemId()),
                    ProcessingMachineUiLayout.getInputSlotBounds(panel),
                    screenWidth,
                    screenHeight
            );
            return;
        }

        if (hoveredMachineSlot == ProcessingMachineUiLayout.OUTPUT_SLOT) {
            Rectangle outputBounds = ProcessingMachineUiLayout.getOutputSlotBounds(panel);
            if (!outputStack.isEmpty()) {
                slots.drawTooltipForItem(
                        batch,
                        font,
                        textures,
                        itemRegistry.get(outputStack.getItemId()),
                        outputBounds,
                        screenWidth,
                        screenHeight
                );
            }
            if (outputSlotHint != null && !outputSlotHint.isBlank()) {
                font.setColor(HINT_TEXT);
                layout.setText(font, outputSlotHint);
                float x = outputBounds.x + (outputBounds.width - layout.width) / 2f;
                float y = GdxUiCoords.bottom(outputBounds, screenHeight) - 8;
                if (x < 8) {
                    x = 8;
                } else if (x + layout.width > screenWidth - 8) {
                    x = screenWidth - layout.width - 8;
                }
                font.draw(batch, outputSlotHint, x, y);
            }
        }
    }

    private void drawProgressArrow(
            SpriteBatch batch,
            GdxTextureCache textures,
            Rectangle bounds,
            int screenHeight,
            double ratio,
            boolean processing
    ) {
        float left = bounds.x;
        float bottom = GdxUiCoords.bottom(bounds, screenHeight);
        GdxBatchUtils.drawSolid(batch, textures, ARROW_TRACK, left, bottom, bounds.width, bounds.height);

        float fillWidth = (bounds.width - 8) * (float) Math.max(0, Math.min(1, ratio));
        if (fillWidth > 0) {
            Color fill = processing ? ARROW_FILL : new Color(ARROW_FILL.r * 0.75f, ARROW_FILL.g * 0.75f, ARROW_FILL.b * 0.75f, 1f);
            GdxBatchUtils.drawSolid(batch, textures, fill, left + 4, bottom + 4, fillWidth, bounds.height - 8);
        }
        GdxBatchUtils.drawSolid(batch, textures, ARROW_BORDER, left, bottom, bounds.width, 2);
        GdxBatchUtils.drawSolid(batch, textures, ARROW_BORDER, left, bottom + bounds.height - 2, bounds.width, 2);

        // Simple arrow tip block on the right
        float tipLeft = left + bounds.width - 4;
        GdxBatchUtils.drawSolid(batch, textures, ARROW_BORDER, tipLeft, bottom + 4, 10, bounds.height - 8);
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

    private static void drawRectBorder(
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
