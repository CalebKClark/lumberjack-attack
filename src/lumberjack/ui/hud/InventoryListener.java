package lumberjack.ui.hud;

/**
 * Receives inventory quantity changes for popup notifications.
 */
public interface InventoryListener {

    void onItemsAdded(String itemId, int quantity);

    void onItemsRemoved(String itemId, int quantity);
}
