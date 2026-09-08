package lumberjack.inventory;

import lumberjack.item.ItemDefinition;
import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;

/**
 * Common {@link SlotTransferPolicy} presets for player and machine slots.
 */
public final class SlotTransferPolicies {

    private SlotTransferPolicies() {
    }

    public static SlotTransferPolicy playerInventory() {
        return new SlotTransferPolicy() {
            @Override
            public boolean canPickUp(ItemStack slotStack, ItemRegistry itemRegistry) {
                return isStorable(slotStack, itemRegistry);
            }

            @Override
            public boolean canPlace(ItemStack cursorStack, ItemStack slotStack, ItemRegistry itemRegistry) {
                return isStorable(cursorStack, itemRegistry);
            }

            @Override
            public boolean canSwap(ItemStack slotStack, ItemStack cursorStack, ItemRegistry itemRegistry) {
                return isStorable(slotStack, itemRegistry) && isStorable(cursorStack, itemRegistry);
            }
        };
    }

    public static SlotTransferPolicy singleItemInput(String allowedItemId) {
        return new SlotTransferPolicy() {
            @Override
            public boolean canPickUp(ItemStack slotStack, ItemRegistry itemRegistry) {
                return !slotStack.isEmpty();
            }

            @Override
            public boolean canPlace(ItemStack cursorStack, ItemStack slotStack, ItemRegistry itemRegistry) {
                return !cursorStack.isEmpty() && allowedItemId.equals(cursorStack.getItemId());
            }

            @Override
            public boolean canSwap(ItemStack slotStack, ItemStack cursorStack, ItemRegistry itemRegistry) {
                return false;
            }
        };
    }

    private static boolean isStorable(ItemStack stack, ItemRegistry itemRegistry) {
        if (stack.isEmpty()) {
            return false;
        }

        ItemDefinition definition = itemRegistry.get(stack.getItemId());
        return !definition.isCurrency();
    }
}
