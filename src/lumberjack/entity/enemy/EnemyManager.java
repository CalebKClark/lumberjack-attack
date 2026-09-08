package lumberjack.entity.enemy;

import java.util.ArrayList;
import java.util.List;

import lumberjack.combat.ReachUtils;
import lumberjack.core.GameConfig;
import lumberjack.entity.Player;
import lumberjack.game.GameSession;
import lumberjack.save.SaveData;
import lumberjack.skills.SkillType;
import lumberjack.world.map.TileMap;
import lumberjack.world.map.WorldMapManager;
import lumberjack.world.tile.TileRegistry;

/**
 * Tracks living enemies, handles AI movement, contact damage, and save/load.
 */
public final class EnemyManager {

    private final List<WorldEnemy> enemies = new ArrayList<>();

    public void resetFromSpawns(
            EnemySpawnRegistry spawnRegistry,
            EnemyRegistry enemyRegistry,
            WorldMapManager worldMaps
    ) {
        enemies.clear();

        for (EnemySpawnDefinition spawn : spawnRegistry.getSpawns()) {
            EnemyDefinition definition = enemyRegistry.get(spawn.getEnemyId());
            worldMaps.ensureMapLoaded(spawn.getMapId());
            TileMap map = worldMaps.getMapOrNull(spawn.getMapId());
            if (map == null) {
                continue;
            }
            int centerX = map.getTileCenterX(spawn.getCol());
            int centerY = map.getTileCenterY(spawn.getRow());
            int x = centerX - definition.getWidth() / 2;
            int y = centerY - definition.getHeight() / 2;

            enemies.add(new WorldEnemy(
                    spawn.getEnemyId(),
                    spawn.getMapId(),
                    x,
                    y,
                    definition.getWidth(),
                    definition.getHeight(),
                    definition.getMaxHealth()
            ));
        }
    }

    public boolean update(GameSession session, double elapsedMs) {
        if (enemies.isEmpty()) {
            return false;
        }

        String currentMapId = session.getCurrentMapId();
        Player player = session.getPlayer();
        TileMap map = session.getCurrentMap();
        TileRegistry tileRegistry = session.getTileRegistry();
        EnemyRegistry enemyRegistry = session.getEnemyRegistry();
        boolean changed = false;

        for (WorldEnemy enemy : enemies) {
            enemy.updateTimers(elapsedMs);
            if (enemy.isFlashing()) {
                changed = true;
            }
        }

        for (WorldEnemy enemy : enemies) {
            if (enemy.isDead() || !currentMapId.equals(enemy.getMapId())) {
                continue;
            }

            EnemyDefinition definition = enemyRegistry.get(enemy.getEnemyId());
            if (moveTowardPlayer(enemy, player, definition.getSpeed(), map, tileRegistry, elapsedMs)) {
                changed = true;
            }

            if (enemy.getBounds().intersects(player.getBounds()) && enemy.canDealContactDamage()) {
                boolean damaged = session.getCombatState().takeDamage(
                        definition.getContactDamage(),
                        session.getInventory(),
                        session.getItemRegistry()
                );
                if (damaged) {
                    player.triggerHitFlash(GameConfig.PLAYER_HIT_FLASH_MS);
                }
                enemy.resetContactCooldown(definition.getContactCooldownMs());
                changed = true;
            }
        }

        return changed;
    }

    public WorldEnemy findAttackableEnemy(GameSession session, int worldX, int worldY) {
        String mapId = session.getCurrentMapId();
        Player player = session.getPlayer();
        WorldEnemy bestMatch = null;
        double bestDistance = Double.MAX_VALUE;

        for (WorldEnemy enemy : enemies) {
            if (enemy.isDead() || !mapId.equals(enemy.getMapId())) {
                continue;
            }

            if (!enemy.containsWorldPoint(worldX, worldY)) {
                continue;
            }

            if (!ReachUtils.isWithinReach(player, enemy)) {
                continue;
            }

            double distance = Math.hypot(
                    enemy.getCenterX() - (player.getX() + player.getWidth() / 2.0),
                    enemy.getCenterY() - (player.getY() + player.getHeight() / 2.0)
            );

            if (distance < bestDistance) {
                bestDistance = distance;
                bestMatch = enemy;
            }
        }

        return bestMatch;
    }

    public boolean damageEnemy(WorldEnemy enemy, int damage, GameSession session) {
        if (enemy.isDead()) {
            return false;
        }

        EnemyDefinition definition = session.getEnemyRegistry().get(enemy.getEnemyId());
        enemy.setHealth(enemy.getHealth() - damage);
        enemy.triggerHitFlash(definition.getHitFlashMs());

        if (enemy.isDead()) {
            session.getDroppedItems().spawn(
                    definition.getDropItemId(),
                    definition.getDropQuantity(),
                    enemy.getCenterX(),
                    enemy.getCenterY()
            );
            if (definition.getCombatXp() > 0) {
                session.getPlayerSkills().grantXp(SkillType.COMBAT, definition.getCombatXp());
            }
            session.getPlayerCollections().onEnemyDefeated(enemy.getEnemyId());
            session.checkForNewRecipeUnlocks();
            enemies.remove(enemy);
            return true;
        }

        return false;
    }

    /** Living enemies on a map (for LibGDX renderer). */
    public List<WorldEnemy> getLivingOnMap(String mapId) {
        List<WorldEnemy> results = new ArrayList<>();
        for (WorldEnemy enemy : enemies) {
            if (!enemy.isDead() && mapId.equals(enemy.getMapId())) {
                results.add(enemy);
            }
        }
        return results;
    }

    public List<SaveData.EnemyEntry> exportEnemies() {
        List<SaveData.EnemyEntry> entries = new ArrayList<>();
        for (WorldEnemy enemy : enemies) {
            if (enemy.isDead()) {
                continue;
            }

            entries.add(new SaveData.EnemyEntry(
                    enemy.getMapId(),
                    enemy.getEnemyId(),
                    enemy.getX(),
                    enemy.getY(),
                    enemy.getHealth()
            ));
        }
        return entries;
    }

    public void restoreEnemies(List<SaveData.EnemyEntry> entries, EnemyRegistry enemyRegistry) {
        enemies.clear();

        for (SaveData.EnemyEntry entry : entries) {
            EnemyDefinition definition = enemyRegistry.get(entry.getEnemyId());
            enemies.add(new WorldEnemy(
                    entry.getEnemyId(),
                    entry.getMapId(),
                    entry.getX(),
                    entry.getY(),
                    definition.getWidth(),
                    definition.getHeight(),
                    entry.getHealth()
            ));
        }
    }

    public void clear() {
        enemies.clear();
    }

    private boolean moveTowardPlayer(
            WorldEnemy enemy,
            Player player,
            int speed,
            TileMap map,
            TileRegistry tileRegistry,
            double elapsedMs
    ) {
        int playerCenterX = player.getX() + player.getWidth() / 2;
        int playerCenterY = player.getY() + player.getHeight() / 2;
        double dx = playerCenterX - enemy.getCenterX();
        double dy = playerCenterY - enemy.getCenterY();
        double length = Math.hypot(dx, dy);

        if (length < 1) {
            return false;
        }

        double moveScale = elapsedMs / GameConfig.FRAME_MS;
        double scaledSpeed = speed * moveScale;
        int moveX = (int) Math.round(scaledSpeed * dx / length);
        int moveY = (int) Math.round(scaledSpeed * dy / length);

        if (moveX == 0 && moveY == 0) {
            return false;
        }

        if (!map.collidesWith(enemy.getBoundsAtOffset(moveX, moveY), tileRegistry)) {
            enemy.move(moveX, moveY);
            return true;
        }

        if (moveX != 0 && !map.collidesWith(enemy.getBoundsAtOffset(moveX, 0), tileRegistry)) {
            enemy.move(moveX, 0);
            return true;
        }

        if (moveY != 0 && !map.collidesWith(enemy.getBoundsAtOffset(0, moveY), tileRegistry)) {
            enemy.move(0, moveY);
            return true;
        }

        return false;
    }
}
