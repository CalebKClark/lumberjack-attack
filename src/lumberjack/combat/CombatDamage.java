package lumberjack.combat;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Rolls player attack damage using effective combat stats.
 */
public final class CombatDamage {

    public record AttackResult(int damage, boolean critical) {
    }

    private CombatDamage() {
    }

    public static AttackResult rollPlayerAttack(EffectiveCombatStats stats) {
        int baseDamage = stats.getDamage();
        boolean critical = ThreadLocalRandom.current().nextDouble(100.0) < stats.getCritChance();
        int damage = critical
                ? (int) Math.round(baseDamage * stats.getCritMultiplier())
                : baseDamage;
        return new AttackResult(Math.max(1, damage), critical);
    }
}
