package lumberjack.inventory;

import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;

/**
 * Fixed-size item storage (chests, crates, etc.) using the same slot rules as {@link Inventory}.
 */
public final class ItemContainer {

    public static final int CHEST_SLOT_COUNT = 27;
    public static final int CHEST_COLUMNS = 9;

    private final ItemRegistry itemRegistry;
    private final ItemStack[] slots;

    public ItemContainer(ItemRegistry itemRegistry, int slotCount) {
        this.itemRegistry = itemRegistry;
        this.slots = new ItemStack[slotCount];
        clear();
    }

    public static ItemContainer createChest(ItemRegistry itemRegistry) {
        return new ItemContainer(itemRegistry, CHEST_SLOT_COUNT);
    }

    public int getSlotCount() {
        return slots.length;
    }

    public ItemStack getSlot(int slotIndex) {
        validateSlotIndex(slotIndex);
        return slots[slotIndex];
    }

    public void setSlot(int slotIndex, ItemStack stack) {
        validateSlotIndex(slotIndex);
        if (stack != null && !stack.isEmpty() && !canStoreInContainer(stack.getItemId())) {
            return;
        }
        slots[slotIndex] = stack == null || stack.isEmpty() ? ItemStack.empty() : stack.copy();
    }

    public void clear() {
        for (int i = 0; i < slots.length; i++) {
            slots[i] = ItemStack.empty();
        }
    }

    public boolean isEmpty() {
        for (ItemStack stack : slots) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean canStoreInContainer(String itemId) {
        return !itemRegistry.get(itemId).isCurrency();
    }

    private void validateSlotIndex(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.length) {
            throw new IndexOutOfBoundsException("Invalid container slot index: " + slotIndex);
        }
    }
}
