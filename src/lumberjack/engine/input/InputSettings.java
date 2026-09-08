package lumberjack.engine.input;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

/**
 * Player-configurable key bindings, persisted under settings/input.properties.
 */
public final class InputSettings {

    private static final Path SETTINGS_PATH = Paths.get("settings", "input.properties");

    private final EnumMap<GameAction, KeyStroke> bindings = new EnumMap<>(GameAction.class);

    public InputSettings() {
        for (GameAction action : GameAction.values()) {
            bindings.put(action, action.getDefaultKeyStroke());
        }
    }

    public static InputSettings load() {
        InputSettings settings = new InputSettings();

        if (!Files.exists(SETTINGS_PATH)) {
            return settings;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(SETTINGS_PATH)) {
            properties.load(input);
        } catch (IOException exception) {
            return settings;
        }

        for (GameAction action : GameAction.values()) {
            String value = properties.getProperty(action.name());
            if (value == null || value.isBlank()) {
                continue;
            }

            KeyStroke keyStroke = parseKeyStroke(value.trim());
            if (keyStroke != null) {
                settings.bindings.put(action, keyStroke);
            }
        }

        return settings;
    }

    /** Parses persisted bindings; numeric values are AWT key codes (see {@link #formatKeyStroke}). */
    private static KeyStroke parseKeyStroke(String value) {
        try {
            int keyCode = Integer.parseInt(value);
            return KeyStroke.getKeyStroke(keyCode, 0);
        } catch (NumberFormatException ignored) {
            return KeyStroke.getKeyStroke(value);
        }
    }

    public void save() throws IOException {
        Files.createDirectories(SETTINGS_PATH.getParent());

        Properties properties = new Properties();
        for (Map.Entry<GameAction, KeyStroke> entry : bindings.entrySet()) {
            properties.setProperty(entry.getKey().name(), formatKeyStroke(entry.getValue()));
        }

        try (OutputStream output = Files.newOutputStream(SETTINGS_PATH)) {
            properties.store(output, "Lumberjack input bindings");
        }
    }

    public KeyStroke getBinding(GameAction action) {
        return bindings.get(action);
    }

    public void setBinding(GameAction action, KeyStroke keyStroke) {
        bindings.put(action, keyStroke);
    }

    public Optional<GameAction> findActionForKey(KeyStroke keyStroke) {
        for (Map.Entry<GameAction, KeyStroke> entry : bindings.entrySet()) {
            if (entry.getValue().equals(keyStroke)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public void resetToDefaults() {
        for (GameAction action : GameAction.values()) {
            bindings.put(action, action.getDefaultKeyStroke());
        }
    }

    public String getHotbarKeyLabel(int hotbarIndex) {
        return displayName(getBinding(GameAction.forHotbarIndex(hotbarIndex)));
    }

    public String getInventoryKeyLabel() {
        return displayName(getBinding(GameAction.INVENTORY));
    }

    public static String formatKeyStroke(KeyStroke keyStroke) {
        if (keyStroke == null) {
            return "UNBOUND";
        }

        StringBuilder builder = new StringBuilder();
        if (keyStroke.getModifiers() != 0) {
            builder.append(KeyStroke.getKeyStroke(keyStroke.getKeyCode(), keyStroke.getModifiers()).toString());
        } else {
            builder.append(keyStroke.getKeyCode());
        }
        return builder.toString();
    }

    public static String displayName(KeyStroke keyStroke) {
        if (keyStroke == null) {
            return "Unbound";
        }

        int modifiers = keyStroke.getModifiers();
        String keyText = KeyEvent.getKeyText(keyStroke.getKeyCode());

        if (modifiers == 0) {
            return keyText;
        }

        return KeyEvent.getModifiersExText(modifiers) + "+" + keyText;
    }
}
