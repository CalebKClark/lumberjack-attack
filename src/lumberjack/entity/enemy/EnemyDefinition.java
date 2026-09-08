package lumberjack.entity.enemy;

import java.awt.Color;

/**
 * Immutable template for one enemy type. Loaded from /enemies/enemies.csv.
 */
public final class EnemyDefinition {

    private final String id;
    private final String name;
    private final double maxHealth;
    private final double contactDamage;
    private final int speed;
    private final String dropItemId;
    private final int dropQuantity;
    private final int width;
    private final int height;
    private final Color color;
    private final double hitFlashMs;
    private final double contactCooldownMs;
    private final int combatXp;

    public EnemyDefinition(
            String id,
            String name,
            double maxHealth,
            double contactDamage,
            int speed,
            String dropItemId,
            int dropQuantity,
            int width,
            int height,
            Color color,
            double hitFlashMs,
            double contactCooldownMs,
            int combatXp
    ) {
        this.id = id;
        this.name = name;
        this.maxHealth = maxHealth;
        this.contactDamage = contactDamage;
        this.speed = speed;
        this.dropItemId = dropItemId;
        this.dropQuantity = dropQuantity;
        this.width = width;
        this.height = height;
        this.color = color;
        this.hitFlashMs = hitFlashMs;
        this.contactCooldownMs = contactCooldownMs;
        this.combatXp = combatXp;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getContactDamage() {
        return contactDamage;
    }

    public int getSpeed() {
        return speed;
    }

    public String getDropItemId() {
        return dropItemId;
    }

    public int getDropQuantity() {
        return dropQuantity;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Color getColor() {
        return color;
    }

    public double getHitFlashMs() {
        return hitFlashMs;
    }

    public double getContactCooldownMs() {
        return contactCooldownMs;
    }

    public int getCombatXp() {
        return combatXp;
    }
}
