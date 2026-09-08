package lumberjack.inventory;

import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;

/**
 * Minecraft-style left- and right-click stack transfers for any single slot + cursor pair.
 */
public final class StackTransfer {

    private StackTransfer() {
    }

    /**
     * Left click: pick up all, place all, merge all possible, or swap stacks.
     *
     * @return the new contents of the clicked slot
     */
    public static ItemStack leftClick(
            ItemStack slotStack,
            InventoryInteraction cursor,
            ItemRegistry itemRegistry,
            SlotTransferPolicy policy
    ) {
        ItemStack carried = cursor.getCarriedStack();

        if (carried.isEmpty()) {
            if (slotStack.isEmpty() || !policy.canPickUp(slotStack, itemRegistry)) {
                return slotStack;
            }

            cursor.setCarriedFrom(slotStack);
            return ItemStack.empty();
        }

        if (!policy.canPlace(carried, slotStack, itemRegistry)) {
            return slotStack;
        }

        if (slotStack.isEmpty()) {
            cursor.clearCarried();
            return carried.copy();
        }

        if (!policy.canPickUp(slotStack, itemRegistry)) {
            return slotStack;
        }

        if (slotStack.getItemId().equals(carried.getItemId())) {
            return mergeStacks(slotStack, carried, itemRegistry, cursor, carried.getQuantity());
        }

        if (!policy.canSwap(slotStack, carried, itemRegistry)) {
            return slotStack;
        }

        cursor.setCarriedFrom(slotStack);
        return carried.copy();
    }

    /**
     * Right click: pick up half (rounded up), place one, merge one, or swap stacks.
     *
     * @return the new contents of the clicked slot
     */
    public static ItemStack rightClick(
            ItemStack slotStack,
            InventoryInteraction cursor,
            ItemRegistry itemRegistry,
            SlotTransferPolicy policy
    ) {
        ItemStack carried = cursor.getCarriedStack();

        if (carried.isEmpty()) {
            if (slotStack.isEmpty() || !policy.canPickUp(slotStack, itemRegistry)) {
                return slotStack;
            }

            int takeCount = (slotStack.getQuantity() + 1) / 2;
            cursor.setCarriedFrom(ItemStack.of(slotStack.getItemId(), takeCount));

            int remaining = slotStack.getQuantity() - takeCount;
            if (remaining <= 0) {
                return ItemStack.empty();
            }
            return ItemStack.of(slotStack.getItemId(), remaining);
        }

        if (!policy.canPlace(carried, slotStack, itemRegistry)) {
            return slotStack;
        }

        if (slotStack.isEmpty()) {
            placeOneFromCursor(carried, cursor);
            return ItemStack.of(carried.getItemId(), 1);
        }

        if (!policy.canPickUp(slotStack, itemRegistry)) {
            return slotStack;
        }

        if (slotStack.getItemId().equals(carried.getItemId())) {
            int maxStack = itemRegistry.get(slotStack.getItemId()).getMaxStack();
            if (slotStack.getQuantity() >= maxStack) {
                return slotStack;
            }

            placeOneFromCursor(carried, cursor);
            return ItemStack.of(slotStack.getItemId(), slotStack.getQuantity() + 1);
        }

        if (!policy.canSwap(slotStack, carried, itemRegistry)) {
            return slotStack;
        }

        cursor.setCarriedFrom(slotStack);
        return carried.copy();
    }

    private static ItemStack mergeStacks(
            ItemStack slotStack,
            ItemStack carried,
            ItemRegistry itemRegistry,
            InventoryInteraction cursor,
            int moveRequested
    ) {
        int maxStack = itemRegistry.get(slotStack.getItemId()).getMaxStack();
        int space = maxStack - slotStack.getQuantity();
        if (space <= 0) {
            return slotStack;
        }

        int moved = Math.min(space, moveRequested);
        setCarriedQuantity(cursor, carried, carried.getQuantity() - moved);
        return ItemStack.of(slotStack.getItemId(), slotStack.getQuantity() + moved);
    }

    private static void placeOneFromCursor(ItemStack carried, InventoryInteraction cursor) {
        if (carried.getQuantity() <= 1) {
            cursor.clearCarried();
            return;
        }
        cursor.setCarriedFrom(ItemStack.of(carried.getItemId(), carried.getQuantity() - 1));
    }

    private static void setCarriedQuantity(InventoryInteraction cursor, ItemStack carried, int newQuantity) {
        if (newQuantity <= 0) {
            cursor.clearCarried();
        } else {
            cursor.setCarriedFrom(ItemStack.of(carried.getItemId(), newQuantity));
        }
    }
}
