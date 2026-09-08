package lumberjack.combat;

import lumberjack.core.GameConfig;
import lumberjack.entity.Entity;
import lumberjack.entity.Player;

/**
 * Shared reach checks for player interactions (chop, melee, etc.).
 */
public final class ReachUtils {

    private ReachUtils() {
    }

    public static boolean isWithinReach(Player player, Entity target) {
        int targetCenterX = target.getX() + target.getWidth() / 2;
        int targetCenterY = target.getY() + target.getHeight() / 2;
        return isWithinReach(player, targetCenterX, targetCenterY);
    }

    public static boolean isWithinReach(Player player, int targetCenterX, int targetCenterY) {
        int playerCenterX = player.getX() + player.getWidth() / 2;
        int playerCenterY = player.getY() + player.getHeight() / 2;

        double distance = Math.hypot(targetCenterX - playerCenterX, targetCenterY - playerCenterY);
        return distance <= GameConfig.CHOP_REACH_DISTANCE;
    }
}
