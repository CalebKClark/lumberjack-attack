package lumberjack.gdx;

import java.awt.Rectangle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.combat.EffectiveCombatStats;
import lumberjack.combat.EffectivePlayerStats;
import lumberjack.core.GameConfig;
import lumberjack.game.GameSession;

/**
 * Native Stats tab.
 */
public final class GdxStatsTab {

    private static final Color SECTION_LABEL = new Color(0.78f, 0.78f, 0.8f, 1f);
    private static final Color STAT_LABEL = new Color(0.71f, 0.71f, 0.73f, 1f);
    private static final Color STAT_BONUS = new Color(0.47f, 0.86f, 0.55f, 1f);
    private static final Color PLACEHOLDER = new Color(0.78f, 0.78f, 0.78f, 1f);
    private static final Color VITALS = new Color(0.86f, 0.47f, 0.47f, 1f);
    private static final Color COMBAT = new Color(0.82f, 0.37f, 0.37f, 1f);
    private static final Color FISHING = new Color(0.35f, 0.61f, 0.88f, 1f);

    private static final int SECTION_HEADER_HEIGHT = 28;
    private static final int SECTION_HEADER_TO_ROWS = 14;
    private static final int ROW_HEIGHT = 32;
    private static final int SECTION_GAP = 18;
    private static final int VALUE_COLUMN = 180;
    private static final int BONUS_COLUMN = 280;

    private final GdxMenuUi ui = new GdxMenuUi();

    public void draw(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            Rectangle content,
            int screenHeight
    ) {
        EffectivePlayerStats stats = EffectivePlayerStats.resolve(
                session.getInventory(),
                session.getItemRegistry()
        );
        EffectiveCombatStats combat = stats.getCombat();
        double currentHealth = session.getCombatState().getCurrentHealth();

        int left = content.x + 36;
        float y = GdxUiCoords.bottom(content, screenHeight) + content.height - 36;

        ui.drawText(batch, font, "Stats", GdxMenuUi.HUD_TEXT, left, y);
        y -= 34;

        String heldLabel = combat.getEquipmentName() != null
                ? "Held: " + combat.getEquipmentName()
                : "Held: Empty hands";
        ui.drawText(batch, font, heldLabel, GdxMenuUi.SUBTEXT, left, y);
        y -= 28;

        y = drawSection(batch, font, textures, left, y, "Vitals", VITALS, new StatRow[] {
                new StatRow(
                        "Health",
                        formatHealth(currentHealth, combat.getMaxHealth()),
                        formatBonus(combat.getEquipment().getBonusMaxHealth(), "", false)
                )
        }, screenHeight);

        y = drawSection(batch, font, textures, left, y, "Combat", COMBAT, new StatRow[] {
                new StatRow("Damage", String.valueOf(combat.getDamage()),
                        formatBonus(combat.getEquipment().getBonusDamage(), "", false)),
                new StatRow("Crit Chance", formatPercent(combat.getCritChance()),
                        formatBonus(combat.getEquipment().getBonusCritChance(), "%", true)),
                new StatRow("Crit Multiplier", formatMultiplier(combat.getCritMultiplier()),
                        formatBonus(combat.getEquipment().getBonusCritMultiplier(), "x", false)),
                new StatRow("Defense", formatPercent(combat.getDefense()),
                        formatBonus(combat.getEquipment().getBonusDefense(), "%", true))
        }, screenHeight);

        y = drawSection(batch, font, textures, left, y, "Fishing", FISHING, new StatRow[] {
                new StatRow("Fishing Speed", formatPercent(stats.getFishingSpeed()),
                        formatBonus(combat.getEquipment().getBonusFishingSpeed(), "%", true)),
                new StatRow("Bite Wait",
                        formatBiteWait(stats.getBiteWaitMinMs(), stats.getBiteWaitMaxMs()), null)
        }, screenHeight);

        ui.drawText(
                batch,
                font,
                "Fishing speed cap: " + (int) GameConfig.FISHING_SPEED_CAP + "%",
                PLACEHOLDER,
                left,
                y - 8
        );
        if (combat.hasEquipmentBonus()) {
            ui.drawText(
                    batch,
                    font,
                    "Bonuses apply from your selected hotbar item.",
                    PLACEHOLDER,
                    left,
                    y - 28
            );
        }
    }

    private float drawSection(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            int left,
            float topBaseline,
            String title,
            Color accent,
            StatRow[] rows,
            int screenHeight
    ) {
        float headerBottom = topBaseline - SECTION_HEADER_HEIGHT + 8;
        GdxBatchUtils.drawSolid(batch, textures, accent, left, headerBottom, 6, SECTION_HEADER_HEIGHT - 8);
        ui.drawText(batch, font, title, SECTION_LABEL, left + 14, topBaseline - 6);

        float y = topBaseline - SECTION_HEADER_HEIGHT - SECTION_HEADER_TO_ROWS;
        for (StatRow row : rows) {
            ui.drawText(batch, font, row.label(), STAT_LABEL, left + 8, y);
            ui.drawText(batch, font, row.value(), GdxMenuUi.HUD_TEXT, left + 8 + VALUE_COLUMN, y);
            if (row.bonus() != null && !row.bonus().isEmpty()) {
                ui.drawText(batch, font, row.bonus(), STAT_BONUS, left + 8 + BONUS_COLUMN, y);
            }
            y -= ROW_HEIGHT;
        }
        return y - SECTION_GAP;
    }

    private static String formatBonus(double bonus, String suffix, boolean isPercent) {
        if (bonus == 0) {
            return "";
        }
        String sign = bonus > 0 ? "+" : "";
        return sign + trimTrailingZero(bonus) + suffix;
    }

    private static String formatBonus(int bonus, String suffix, boolean isPercent) {
        return formatBonus((double) bonus, suffix, isPercent);
    }

    private static String trimTrailingZero(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static String formatPercent(double value) {
        return trimTrailingZero(value) + "%";
    }

    private static String formatMultiplier(double value) {
        return trimTrailingZero(value) + "x";
    }

    private static String formatHealth(double current, int max) {
        return (int) Math.ceil(current) + " / " + max;
    }

    private static String formatBiteWait(double minMs, double maxMs) {
        return formatSeconds(minMs) + "-" + formatSeconds(maxMs) + " s";
    }

    private static String formatSeconds(double ms) {
        double seconds = ms / 1000.0;
        if (seconds == (long) seconds) {
            return String.valueOf((long) seconds);
        }
        return String.format("%.1f", seconds);
    }

    private record StatRow(String label, String value, String bonus) {
    }
}
