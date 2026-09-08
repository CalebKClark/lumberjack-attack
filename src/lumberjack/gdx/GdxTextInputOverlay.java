package lumberjack.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * In-game text field overlay for naming saves (Y-up ScreenViewport coords).
 */
public final class GdxTextInputOverlay {

    private static final Color OVERLAY = new Color(0f, 0f, 0f, 0.55f);
    private static final Color PANEL = new Color(0.18f, 0.18f, 0.22f, 0.96f);
    private static final Color FIELD = new Color(0.12f, 0.12f, 0.15f, 1f);
    private static final Color FIELD_BORDER = new Color(0.55f, 0.55f, 0.62f, 1f);
    private static final Color BUTTON = new Color(0.28f, 0.28f, 0.34f, 1f);
    private static final Color BUTTON_HOVER = new Color(0.38f, 0.38f, 0.46f, 1f);
    private static final Color TEXT = new Color(0.95f, 0.95f, 0.95f, 1f);
    private static final Color HINT = new Color(1f, 0.7f, 0.45f, 1f);

    private static final int PANEL_WIDTH = 480;
    private static final int PANEL_HEIGHT = 240;
    private static final int FIELD_WIDTH = 400;
    private static final int FIELD_HEIGHT = 44;
    private static final int BUTTON_WIDTH = 140;
    private static final int BUTTON_HEIGHT = 48;
    private static final int MAX_LENGTH = 40;

    private final GlyphLayout layout = new GlyphLayout();
    private final StringBuilder text = new StringBuilder();
    private final InputProcessor typingProcessor = new InputAdapter() {
        @Override
        public boolean keyTyped(char character) {
            if (!visible) {
                return false;
            }
            if (character == '\b') {
                if (text.length() > 0) {
                    text.deleteCharAt(text.length() - 1);
                }
                errorMessage = "";
                return true;
            }
            if (character < 32 || character == 127) {
                return false;
            }
            if (text.length() >= MAX_LENGTH) {
                return true;
            }
            text.append(character);
            errorMessage = "";
            return true;
        }

        @Override
        public boolean keyDown(int keycode) {
            if (!visible) {
                return false;
            }
            if (keycode == Input.Keys.ENTER) {
                submit();
                return true;
            }
            if (keycode == Input.Keys.ESCAPE) {
                cancel();
                return true;
            }
            // Backspace is handled in keyTyped to avoid deleting twice.
            return keycode == Input.Keys.BACKSPACE;
        }
    };

    private boolean visible;
    private String promptTitle = "Name your save";
    private String errorMessage = "";
    private String submittedText;
    private boolean cancelled;
    private InputProcessor previousProcessor;

    public void open(String promptTitle, String initialText) {
        this.promptTitle = promptTitle == null ? "Name your save" : promptTitle;
        text.setLength(0);
        if (initialText != null) {
            text.append(initialText);
        }
        errorMessage = "";
        submittedText = null;
        cancelled = false;
        visible = true;
        previousProcessor = Gdx.input.getInputProcessor();
        Gdx.input.setInputProcessor(typingProcessor);
    }

    public void close() {
        visible = false;
        submittedText = null;
        cancelled = false;
        errorMessage = "";
        if (Gdx.input.getInputProcessor() == typingProcessor) {
            Gdx.input.setInputProcessor(previousProcessor);
        }
        previousProcessor = null;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setError(String message) {
        errorMessage = message == null ? "" : message;
    }

    /**
     * @return trimmed text once submitted, empty string if cancelled, or {@code null} while open
     */
    public String pollResult() {
        if (cancelled) {
            cancelled = false;
            close();
            return "";
        }
        if (submittedText != null) {
            String result = submittedText;
            submittedText = null;
            close();
            return result;
        }
        return null;
    }

    public void updateInput(int screenWidth, int screenHeight) {
        if (!visible) {
            return;
        }

        if (!GdxInputHelper.isPrimaryMouseJustPressed()) {
            return;
        }

        int mouseX = GdxInputHelper.mouseX();
        int mouseY = GdxInputHelper.mouseYBottomUp();
        int hit = hitTestButton(screenWidth, screenHeight, mouseX, mouseY);
        if (hit == 0) {
            submit();
        } else if (hit == 1) {
            cancel();
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

        font.setColor(TEXT);
        layout.setText(font, promptTitle);
        font.draw(
                batch,
                promptTitle,
                screenWidth / 2f - layout.width / 2f,
                panelBottom + PANEL_HEIGHT - 36
        );

        int fieldLeft = screenWidth / 2 - FIELD_WIDTH / 2;
        int fieldBottom = panelBottom + 110;
        GdxBatchUtils.drawSolid(batch, textures, FIELD, fieldLeft, fieldBottom, FIELD_WIDTH, FIELD_HEIGHT);
        GdxBatchUtils.drawSolid(batch, textures, FIELD_BORDER, fieldLeft, fieldBottom + FIELD_HEIGHT - 2, FIELD_WIDTH, 2);

        boolean blink = ((int) (System.currentTimeMillis() / 500L) % 2) == 0;
        String display = text + (blink ? "|" : " ");
        font.setColor(TEXT);
        layout.setText(font, display);
        font.draw(batch, display, fieldLeft + 12, fieldBottom + (FIELD_HEIGHT + layout.height) / 2f);

        if (!errorMessage.isBlank()) {
            font.setColor(HINT);
            layout.setText(font, errorMessage);
            font.draw(batch, errorMessage, screenWidth / 2f - layout.width / 2f, fieldBottom - 12);
        }

        drawButton(batch, font, textures, "Create", 0, screenWidth, screenHeight, mouseX, mouseY);
        drawButton(batch, font, textures, "Cancel", 1, screenWidth, screenHeight, mouseX, mouseY);
    }

    private void submit() {
        String trimmed = text.toString().trim();
        if (trimmed.isEmpty()) {
            errorMessage = "Save name cannot be empty.";
            return;
        }
        submittedText = trimmed.length() > MAX_LENGTH ? trimmed.substring(0, MAX_LENGTH) : trimmed;
    }

    private void cancel() {
        cancelled = true;
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
        int bottom = panelBottom + 28;
        int left = startLeft + buttonIndex * (BUTTON_WIDTH + gap);
        return new int[] { left, bottom };
    }
}
