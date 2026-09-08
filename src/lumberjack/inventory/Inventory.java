package lumberjack.inventory;

import lumberjack.core.GameConfig;
import lumberjack.item.ItemDefinition;
import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;
import lumberjack.ui.hud.InventoryListener;

import java.util.Map;

/**
 * Player inventory: 36 slots total.
 *
 * Slot layout (Minecraft-style):
 *   0-8   = hotbar (always visible)
 *   9-35  = backpack (shown in full inventory screen)
 *
 * The hotbar is NOT a separate inventory — it's the first row of the same array.
 * That way moving items between backpack and hotbar is just swapping slot indices.
 */
public final class Inventory {

    private final ItemRegistry itemRegistry;
    private final ItemStack[] slots;
    private int selectedHotbarSlot;
    private InventoryListener inventoryListener;
    private boolean suppressNotifications;

    public Inventory(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
        this.slots = new ItemStack[GameConfig.INVENTORY_SLOTS];
        this.selectedHotbarSlot = 0;

        for (int i = 0; i < slots.length; i++) {
            slots[i] = ItemStack.empty();
        }
    }

    public int getSlotCount() {
        return slots.length;
    }

    public int getHotbarSlotCount() {
        return GameConfig.HOTBAR_SLOTS;
    }

    public int getBackpackSlotCount() {
        return GameConfig.BACKPACK_SLOTS;
    }

    public boolean isHotbarSlot(int slotIndex) {
        return slotIndex >= 0 && slotIndex < GameConfig.HOTBAR_SLOTS;
    }

    public int getSelectedHotbarSlot() {
        return selectedHotbarSlot;
    }

    public void selectHotbarSlot(int slotIndex) {
        if (!isHotbarSlot(slotIndex)) {
            throw new IllegalArgumentException("Hotbar slot must be 0-" + (GameConfig.HOTBAR_SLOTS - 1));
        }
        selectedHotbarSlot = slotIndex;
    }

    public ItemStack getSlot(int slotIndex) {
        validateSlotIndex(slotIndex);
        return slots[slotIndex];
    }

    public void setSlot(int slotIndex, ItemStack stack) {
        validateSlotIndex(slotIndex);
        if (stack != null && !stack.isEmpty() && !canStoreInInventory(stack.getItemId())) {
            return;
        }
        slots[slotIndex] = stack == null || stack.isEmpty() ? ItemStack.empty() : stack.copy();
    }

    public void clearAllSlots() {
        for (int i = 0; i < slots.length; i++) {
            slots[i] = ItemStack.empty();
        }
        selectedHotbarSlot = 0;
    }

    public void setInventoryListener(InventoryListener inventoryListener) {
        this.inventoryListener = inventoryListener;
    }

    public void setSuppressNotifications(boolean suppressNotifications) {
        this.suppressNotifications = suppressNotifications;
    }

    public ItemStack getSelectedHotbarStack() {
        return getSlot(selectedHotbarSlot);
    }

    /**
     * Tries to add items anywhere in the inventory (merge stacks first, then empty slots).
     *
     * @return how many items could NOT be added (0 = everything fit)
     */
    public int addItem(String itemId, int quantity) {
        if (quantity <= 0) {
            return 0;
        }

        if (!canStoreInInventory(itemId)) {
            return quantity;
        }

        ItemDefinition definition = itemRegistry.get(itemId);
        int remaining = quantity;

        remaining = mergeIntoExistingStacks(itemId, definition.getMaxStack(), remaining);
        remaining = fillEmptySlots(itemId, definition.getMaxStack(), remaining);

        int added = quantity - remaining;
        notifyItemsAdded(itemId, added);

        return remaining;
    }

    public int countItem(String itemId) {
        int total = 0;
        for (ItemStack stack : slots) {
            if (!stack.isEmpty() && stack.getItemId().equals(itemId)) {
                total += stack.getQuantity();
            }
        }
        return total;
    }

    public boolean canAddItem(String itemId, int quantity) {
        return computeAddOverflow(itemId, quantity) == 0;
    }

    /**
     * Checks whether the output can fit after the given ingredients are consumed.
     */
    public boolean canFitAfterRemoving(String outputItemId, int outputQuantity, Map<String, Integer> toRemove) {
        if (!itemRegistry.exists(outputItemId)) {
            return false;
        }

        ItemStack[] simulated = new ItemStack[slots.length];
        for (int i = 0; i < slots.length; i++) {
            simulated[i] = slots[i].isEmpty() ? ItemStack.empty() : slots[i].copy();
        }

        for (Map.Entry<String, Integer> entry : toRemove.entrySet()) {
            if (!itemRegistry.exists(entry.getKey())) {
                return false;
            }

            int remaining = entry.getValue();
            for (int i = simulated.length - 1; i >= 0 && remaining > 0; i--) {
                ItemStack stack = simulated[i];
                if (stack.isEmpty() || !stack.getItemId().equals(entry.getKey())) {
                    continue;
                }

                int removed = Math.min(stack.getQuantity(), remaining);
                stack.setQuantity(stack.getQuantity() - removed);
                remaining -= removed;
                if (stack.getQuantity() <= 0) {
                    simulated[i] = ItemStack.empty();
                }
            }

            if (remaining > 0) {
                return false;
            }
        }

        ItemDefinition outputDefinition = itemRegistry.get(outputItemId);
        int maxStack = outputDefinition.getMaxStack();
        int remainingOutput = outputQuantity;

        for (ItemStack stack : simulated) {
            if (stack.isEmpty() || !stack.getItemId().equals(outputItemId)) {
                continue;
            }

            int space = maxStack - stack.getQuantity();
            if (space > 0) {
                remainingOutput -= Math.min(space, remainingOutput);
            }
        }

        for (ItemStack stack : simulated) {
            if (!stack.isEmpty() || remainingOutput <= 0) {
                continue;
            }

            remainingOutput -= Math.min(maxStack, remainingOutput);
        }

        return remainingOutput == 0;
    }

    private int computeAddOverflow(String itemId, int quantity) {
        if (quantity <= 0) {
            return 0;
        }

        ItemDefinition definition = itemRegistry.get(itemId);
        int remaining = quantity;
        remaining = simulateMergeIntoExistingStacks(itemId, definition.getMaxStack(), remaining);
        remaining = simulateFillEmptySlots(itemId, definition.getMaxStack(), remaining);
        return remaining;
    }

    /**
     * Removes up to {@code quantity} of an item from the inventory.
     *
     * @return how many items were actually removed
     */
    public int removeItem(String itemId, int quantity) {
        if (quantity <= 0) {
            return 0;
        }

        int remaining = quantity;
        for (int i = slots.length - 1; i >= 0 && remaining > 0; i--) {
            ItemStack stack = slots[i];
            if (stack.isEmpty() || !stack.getItemId().equals(itemId)) {
                continue;
            }

            int removed = Math.min(stack.getQuantity(), remaining);
            stack.setQuantity(stack.getQuantity() - removed);
            remaining -= removed;

            if (stack.getQuantity() <= 0) {
                slots[i] = ItemStack.empty();
            }
        }

        int removedTotal = quantity - remaining;
        notifyItemsRemoved(itemId, removedTotal);
        return removedTotal;
    }

    private void notifyItemsAdded(String itemId, int quantity) {
        if (suppressNotifications || quantity <= 0 || inventoryListener == null) {
            return;
        }
        inventoryListener.onItemsAdded(itemId, quantity);
    }

    private void notifyItemsRemoved(String itemId, int quantity) {
        if (suppressNotifications || quantity <= 0 || inventoryListener == null) {
            return;
        }
        inventoryListener.onItemsRemoved(itemId, quantity);
    }

    private int mergeIntoExistingStacks(String itemId, int maxStack, int remaining) {
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            ItemStack stack = slots[i];

            if (stack.isEmpty() || !stack.getItemId().equals(itemId)) {
                continue;
            }

            int space = maxStack - stack.getQuantity();
            if (space <= 0) {
                continue;
            }

            int moved = Math.min(space, remaining);
            stack.setQuantity(stack.getQuantity() + moved);
            remaining -= moved;
        }

        return remaining;
    }

    private int fillEmptySlots(String itemId, int maxStack, int remaining) {
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            if (!slots[i].isEmpty()) {
                continue;
            }

            int moved = Math.min(maxStack, remaining);
            slots[i] = ItemStack.of(itemId, moved);
            remaining -= moved;
        }

        return remaining;
    }

    private int simulateMergeIntoExistingStacks(String itemId, int maxStack, int remaining) {
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            ItemStack stack = slots[i];

            if (stack.isEmpty() || !stack.getItemId().equals(itemId)) {
                continue;
            }

            int space = maxStack - stack.getQuantity();
            if (space <= 0) {
                continue;
            }

            remaining -= Math.min(space, remaining);
        }

        return remaining;
    }

    private int simulateFillEmptySlots(String itemId, int maxStack, int remaining) {
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            if (!slots[i].isEmpty()) {
                continue;
            }

            remaining -= Math.min(maxStack, remaining);
        }

        return remaining;
    }

    private void validateSlotIndex(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.length) {
            throw new IndexOutOfBoundsException("Invalid slot index: " + slotIndex);
        }
    }

    private boolean canStoreInInventory(String itemId) {
        return !itemRegistry.get(itemId).isCurrency();
    }

    /** Removes every stack of the given item id and returns the total quantity removed. */
    public int removeAllOfItem(String itemId) {
        int total = 0;
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = slots[i];
            if (!stack.isEmpty() && stack.getItemId().equals(itemId)) {
                total += stack.getQuantity();
                slots[i] = ItemStack.empty();
            }
        }
        return total;
    }
}
