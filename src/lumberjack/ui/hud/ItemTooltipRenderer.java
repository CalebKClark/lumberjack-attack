package lumberjack.ui.hud;

import java.util.ArrayList;
import java.util.List;

import lumberjack.combat.StatModifiers;
import lumberjack.item.ItemDefinition;

/**
 * Shared item tooltip text helpers used by LibGDX HUD overlays.
 */
public final class ItemTooltipRenderer {

    private ItemTooltipRenderer() {
    }

    public static List<String> buildStatLines(StatModifiers stats) {
        List<String> lines = new ArrayList<>();
        if (stats.isEmpty()) {
            return lines;
        }

        if (stats.getBonusDamage() != 0) {
            lines.add("Damage: " + formatSigned(stats.getBonusDamage()));
        }
        if (stats.getBonusCritChance() != 0) {
            lines.add("Crit Chance: " + formatSigned(stats.getBonusCritChance()) + "%");
        }
        if (stats.getBonusCritMultiplier() != 0) {
            lines.add("Crit Multiplier: " + formatSigned(stats.getBonusCritMultiplier()) + "×");
        }
        if (stats.getBonusMaxHealth() != 0) {
            lines.add("Max Health: " + formatSigned(stats.getBonusMaxHealth()));
        }
        if (stats.getBonusDefense() != 0) {
            lines.add("Defense: " + formatSigned(stats.getBonusDefense()) + "%");
        }
        if (stats.getBonusFishingSpeed() != 0) {
            lines.add("Fishing Speed: " + formatSigned(stats.getBonusFishingSpeed()) + "%");
        }
        return lines;
    }

    public static List<String> buildFoodLines(ItemDefinition item) {
        List<String> lines = new ArrayList<>();
        if (item == null || !item.isEdible()) {
            return lines;
        }
        if (item.getFoodHealth() > 0) {
            lines.add("Health: +" + item.getFoodHealth());
        }
        if (item.getFoodEnergy() > 0) {
            lines.add("Energy: +" + item.getFoodEnergy());
        }
        return lines;
    }

    private static String formatSigned(double value) {
        if (value > 0) {
            return "+" + trimTrailingZero(value);
        }
        return trimTrailingZero(value);
    }

    private static String formatSigned(int value) {
        return formatSigned((double) value);
    }

    private static String trimTrailingZero(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
