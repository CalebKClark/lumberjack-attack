package lumberjack.gdx;

import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.engine.DisplaySettings;
import lumberjack.engine.input.GameAction;
import lumberjack.engine.input.InputSettings;

import javax.swing.KeyStroke;

/**
 * Shared settings UI (main menu and pause): zoom and key rebinds.
 */
public final class GdxSettingsPanel {

    private static final Color OVERLAY = new Color(0.1f, 0.1f, 0.12f, 0.94f);
    private static final Color PANEL = new Color(0.18f, 0.18f, 0.22f, 0.98f);
    private static final Color BUTTON_BG = new Color(0.26f, 0.26f, 0.32f, 1f);
    private static final Color BUTTON_HOVER = new Color(0.36f, 0.36f, 0.44f, 1f);
    private static final Color BUTTON_BORDER = new Color(0.75f, 0.75f, 0.8f, 1f);
    private static final Color TEXT = new Color(0.95f, 0.95f, 0.95f, 1f);
    private static final Color MUTED = new Color(0.7f, 0.72f, 0.78f, 1f);
    private static final Color LISTENING = new Color(1f, 0.85f, 0.35f, 1f);

    private static final int PANEL_WIDTH = 720;
    private static final int PANEL_HEIGHT = 760;
    private static final int ROW_HEIGHT = 34;
    private static final int ROW_GAP = 4;
    private static final int SMALL_BTN_W = 44;
    private static final int SMALL_BTN_H = 36;
    private static final int KEY_BTN_W = 120;
    private static final int FOOTER_BTN_H = 40;
    private static final int FOOTER_GAP = 20;

    private final GlyphLayout layout = new GlyphLayout();
    private GameAction listeningAction;
    private String statusMessage = "";

    /**
     * @return true if the player pressed Back (leave settings)
     */
    public boolean update(InputSettings inputSettings, DisplaySettings displaySettings, int screenWidth, int screenHeight) {
        if (listeningAction != null) {
            pollRebind(inputSettings);
            return false;
        }

        if (!GdxInputHelper.isPrimaryMouseJustPressed()) {
            return false;
        }

        Layout layoutPos = layoutFor(screenWidth, screenHeight);
        int mouseX = GdxInputHelper.mouseX();
        int mouseY = GdxInputHelper.mouseYBottomUp();
        int action = hitTest(layoutPos, mouseX, mouseY);

        if (action == Hit.BACK) {
            saveInput(inputSettings);
            saveDisplay(displaySettings);
            statusMessage = "";
            return true;
        }
        if (action == Hit.ZOOM_MINUS) {
            displaySettings.setZoomPercent(displaySettings.getZoomPercent() - 10);
            saveDisplay(displaySettings);
            return false;
        }
        if (action == Hit.ZOOM_PLUS) {
            displaySettings.setZoomPercent(displaySettings.getZoomPercent() + 10);
            saveDisplay(displaySettings);
            return false;
        }
        if (action == Hit.DEBUG_TOGGLE) {
            displaySettings.toggleDebugEnabled();
            saveDisplay(displaySettings);
            statusMessage = displaySettings.isDebugEnabled()
                    ? "Debug enabled (hitboxes on)."
                    : "Debug disabled.";
            return false;
        }
        if (action == Hit.RESET) {
            inputSettings.resetToDefaults();
            saveInput(inputSettings);
            statusMessage = "Controls reset to defaults.";
            return false;
        }
        if (action >= 0 && action < GameAction.values().length) {
            listeningAction = GameAction.values()[action];
            statusMessage = "Press a key for " + listeningAction.getLabel() + " (Esc to cancel)";
        }
        return false;
    }

    public boolean isListening() {
        return listeningAction != null;
    }

    public void cancelListening() {
        listeningAction = null;
        statusMessage = "";
    }

    public void draw(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            InputSettings inputSettings,
            DisplaySettings displaySettings,
            int screenWidth,
            int screenHeight
    ) {
        int mouseX = GdxInputHelper.mouseX();
        int mouseY = GdxInputHelper.mouseYBottomUp();
        Layout pos = layoutFor(screenWidth, screenHeight);

        GdxBatchUtils.drawSolid(batch, textures, OVERLAY, 0, 0, screenWidth, screenHeight);
        GdxBatchUtils.drawSolid(batch, textures, PANEL, pos.panelLeft, pos.panelBottom, PANEL_WIDTH, PANEL_HEIGHT);

        font.setColor(TEXT);
        font.getData().setScale(1.6f);
        layout.setText(font, "Settings");
        font.draw(batch, "Settings", screenWidth / 2f - layout.width / 2f, pos.panelBottom + PANEL_HEIGHT - 36);
        font.getData().setScale(1.15f);

        font.setColor(MUTED);
        font.draw(batch, "Zoom", pos.panelLeft + 32, pos.zoomBottom + 24);
        drawWideButton(batch, font, textures, "-", pos.zoomMinusLeft, pos.zoomBottom, SMALL_BTN_W, SMALL_BTN_H, mouseX, mouseY);
        drawWideButton(batch, font, textures, "+", pos.zoomPlusLeft, pos.zoomBottom, SMALL_BTN_W, SMALL_BTN_H, mouseX, mouseY);
        font.setColor(TEXT);
        String zoomLabel = displaySettings.getZoomPercent() + "%";
        layout.setText(font, zoomLabel);
        font.draw(batch, zoomLabel, pos.panelLeft + 260, pos.zoomBottom + 24);

        String debugLabel = displaySettings.isDebugEnabled() ? "Enable Debug: ON" : "Enable Debug: OFF";
        drawWideButton(
                batch,
                font,
                textures,
                debugLabel,
                pos.debugLeft,
                pos.debugBottom,
                220,
                SMALL_BTN_H,
                mouseX,
                mouseY
        );

        font.setColor(MUTED);
        font.draw(batch, "Controls", pos.panelLeft + 32, pos.controlsLabelY);

        GameAction[] actions = GameAction.values();
        for (int i = 0; i < actions.length; i++) {
            drawKeybindRow(batch, font, textures, actions[i], inputSettings, pos, keybindRowBottom(pos, i), mouseX, mouseY);
        }

        drawWideButton(batch, font, textures, "Reset Controls", pos.resetLeft, pos.footerBottom, 220, FOOTER_BTN_H, mouseX, mouseY);
        drawWideButton(batch, font, textures, "Back", pos.backLeft, pos.footerBottom, 128, FOOTER_BTN_H, mouseX, mouseY);

        if (statusMessage != null && !statusMessage.isBlank()) {
            font.setColor(listeningAction != null ? LISTENING : MUTED);
            layout.setText(font, statusMessage);
            font.draw(batch, statusMessage, screenWidth / 2f - layout.width / 2f, pos.panelBottom + 28);
        }
    }

    private void pollRebind(InputSettings inputSettings) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            listeningAction = null;
            statusMessage = "Rebind cancelled.";
            return;
        }

        // LibGDX key codes are not dense — scan a safe upper bound used by desktop keys.
        for (int key = 0; key <= Input.Keys.MAX_KEYCODE; key++) {
            if (!Gdx.input.isKeyJustPressed(key) || key == Input.Keys.ESCAPE) {
                continue;
            }

            KeyStroke stroke = GdxKeyCodes.keyStrokeFromGdx(key);
            if (stroke == null) {
                statusMessage = "That key isn't supported yet.";
                return;
            }

            GameAction listening = listeningAction;
            inputSettings.findActionForKey(stroke).ifPresent(existing -> {
                if (existing != listening) {
                    inputSettings.setBinding(existing, inputSettings.getBinding(listening));
                }
            });
            inputSettings.setBinding(listening, stroke);
            saveInput(inputSettings);
            statusMessage = listening.getLabel() + " -> " + InputSettings.displayName(stroke);
            listeningAction = null;
            return;
        }
    }

    private void drawKeybindRow(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameAction action,
            InputSettings inputSettings,
            Layout pos,
            int rowBottom,
            int mouseX,
            int mouseY
    ) {
        font.setColor(TEXT);
        font.draw(batch, action.getLabel(), pos.panelLeft + 32, rowBottom + 22);

        String keyLabel = action == listeningAction
                ? "..."
                : InputSettings.displayName(inputSettings.getBinding(action));
        drawWideButton(batch, font, textures, keyLabel, pos.keyBtnLeft, rowBottom, KEY_BTN_W, ROW_HEIGHT, mouseX, mouseY);
    }

    private void drawWideButton(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            String label,
            int left,
            int bottom,
            int width,
            int height,
            int mouseX,
            int mouseY
    ) {
        boolean hovered = mouseX >= left && mouseX < left + width
                && mouseY >= bottom && mouseY < bottom + height;
        GdxBatchUtils.drawSolid(batch, textures, hovered ? BUTTON_HOVER : BUTTON_BG, left, bottom, width, height);
        GdxBatchUtils.drawSolid(batch, textures, BUTTON_BORDER, left, bottom + height - 2, width, 2);
        font.setColor(TEXT);
        layout.setText(font, label);
        font.draw(batch, label, left + (width - layout.width) / 2f, bottom + (height + layout.height) / 2f);
    }

    private int hitTest(Layout pos, int mouseX, int mouseY) {
        // Footer first so Back/Reset never lose to overlapping keybind hitboxes.
        if (contains(mouseX, mouseY, pos.resetLeft, pos.footerBottom, 220, FOOTER_BTN_H)) {
            return Hit.RESET;
        }
        if (contains(mouseX, mouseY, pos.backLeft, pos.footerBottom, 128, FOOTER_BTN_H)) {
            return Hit.BACK;
        }
        if (contains(mouseX, mouseY, pos.zoomMinusLeft, pos.zoomBottom, SMALL_BTN_W, SMALL_BTN_H)) {
            return Hit.ZOOM_MINUS;
        }
        if (contains(mouseX, mouseY, pos.zoomPlusLeft, pos.zoomBottom, SMALL_BTN_W, SMALL_BTN_H)) {
            return Hit.ZOOM_PLUS;
        }
        if (contains(mouseX, mouseY, pos.debugLeft, pos.debugBottom, 220, SMALL_BTN_H)) {
            return Hit.DEBUG_TOGGLE;
        }

        GameAction[] actions = GameAction.values();
        for (int i = 0; i < actions.length; i++) {
            int rowBottom = keybindRowBottom(pos, i);
            if (contains(mouseX, mouseY, pos.keyBtnLeft, rowBottom, KEY_BTN_W, ROW_HEIGHT)) {
                return i;
            }
        }
        return Hit.NONE;
    }

    private static Layout layoutFor(int screenWidth, int screenHeight) {
        Layout pos = new Layout();
        pos.panelLeft = (screenWidth - PANEL_WIDTH) / 2;
        pos.panelBottom = (screenHeight - PANEL_HEIGHT) / 2;
        pos.zoomBottom = pos.panelBottom + PANEL_HEIGHT - 100;
        pos.zoomMinusLeft = pos.panelLeft + 140;
        pos.zoomPlusLeft = pos.zoomMinusLeft + SMALL_BTN_W + 12;
        pos.debugBottom = pos.zoomBottom - 48;
        pos.debugLeft = pos.panelLeft + 32;
        pos.controlsLabelY = pos.debugBottom - 28;
        pos.listTop = pos.controlsLabelY - 20;
        pos.keyBtnLeft = pos.panelLeft + PANEL_WIDTH - KEY_BTN_W - 32;

        int actionCount = GameAction.values().length;
        int lastRowBottom = keybindRowBottom(pos, actionCount - 1);
        pos.footerBottom = lastRowBottom - FOOTER_GAP - FOOTER_BTN_H;
        pos.resetLeft = pos.panelLeft + 32;
        pos.backLeft = pos.panelLeft + PANEL_WIDTH - 160;
        return pos;
    }

    private static int keybindRowBottom(Layout pos, int index) {
        return pos.listTop - (index + 1) * (ROW_HEIGHT + ROW_GAP);
    }

    private static boolean contains(int mx, int my, int left, int bottom, int width, int height) {
        return mx >= left && mx < left + width && my >= bottom && my < bottom + height;
    }

    private void saveInput(InputSettings inputSettings) {
        try {
            inputSettings.save();
        } catch (IOException exception) {
            statusMessage = "Could not save keybinds.";
        }
    }

    private void saveDisplay(DisplaySettings displaySettings) {
        try {
            displaySettings.save();
        } catch (IOException exception) {
            statusMessage = "Could not save display settings.";
        }
    }

    private static final class Layout {
        int panelLeft;
        int panelBottom;
        int zoomBottom;
        int zoomMinusLeft;
        int zoomPlusLeft;
        int debugBottom;
        int debugLeft;
        int controlsLabelY;
        int listTop;
        int keyBtnLeft;
        int footerBottom;
        int resetLeft;
        int backLeft;
    }

    private static final class Hit {
        static final int NONE = -1;
        static final int BACK = -2;
        static final int ZOOM_MINUS = -3;
        static final int ZOOM_PLUS = -4;
        static final int RESET = -5;
        static final int DEBUG_TOGGLE = -6;
    }
}
