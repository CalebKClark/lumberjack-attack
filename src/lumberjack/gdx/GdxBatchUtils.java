package lumberjack.gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Shared LibGDX draw helpers. World-space textures use {@code flipY} because the
 * game uses a top-down Y axis for world coordinates.
 */
public final class GdxBatchUtils {

    private GdxBatchUtils() {
    }

    public static void drawWorldTexture(
            SpriteBatch batch,
            Texture texture,
            float x,
            float y,
            float width,
            float height
    ) {
        drawWorldTexture(batch, texture, x, y, width, height, 0);
    }

    /**
     * @param rotationQuarters clockwise 90° steps (0–3), matching the world editor.
     */
    public static void drawWorldTexture(
            SpriteBatch batch,
            Texture texture,
            float x,
            float y,
            float width,
            float height,
            int rotationQuarters
    ) {
        int rot = rotationQuarters & 3;
        // Same anchor as the unrotated path: (x, y+height) with negative height,
        // which matches the game's top-down / Y-down world projection.
        if (rot == 0) {
            batch.draw(texture, x, y + height, width, -height);
            return;
        }
        float originX = width / 2f;
        float originY = -height / 2f;
        // Positive degrees here reads as clockwise on-screen under the Y-flipped draw.
        float degrees = rot * 90f;
        batch.draw(
                texture,
                x,
                y + height,
                originX,
                originY,
                width,
                -height,
                1f,
                1f,
                degrees,
                0,
                0,
                texture.getWidth(),
                texture.getHeight(),
                false,
                false
        );
    }

    public static void drawWorldTexture(SpriteBatch batch, Texture texture, float x, float y) {
        float height = texture.getHeight();
        drawWorldTexture(batch, texture, x, y, texture.getWidth(), height, 0);
    }

    public static void drawScreenTexture(
            SpriteBatch batch,
            Texture texture,
            float x,
            float y,
            float width,
            float height
    ) {
        drawScreenTexture(batch, texture, x, y, width, height, 0);
    }

    public static void drawScreenTexture(
            SpriteBatch batch,
            Texture texture,
            float x,
            float y,
            float width,
            float height,
            int rotationQuarters
    ) {
        int rot = rotationQuarters & 3;
        if (rot == 0) {
            batch.draw(texture, x, y, width, height);
            return;
        }
        float originX = width / 2f;
        float originY = height / 2f;
        float degrees = -rot * 90f;
        batch.draw(
                texture,
                x,
                y,
                originX,
                originY,
                width,
                height,
                1f,
                1f,
                degrees,
                0,
                0,
                texture.getWidth(),
                texture.getHeight(),
                false,
                false
        );
    }

    public static void drawScreenTextureTopDown(
            SpriteBatch batch,
            Texture texture,
            float x,
            float y,
            float width,
            float height
    ) {
        batch.draw(texture, x, y, width, height);
    }

    public static void drawSolid(
            SpriteBatch batch,
            GdxTextureCache textures,
            Color color,
            float x,
            float y,
            float width,
            float height
    ) {
        float previousR = batch.getColor().r;
        float previousG = batch.getColor().g;
        float previousB = batch.getColor().b;
        float previousA = batch.getColor().a;
        batch.setColor(color);
        batch.draw(textures.getWhitePixel(), x, y, width, height);
        batch.setColor(previousR, previousG, previousB, previousA);
    }

    public static Color toGdx(java.awt.Color awtColor) {
        return new Color(
                awtColor.getRed() / 255f,
                awtColor.getGreen() / 255f,
                awtColor.getBlue() / 255f,
                1f
        );
    }
}
