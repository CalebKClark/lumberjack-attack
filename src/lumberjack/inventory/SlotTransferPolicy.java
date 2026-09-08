package lumberjack.inventory;

import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;

/**
 * Rules governing what may enter or leave a single slot during stack transfers.
 */
public interface SlotTransferPolicy {

    /** Whether the player may pick up some or all of {@code slotStack} from this slot. */
    boolean canPickUp(ItemStack slotStack, ItemRegistry itemRegistry);

    /** Whether one or more items from {@code cursorStack} may be placed into this slot. */
    boolean canPlace(ItemStack cursorStack, ItemStack slotStack, ItemRegistry itemRegistry);

    /** Whether a full stack swap is allowed when cursor and slot hold different items. */
    boolean canSwap(ItemStack slotStack, ItemStack cursorStack, ItemRegistry itemRegistry);
}
