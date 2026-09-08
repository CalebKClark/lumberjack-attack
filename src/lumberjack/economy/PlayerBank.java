package lumberjack.economy;

/**
 * Stores currency balances that are not held in the physical inventory.
 */
public final class PlayerBank {

    public static final String WOOD_CHIPS_ID = "wood_chips";

    private long woodChips;

    public long getWoodChips() {
        return woodChips;
    }

    public void setWoodChips(long woodChips) {
        this.woodChips = Math.max(0, woodChips);
    }

    public void depositWoodChips(long amount) {
        if (amount > 0) {
            woodChips += amount;
        }
    }

    public void reset() {
        woodChips = 0;
    }
}
