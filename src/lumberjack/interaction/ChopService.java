package lumberjack.interaction;

import lumberjack.core.GameConfig;
import lumberjack.entity.Player;
import lumberjack.game.GameSession;
import lumberjack.inventory.Inventory;
import lumberjack.item.ItemDefinition;
import lumberjack.item.ItemStack;
import lumberjack.skills.SkillType;
import lumberjack.world.map.TileMap;
import lumberjack.world.regrowth.TreeHitTracker;
import lumberjack.world.regrowth.TreeRegrowthManager;
import lumberjack.world.tile.TallTreeSprites;
import lumberjack.world.tile.TileDefinition;

/**
 * Handles chopping choppable world tiles (pine trees for now).
 * Swing starts on click; tree damage applies when the axe animation finishes
 * (same for every facing once those sheets exist).
 */
public final class ChopService {

    private final TreeRegrowthManager regrowthManager;
    private final TreeHitTracker hitTracker;
    private PendingChop pendingChop;

    public ChopService(TreeRegrowthManager regrowthManager, TreeHitTracker hitTracker) {
        this.regrowthManager = regrowthManager;
        this.hitTracker = hitTracker;
    }

    /**
     * Validates the target and starts a locked axe swing. Damage is deferred until
     * {@link #onAxeSwingCompleted(GameSession, boolean)} sees the swing complete.
     */
    public boolean tryChopAtWorldPosition(GameSession session, int worldX, int worldY) {
        Player player = session.getPlayer();
        if (player.isActionLocked() || pendingChop != null) {
            return false;
        }

        ItemDefinition heldAxe = getHeldAxe(session.getInventory(), session.getItemRegistry());
        if (heldAxe == null) {
            return false;
        }

        TileMap map = session.getCurrentMap();
        int[] target = resolveChopTarget(session, map, worldX, worldY);
        if (target == null) {
            return false;
        }

        int col = target[0];
        int row = target[1];
        TileDefinition tile = session.getTileRegistry().get(map.getTileIdAt(col, row));
        if (!tile.isChoppable() || tile.getDropItemId() == null) {
            return false;
        }

        int tileCenterX = map.getTileCenterX(col);
        int tileCenterY = map.getTileCenterY(row);
        int playerCenterX = player.getX() + player.getWidth() / 2;
        int playerCenterY = player.getY() + player.getHeight() / 2;

        double distance = Math.hypot(tileCenterX - playerCenterX, tileCenterY - playerCenterY);
        if (distance > GameConfig.CHOP_REACH_DISTANCE) {
            return false;
        }

        if (!session.trySpendActionEnergy()) {
            return true;
        }

        player.faceToward(tileCenterX, tileCenterY);
        player.triggerAxeSwing();
        pendingChop = new PendingChop(
                session.getCurrentMapId(),
                col,
                row,
                heldAxe.getTreeDamage()
        );
        return true;
    }

    /** Apply deferred chop damage after the axe swing animation completes. */
    public void onAxeSwingCompleted(GameSession session, boolean swingCompleted) {
        if (!swingCompleted || pendingChop == null) {
            return;
        }

        PendingChop pending = pendingChop;
        pendingChop = null;
        applyPendingChop(session, pending);
    }

    private void applyPendingChop(GameSession session, PendingChop pending) {
        if (!pending.mapId.equals(session.getCurrentMapId())) {
            return;
        }

        TileMap map = session.getCurrentMap();
        if (!map.isInBounds(pending.col, pending.row)) {
            return;
        }

        TileDefinition tile = session.getTileRegistry().get(map.getTileIdAt(pending.col, pending.row));
        if (!tile.isChoppable() || tile.getDropItemId() == null) {
            return;
        }

        int tileCenterX = map.getTileCenterX(pending.col);
        int tileCenterY = map.getTileCenterY(pending.row);
        int toughness = Math.max(1, tile.getToughness());
        int remaining = hitTracker.applyHit(
                pending.mapId,
                pending.col,
                pending.row,
                toughness,
                pending.treeDamage
        );
        if (remaining > 0) {
            return;
        }

        fellTree(session, map, pending.col, pending.row, tile, tileCenterX, tileCenterY);
    }

    private void fellTree(
            GameSession session,
            TileMap map,
            int col,
            int row,
            TileDefinition tile,
            int tileCenterX,
            int tileCenterY
    ) {
        hitTracker.clear(session.getCurrentMapId(), col, row);

        int quantity = tile.rollDropQuantity();
        if (TallTreeSprites.isTallTree(tile.getId())) {
            float treeLeft = TallTreeSprites.drawX(col, tile.getId());
            float treeTop = TallTreeSprites.drawY(row, tile.getId());
            session.getDroppedItems().spawnCanopyRain(
                    tile.getDropItemId(),
                    quantity,
                    treeLeft,
                    treeTop,
                    TallTreeSprites.spriteWidth(tile.getId()),
                    TallTreeSprites.spriteHeight(tile.getId()),
                    tileCenterY
            );
        } else {
            session.getDroppedItems().spawn(tile.getDropItemId(), quantity, tileCenterX, tileCenterY);
        }

        if (tile.getRegrowthGameMinutes() > 0) {
            map.setTileIdAt(col, row, TallTreeSprites.stumpTileIdForTree(tile.getId()));
            regrowthManager.schedule(
                    session.getCurrentMapId(),
                    col,
                    row,
                    tile.getId(),
                    tile.getRegrowthGameMinutes() * (double) GameConfig.REAL_MS_PER_GAME_MINUTE
            );
        } else {
            map.setTileIdAt(col, row, GameConfig.GRASS_TILE_ID);
        }

        if (tile.getForagingXp() > 0) {
            session.getPlayerSkills().grantXp(SkillType.FORAGING, tile.getForagingXp());
        }
    }

    /**
     * Prefer a tall tree whose canopy contains the click; otherwise use the tile under the cursor.
     */
    private int[] resolveChopTarget(GameSession session, TileMap map, int worldX, int worldY) {
        int tileSize = GameConfig.TILE_SIZE;
        int clickCol = map.getColumnAtWorldX(worldX);
        int clickRow = map.getRowAtWorldY(worldY);

        int searchRadius = Math.max(2, GameConfig.PINE_TREE_SPRITE_HEIGHT / tileSize + 1);
        int bestCol = -1;
        int bestRow = -1;
        int bestDistSq = Integer.MAX_VALUE;

        for (int row = clickRow - searchRadius; row <= clickRow + searchRadius; row++) {
            for (int col = clickCol - searchRadius; col <= clickCol + searchRadius; col++) {
                if (!map.isInBounds(col, row)) {
                    continue;
                }
                int tileId = map.getTileIdAt(col, row);
                if (!TallTreeSprites.isTallTree(tileId)) {
                    continue;
                }
                if (!TallTreeSprites.containsWorldPoint(col, row, tileId, worldX, worldY)) {
                    continue;
                }
                int dx = map.getTileCenterX(col) - worldX;
                int dy = map.getTileCenterY(row) - worldY;
                int distSq = dx * dx + dy * dy;
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    bestCol = col;
                    bestRow = row;
                }
            }
        }

        if (bestCol >= 0) {
            return new int[] {bestCol, bestRow};
        }

        if (!map.isInBounds(clickCol, clickRow)) {
            return null;
        }
        return new int[] {clickCol, clickRow};
    }

    private ItemDefinition getHeldAxe(Inventory inventory, lumberjack.item.ItemRegistry itemRegistry) {
        ItemStack held = inventory.getSelectedHotbarStack();
        if (held.isEmpty()) {
            return null;
        }
        ItemDefinition item = itemRegistry.get(held.getItemId());
        return item.isAxe() ? item : null;
    }

    private record PendingChop(String mapId, int col, int row, int treeDamage) {
    }
}
