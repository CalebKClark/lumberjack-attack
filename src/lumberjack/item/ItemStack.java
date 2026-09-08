package lumberjack.item;

/**
 * One stack of items sitting in a single inventory slot.
 *
 * Why a separate class instead of just storing item id + count on Inventory?
 * - Empty slots are explicit (isEmpty()) instead of magic numbers like id = -1
 * - Stacking/splitting logic lives in one place
 * - Later we can add durability, enchantments, etc. without rewriting Inventory
 */
public final class ItemStack {

    private final String itemId;
    private int quantity;

    private ItemStack(String itemId, int quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public static ItemStack empty() {
        return new ItemStack(null, 0);
    }

    public static ItemStack of(String itemId, int quantity) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return new ItemStack(itemId, quantity);
    }

    public boolean isEmpty() {
        return itemId == null || quantity <= 0;
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public ItemStack copy() {
        if (isEmpty()) {
            return empty();
        }
        return of(itemId, quantity);
    }
}
