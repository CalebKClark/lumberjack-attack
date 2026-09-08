package lumberjack.world.placeable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lumberjack.inventory.ItemContainer;
import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;
import lumberjack.save.SaveData;

/**
 * Tracks chest storage contents per placed chest.
 */
public final class ChestManager {

    public static final String CHEST_ITEM_ID = "chest";

    private final ItemRegistry itemRegistry;
    private final Map<PlacedObjectKey, ItemContainer> chests = new HashMap<>();

    public ChestManager(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public ItemContainer getOrCreate(PlacedObjectKey key) {
        return chests.computeIfAbsent(key, ignored -> ItemContainer.createChest(itemRegistry));
    }

    public ItemContainer get(PlacedObjectKey key) {
        return chests.get(key);
    }

    public void remove(PlacedObjectKey key) {
        chests.remove(key);
    }

    public void clear() {
        chests.clear();
    }

    public List<SaveData.ChestSlotEntry> exportStates() {
        List<SaveData.ChestSlotEntry> entries = new ArrayList<>();
        for (Map.Entry<PlacedObjectKey, ItemContainer> entry : chests.entrySet()) {
            PlacedObjectKey key = entry.getKey();
            ItemContainer container = entry.getValue();
            for (int slot = 0; slot < container.getSlotCount(); slot++) {
                ItemStack stack = container.getSlot(slot);
                if (!stack.isEmpty()) {
                    entries.add(new SaveData.ChestSlotEntry(
                            key.mapId(),
                            key.col(),
                            key.row(),
                            slot,
                            stack.getItemId(),
                            stack.getQuantity()
                    ));
                }
            }
        }
        return entries;
    }

    public void restoreAll(List<SaveData.ChestSlotEntry> entries) {
        clear();
        for (SaveData.ChestSlotEntry entry : entries) {
            PlacedObjectKey key = new PlacedObjectKey(entry.getMapId(), entry.getCol(), entry.getRow());
            ItemContainer container = getOrCreate(key);
            container.setSlot(entry.getSlotIndex(), ItemStack.of(entry.getItemId(), entry.getQuantity()));
        }
    }
}
