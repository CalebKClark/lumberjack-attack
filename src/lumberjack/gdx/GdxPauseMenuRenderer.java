package lumberjack.gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Pause menu drawn in ScreenViewport (Y-up) coordinates.
 */
public final class GdxPauseMenuRenderer {

    private static final Color OVERLAY = new Color(0.12f, 0.12f, 0.12f, 0.92f);
    private static final Color BUTTON_BG = new Color(0.22f, 0.22f, 0.26f, 1f);
    private static final Color BUTTON_HOVER = new Color(0.32f, 0.32f, 0.38f, 1f);
    private static final Color BUTTON_BORDER = new Color(0.75f, 0.75f, 0.8f, 1f);
    private static final Color TEXT = new Color(0.95f, 0.95f, 0.95f, 1f);

    public static final int BUTTON_WIDTH = 320;
    public static final int BUTTON_HEIGHT = 56;
    public static final int BUTTON_GAP = 14;

    private static final String[] LABELS = {
            "Continue",
            "Settings",
            "Save",
            "Save and Exit"
    };

    private final GlyphLayout layout = new GlyphLayout();

    public void draw(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            int screenWidth,
            int screenHeight
    ) {
        int mouseX = GdxInputHelper.mouseX();
        int mouseY = GdxInputHelper.mouseYBottomUp();

        GdxBatchUtils.drawSolid(batch, textures, OVERLAY, 0, 0, screenWidth, screenHeight);

        int centerX = screenWidth / 2;
        int totalHeight = BUTTON_HEIGHT * LABELS.length + BUTTON_GAP * (LABELS.length - 1);
        int topOfCluster = (screenHeight + totalHeight) / 2;

        for (int index = 0; index < LABELS.length; index++) {
            int bottom = topOfCluster - (index + 1) * BUTTON_HEIGHT - index * BUTTON_GAP;
            int left = centerX - BUTTON_WIDTH / 2;
            boolean hovered = mouseX >= left && mouseX < left + BUTTON_WIDTH
                    && mouseY >= bottom && mouseY < bottom + BUTTON_HEIGHT;

            GdxBatchUtils.drawSolid(
                    batch,
                    textures,
                    hovered ? BUTTON_HOVER : BUTTON_BG,
                    left,
                    bottom,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT
            );
            GdxBatchUtils.drawSolid(batch, textures, BUTTON_BORDER, left, bottom + BUTTON_HEIGHT - 2, BUTTON_WIDTH, 2);

            font.setColor(TEXT);
            layout.setText(font, LABELS[index]);
            font.draw(
                    batch,
                    LABELS[index],
                    centerX - layout.width / 2f,
                    bottom + (BUTTON_HEIGHT + layout.height) / 2f
            );
        }
    }

    public int getClickedButton(int screenWidth, int screenHeight) {
        int mouseX = GdxInputHelper.mouseX();
        int mouseY = GdxInputHelper.mouseYBottomUp();

        int centerX = screenWidth / 2;
        int totalHeight = BUTTON_HEIGHT * LABELS.length + BUTTON_GAP * (LABELS.length - 1);
        int topOfCluster = (screenHeight + totalHeight) / 2;

        for (int index = 0; index < LABELS.length; index++) {
            int bottom = topOfCluster - (index + 1) * BUTTON_HEIGHT - index * BUTTON_GAP;
            int left = centerX - BUTTON_WIDTH / 2;
            if (mouseX >= left && mouseX < left + BUTTON_WIDTH
                    && mouseY >= bottom && mouseY < bottom + BUTTON_HEIGHT) {
                return index;
            }
        }
        return -1;
    }
}
