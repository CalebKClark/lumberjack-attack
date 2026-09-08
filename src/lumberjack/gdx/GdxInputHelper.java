package lumberjack.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import lumberjack.engine.input.GameAction;
import lumberjack.engine.input.InputSettings;

import javax.swing.KeyStroke;

/**
 * Mouse/key helpers. LibGDX {@link Input#getY()} is top-down (0 = top), matching HUD layouts.
 * ScreenViewport drawing is bottom-up (0 = bottom) — use {@link #mouseYBottomUp()}.
 */
public final class GdxInputHelper {

    private GdxInputHelper() {
    }

    public static int mouseX() {
        return Gdx.input.getX();
    }

    /** Top-down Y (0 at top) — use with HotbarLayout and other HUD layouts. */
    public static int mouseYTopDown() {
        return Gdx.input.getY();
    }

    /** Bottom-up Y (0 at bottom) — use with ScreenViewport button hit-tests. */
    public static int mouseYBottomUp() {
        return Gdx.graphics.getHeight() - Gdx.input.getY();
    }

    public static boolean isActionJustPressed(GameAction action, InputSettings inputSettings) {
        KeyStroke keyStroke = inputSettings.getBinding(action);
        return GdxKeyCodes.isActionKeyJustPressed(Gdx.input, keyStroke);
    }

    public static boolean isPrimaryMouseJustPressed() {
        return Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
    }

    public static boolean isSecondaryMouseJustPressed() {
        return Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT);
    }
}
