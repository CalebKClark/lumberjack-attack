package lumberjack.gdx;

import java.awt.Rectangle;

/**
 * Layout helpers use top-down Y; ScreenViewport drawing is bottom-up.
 */
public final class GdxUiCoords {

    private GdxUiCoords() {
    }

    public static float bottom(Rectangle topDown, int screenHeight) {
        return screenHeight - topDown.y - topDown.height;
    }

    public static float bottom(int topDownY, int height, int screenHeight) {
        return screenHeight - topDownY - height;
    }

    /** Convert a top-down mouse Y into bottom-up for drawing at the cursor. */
    public static float mouseBottomUp(int mouseYTopDown, int screenHeight) {
        return screenHeight - mouseYTopDown;
    }
}
