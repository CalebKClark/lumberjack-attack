package lumberjack.gdx;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.item.ItemDefinition;
import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;
import lumberjack.ui.hud.HudTheme;
import lumberjack.ui.hud.HudTheme;
import lumberjack.ui.hud.ItemTooltipRenderer;

/**
 * Shared LibGDX item-slot / tooltip / carried-cursor drawing (Y-up ScreenViewport).
 */
public final class GdxItemSlotDrawer {

    private static final Color SLOT_BACKGROUND = new Color(0f, 0f, 0f, 0.63f);
    private static final Color SLOT_BORDER = new Color(1f, 1f, 1f, 0.35f);
    private static final Color SLOT_SELECTED = new Color(1f, 1f, 0.47f, 0.86f);
    private static final Color HUD_TEXT = new Color(0.96f, 0.96f, 0.96f, 1f);
    private static final Color TOOLTIP_BG = new Color(0.1f, 0.1f, 0.12f, 0.92f);
    private static final Color TOOLTIP_BORDER = new Color(1f, 1f, 1f, 0.35f);
    private static final Color TOOLTIP_NAME = new Color(0.96f, 0.96f, 0.96f, 1f);
    private static final Color TOOLTIP_TYPE = new Color(0.47f, 0.69f, 1f, 1f);
    private static final Color TOOLTIP_STAT = new Color(0.47f, 0.86f, 0.55f, 1f);

    private final GlyphLayout layout = new GlyphLayout();

    public void drawSlot(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            ItemRegistry itemRegistry,
            ItemStack stack,
            float left,
            float bottom,
            int size,
            boolean selected
    ) {
        GdxBatchUtils.drawSolid(batch, textures, SLOT_BACKGROUND, left, bottom, size, size);
        Color border = selected ? SLOT_SELECTED : SLOT_BORDER;
        GdxBatchUtils.drawSolid(batch, textures, border, left, bottom, size, 2);
        GdxBatchUtils.drawSolid(batch, textures, border, left, bottom, 2, size);
        GdxBatchUtils.drawSolid(batch, textures, border, left + size - 2, bottom, 2, size);
        GdxBatchUtils.drawSolid(batch, textures, border, left, bottom + size - 2, size, 2);

        if (stack == null || stack.isEmpty()) {
            return;
        }

        int padding = HudTheme.SLOT_ICON_PADDING;
        int iconSize = size - padding * 2;
        String itemId = stack.getItemId();
        var sprite = textures.getOptional("items/sprites/" + itemId + ".png");
        if (sprite != null) {
            GdxBatchUtils.drawScreenTexture(batch, sprite, left + padding, bottom + padding, iconSize, iconSize);
        } else {
            GdxBatchUtils.drawSolid(
                    batch,
                    textures,
                    GdxBatchUtils.toGdx(itemRegistry.get(itemId).getDebugColor()),
                    left + padding,
                    bottom + padding,
                    iconSize,
                    iconSize
            );
        }

        if (stack.getQuantity() > 1) {
            String qty = String.valueOf(stack.getQuantity());
            float previousScale = font.getData().scaleX;
            font.getData().setScale(stack.getQuantity() >= 100 ? 1.0f : 1.15f);
            layout.setText(font, qty);
            font.setColor(HUD_TEXT);
            font.draw(batch, qty, left + size - layout.width - 6, bottom + 10 + layout.height);
            font.getData().setScale(previousScale);
        }
    }

    public void drawTopDownSlot(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            ItemRegistry itemRegistry,
            ItemStack stack,
            java.awt.Rectangle topDownBounds,
            int screenHeight,
            boolean selected
    ) {
        drawSlot(
                batch,
                font,
                textures,
                itemRegistry,
                stack,
                topDownBounds.x,
                GdxUiCoords.bottom(topDownBounds, screenHeight),
                topDownBounds.width,
                selected
        );
    }

    public void drawCarried(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            ItemRegistry itemRegistry,
            ItemStack stack,
            int mouseX,
            int mouseYTopDown,
            int screenHeight
    ) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        float left = mouseX - HudTheme.SLOT_SIZE / 2f;
        float bottom = GdxUiCoords.mouseBottomUp(mouseYTopDown, screenHeight) - HudTheme.SLOT_SIZE / 2f;
        drawSlot(batch, font, textures, itemRegistry, stack, left, bottom, HudTheme.SLOT_SIZE, false);
    }

    public void drawTooltipForItem(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            ItemDefinition item,
            java.awt.Rectangle topDownSlot,
            int screenWidth,
            int screenHeight
    ) {
        if (item == null) {
            return;
        }
        float slotLeft = topDownSlot.x;
        float slotBottom = GdxUiCoords.bottom(topDownSlot, screenHeight);
        drawTooltip(batch, font, textures, item, slotLeft, slotBottom, topDownSlot.width, topDownSlot.height,
                screenWidth, screenHeight);
    }

    public void drawTooltip(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            ItemDefinition item,
            float slotLeft,
            float slotBottom,
            float slotWidth,
            float slotHeight,
            int screenWidth,
            int screenHeight
    ) {
        String name = item.getName();
        String typeLabel = item.getTypeLabel();
        List<String> statLines = new java.util.ArrayList<>(ItemTooltipRenderer.buildStatLines(item.getStatModifiers()));
        statLines.addAll(ItemTooltipRenderer.buildFoodLines(item));

        float previousScale = font.getData().scaleX;
        font.getData().setScale(1.15f);
        layout.setText(font, name);
        float nameWidth = layout.width;
        float nameHeight = layout.height;

        font.getData().setScale(1.0f);
        layout.setText(font, typeLabel);
        float bodyWidth = layout.width;
        float bodyHeight = layout.height;
        for (String statLine : statLines) {
            layout.setText(font, statLine);
            bodyWidth = Math.max(bodyWidth, layout.width);
        }

        int padding = 10;
        int lineGap = 4;
        float boxWidth = Math.max(nameWidth, bodyWidth) + padding * 2;
        float boxHeight = padding * 2
                + nameHeight
                + lineGap
                + bodyHeight
                + statLines.size() * (bodyHeight + lineGap);

        float boxX = slotLeft + (slotWidth - boxWidth) / 2f;
        float boxBottom = slotBottom + slotHeight + 8;
        if (boxBottom + boxHeight > screenHeight - 8) {
            boxBottom = slotBottom - boxHeight - 8;
        }
        if (boxX < 8) {
            boxX = 8;
        } else if (boxX + boxWidth > screenWidth - 8) {
            boxX = screenWidth - boxWidth - 8;
        }

        GdxBatchUtils.drawSolid(batch, textures, TOOLTIP_BG, boxX, boxBottom, boxWidth, boxHeight);
        GdxBatchUtils.drawSolid(batch, textures, TOOLTIP_BORDER, boxX, boxBottom + boxHeight - 2, boxWidth, 2);

        float textY = boxBottom + boxHeight - padding;
        font.getData().setScale(1.15f);
        font.setColor(TOOLTIP_NAME);
        font.draw(batch, name, boxX + padding, textY);
        textY -= nameHeight + lineGap;

        font.getData().setScale(1.0f);
        font.setColor(TOOLTIP_TYPE);
        font.draw(batch, typeLabel, boxX + padding, textY);
        textY -= bodyHeight + lineGap;

        font.setColor(TOOLTIP_STAT);
        for (String statLine : statLines) {
            font.draw(batch, statLine, boxX + padding, textY);
            textY -= bodyHeight + lineGap;
        }

        font.getData().setScale(previousScale);
    }

    public void drawCenteredLabel(
            SpriteBatch batch,
            BitmapFont font,
            String text,
            Color color,
            float centerX,
            float baselineY
    ) {
        font.setColor(color);
        layout.setText(font, text);
        font.draw(batch, text, centerX - layout.width / 2f, baselineY);
    }
}
