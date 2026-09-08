package lumberjack.combat;

import lumberjack.core.GameConfig;
import lumberjack.inventory.Inventory;
import lumberjack.item.ItemDefinition;
import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;

/**
 * Resolved combat stats = player base + bonuses from the selected hotbar item.
 */
public final class EffectiveCombatStats {

    private final int damage;
    private final double critChance;
    private final double critMultiplier;
    private final int maxHealth;
    private final double defense;

    private final int baseDamage;
    private final double baseCritChance;
    private final double baseCritMultiplier;
    private final int baseMaxHealth;
    private final double baseDefense;

    private final StatModifiers equipment;
    private final String equipmentName;

    private EffectiveCombatStats(
            int damage,
            double critChance,
            double critMultiplier,
            int maxHealth,
            double defense,
            int baseDamage,
            double baseCritChance,
            double baseCritMultiplier,
            int baseMaxHealth,
            double baseDefense,
            StatModifiers equipment,
            String equipmentName
    ) {
        this.damage = damage;
        this.critChance = critChance;
        this.critMultiplier = critMultiplier;
        this.maxHealth = maxHealth;
        this.defense = defense;
        this.baseDamage = baseDamage;
        this.baseCritChance = baseCritChance;
        this.baseCritMultiplier = baseCritMultiplier;
        this.baseMaxHealth = baseMaxHealth;
        this.baseDefense = baseDefense;
        this.equipment = equipment;
        this.equipmentName = equipmentName;
    }

    public static EffectiveCombatStats resolve(Inventory inventory, ItemRegistry itemRegistry) {
        StatModifiers equipment = StatModifiers.ZERO;
        String equipmentName = null;

        ItemStack held = inventory.getSelectedHotbarStack();
        if (!held.isEmpty()) {
            ItemDefinition item = itemRegistry.get(held.getItemId());
            equipment = item.getStatModifiers();
            equipmentName = item.getName();
        }

        int damage = GameConfig.BASE_DAMAGE + equipment.getBonusDamage();
        double critChance = clamp(
                GameConfig.BASE_CRIT_CHANCE + equipment.getBonusCritChance(),
                0,
                GameConfig.CRIT_CHANCE_CAP
        );
        double critMultiplier = GameConfig.BASE_CRIT_MULTIPLIER + equipment.getBonusCritMultiplier();
        int maxHealth = GameConfig.BASE_MAX_HEALTH + equipment.getBonusMaxHealth();
        double defense = clamp(
                GameConfig.BASE_DEFENSE + equipment.getBonusDefense(),
                0,
                GameConfig.DEFENSE_CAP
        );

        return new EffectiveCombatStats(
                damage,
                critChance,
                critMultiplier,
                maxHealth,
                defense,
                GameConfig.BASE_DAMAGE,
                GameConfig.BASE_CRIT_CHANCE,
                GameConfig.BASE_CRIT_MULTIPLIER,
                GameConfig.BASE_MAX_HEALTH,
                GameConfig.BASE_DEFENSE,
                equipment,
                equipmentName
        );
    }

    public int getDamage() {
        return damage;
    }

    public double getCritChance() {
        return critChance;
    }

    public double getCritMultiplier() {
        return critMultiplier;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public double getDefense() {
        return defense;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public double getBaseCritChance() {
        return baseCritChance;
    }

    public double getBaseCritMultiplier() {
        return baseCritMultiplier;
    }

    public int getBaseMaxHealth() {
        return baseMaxHealth;
    }

    public double getBaseDefense() {
        return baseDefense;
    }

    public StatModifiers getEquipment() {
        return equipment;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public boolean hasEquipmentBonus() {
        return equipment != null && !equipment.isEmpty();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
