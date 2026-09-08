package lumberjack.world.placeable;

import java.util.HashMap;
import java.util.Map;

import lumberjack.item.ItemRegistry;
import lumberjack.save.SaveData;

/**
 * Tracks wood chipper machine inventories and processing state per placed object.
 */
public final class WoodChipperManager {

    private final Map<PlacedObjectKey, WoodChipperMachine> machines = new HashMap<>();

    public WoodChipperMachine getOrCreate(PlacedObjectKey key) {
        return machines.computeIfAbsent(key, ignored -> new WoodChipperMachine());
    }

    public WoodChipperMachine get(PlacedObjectKey key) {
        return machines.get(key);
    }

    public void remove(PlacedObjectKey key) {
        machines.remove(key);
    }

    public void clear() {
        machines.clear();
    }

    public void updateAll(double elapsedMs, ItemRegistry itemRegistry) {
        if (elapsedMs <= 0) {
            return;
        }

        for (WoodChipperMachine machine : machines.values()) {
            machine.update(elapsedMs, itemRegistry);
        }
    }

    public java.util.List<SaveData.WoodChipperEntry> exportStates() {
        java.util.List<SaveData.WoodChipperEntry> entries = new java.util.ArrayList<>();
        for (Map.Entry<PlacedObjectKey, WoodChipperMachine> entry : machines.entrySet()) {
            PlacedObjectKey key = entry.getKey();
            WoodChipperMachine machine = entry.getValue();
            entries.add(new SaveData.WoodChipperEntry(
                    key.mapId(),
                    key.col(),
                    key.row(),
                    stackId(machine.getInputStack()),
                    stackQuantity(machine.getInputStack()),
                    stackId(machine.getOutputStack()),
                    stackQuantity(machine.getOutputStack()),
                    machine.getProgressMs(),
                    machine.isProcessing()
            ));
        }
        return entries;
    }

    public void restoreAll(java.util.List<SaveData.WoodChipperEntry> entries) {
        clear();
        for (SaveData.WoodChipperEntry entry : entries) {
            PlacedObjectKey key = new PlacedObjectKey(entry.getMapId(), entry.getCol(), entry.getRow());
            WoodChipperMachine machine = getOrCreate(key);
            machine.restore(
                    entry.getInputItemId(),
                    entry.getInputQuantity(),
                    entry.getOutputItemId(),
                    entry.getOutputQuantity(),
                    entry.getProgressMs(),
                    entry.isProcessing()
            );
        }
    }

    private static String stackId(lumberjack.item.ItemStack stack) {
        return stack.isEmpty() ? "" : stack.getItemId();
    }

    private static int stackQuantity(lumberjack.item.ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getQuantity();
    }
}
