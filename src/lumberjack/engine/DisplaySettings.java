package lumberjack.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import lumberjack.core.GameConfig;

/**
 * Player-configurable display options, persisted under settings/display.properties.
 */
public final class DisplaySettings {

    public static final int MIN_ZOOM_PERCENT = 0;
    public static final int MAX_ZOOM_PERCENT = 100;
    /** Higher default so 16×16 tiles aren't tiny on 1080p+. */
    public static final int DEFAULT_ZOOM_PERCENT = 85;

    private static final Path SETTINGS_PATH = Paths.get("settings", "display.properties");
    private static final String ZOOM_KEY = "zoom_percent";
    private static final String DEBUG_KEY = "enable_debug";

    private int zoomPercent = DEFAULT_ZOOM_PERCENT;
    private boolean debugEnabled;

    public static DisplaySettings load() {
        DisplaySettings settings = new DisplaySettings();

        if (!Files.exists(SETTINGS_PATH)) {
            return settings;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(SETTINGS_PATH)) {
            properties.load(input);
        } catch (IOException exception) {
            return settings;
        }

        settings.setZoomPercent(parseZoomPercent(properties.getProperty(ZOOM_KEY)));
        settings.setDebugEnabled(parseBoolean(properties.getProperty(DEBUG_KEY), false));
        return settings;
    }

    public void save() throws IOException {
        Files.createDirectories(SETTINGS_PATH.getParent());

        Properties properties = new Properties();
        properties.setProperty(ZOOM_KEY, String.valueOf(getZoomPercent()));
        properties.setProperty(DEBUG_KEY, String.valueOf(debugEnabled));

        try (OutputStream output = Files.newOutputStream(SETTINGS_PATH)) {
            properties.store(output, "Lumberjack display settings");
        }
    }

    public int getZoomPercent() {
        return zoomPercent;
    }

    public void setZoomPercent(int zoomPercent) {
        this.zoomPercent = clampZoomPercent(zoomPercent);
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public void toggleDebugEnabled() {
        debugEnabled = !debugEnabled;
    }

    /**
     * World render scale. 0% zoom = 1.0 (original view); 100% zoom = max zoom-in.
     * Interpolates linearly by visible world area so slider steps feel even to the player.
     */
    public double getZoomScale() {
        double zoomT = zoomPercent / (double) MAX_ZOOM_PERCENT;
        double minVisibleFraction = 1.0 / GameConfig.MAX_VIEW_ZOOM_SCALE;
        double visibleFraction = 1.0 - zoomT * (1.0 - minVisibleFraction);
        return 1.0 / visibleFraction;
    }

    public int screenToWorldX(int screenX, Camera camera) {
        return (int) Math.round(screenX / getZoomScale()) + camera.getX();
    }

    public int screenToWorldY(int screenY, Camera camera) {
        return (int) Math.round(screenY / getZoomScale()) + camera.getY();
    }

    public int getEffectiveViewportWidth(int viewportWidth) {
        return Math.max(1, (int) Math.round(viewportWidth / getZoomScale()));
    }

    public int getEffectiveViewportHeight(int viewportHeight) {
        return Math.max(1, (int) Math.round(viewportHeight / getZoomScale()));
    }

    public void resetToDefaults() {
        zoomPercent = DEFAULT_ZOOM_PERCENT;
        debugEnabled = false;
    }

    private static int parseZoomPercent(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_ZOOM_PERCENT;
        }

        try {
            return clampZoomPercent(Integer.parseInt(value.trim()));
        } catch (NumberFormatException exception) {
            return DEFAULT_ZOOM_PERCENT;
        }
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private static int clampZoomPercent(int zoomPercent) {
        return Math.max(MIN_ZOOM_PERCENT, Math.min(MAX_ZOOM_PERCENT, zoomPercent));
    }
}
