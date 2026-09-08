package lumberjack.entity;

/**
 * Cardinal direction the player sprite faces.
 */
public enum PlayerFacing {
    DOWN,
    UP,
    LEFT,
    RIGHT;

    public String getSpriteKey() {
        return name().toLowerCase();
    }

    /** Picks a facing from movement delta; vertical axis wins on diagonals. */
    public static PlayerFacing fromMovement(int dx, int dy) {
        if (dy < 0) {
            return UP;
        }
        if (dy > 0) {
            return DOWN;
        }
        if (dx < 0) {
            return LEFT;
        }
        if (dx > 0) {
            return RIGHT;
        }
        return DOWN;
    }

    /** Picks a facing toward a world target from the player center. */
    public static PlayerFacing toward(int fromCenterX, int fromCenterY, int targetX, int targetY) {
        int dx = targetX - fromCenterX;
        int dy = targetY - fromCenterY;
        if (Math.abs(dy) >= Math.abs(dx)) {
            return dy < 0 ? UP : DOWN;
        }
        return dx < 0 ? LEFT : RIGHT;
    }
}
