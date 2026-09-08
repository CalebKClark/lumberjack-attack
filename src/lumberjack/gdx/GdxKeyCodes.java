package lumberjack.gdx;

import java.awt.event.KeyEvent;

import com.badlogic.gdx.Input;

import javax.swing.KeyStroke;

/**
 * Converts between AWT key codes (stored in {@link lumberjack.engine.input.InputSettings})
 * and LibGDX {@link Input.Keys} codes.
 */
public final class GdxKeyCodes {

    private GdxKeyCodes() {
    }

    public static int awtToGdx(int awtKeyCode) {
        if (awtKeyCode >= KeyEvent.VK_A && awtKeyCode <= KeyEvent.VK_Z) {
            return Input.Keys.A + (awtKeyCode - KeyEvent.VK_A);
        }
        if (awtKeyCode >= KeyEvent.VK_0 && awtKeyCode <= KeyEvent.VK_9) {
            return Input.Keys.NUM_0 + (awtKeyCode - KeyEvent.VK_0);
        }
        if (awtKeyCode >= KeyEvent.VK_NUMPAD0 && awtKeyCode <= KeyEvent.VK_NUMPAD9) {
            return Input.Keys.NUMPAD_0 + (awtKeyCode - KeyEvent.VK_NUMPAD0);
        }
        if (awtKeyCode >= KeyEvent.VK_F1 && awtKeyCode <= KeyEvent.VK_F12) {
            return Input.Keys.F1 + (awtKeyCode - KeyEvent.VK_F1);
        }

        return switch (awtKeyCode) {
            case KeyEvent.VK_ESCAPE -> Input.Keys.ESCAPE;
            case KeyEvent.VK_SPACE -> Input.Keys.SPACE;
            case KeyEvent.VK_ENTER -> Input.Keys.ENTER;
            case KeyEvent.VK_SHIFT -> Input.Keys.SHIFT_LEFT;
            case KeyEvent.VK_CONTROL -> Input.Keys.CONTROL_LEFT;
            case KeyEvent.VK_ALT -> Input.Keys.ALT_LEFT;
            case KeyEvent.VK_TAB -> Input.Keys.TAB;
            case KeyEvent.VK_BACK_SPACE -> Input.Keys.BACKSPACE;
            case KeyEvent.VK_UP -> Input.Keys.UP;
            case KeyEvent.VK_DOWN -> Input.Keys.DOWN;
            case KeyEvent.VK_LEFT -> Input.Keys.LEFT;
            case KeyEvent.VK_RIGHT -> Input.Keys.RIGHT;
            case KeyEvent.VK_INSERT -> Input.Keys.INSERT;
            case KeyEvent.VK_DELETE -> Input.Keys.FORWARD_DEL;
            case KeyEvent.VK_HOME -> Input.Keys.HOME;
            case KeyEvent.VK_END -> Input.Keys.END;
            case KeyEvent.VK_PAGE_UP -> Input.Keys.PAGE_UP;
            case KeyEvent.VK_PAGE_DOWN -> Input.Keys.PAGE_DOWN;
            case KeyEvent.VK_CAPS_LOCK -> Input.Keys.CAPS_LOCK;
            case KeyEvent.VK_NUM_LOCK -> Input.Keys.NUM_LOCK;
            case KeyEvent.VK_SCROLL_LOCK -> Input.Keys.SCROLL_LOCK;
            case KeyEvent.VK_PAUSE -> Input.Keys.PAUSE;
            case KeyEvent.VK_PRINTSCREEN -> Input.Keys.PRINT_SCREEN;
            case KeyEvent.VK_COMMA -> Input.Keys.COMMA;
            case KeyEvent.VK_PERIOD -> Input.Keys.PERIOD;
            case KeyEvent.VK_SLASH -> Input.Keys.SLASH;
            case KeyEvent.VK_BACK_SLASH -> Input.Keys.BACKSLASH;
            case KeyEvent.VK_SEMICOLON -> Input.Keys.SEMICOLON;
            case KeyEvent.VK_QUOTE -> Input.Keys.APOSTROPHE;
            case KeyEvent.VK_OPEN_BRACKET -> Input.Keys.LEFT_BRACKET;
            case KeyEvent.VK_CLOSE_BRACKET -> Input.Keys.RIGHT_BRACKET;
            case KeyEvent.VK_BACK_QUOTE -> Input.Keys.GRAVE;
            case KeyEvent.VK_MINUS -> Input.Keys.MINUS;
            case KeyEvent.VK_EQUALS -> Input.Keys.EQUALS;
            case KeyEvent.VK_MULTIPLY -> Input.Keys.NUMPAD_MULTIPLY;
            case KeyEvent.VK_ADD -> Input.Keys.NUMPAD_ADD;
            case KeyEvent.VK_SUBTRACT -> Input.Keys.NUMPAD_SUBTRACT;
            case KeyEvent.VK_DECIMAL -> Input.Keys.NUMPAD_DOT;
            case KeyEvent.VK_DIVIDE -> Input.Keys.NUMPAD_DIVIDE;
            case KeyEvent.VK_META -> Input.Keys.SYM;
            default -> -1;
        };
    }

    public static int gdxToAwt(int gdxKeyCode) {
        if (gdxKeyCode >= Input.Keys.A && gdxKeyCode <= Input.Keys.Z) {
            return KeyEvent.VK_A + (gdxKeyCode - Input.Keys.A);
        }
        if (gdxKeyCode >= Input.Keys.NUM_0 && gdxKeyCode <= Input.Keys.NUM_9) {
            return KeyEvent.VK_0 + (gdxKeyCode - Input.Keys.NUM_0);
        }
        if (gdxKeyCode >= Input.Keys.NUMPAD_0 && gdxKeyCode <= Input.Keys.NUMPAD_9) {
            return KeyEvent.VK_NUMPAD0 + (gdxKeyCode - Input.Keys.NUMPAD_0);
        }
        if (gdxKeyCode >= Input.Keys.F1 && gdxKeyCode <= Input.Keys.F12) {
            return KeyEvent.VK_F1 + (gdxKeyCode - Input.Keys.F1);
        }

        return switch (gdxKeyCode) {
            case Input.Keys.ESCAPE -> KeyEvent.VK_ESCAPE;
            case Input.Keys.SPACE -> KeyEvent.VK_SPACE;
            case Input.Keys.ENTER, Input.Keys.NUMPAD_ENTER -> KeyEvent.VK_ENTER;
            case Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT -> KeyEvent.VK_SHIFT;
            case Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT -> KeyEvent.VK_CONTROL;
            case Input.Keys.ALT_LEFT, Input.Keys.ALT_RIGHT -> KeyEvent.VK_ALT;
            case Input.Keys.TAB -> KeyEvent.VK_TAB;
            case Input.Keys.BACKSPACE -> KeyEvent.VK_BACK_SPACE;
            case Input.Keys.UP -> KeyEvent.VK_UP;
            case Input.Keys.DOWN -> KeyEvent.VK_DOWN;
            case Input.Keys.LEFT -> KeyEvent.VK_LEFT;
            case Input.Keys.RIGHT -> KeyEvent.VK_RIGHT;
            case Input.Keys.INSERT -> KeyEvent.VK_INSERT;
            case Input.Keys.FORWARD_DEL -> KeyEvent.VK_DELETE;
            case Input.Keys.HOME -> KeyEvent.VK_HOME;
            case Input.Keys.END -> KeyEvent.VK_END;
            case Input.Keys.PAGE_UP -> KeyEvent.VK_PAGE_UP;
            case Input.Keys.PAGE_DOWN -> KeyEvent.VK_PAGE_DOWN;
            case Input.Keys.CAPS_LOCK -> KeyEvent.VK_CAPS_LOCK;
            case Input.Keys.NUM_LOCK -> KeyEvent.VK_NUM_LOCK;
            case Input.Keys.SCROLL_LOCK -> KeyEvent.VK_SCROLL_LOCK;
            case Input.Keys.PAUSE -> KeyEvent.VK_PAUSE;
            case Input.Keys.PRINT_SCREEN -> KeyEvent.VK_PRINTSCREEN;
            case Input.Keys.COMMA -> KeyEvent.VK_COMMA;
            case Input.Keys.PERIOD -> KeyEvent.VK_PERIOD;
            case Input.Keys.SLASH -> KeyEvent.VK_SLASH;
            case Input.Keys.BACKSLASH -> KeyEvent.VK_BACK_SLASH;
            case Input.Keys.SEMICOLON -> KeyEvent.VK_SEMICOLON;
            case Input.Keys.APOSTROPHE -> KeyEvent.VK_QUOTE;
            case Input.Keys.LEFT_BRACKET -> KeyEvent.VK_OPEN_BRACKET;
            case Input.Keys.RIGHT_BRACKET -> KeyEvent.VK_CLOSE_BRACKET;
            case Input.Keys.GRAVE -> KeyEvent.VK_BACK_QUOTE;
            case Input.Keys.MINUS -> KeyEvent.VK_MINUS;
            case Input.Keys.EQUALS -> KeyEvent.VK_EQUALS;
            case Input.Keys.NUMPAD_MULTIPLY -> KeyEvent.VK_MULTIPLY;
            case Input.Keys.NUMPAD_ADD -> KeyEvent.VK_ADD;
            case Input.Keys.NUMPAD_SUBTRACT -> KeyEvent.VK_SUBTRACT;
            case Input.Keys.NUMPAD_DOT -> KeyEvent.VK_DECIMAL;
            case Input.Keys.NUMPAD_DIVIDE -> KeyEvent.VK_DIVIDE;
            case Input.Keys.SYM -> KeyEvent.VK_META;
            default -> -1;
        };
    }

    public static KeyStroke keyStrokeFromGdx(int gdxKeyCode) {
        int awt = gdxToAwt(gdxKeyCode);
        if (awt < 0) {
            return null;
        }
        return KeyStroke.getKeyStroke(awt, 0);
    }

    public static boolean isActionKeyPressed(com.badlogic.gdx.Input input, KeyStroke keyStroke) {
        if (keyStroke == null) {
            return false;
        }
        return anyMappedKeyMatches(input, keyStroke.getKeyCode(), false);
    }

    public static boolean isActionKeyJustPressed(com.badlogic.gdx.Input input, KeyStroke keyStroke) {
        if (keyStroke == null) {
            return false;
        }
        return anyMappedKeyMatches(input, keyStroke.getKeyCode(), true);
    }

    private static boolean anyMappedKeyMatches(com.badlogic.gdx.Input input, int awtKeyCode, boolean justPressed) {
        return switch (awtKeyCode) {
            case KeyEvent.VK_SHIFT -> matchesEither(input, Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT, justPressed);
            case KeyEvent.VK_CONTROL -> matchesEither(input, Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT, justPressed);
            case KeyEvent.VK_ALT -> matchesEither(input, Input.Keys.ALT_LEFT, Input.Keys.ALT_RIGHT, justPressed);
            case KeyEvent.VK_ENTER -> matchesEither(input, Input.Keys.ENTER, Input.Keys.NUMPAD_ENTER, justPressed);
            default -> {
                int gdx = awtToGdx(awtKeyCode);
                yield gdx >= 0 && keyMatches(input, gdx, justPressed);
            }
        };
    }

    private static boolean matchesEither(
            com.badlogic.gdx.Input input,
            int first,
            int second,
            boolean justPressed
    ) {
        return keyMatches(input, first, justPressed) || keyMatches(input, second, justPressed);
    }

    private static boolean keyMatches(com.badlogic.gdx.Input input, int gdxKeyCode, boolean justPressed) {
        return justPressed ? input.isKeyJustPressed(gdxKeyCode) : input.isKeyPressed(gdxKeyCode);
    }
}
