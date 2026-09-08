package lumberjack.gdx;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.core.GameConfig;
import lumberjack.engine.input.InputSettings;
import lumberjack.game.GameSession;
import lumberjack.item.ItemDefinition;
import lumberjack.item.ItemStack;
import lumberjack.time.DayPhase;
import lumberjack.time.GameClock;
import lumberjack.ui.hud.HotbarLayout;
import lumberjack.ui.hud.HudTheme;
import lumberjack.ui.hud.ItemTooltipRenderer;

/**
 * In-game HUD: clock/currency badges, health, hotbar (qty + binds + tooltip), notifications.
 */
public final class GdxHudRenderer {

    private static final Color SLOT_BACKGROUND = new Color(0f, 0f, 0f, 0.63f);
    private static final Color SLOT_BORDER = new Color(1f, 1f, 1f, 0.35f);
    private static final Color SLOT_SELECTED = new Color(1f, 1f, 0.47f, 0.86f);
    private static final Color HUD_TEXT = new Color(0.96f, 0.96f, 0.96f, 1f);
    private static final Color DAY_TEXT = new Color(1f, 0.96f, 0.78f, 1f);
    private static final Color NIGHT_TEXT = new Color(0.67f, 0.78f, 1f, 1f);
    private static final Color DAY_BADGE = new Color(1f, 0.78f, 0.31f, 0.24f);
    private static final Color NIGHT_BADGE = new Color(0.31f, 0.47f, 0.86f, 0.31f);
    private static final Color CURRENCY_BADGE = new Color(0.35f, 0.55f, 0.28f, 0.35f);
    private static final Color CURRENCY_TEXT = new Color(0.85f, 0.95f, 0.75f, 1f);
    private static final Color KEY_LABEL = new Color(1f, 1f, 1f, 0.7f);
    private static final Color TOOLTIP_BG = new Color(0.1f, 0.1f, 0.12f, 0.92f);
    private static final Color TOOLTIP_BORDER = new Color(1f, 1f, 1f, 0.35f);
    private static final Color TOOLTIP_NAME = new Color(0.96f, 0.96f, 0.96f, 1f);
    private static final Color TOOLTIP_TYPE = new Color(0.47f, 0.69f, 1f, 1f);
    private static final Color TOOLTIP_STAT = new Color(0.47f, 0.86f, 0.55f, 1f);

    private final GlyphLayout layout = new GlyphLayout();
    private final GdxNotificationRenderer notificationRenderer = new GdxNotificationRenderer();

    public void draw(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            InputSettings inputSettings,
            int screenWidth,
            int screenHeight
    ) {
        drawClock(batch, font, textures, session.getClock(), screenWidth, screenHeight);
        drawWoodChips(batch, font, textures, session, screenWidth, screenHeight);
        drawHealthAndEnergy(batch, font, textures, session, screenWidth, screenHeight);
        drawHotbar(batch, font, textures, session, inputSettings, screenWidth, screenHeight);
        notificationRenderer.draw(batch, font, session.getNotificationManager(), screenWidth, screenHeight);
    }

    private void drawClock(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameClock clock,
            int screenWidth,
            int screenHeight
    ) {
        String text = clock.formatHudText();
        boolean isDay = clock.getPhase() == DayPhase.DAY;
        layout.setText(font, text);

        float textX = screenWidth - layout.width - 24;
        float textY = screenHeight - 36;
        float padX = 12;
        float padY = 6;

        GdxBatchUtils.drawSolid(
                batch,
                textures,
                isDay ? DAY_BADGE : NIGHT_BADGE,
                textX - padX,
                textY - layout.height - padY,
                layout.width + padX * 2,
                layout.height + padY * 2
        );
        font.setColor(isDay ? DAY_TEXT : NIGHT_TEXT);
        font.draw(batch, text, textX, textY);
    }

    private void drawWoodChips(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            int screenWidth,
            int screenHeight
    ) {
        String text = "WoodChips: " + session.getPlayerBank().getWoodChips();
        layout.setText(font, text);

        float textX = screenWidth - layout.width - 24;
        float textY = screenHeight - 72;
        float padX = 12;
        float padY = 6;

        GdxBatchUtils.drawSolid(
                batch,
                textures,
                CURRENCY_BADGE,
                textX - padX,
                textY - layout.height - padY,
                layout.width + padX * 2,
                layout.height + padY * 2
        );
        font.setColor(CURRENCY_TEXT);
        font.draw(batch, text, textX, textY);
    }

    private void drawHealthAndEnergy(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            int screenWidth,
            int screenHeight
    ) {
        int barWidth = HudTheme.HEALTH_BAR_WIDTH;
        int barHeight = HudTheme.HEALTH_BAR_HEIGHT;
        int x = screenWidth - barWidth - HudTheme.HEALTH_BAR_MARGIN_RIGHT;
        int energyY = HudTheme.HEALTH_BAR_MARGIN_BOTTOM;
        int healthY = energyY + barHeight + HudTheme.VITAL_BAR_GAP;

        int maxHealth = session.getCombatState()
                .getEffectiveStats(session.getInventory(), session.getItemRegistry())
                .getMaxHealth();
        double currentHealth = session.getCombatState().getCurrentHealth();
        double healthRatio = maxHealth <= 0 ? 0 : currentHealth / maxHealth;

        GdxBatchUtils.drawSolid(batch, textures, new Color(0.1f, 0.1f, 0.12f, 0.9f), x, healthY, barWidth, barHeight);
        GdxBatchUtils.drawSolid(
                batch,
                textures,
                new Color(0.82f, 0.18f, 0.18f, 1f),
                x + 2,
                healthY + 2,
                (float) ((barWidth - 4) * healthRatio),
                barHeight - 4
        );

        String healthText = (int) Math.ceil(currentHealth) + " / " + maxHealth;
        font.setColor(HUD_TEXT);
        layout.setText(font, healthText);
        font.draw(
                batch,
                healthText,
                x + (barWidth - layout.width) / 2f,
                healthY + barHeight / 2f + layout.height / 2f
        );

        int maxEnergy = session.getEnergyState().getMaxEnergy();
        double currentEnergy = session.getEnergyState().getCurrentEnergy();
        double energyRatio = maxEnergy <= 0 ? 0 : currentEnergy / maxEnergy;

        GdxBatchUtils.drawSolid(batch, textures, new Color(0.1f, 0.1f, 0.12f, 0.9f), x, energyY, barWidth, barHeight);
        GdxBatchUtils.drawSolid(
                batch,
                textures,
                new Color(0.22f, 0.72f, 0.28f, 1f),
                x + 2,
                energyY + 2,
                (float) ((barWidth - 4) * energyRatio),
                barHeight - 4
        );

        String energyText = (int) Math.ceil(currentEnergy) + " / " + maxEnergy;
        font.setColor(HUD_TEXT);
        layout.setText(font, energyText);
        font.draw(
                batch,
                energyText,
                x + (barWidth - layout.width) / 2f,
                energyY + barHeight / 2f + layout.height / 2f
        );
    }

    private void drawHotbar(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            InputSettings inputSettings,
            int screenWidth,
            int screenHeight
    ) {
        int mouseX = GdxInputHelper.mouseX();
        int mouseYTopDown = GdxInputHelper.mouseYTopDown();
        int hoveredSlot = -1;
        if (!session.isInventoryOpen()
                && !session.isChestOpen()
                && !session.isWoodChipperOpen()
                && !session.isMenuPaused()) {
            hoveredSlot = HotbarLayout.getSlotAtPoint(mouseX, mouseYTopDown, screenWidth, screenHeight);
        }

        for (int slot = 0; slot < GameConfig.HOTBAR_SLOTS; slot++) {
            java.awt.Rectangle bounds = HotbarLayout.getSlotBounds(slot, screenWidth, screenHeight);
            int drawY = screenHeight - bounds.y - bounds.height;
            boolean selected = slot == session.getInventory().getSelectedHotbarSlot();

            drawSlotFrame(batch, textures, bounds.x, drawY, bounds.width, bounds.height, selected);

            ItemStack stack = session.getInventory().getSlot(slot);
            if (!stack.isEmpty()) {
                drawItemInSlot(batch, textures, session, stack, bounds.x, drawY, bounds.width);
                if (stack.getQuantity() > 1) {
                    String qty = String.valueOf(stack.getQuantity());
                    float previousScale = font.getData().scaleX;
                    font.getData().setScale(stack.getQuantity() >= 100 ? 1.05f : 1.2f);
                    layout.setText(font, qty);
                    font.setColor(HUD_TEXT);
                    font.draw(
                            batch,
                            qty,
                            bounds.x + bounds.width - layout.width - 8,
                            drawY + 12 + layout.height
                    );
                    font.getData().setScale(previousScale);
                }
            }

            String keyLabel = inputSettings.getHotbarKeyLabel(slot);
            if (keyLabel != null && !keyLabel.isEmpty()) {
                font.setColor(KEY_LABEL);
                font.draw(batch, keyLabel, bounds.x + 6, drawY + bounds.height - 8);
            }
        }

        if (hoveredSlot >= 0) {
            ItemStack stack = session.getInventory().getSlot(hoveredSlot);
            if (!stack.isEmpty()) {
                java.awt.Rectangle bounds = HotbarLayout.getSlotBounds(hoveredSlot, screenWidth, screenHeight);
                int drawY = screenHeight - bounds.y - bounds.height;
                drawTooltip(
                        batch,
                        font,
                        textures,
                        session.getItemRegistry().get(stack.getItemId()),
                        bounds.x,
                        drawY,
                        bounds.width,
                        bounds.height,
                        screenWidth,
                        screenHeight
                );
            }
        }
    }

    private void drawSlotFrame(
            SpriteBatch batch,
            GdxTextureCache textures,
            int x,
            int y,
            int width,
            int height,
            boolean selected
    ) {
        GdxBatchUtils.drawSolid(batch, textures, SLOT_BACKGROUND, x, y, width, height);
        Color border = selected ? SLOT_SELECTED : SLOT_BORDER;
        GdxBatchUtils.drawSolid(batch, textures, border, x, y, width, 2);
        GdxBatchUtils.drawSolid(batch, textures, border, x, y, 2, height);
        GdxBatchUtils.drawSolid(batch, textures, border, x + width - 2, y, 2, height);
        GdxBatchUtils.drawSolid(batch, textures, border, x, y + height - 2, width, 2);
    }

    private void drawItemInSlot(
            SpriteBatch batch,
            GdxTextureCache textures,
            GameSession session,
            ItemStack stack,
            int slotX,
            int slotY,
            int slotSize
    ) {
        int padding = HudTheme.SLOT_ICON_PADDING;
        int iconSize = slotSize - padding * 2;
        String itemId = stack.getItemId();
        var sprite = textures.getOptional("items/sprites/" + itemId + ".png");
        if (sprite != null) {
            GdxBatchUtils.drawScreenTexture(
                    batch,
                    sprite,
                    slotX + padding,
                    slotY + padding,
                    iconSize,
                    iconSize
            );
        } else {
            GdxBatchUtils.drawSolid(
                    batch,
                    textures,
                    GdxBatchUtils.toGdx(session.getItemRegistry().get(itemId).getDebugColor()),
                    slotX + padding,
                    slotY + padding,
                    iconSize,
                    iconSize
            );
        }
    }

    private void drawTooltip(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            ItemDefinition item,
            int slotX,
            int slotY,
            int slotWidth,
            int slotHeight,
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

        float boxX = slotX + (slotWidth - boxWidth) / 2f;
        float boxBottom = slotY + slotHeight + 8;
        if (boxBottom + boxHeight > screenHeight - 8) {
            boxBottom = slotY - boxHeight - 8;
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
        layout.setText(font, name);
        font.draw(batch, name, boxX + padding, textY);
        textY -= nameHeight + lineGap;

        font.getData().setScale(1.0f);
        font.setColor(TOOLTIP_TYPE);
        layout.setText(font, typeLabel);
        font.draw(batch, typeLabel, boxX + padding, textY);
        textY -= bodyHeight + lineGap;

        font.setColor(TOOLTIP_STAT);
        for (String statLine : statLines) {
            layout.setText(font, statLine);
            font.draw(batch, statLine, boxX + padding, textY);
            textY -= bodyHeight + lineGap;
        }

        font.getData().setScale(previousScale);
    }
}
