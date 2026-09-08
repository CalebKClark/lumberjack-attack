package lumberjack.world.placeable;

import lumberjack.core.GameConfig;
import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;

/**
 * Input/output state and smelting-style processing for one wood chipper.
 * Progress advances only while the in-game clock is running.
 */
public final class WoodChipperMachine {

    public static final String MACHINE_ITEM_ID = "wood_chipper";
    public static final String INPUT_ITEM_ID = "pine_log";
    public static final String OUTPUT_ITEM_ID = "wood_chips";
    public static final int CHIPS_PER_LOG = 5;

    /** Elapsed game-time ms required per log (5 in-game minutes at default clock speed). */
    public static final double PROCESS_DURATION_MS =
            GameConfig.WOOD_CHIPPER_PROCESS_GAME_MINUTES * (double) GameConfig.REAL_MS_PER_GAME_MINUTE;

    private ItemStack inputStack = ItemStack.empty();
    private ItemStack outputStack = ItemStack.empty();
    private double progressMs;
    private boolean processing;

    public ItemStack getInputStack() {
        return inputStack;
    }

    public ItemStack getOutputStack() {
        return outputStack;
    }

    public double getProgressMs() {
        return progressMs;
    }

    public double getProgressRatio() {
        if (PROCESS_DURATION_MS <= 0) {
            return 0;
        }
        return Math.min(1.0, progressMs / PROCESS_DURATION_MS);
    }

    public boolean isProcessing() {
        return processing;
    }

    /** True when input and output are both empty (safe to pick up / destroy). */
    public boolean isEmpty() {
        return inputStack.isEmpty() && outputStack.isEmpty();
    }

    public void setInputStack(ItemStack stack) {
        inputStack = stack == null || stack.isEmpty() ? ItemStack.empty() : stack.copy();
        normalizeState();
    }

    public void setOutputStack(ItemStack stack) {
        outputStack = stack == null || stack.isEmpty() ? ItemStack.empty() : stack.copy();
    }

    public void restore(
            String inputItemId,
            int inputQuantity,
            String outputItemId,
            int outputQuantity,
            double savedProgressMs,
            boolean savedProcessing
    ) {
        inputStack = toStack(inputItemId, inputQuantity);
        outputStack = toStack(outputItemId, outputQuantity);
        progressMs = Math.max(0, savedProgressMs);
        processing = savedProcessing;
        normalizeState();
    }

    public void update(double elapsedMs, ItemRegistry itemRegistry) {
        if (elapsedMs <= 0) {
            return;
        }

        normalizeState();

        if (processing) {
            if (!canAcceptOutputBatch(itemRegistry) || !hasProcessableInput()) {
                return;
            }

            progressMs += elapsedMs;
            while (progressMs >= PROCESS_DURATION_MS) {
                if (!completeOneBatch(itemRegistry)) {
                    progressMs = Math.min(progressMs, PROCESS_DURATION_MS);
                    break;
                }
                progressMs -= PROCESS_DURATION_MS;
            }
            return;
        }

        if (hasProcessableInput() && canAcceptOutputBatch(itemRegistry)) {
            processing = true;
            progressMs = 0;
        }
    }

    private boolean completeOneBatch(ItemRegistry itemRegistry) {
        if (!hasProcessableInput() || !canAcceptOutputBatch(itemRegistry)) {
            processing = false;
            return false;
        }

        if (inputStack.getQuantity() <= 1) {
            inputStack = ItemStack.empty();
        } else {
            inputStack.setQuantity(inputStack.getQuantity() - 1);
        }

        if (outputStack.isEmpty()) {
            outputStack = ItemStack.of(OUTPUT_ITEM_ID, CHIPS_PER_LOG);
        } else {
            outputStack.setQuantity(outputStack.getQuantity() + CHIPS_PER_LOG);
        }

        processing = hasProcessableInput() && canAcceptOutputBatch(itemRegistry);
        if (!processing) {
            progressMs = 0;
        }
        return true;
    }

    private boolean hasProcessableInput() {
        return !inputStack.isEmpty() && INPUT_ITEM_ID.equals(inputStack.getItemId());
    }

    private boolean canAcceptOutputBatch(ItemRegistry itemRegistry) {
        if (outputStack.isEmpty()) {
            return true;
        }
        if (!OUTPUT_ITEM_ID.equals(outputStack.getItemId())) {
            return false;
        }

        int maxStack = itemRegistry.get(OUTPUT_ITEM_ID).getMaxStack();
        return outputStack.getQuantity() + CHIPS_PER_LOG <= maxStack;
    }

    private void normalizeState() {
        if (inputStack.isEmpty()) {
            inputStack = ItemStack.empty();
        }
        if (outputStack.isEmpty()) {
            outputStack = ItemStack.empty();
        }
        if (!hasProcessableInput()) {
            processing = false;
            progressMs = 0;
        }
    }

    private static ItemStack toStack(String itemId, int quantity) {
        if (itemId == null || itemId.isBlank() || quantity <= 0) {
            return ItemStack.empty();
        }
        return ItemStack.of(itemId, quantity);
    }
}
