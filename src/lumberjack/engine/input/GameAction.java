package lumberjack.engine.input;

import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

/**
 * Bindable in-game actions shown in the settings menu.
 */
public enum GameAction {

    HOTBAR_SLOT_1("Hotbar Slot 1", KeyEvent.VK_1),
    HOTBAR_SLOT_2("Hotbar Slot 2", KeyEvent.VK_2),
    HOTBAR_SLOT_3("Hotbar Slot 3", KeyEvent.VK_3),
    HOTBAR_SLOT_4("Hotbar Slot 4", KeyEvent.VK_4),
    HOTBAR_SLOT_5("Hotbar Slot 5", KeyEvent.VK_5),
    HOTBAR_SLOT_6("Hotbar Slot 6", KeyEvent.VK_6),
    HOTBAR_SLOT_7("Hotbar Slot 7", KeyEvent.VK_7),
    HOTBAR_SLOT_8("Hotbar Slot 8", KeyEvent.VK_8),
    HOTBAR_SLOT_9("Hotbar Slot 9", KeyEvent.VK_9),
    INVENTORY("Inventory", KeyEvent.VK_E);

    private final String label;
    private final int defaultKeyCode;

    GameAction(String label, int defaultKeyCode) {
        this.label = label;
        this.defaultKeyCode = defaultKeyCode;
    }

    public String getLabel() {
        return label;
    }

    public KeyStroke getDefaultKeyStroke() {
        return KeyStroke.getKeyStroke(defaultKeyCode, 0);
    }

    public int getHotbarIndex() {
        if (name().startsWith("HOTBAR_SLOT_")) {
            return Integer.parseInt(name().substring("HOTBAR_SLOT_".length())) - 1;
        }
        return -1;
    }

    public boolean isHotbarAction() {
        return getHotbarIndex() >= 0;
    }

    public static GameAction forHotbarIndex(int hotbarIndex) {
        if (hotbarIndex < 0 || hotbarIndex > 8) {
            throw new IllegalArgumentException("Hotbar index must be 0-8: " + hotbarIndex);
        }
        return values()[hotbarIndex];
    }
}
