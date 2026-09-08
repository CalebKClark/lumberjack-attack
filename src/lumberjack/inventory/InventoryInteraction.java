package lumberjack.inventory;

import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;

/**
 * Handles Minecraft-style inventory cursor interactions for the player inventory.
 */
public final class InventoryInteraction {

    private static final SlotTransferPolicy PLAYER_POLICY = SlotTransferPolicies.playerInventory();

    private ItemStack carriedStack = ItemStack.empty();

    public ItemStack getCarriedStack() {
        return carriedStack;
    }

    public boolean isCarrying() {
        return !carriedStack.isEmpty();
    }

    public void clearCarried() {
        carriedStack = ItemStack.empty();
    }

    public void setCarriedFrom(ItemStack stack) {
        carriedStack = stack.isEmpty() ? ItemStack.empty() : stack.copy();
    }

    public void handleLeftClick(Inventory inventory, ItemRegistry itemRegistry, int slotIndex) {
        handleClick(inventory, itemRegistry, slotIndex, false);
    }

    public void handleRightClick(Inventory inventory, ItemRegistry itemRegistry, int slotIndex) {
        handleClick(inventory, itemRegistry, slotIndex, true);
    }

    private void handleClick(Inventory inventory, ItemRegistry itemRegistry, int slotIndex, boolean rightClick) {
        ItemStack slotStack = inventory.getSlot(slotIndex);
        ItemStack updated = rightClick
                ? StackTransfer.rightClick(slotStack, this, itemRegistry, PLAYER_POLICY)
                : StackTransfer.leftClick(slotStack, this, itemRegistry, PLAYER_POLICY);
        inventory.setSlot(slotIndex, updated);
    }

    /**
     * Left- or right-clicks an arbitrary slot using custom transfer rules (machine slots, etc.).
     *
     * @return the new slot contents after the click
     */
    public ItemStack handleSlotClick(
            ItemStack slotStack,
            ItemRegistry itemRegistry,
            SlotTransferPolicy policy,
            boolean rightClick
    ) {
        return rightClick
                ? StackTransfer.rightClick(slotStack, this, itemRegistry, policy)
                : StackTransfer.leftClick(slotStack, this, itemRegistry, policy);
    }

    /**
     * Tries to put the carried stack back into the inventory when closing the menu.
     * Currency on the cursor is deposited into the bank instead.
     *
     * @return true if the cursor is empty afterward (safe to close)
     */
    public boolean stowCarriedStack(
            Inventory inventory,
            ItemRegistry itemRegistry,
            lumberjack.economy.PlayerBank bank
    ) {
        if (!isCarrying()) {
            return true;
        }

        if (itemRegistry.get(carriedStack.getItemId()).isCurrency()) {
            bank.depositWoodChips(carriedStack.getQuantity());
            clearCarried();
            return true;
        }

        return stowCarriedStack(inventory, itemRegistry);
    }

    /**
     * Tries to put the carried stack back into the inventory when closing the menu.
     *
     * @return true if the cursor is empty afterward (safe to close)
     */
    public boolean stowCarriedStack(Inventory inventory, ItemRegistry itemRegistry) {
        if (!isCarrying()) {
            return true;
        }

        String itemId = carriedStack.getItemId();
        int quantity = carriedStack.getQuantity();

        int remaining = inventory.addItem(itemId, quantity);
        if (remaining <= 0) {
            clearCarried();
            return true;
        }

        if (remaining < quantity) {
            carriedStack = ItemStack.of(itemId, remaining);
            quantity = remaining;
        }

        for (int slot = 0; slot < inventory.getSlotCount(); slot++) {
            if (!inventory.getSlot(slot).isEmpty()) {
                continue;
            }

            inventory.setSlot(slot, ItemStack.of(itemId, quantity));
            clearCarried();
            return true;
        }

        return false;
    }
}
