package lumberjack.combat;

import lumberjack.core.GameConfig;
import lumberjack.inventory.Inventory;
import lumberjack.item.ItemDefinition;
import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;

/**
 * Resolved player stats from base values plus the selected hotbar item.
 */
public final class EffectivePlayerStats {

    private final EffectiveCombatStats combat;
    private final double fishingSpeed;
    private final double biteWaitMinMs;
    private final double biteWaitMaxMs;

    private EffectivePlayerStats(
            EffectiveCombatStats combat,
            double fishingSpeed,
            double biteWaitMinMs,
            double biteWaitMaxMs
    ) {
        this.combat = combat;
        this.fishingSpeed = fishingSpeed;
        this.biteWaitMinMs = biteWaitMinMs;
        this.biteWaitMaxMs = biteWaitMaxMs;
    }

    public static EffectivePlayerStats resolve(Inventory inventory, ItemRegistry itemRegistry) {
        EffectiveCombatStats combat = EffectiveCombatStats.resolve(inventory, itemRegistry);
        double fishingSpeed = resolveFishingSpeed(inventory, itemRegistry);
        double cappedSpeed = Math.min(fishingSpeed, GameConfig.FISHING_SPEED_CAP);
        double range = GameConfig.FISHING_BITE_MAX_MS - GameConfig.FISHING_BITE_MIN_MS;
        double effectiveRange = range * (1.0 - cappedSpeed / 100.0);
        double biteWaitMinMs = GameConfig.FISHING_BITE_MIN_MS;
        double biteWaitMaxMs = GameConfig.FISHING_BITE_MIN_MS + effectiveRange;

        return new EffectivePlayerStats(combat, fishingSpeed, biteWaitMinMs, biteWaitMaxMs);
    }

    public static double resolveFishingSpeed(Inventory inventory, ItemRegistry itemRegistry) {
        ItemStack held = inventory.getSelectedHotbarStack();
        if (held.isEmpty()) {
            return 0;
        }

        ItemDefinition item = itemRegistry.get(held.getItemId());
        return item.getStatModifiers().getBonusFishingSpeed();
    }

    public EffectiveCombatStats getCombat() {
        return combat;
    }

    public double getFishingSpeed() {
        return fishingSpeed;
    }

    public double getBiteWaitMinMs() {
        return biteWaitMinMs;
    }

    public double getBiteWaitMaxMs() {
        return biteWaitMaxMs;
    }
}
