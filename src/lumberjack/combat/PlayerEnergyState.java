package lumberjack.combat;

import lumberjack.core.GameConfig;

/**
 * Player energy / stamina. Spent on axe swings and fishing casts; restored by food.
 */
public final class PlayerEnergyState {

    private int maxEnergy = GameConfig.BASE_MAX_ENERGY;
    private double currentEnergy = GameConfig.BASE_MAX_ENERGY;

    public double getCurrentEnergy() {
        return currentEnergy;
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public void setMaxEnergy(int maxEnergy) {
        this.maxEnergy = Math.max(1, maxEnergy);
        clampToMax();
    }

    public void setCurrentEnergy(double currentEnergy) {
        this.currentEnergy = Math.max(0, currentEnergy);
        clampToMax();
    }

    public boolean hasEnergy(int amount) {
        return currentEnergy >= amount;
    }

    /** True when energy is below the low-energy move-speed threshold. */
    public boolean isLowEnergy() {
        return currentEnergy < maxEnergy * GameConfig.LOW_ENERGY_SPEED_THRESHOLD;
    }

    public double getMoveSpeedMultiplier() {
        return isLowEnergy() ? GameConfig.LOW_ENERGY_SPEED_MULTIPLIER : 1.0;
    }

    /** @return true if the cost was paid */
    public boolean trySpend(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (currentEnergy < amount) {
            return false;
        }
        currentEnergy -= amount;
        return true;
    }

    public void restore(double amount) {
        if (amount <= 0) {
            return;
        }
        currentEnergy = Math.min(maxEnergy, currentEnergy + amount);
    }

    public void resetToFull() {
        currentEnergy = maxEnergy;
    }

    public void clampToMax() {
        if (currentEnergy > maxEnergy) {
            currentEnergy = maxEnergy;
        }
    }
}
