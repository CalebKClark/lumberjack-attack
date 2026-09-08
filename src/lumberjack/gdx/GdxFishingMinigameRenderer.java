package lumberjack.gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.fishing.minigame.FishingMinigame;
import lumberjack.game.GameSession;
import lumberjack.item.ItemRegistry;

/**
 * Fishing minigame overlay for LibGDX.
 * Uses bottom-up screen coordinates (same as the rest of the LibGDX HUD).
 */
public final class GdxFishingMinigameRenderer {

    private static final Color PANEL_BG = new Color(0.06f, 0.18f, 0.33f, 1f);
    private static final Color PANEL_BORDER = new Color(0.78f, 0.9f, 1f, 1f);
    private static final Color LINE_COLOR = new Color(0.82f, 0.82f, 0.82f, 1f);
    private static final Color DEPTH_TEXT = new Color(0.7f, 0.82f, 0.94f, 1f);
    private static final Color DIM_OVERLAY = new Color(0f, 0f, 0f, 0.67f);

    private final GlyphLayout layout = new GlyphLayout();

    public void draw(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            int screenWidth,
            int screenHeight
    ) {
        FishingMinigame minigame = session.getFishingManager().getActiveMinigame();
        if (minigame == null) {
            return;
        }

        batch.setColor(1f, 1f, 1f, 1f);

        ItemRegistry itemRegistry = session.getItemRegistry();
        int panelWidth = minigame.getPanelWidth();
        int panelHeight = minigame.getPanelHeight();
        int panelX = (screenWidth - panelWidth) / 2;
        int panelTop = (screenHeight - panelHeight) / 2;

        drawRectFromTop(batch, textures, DIM_OVERLAY, 0, 0, screenWidth, screenHeight, screenHeight);
        drawRectFromTop(batch, textures, PANEL_BG, panelX, panelTop, panelWidth, panelHeight, screenHeight);
        drawRectFromTop(batch, textures, PANEL_BORDER, panelX, panelTop, panelWidth, 2, screenHeight);
        drawRectFromTop(batch, textures, PANEL_BORDER, panelX, panelTop, 2, panelHeight, screenHeight);
        drawRectFromTop(batch, textures, PANEL_BORDER, panelX + panelWidth - 2, panelTop, 2, panelHeight, screenHeight);
        drawRectFromTop(batch, textures, PANEL_BORDER, panelX, panelTop + panelHeight - 2, panelWidth, 2, screenHeight);

        font.setColor(DEPTH_TEXT);
        font.draw(batch, "Fishing — A / D to move", panelX + 16, textBaselineFromTop(panelTop + 28, screenHeight));
        font.draw(batch, "Depth: " + (int) minigame.getDepth(), panelX + 16, textBaselineFromTop(panelTop + 52, screenHeight));

        int hookScreenX = panelX + (int) Math.round(minigame.getLineX());
        int hookFromTop = panelTop + minigame.getHookScreenY();
        int playAreaTop = panelTop + minigame.getPlayAreaTop();
        int lineHeight = hookFromTop - playAreaTop;
        drawRectFromTop(batch, textures, LINE_COLOR, hookScreenX, playAreaTop, 2, lineHeight, screenHeight);

        float savedScaleX = font.getData().scaleX;
        float savedScaleY = font.getData().scaleY;

        for (FishingMinigame.MinigameEntity entity : minigame.getEntities()) {
            int entityX = panelX + (int) Math.round(entity.getX());
            int entityTop = panelTop + (int) Math.round(minigame.worldYToScreenY(entity.getY()));
            int entityWidth = entity.getWidth();
            int entityHeight = entity.getHeight();

            if (entityTop + entityHeight < playAreaTop || entityTop > panelTop + panelHeight) {
                continue;
            }

            var sprite = textures.getOptional("items/sprites/" + entity.getItemId() + ".png");
            if (sprite != null) {
                drawTextureFromTop(batch, sprite, entityX, entityTop, entityWidth, entityHeight, screenHeight);
            } else {
                drawRectFromTop(
                        batch,
                        textures,
                        GdxBatchUtils.toGdx(itemRegistry.get(entity.getItemId()).getDebugColor()),
                        entityX,
                        entityTop,
                        entityWidth,
                        entityHeight,
                        screenHeight
                );
            }

            font.getData().setScale(savedScaleX * 0.85f, savedScaleY * 0.85f);
            font.setColor(entity.isStationary() ? new Color(0.9f, 0.82f, 0.7f, 1f) : Color.WHITE);
            font.draw(
                    batch,
                    itemRegistry.get(entity.getItemId()).getName(),
                    entityX + 4,
                    textBaselineFromTop(entityTop - 4, screenHeight)
            );
        }

        font.getData().setScale(savedScaleX, savedScaleY);

        drawRectFromTop(batch, textures, Color.WHITE, hookScreenX - 7, hookFromTop - 7, 14, 14, screenHeight);
        drawRectFromTop(batch, textures, Color.DARK_GRAY, hookScreenX - 7, hookFromTop - 7, 14, 2, screenHeight);
        drawRectFromTop(batch, textures, Color.DARK_GRAY, hookScreenX - 7, hookFromTop + 5, 14, 2, screenHeight);
        drawRectFromTop(batch, textures, Color.DARK_GRAY, hookScreenX - 7, hookFromTop - 7, 2, 14, screenHeight);
        drawRectFromTop(batch, textures, Color.DARK_GRAY, hookScreenX + 5, hookFromTop - 7, 2, 14, screenHeight);

        if (minigame.isFinished()) {
            if (minigame.getCaughtItemId() != null) {
                drawCatchPopup(batch, font, itemRegistry, hookScreenX, hookFromTop, screenHeight, minigame.getCaughtItemId());
            }
            drawResultBanner(batch, font, textures, itemRegistry, panelX, panelTop, panelWidth, panelHeight, screenHeight, minigame);
        }
    }

    private static int textBaselineFromTop(int yFromTop, int screenHeight) {
        return screenHeight - yFromTop;
    }

    private static void drawRectFromTop(
            SpriteBatch batch,
            GdxTextureCache textures,
            Color color,
            int x,
            int top,
            int width,
            int height,
            int screenHeight
    ) {
        GdxBatchUtils.drawSolid(batch, textures, color, x, screenHeight - top - height, width, height);
    }

    private static void drawTextureFromTop(
            SpriteBatch batch,
            com.badlogic.gdx.graphics.Texture texture,
            int x,
            int top,
            int width,
            int height,
            int screenHeight
    ) {
        GdxBatchUtils.drawScreenTexture(batch, texture, x, screenHeight - top - height, width, height);
    }

    private void drawCatchPopup(
            SpriteBatch batch,
            BitmapFont font,
            ItemRegistry itemRegistry,
            int hookScreenX,
            int hookFromTop,
            int screenHeight,
            String caughtItemId
    ) {
        String message = "+ " + itemRegistry.get(caughtItemId).getName() + " x1";
        layout.setText(font, message);
        float x = hookScreenX - layout.width / 2f;
        float y = textBaselineFromTop(hookFromTop - 28, screenHeight);

        font.setColor(0f, 0f, 0f, 0.63f);
        font.draw(batch, message, x + 1, y + 1);
        font.setColor(1f, 0.9f, 0.27f, 1f);
        font.draw(batch, message, x, y);
    }

    private void drawResultBanner(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            ItemRegistry itemRegistry,
            int panelX,
            int panelTop,
            int panelWidth,
            int panelHeight,
            int screenHeight,
            FishingMinigame minigame
    ) {
        String message = minigame.getCaughtItemId() != null
                ? "Caught: " + itemRegistry.get(minigame.getCaughtItemId()).getName()
                : "Nothing caught";

        layout.setText(font, message);
        int boxWidth = (int) layout.width + 40;
        int boxHeight = 48;
        int boxX = panelX + (panelWidth - boxWidth) / 2;
        int boxTop = panelTop + (panelHeight - boxHeight) / 2;

        drawRectFromTop(batch, textures, new Color(0f, 0f, 0f, 0.7f), boxX, boxTop, boxWidth, boxHeight, screenHeight);
        font.setColor(Color.WHITE);
        font.draw(batch, message, boxX + 20, textBaselineFromTop(boxTop + 32, screenHeight));
    }
}
