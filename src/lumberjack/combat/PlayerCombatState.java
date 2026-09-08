package lumberjack.combat;

import lumberjack.inventory.Inventory;
import lumberjack.item.ItemRegistry;

/**
 * Player health and access to effective combat stats.
 */
public final class PlayerCombatState {

    private double currentHealth;

    public PlayerCombatState() {
        resetToFullHealth(null, null);
    }

    public double getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(double currentHealth) {
        this.currentHealth = Math.max(0, currentHealth);
    }

    public boolean isDead() {
        return currentHealth <= 0;
    }

    public EffectiveCombatStats getEffectiveStats(Inventory inventory, ItemRegistry itemRegistry) {
        return EffectiveCombatStats.resolve(inventory, itemRegistry);
    }

    public void syncHealthToMax(Inventory inventory, ItemRegistry itemRegistry) {
        currentHealth = getEffectiveStats(inventory, itemRegistry).getMaxHealth();
    }

    public void clampHealthToMax(Inventory inventory, ItemRegistry itemRegistry) {
        int maxHealth = getEffectiveStats(inventory, itemRegistry).getMaxHealth();
        if (currentHealth > maxHealth) {
            currentHealth = maxHealth;
        }
    }

    /** @return true if any damage was applied */
    public boolean takeDamage(double rawDamage, Inventory inventory, ItemRegistry itemRegistry) {
        if (rawDamage <= 0 || currentHealth <= 0) {
            return false;
        }

        EffectiveCombatStats stats = getEffectiveStats(inventory, itemRegistry);
        double reduced = rawDamage * (1.0 - stats.getDefense() / 100.0);
        if (reduced <= 0) {
            return false;
        }

        currentHealth = Math.max(0, currentHealth - reduced);
        return true;
    }

    public void resetToFullHealth(Inventory inventory, ItemRegistry itemRegistry) {
        if (inventory == null || itemRegistry == null) {
            currentHealth = lumberjack.core.GameConfig.BASE_MAX_HEALTH;
            return;
        }
        syncHealthToMax(inventory, itemRegistry);
    }

    public void heal(double amount, Inventory inventory, ItemRegistry itemRegistry) {
        if (amount <= 0) {
            return;
        }
        currentHealth += amount;
        clampHealthToMax(inventory, itemRegistry);
    }
}
