package lumberjack.game;

import java.util.ArrayList;
import java.util.List;

import lumberjack.data.CsvLoader;
import lumberjack.item.ItemStack;

/**
 * Loads the new-game inventory from {@code /game/starter_loadout.csv}.
 */
public final class StarterLoadout {

    private static final String DEFAULT_PATH = "/game/starter_loadout.csv";

    private final List<Entry> entries = new ArrayList<>();
    private final int defaultHotbarSlot;

    public StarterLoadout() {
        this(DEFAULT_PATH);
    }

    public StarterLoadout(String resourcePath) {
        for (String[] row : CsvLoader.readRows(resourcePath)) {
            int slot = CsvLoader.parseInt(row, 0, -1);
            String itemId = CsvLoader.cell(row, 1);
            int quantity = CsvLoader.parseInt(row, 2, 1);
            if (slot < 0 || itemId.isBlank() || quantity <= 0) {
                continue;
            }
            entries.add(new Entry(slot, itemId, quantity));
        }
        defaultHotbarSlot = entries.stream().mapToInt(Entry::slot).min().orElse(0);
    }

    public List<Entry> getEntries() {
        return List.copyOf(entries);
    }

    public int getDefaultHotbarSlot() {
        return defaultHotbarSlot;
    }

    public record Entry(int slot, String itemId, int quantity) {
        public ItemStack toStack() {
            return ItemStack.of(itemId, quantity);
        }
    }
}
