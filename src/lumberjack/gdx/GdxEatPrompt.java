package lumberjack.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.interaction.EatPrompt;

/**
 * Yes/No eat confirmation, matching the sleep prompt layout.
 */
public final class GdxEatPrompt implements EatPrompt {

    private static final Color OVERLAY = new Color(0f, 0f, 0f, 0.55f);
    private static final Color PANEL = new Color(0.18f, 0.18f, 0.22f, 0.96f);
    private static final Color BUTTON = new Color(0.28f, 0.28f, 0.34f, 1f);
    private static final Color BUTTON_HOVER = new Color(0.38f, 0.38f, 0.46f, 1f);
    private static final Color TEXT = new Color(0.95f, 0.95f, 0.95f, 1f);

    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 180;
    private static final int BUTTON_WIDTH = 140;
    private static final int BUTTON_HEIGHT = 48;

    private final GlyphLayout layout = new GlyphLayout();

    private boolean visible;
    private Boolean pendingResult;
    private String itemName = "";

    @Override
    public Boolean confirmEat(String itemName) {
        this.itemName = itemName == null ? "Food" : itemName;
        if (pendingResult != null) {
            Boolean result = pendingResult;
            pendingResult = null;
            visible = false;
            return result;
        }

        visible = true;
        return null;
    }

    @Override
    public void cancelEatPrompt() {
        visible = false;
        pendingResult = null;
        itemName = "";
    }

    @Override
    public boolean isEatPromptVisible() {
        return visible;
    }

    public void updateInput(int screenWidth, int screenHeight) {
        if (!visible) {
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            pendingResult = Boolean.FALSE;
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.Y)) {
            pendingResult = Boolean.TRUE;
            return;
        }

        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            return;
        }

        int mouseX = GdxInputHelper.mouseX();
        int mouseY = GdxInputHelper.mouseYBottomUp();
        int hit = hitTestButton(screenWidth, screenHeight, mouseX, mouseY);
        if (hit == 0) {
            pendingResult = Boolean.TRUE;
        } else if (hit == 1) {
            pendingResult = Boolean.FALSE;
        }
    }

    public void draw(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            int screenWidth,
            int screenHeight
    ) {
        if (!visible) {
            return;
        }

        int mouseX = GdxInputHelper.mouseX();
        int mouseY = GdxInputHelper.mouseYBottomUp();

        GdxBatchUtils.drawSolid(batch, textures, OVERLAY, 0, 0, screenWidth, screenHeight);

        int panelLeft = (screenWidth - PANEL_WIDTH) / 2;
        int panelBottom = (screenHeight - PANEL_HEIGHT) / 2;
        GdxBatchUtils.drawSolid(batch, textures, PANEL, panelLeft, panelBottom, PANEL_WIDTH, PANEL_HEIGHT);

        String message = "Eat \"" + itemName + "\"?";
        font.setColor(TEXT);
        layout.setText(font, message);
        font.draw(
                batch,
                message,
                screenWidth / 2f - layout.width / 2f,
                panelBottom + PANEL_HEIGHT - 48
        );

        drawButton(batch, font, textures, "Yes", 0, screenWidth, screenHeight, mouseX, mouseY);
        drawButton(batch, font, textures, "No", 1, screenWidth, screenHeight, mouseX, mouseY);
    }

    private void drawButton(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            String label,
            int buttonIndex,
            int screenWidth,
            int screenHeight,
            int mouseX,
            int mouseY
    ) {
        int[] bounds = buttonBounds(buttonIndex, screenWidth, screenHeight);
        int left = bounds[0];
        int bottom = bounds[1];
        boolean hovered = mouseX >= left && mouseX < left + BUTTON_WIDTH
                && mouseY >= bottom && mouseY < bottom + BUTTON_HEIGHT;

        GdxBatchUtils.drawSolid(
                batch,
                textures,
                hovered ? BUTTON_HOVER : BUTTON,
                left,
                bottom,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
        );

        font.setColor(TEXT);
        layout.setText(font, label);
        font.draw(
                batch,
                label,
                left + (BUTTON_WIDTH - layout.width) / 2f,
                bottom + (BUTTON_HEIGHT + layout.height) / 2f
        );
    }

    private int hitTestButton(int screenWidth, int screenHeight, int mouseX, int mouseY) {
        for (int index = 0; index < 2; index++) {
            int[] bounds = buttonBounds(index, screenWidth, screenHeight);
            if (mouseX >= bounds[0] && mouseX < bounds[0] + BUTTON_WIDTH
                    && mouseY >= bounds[1] && mouseY < bounds[1] + BUTTON_HEIGHT) {
                return index;
            }
        }
        return -1;
    }

    private static int[] buttonBounds(int buttonIndex, int screenWidth, int screenHeight) {
        int panelLeft = (screenWidth - PANEL_WIDTH) / 2;
        int panelBottom = (screenHeight - PANEL_HEIGHT) / 2;
        int gap = 24;
        int totalWidth = BUTTON_WIDTH * 2 + gap;
        int startLeft = panelLeft + (PANEL_WIDTH - totalWidth) / 2;
        int bottom = panelBottom + 36;
        int left = startLeft + buttonIndex * (BUTTON_WIDTH + gap);
        return new int[] { left, bottom };
    }
}
