package lumberjack.gdx;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import lumberjack.core.GameConfig;

/**
 * Desktop entry point for the LibGDX build.
 */
public final class DesktopLauncher {

    public static void main(String[] args) {
        // Avoid accidental AWT window creation if any shared code touches Toolkit.
        System.setProperty("java.awt.headless", "true");

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(GameConfig.GAME_TITLE + " " + GameConfig.GAME_VERSION);
        config.setWindowedMode(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setAutoIconify(false);

        new Lwjgl3Application(new LumberjackGame(), config);
    }

    private DesktopLauncher() {
    }
}
