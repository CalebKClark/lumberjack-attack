package lumberjack.entity.enemy;

import lumberjack.entity.Entity;

/**
 * A living enemy instance in the world.
 */
public final class WorldEnemy extends Entity {

    private final String enemyId;
    private final String mapId;
    private double health;
    private double hitFlashRemainingMs;
    private double contactCooldownRemainingMs;

    public WorldEnemy(String enemyId, String mapId, int x, int y, int width, int height, double health) {
        super(x, y, width, height);
        this.enemyId = enemyId;
        this.mapId = mapId;
        this.health = health;
    }

    public String getEnemyId() {
        return enemyId;
    }

    public String getMapId() {
        return mapId;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = Math.max(0, health);
    }

    public boolean isDead() {
        return health <= 0;
    }

    public boolean isFlashing() {
        return hitFlashRemainingMs > 0;
    }

    public void triggerHitFlash(double durationMs) {
        hitFlashRemainingMs = Math.max(hitFlashRemainingMs, durationMs);
    }

    public void updateTimers(double elapsedMs) {
        if (hitFlashRemainingMs > 0) {
            hitFlashRemainingMs = Math.max(0, hitFlashRemainingMs - elapsedMs);
        }
        if (contactCooldownRemainingMs > 0) {
            contactCooldownRemainingMs = Math.max(0, contactCooldownRemainingMs - elapsedMs);
        }
    }

    public boolean canDealContactDamage() {
        return contactCooldownRemainingMs <= 0;
    }

    public void resetContactCooldown(double cooldownMs) {
        contactCooldownRemainingMs = cooldownMs;
    }

    public int getCenterX() {
        return x + width / 2;
    }

    public int getCenterY() {
        return y + height / 2;
    }

    public boolean containsWorldPoint(int worldX, int worldY) {
        return bounds.contains(worldX, worldY);
    }
}
