package lumberjack.combat;

/**
 * Stat bonuses granted by an equipped item (selected hotbar slot).
 */
public final class StatModifiers {

    public static final StatModifiers ZERO = new StatModifiers(0, 0, 0, 0, 0, 0);

    private final int bonusDamage;
    private final double bonusCritChance;
    private final double bonusCritMultiplier;
    private final int bonusMaxHealth;
    private final double bonusDefense;
    private final double bonusFishingSpeed;

    public StatModifiers(
            int bonusDamage,
            double bonusCritChance,
            double bonusCritMultiplier,
            int bonusMaxHealth,
            double bonusDefense,
            double bonusFishingSpeed
    ) {
        this.bonusDamage = bonusDamage;
        this.bonusCritChance = bonusCritChance;
        this.bonusCritMultiplier = bonusCritMultiplier;
        this.bonusMaxHealth = bonusMaxHealth;
        this.bonusDefense = bonusDefense;
        this.bonusFishingSpeed = bonusFishingSpeed;
    }

    public int getBonusDamage() {
        return bonusDamage;
    }

    public double getBonusCritChance() {
        return bonusCritChance;
    }

    public double getBonusCritMultiplier() {
        return bonusCritMultiplier;
    }

    public int getBonusMaxHealth() {
        return bonusMaxHealth;
    }

    public double getBonusDefense() {
        return bonusDefense;
    }

    public double getBonusFishingSpeed() {
        return bonusFishingSpeed;
    }

    public boolean isEmpty() {
        return bonusDamage == 0
                && bonusCritChance == 0
                && bonusCritMultiplier == 0
                && bonusMaxHealth == 0
                && bonusDefense == 0
                && bonusFishingSpeed == 0;
    }
}
