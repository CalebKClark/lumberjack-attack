package lumberjack.engine.input;

/**
 * Tracks keyboard movement state for gameplay.
 * LibGDX polls keys each frame via {@link #setMovementPressed}.
 */
public final class KeyboardInput {

    private boolean upPressed;
    private boolean downPressed;
    private boolean leftPressed;
    private boolean rightPressed;

    public int getHorizontalAxis() {
        int axis = 0;
        if (leftPressed) {
            axis -= 1;
        }
        if (rightPressed) {
            axis += 1;
        }
        return axis;
    }

    public int getVerticalAxis() {
        int axis = 0;
        if (upPressed) {
            axis -= 1;
        }
        if (downPressed) {
            axis += 1;
        }
        return axis;
    }

    /** Clears stuck movement keys after modal UI or focus changes. */
    public void clearAll() {
        upPressed = false;
        downPressed = false;
        leftPressed = false;
        rightPressed = false;
    }

    /** Used by the LibGDX backend to poll keyboard state each frame. */
    public void setMovementPressed(boolean up, boolean down, boolean left, boolean right) {
        upPressed = up;
        downPressed = down;
        leftPressed = left;
        rightPressed = right;
    }
}
