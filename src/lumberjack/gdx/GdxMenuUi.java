package lumberjack.gdx;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.item.ItemRegistry;

/**
 * Shared drawing helpers for native player-menu tabs (Y-up ScreenViewport).
 */
public final class GdxMenuUi {

    public static final Color HUD_TEXT = new Color(0.96f, 0.96f, 0.96f, 1f);
    public static final Color SUBTEXT = new Color(0.71f, 0.71f, 0.73f, 1f);
    public static final Color ROW_BG = new Color(0.18f, 0.19f, 0.23f, 0.86f);
    public static final Color ROW_BORDER = new Color(1f, 1f, 1f, 0.2f);
    public static final Color UNKNOWN_TEXT = new Color(0.51f, 0.51f, 0.53f, 1f);
    public static final Color TOOLTIP_BG = new Color(0.1f, 0.1f, 0.12f, 0.94f);
    public static final Color TOOLTIP_BORDER = new Color(1f, 1f, 1f, 0.35f);
    public static final Color PLACEHOLDER_SWATCH = new Color(0.27f, 0.27f, 0.29f, 1f);

    private final GlyphLayout layout = new GlyphLayout();

    public GlyphLayout layout() {
        return layout;
    }

    public void drawRect(
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

    public void drawBorder(
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

    public void drawAccentRow(
            SpriteBatch batch,
            GdxTextureCache textures,
            Rectangle row,
            Color accent,
            int screenHeight
    ) {
        drawRect(batch, textures, ROW_BG, row, screenHeight);
        GdxBatchUtils.drawSolid(
                batch,
                textures,
                accent,
                row.x,
                GdxUiCoords.bottom(row, screenHeight),
                8,
                row.height
        );
        drawBorder(batch, textures, ROW_BORDER, row, screenHeight);
    }

    public void drawText(
            SpriteBatch batch,
            BitmapFont font,
            String text,
            Color color,
            float x,
            float baselineY
    ) {
        font.setColor(color);
        font.draw(batch, text, x, baselineY);
    }

    public float measureWidth(BitmapFont font, String text) {
        layout.setText(font, text);
        return layout.width;
    }

    public float measureHeight(BitmapFont font, String text) {
        layout.setText(font, text);
        return layout.height;
    }

    public List<String> wrapText(BitmapFont font, String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (measureWidth(font, candidate) <= maxWidth) {
                current = new StringBuilder(candidate);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    public void drawItemSwatch(
            SpriteBatch batch,
            GdxTextureCache textures,
            ItemRegistry itemRegistry,
            String itemId,
            float left,
            float bottom,
            float size
    ) {
        var sprite = itemId == null ? null : textures.getOptional("items/sprites/" + itemId + ".png");
        if (sprite != null) {
            GdxBatchUtils.drawScreenTexture(batch, sprite, left, bottom, size, size);
        } else if (itemId != null && itemRegistry.exists(itemId)) {
            GdxBatchUtils.drawSolid(
                    batch,
                    textures,
                    GdxBatchUtils.toGdx(itemRegistry.get(itemId).getDebugColor()),
                    left,
                    bottom,
                    size,
                    size
            );
        } else {
            GdxBatchUtils.drawSolid(batch, textures, PLACEHOLDER_SWATCH, left, bottom, size, size);
        }
        GdxBatchUtils.drawSolid(batch, textures, ROW_BORDER, left, bottom, size, 1);
        GdxBatchUtils.drawSolid(batch, textures, ROW_BORDER, left, bottom + size - 1, size, 1);
        GdxBatchUtils.drawSolid(batch, textures, ROW_BORDER, left, bottom, 1, size);
        GdxBatchUtils.drawSolid(batch, textures, ROW_BORDER, left + size - 1, bottom, 1, size);
    }

    public void drawBackButton(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            Rectangle bounds,
            int screenHeight
    ) {
        drawRect(batch, textures, ROW_BG, bounds, screenHeight);
        drawBorder(batch, textures, ROW_BORDER, bounds, screenHeight);
        drawText(
                batch,
                font,
                "<- Back",
                HUD_TEXT,
                bounds.x + 14,
                GdxUiCoords.bottom(bounds, screenHeight) + (bounds.height + measureHeight(font, "Back")) / 2f
        );
    }
}
