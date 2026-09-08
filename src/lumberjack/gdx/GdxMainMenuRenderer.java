package lumberjack.gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.core.GameConfig;
import lumberjack.save.SaveSummary;

import java.util.List;

/**
 * Main menu / load-list drawing and hit tests (Y-up ScreenViewport coords).
 */
public final class GdxMainMenuRenderer {

    public static final int BACK_ACTION = -2;
    public static final int SCROLL_UP_ACTION = -3;
    public static final int SCROLL_DOWN_ACTION = -4;
    public static final int WORLD_EDIT_ACTION = -5;
    private static final int DELETE_ACTION_BASE = -1000;

    private static final Color TITLE = new Color(0.95f, 0.92f, 0.82f, 1f);
    private static final Color SUBTITLE = new Color(0.7f, 0.72f, 0.78f, 1f);
    private static final Color BUTTON_BG = new Color(0.22f, 0.22f, 0.26f, 1f);
    private static final Color BUTTON_HOVER = new Color(0.34f, 0.34f, 0.4f, 1f);
    private static final Color BUTTON_BORDER = new Color(0.75f, 0.75f, 0.8f, 1f);
    private static final Color DELETE_BG = new Color(0.42f, 0.22f, 0.22f, 1f);
    private static final Color DELETE_HOVER = new Color(0.55f, 0.28f, 0.28f, 1f);
    private static final Color TEXT = new Color(0.95f, 0.95f, 0.95f, 1f);
    private static final Color STATUS = new Color(1f, 0.85f, 0.45f, 1f);

    private static final int BUTTON_WIDTH = 360;
    private static final int BUTTON_HEIGHT = 56;
    private static final int BUTTON_GAP = 14;
    private static final int LOAD_ROW_HEIGHT = 48;
    private static final int LOAD_ROW_GAP = 10;
    private static final int LOAD_ROW_WIDTH = 520;
    private static final int DELETE_WIDTH = 100;
    private static final int ROW_GAP = 12;
    private static final int MAX_VISIBLE_SAVES = 8;

    private static final String[] HOME_LABELS = {
            "New Game",
            "Load Game",
            "Settings",
            "Quit"
    };

    private final GlyphLayout layout = new GlyphLayout();

    public static int deleteAction(int index) {
        return DELETE_ACTION_BASE - index;
    }

    public static boolean isDeleteAction(int action) {
        return action <= DELETE_ACTION_BASE;
    }

    public static int deleteIndex(int action) {
        return DELETE_ACTION_BASE - action;
    }

    public void drawHome(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            int screenWidth,
            int screenHeight,
            String statusMessage
    ) {
        int mouseX = GdxInputHelper.mouseX();
        int mouseY = GdxInputHelper.mouseYBottomUp();

        font.setColor(TITLE);
        font.getData().setScale(2.2f);
        layout.setText(font, GameConfig.GAME_TITLE);
        font.draw(batch, GameConfig.GAME_TITLE, screenWidth / 2f - layout.width / 2f, screenHeight - 120);
        font.getData().setScale(1.4f);

        int totalHeight = BUTTON_HEIGHT * HOME_LABELS.length + BUTTON_GAP * (HOME_LABELS.length - 1);
        int topOfCluster = (screenHeight + totalHeight) / 2 - 20;

        for (int index = 0; index < HOME_LABELS.length; index++) {
            drawButton(
                    batch,
                    font,
                    textures,
                    HOME_LABELS[index],
                    buttonBottom(topOfCluster, index),
                    screenWidth,
                    mouseX,
                    mouseY
            );
        }

        drawWorldEditButton(batch, font, textures, screenWidth, screenHeight, mouseX, mouseY);
        drawStatus(batch, font, statusMessage, screenWidth, 48);
    }

    private void drawWorldEditButton(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            int screenWidth,
            int screenHeight,
            int mouseX,
            int mouseY
    ) {
        if (!GameConfig.ENABLE_WORLD_EDIT) {
            return;
        }
        int[] box = worldEditButtonBox(screenWidth, screenHeight);
        drawRectButton(
                batch,
                font,
                textures,
                "World Edit",
                box[0],
                box[1],
                box[2],
                box[3],
                mouseX,
                mouseY,
                BUTTON_BG,
                BUTTON_HOVER
        );
    }

    private static int[] worldEditButtonBox(int screenWidth, int screenHeight) {
        int width = 160;
        int height = 48;
        int left = screenWidth - width - 32;
        int bottom = screenHeight / 2 - height / 2;
        return new int[] {left, bottom, width, height};
    }

    public void drawLoadList(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            int screenWidth,
            int screenHeight,
            List<SaveSummary> saves,
            int scrollOffset,
            String statusMessage
    ) {
        int mouseX = GdxInputHelper.mouseX();
        int mouseY = GdxInputHelper.mouseYBottomUp();

        font.setColor(TITLE);
        font.getData().setScale(1.8f);
        layout.setText(font, "Load Game");
        font.draw(batch, "Load Game", screenWidth / 2f - layout.width / 2f, screenHeight - 100);
        font.getData().setScale(1.4f);

        int maxScroll = maxScroll(saves.size());
        int clampedScroll = clampScroll(scrollOffset, saves.size());
        int visible = Math.min(saves.size() - clampedScroll, MAX_VISIBLE_SAVES);
        int topOfList = screenHeight - 180;
        int totalRowWidth = LOAD_ROW_WIDTH + ROW_GAP + DELETE_WIDTH;
        int rowLeft = screenWidth / 2 - totalRowWidth / 2;

        for (int row = 0; row < visible; row++) {
            int index = clampedScroll + row;
            SaveSummary save = saves.get(index);
            int bottom = topOfList - (row + 1) * LOAD_ROW_HEIGHT - row * LOAD_ROW_GAP;
            String label = save.isLoadable()
                    ? save.getSaveTitle() + "  —  " + save.getDetailText()
                    : save.getSaveTitle() + "  —  corrupt";
            drawRectButton(
                    batch,
                    font,
                    textures,
                    label,
                    rowLeft,
                    bottom,
                    LOAD_ROW_WIDTH,
                    LOAD_ROW_HEIGHT,
                    mouseX,
                    mouseY,
                    BUTTON_BG,
                    BUTTON_HOVER
            );
            drawRectButton(
                    batch,
                    font,
                    textures,
                    "Delete",
                    rowLeft + LOAD_ROW_WIDTH + ROW_GAP,
                    bottom,
                    DELETE_WIDTH,
                    LOAD_ROW_HEIGHT,
                    mouseX,
                    mouseY,
                    DELETE_BG,
                    DELETE_HOVER
            );
        }

        if (maxScroll > 0) {
            int scrollBtnSize = 40;
            int scrollLeft = rowLeft + totalRowWidth + 16;
            int upBottom = topOfList - LOAD_ROW_HEIGHT;
            int downBottom = topOfList - visible * LOAD_ROW_HEIGHT - (visible - 1) * LOAD_ROW_GAP;
            drawRectButton(
                    batch,
                    font,
                    textures,
                    "^",
                    scrollLeft,
                    upBottom,
                    scrollBtnSize,
                    scrollBtnSize,
                    mouseX,
                    mouseY,
                    BUTTON_BG,
                    BUTTON_HOVER
            );
            drawRectButton(
                    batch,
                    font,
                    textures,
                    "v",
                    scrollLeft,
                    downBottom,
                    scrollBtnSize,
                    scrollBtnSize,
                    mouseX,
                    mouseY,
                    BUTTON_BG,
                    BUTTON_HOVER
            );

            font.setColor(SUBTITLE);
            String hint = "Up/Down (" + (clampedScroll + 1) + "-" + (clampedScroll + visible)
                    + " of " + saves.size() + ")";
            layout.setText(font, hint);
            font.draw(batch, hint, screenWidth / 2f - layout.width / 2f, 150);
        }

        int backBottom = 60;
        drawWideButton(batch, font, textures, "Back", backBottom, screenWidth, mouseX, mouseY, BUTTON_WIDTH, BUTTON_HEIGHT);
        drawStatus(batch, font, statusMessage, screenWidth, backBottom + BUTTON_HEIGHT + 24);
    }

    public static int maxScroll(int saveCount) {
        return Math.max(0, saveCount - MAX_VISIBLE_SAVES);
    }

    public static int clampScroll(int scrollOffset, int saveCount) {
        return Math.max(0, Math.min(scrollOffset, maxScroll(saveCount)));
    }

    public int getHomeButtonAt(int screenWidth, int screenHeight, int mouseX, int mouseY) {
        if (GameConfig.ENABLE_WORLD_EDIT) {
            int[] worldEdit = worldEditButtonBox(screenWidth, screenHeight);
            if (mouseX >= worldEdit[0] && mouseX < worldEdit[0] + worldEdit[2]
                    && mouseY >= worldEdit[1] && mouseY < worldEdit[1] + worldEdit[3]) {
                return WORLD_EDIT_ACTION;
            }
        }

        int totalHeight = BUTTON_HEIGHT * HOME_LABELS.length + BUTTON_GAP * (HOME_LABELS.length - 1);
        int topOfCluster = (screenHeight + totalHeight) / 2 - 20;
        int left = screenWidth / 2 - BUTTON_WIDTH / 2;

        for (int index = 0; index < HOME_LABELS.length; index++) {
            int bottom = buttonBottom(topOfCluster, index);
            if (mouseX >= left && mouseX < left + BUTTON_WIDTH
                    && mouseY >= bottom && mouseY < bottom + BUTTON_HEIGHT) {
                return index;
            }
        }
        return -1;
    }

    public int getLoadActionAt(
            int screenWidth,
            int screenHeight,
            int mouseX,
            int mouseY,
            int saveCount,
            int scrollOffset
    ) {
        int clampedScroll = clampScroll(scrollOffset, saveCount);
        int visible = Math.min(saveCount - clampedScroll, MAX_VISIBLE_SAVES);
        int topOfList = screenHeight - 180;
        int totalRowWidth = LOAD_ROW_WIDTH + ROW_GAP + DELETE_WIDTH;
        int rowLeft = screenWidth / 2 - totalRowWidth / 2;

        int maxScroll = maxScroll(saveCount);
        if (maxScroll > 0) {
            int scrollBtnSize = 40;
            int scrollLeft = rowLeft + totalRowWidth + 16;
            int upBottom = topOfList - LOAD_ROW_HEIGHT;
            int downBottom = topOfList - visible * LOAD_ROW_HEIGHT - (visible - 1) * LOAD_ROW_GAP;
            if (mouseX >= scrollLeft && mouseX < scrollLeft + scrollBtnSize
                    && mouseY >= upBottom && mouseY < upBottom + scrollBtnSize) {
                return SCROLL_UP_ACTION;
            }
            if (mouseX >= scrollLeft && mouseX < scrollLeft + scrollBtnSize
                    && mouseY >= downBottom && mouseY < downBottom + scrollBtnSize) {
                return SCROLL_DOWN_ACTION;
            }
        }

        for (int row = 0; row < visible; row++) {
            int index = clampedScroll + row;
            int bottom = topOfList - (row + 1) * LOAD_ROW_HEIGHT - row * LOAD_ROW_GAP;

            if (mouseX >= rowLeft && mouseX < rowLeft + LOAD_ROW_WIDTH
                    && mouseY >= bottom && mouseY < bottom + LOAD_ROW_HEIGHT) {
                return index;
            }

            int deleteLeft = rowLeft + LOAD_ROW_WIDTH + ROW_GAP;
            if (mouseX >= deleteLeft && mouseX < deleteLeft + DELETE_WIDTH
                    && mouseY >= bottom && mouseY < bottom + LOAD_ROW_HEIGHT) {
                return deleteAction(index);
            }
        }

        int backLeft = screenWidth / 2 - BUTTON_WIDTH / 2;
        int backBottom = 60;
        if (mouseX >= backLeft && mouseX < backLeft + BUTTON_WIDTH
                && mouseY >= backBottom && mouseY < backBottom + BUTTON_HEIGHT) {
            return BACK_ACTION;
        }
        return -1;
    }

    private static int buttonBottom(int topOfCluster, int index) {
        return topOfCluster - (index + 1) * BUTTON_HEIGHT - index * BUTTON_GAP;
    }

    private void drawButton(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            String label,
            int bottom,
            int screenWidth,
            int mouseX,
            int mouseY
    ) {
        drawWideButton(batch, font, textures, label, bottom, screenWidth, mouseX, mouseY, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private void drawWideButton(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            String label,
            int bottom,
            int screenWidth,
            int mouseX,
            int mouseY,
            int width,
            int height
    ) {
        int left = screenWidth / 2 - width / 2;
        drawRectButton(batch, font, textures, label, left, bottom, width, height, mouseX, mouseY, BUTTON_BG, BUTTON_HOVER);
    }

    private void drawRectButton(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            String label,
            int left,
            int bottom,
            int width,
            int height,
            int mouseX,
            int mouseY,
            Color background,
            Color hover
    ) {
        boolean hovered = mouseX >= left && mouseX < left + width
                && mouseY >= bottom && mouseY < bottom + height;

        GdxBatchUtils.drawSolid(batch, textures, hovered ? hover : background, left, bottom, width, height);
        GdxBatchUtils.drawSolid(batch, textures, BUTTON_BORDER, left, bottom + height - 2, width, 2);

        font.setColor(TEXT);
        layout.setText(font, label);
        float textX = left + Math.max(12, (width - layout.width) / 2f);
        font.draw(batch, label, textX, bottom + (height + layout.height) / 2f);
    }

    private void drawStatus(SpriteBatch batch, BitmapFont font, String statusMessage, int screenWidth, int y) {
        if (statusMessage == null || statusMessage.isBlank()) {
            return;
        }
        font.setColor(STATUS);
        layout.setText(font, statusMessage);
        font.draw(batch, statusMessage, screenWidth / 2f - layout.width / 2f, y);
    }
}
