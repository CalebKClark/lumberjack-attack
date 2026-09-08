package lumberjack.interaction;

import lumberjack.economy.PlayerBank;
import lumberjack.inventory.InventoryInteraction;
import lumberjack.inventory.SlotTransferPolicies;
import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;
import lumberjack.world.placeable.WoodChipperMachine;

/**
 * Click handling for wood chipper machine slots.
 */
public final class WoodChipperSlotInteraction {

    private WoodChipperSlotInteraction() {
    }

    public static void handleInputClick(
            WoodChipperMachine machine,
            InventoryInteraction cursor,
            ItemRegistry itemRegistry,
            boolean rightClick
    ) {
        ItemStack updated = cursor.handleSlotClick(
                machine.getInputStack(),
                itemRegistry,
                SlotTransferPolicies.singleItemInput(WoodChipperMachine.INPUT_ITEM_ID),
                rightClick
        );
        machine.setInputStack(updated);
    }

    public static void handleOutputClick(WoodChipperMachine machine, PlayerBank bank, boolean rightClick) {
        ItemStack outputStack = machine.getOutputStack();
        if (outputStack.isEmpty() || !WoodChipperMachine.OUTPUT_ITEM_ID.equals(outputStack.getItemId())) {
            return;
        }

        if (rightClick) {
            bank.depositWoodChips(1);
            if (outputStack.getQuantity() <= 1) {
                machine.setOutputStack(ItemStack.empty());
            } else {
                machine.setOutputStack(ItemStack.of(outputStack.getItemId(), outputStack.getQuantity() - 1));
            }
            return;
        }

        bank.depositWoodChips(outputStack.getQuantity());
        machine.setOutputStack(ItemStack.empty());
    }
}
