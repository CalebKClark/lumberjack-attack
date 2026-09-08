package lumberjack.world.map;

import lumberjack.core.GameConfig;
import lumberjack.entity.Player;
import lumberjack.game.GameSession;
import lumberjack.world.building.BuildingManager;
import lumberjack.world.building.WorldBuilding;
import lumberjack.world.drop.DroppedItemManager;

/**
 * Checks whether the player stepped on a transition tile or building door and moves maps.
 */
public final class MapTransitionService {

    private static final double COOLDOWN_MS = 400.0;
    /**
     * Cabin exit: require the player's center to reach this fraction down the door tile
     * before leaving (homestead building door is unchanged).
     */
    private static final double CABIN_DOOR_TRIGGER_DEPTH = 0.75;

    private final MapTransitionRegistry transitionRegistry;
    private final WorldMapManager worldMaps;
    private final BuildingManager buildingManager;
    private double cooldownRemainingMs;

    public MapTransitionService(
            MapTransitionRegistry transitionRegistry,
            WorldMapManager worldMaps,
            BuildingManager buildingManager
    ) {
        this.transitionRegistry = transitionRegistry;
        this.worldMaps = worldMaps;
        this.buildingManager = buildingManager;
    }

    public boolean update(
            GameSession session,
            Player player,
            DroppedItemManager droppedItems,
            double elapsedMs
    ) {
        if (cooldownRemainingMs > 0) {
            cooldownRemainingMs = Math.max(0, cooldownRemainingMs - elapsedMs);
            return false;
        }

        TileMap map = worldMaps.getCurrentMap();
        int centerX = player.getX() + player.getWidth() / 2;
        int centerY = player.getY() + player.getHeight() / 2;
        int col = map.getColumnAtWorldX(centerX);
        int row = map.getRowAtWorldY(centerY);

        WorldBuilding doorBuilding = buildingManager.findDoorAt(worldMaps.getCurrentMapId(), centerX, centerY);
        if (doorBuilding != null) {
            return applyTransition(
                    session,
                    player,
                    droppedItems,
                    doorBuilding.getDoorTargetMapId(),
                    doorBuilding.getDoorSpawnCol(),
                    doorBuilding.getDoorSpawnRow()
            );
        }

        return transitionRegistry.find(worldMaps.getCurrentMapId(), col, row)
                .filter(transition -> isTileTransitionReady(player, transition))
                .map(transition -> applyTransition(
                        session,
                        player,
                        droppedItems,
                        transition.getTargetMapId(),
                        transition.getSpawnCol(),
                        transition.getSpawnRow()
                ))
                .orElse(false);
    }

    /**
     * Inside the cabin, wait until the player has walked most of the way down the door tile.
     * Outside (building door / other maps) still triggers on tile entry.
     */
    private static boolean isTileTransitionReady(Player player, MapTransition transition) {
        if (!GameConfig.STARTING_MAP.equals(transition.getSourceMapId())) {
            return true;
        }

        int tileTop = transition.getRow() * GameConfig.TILE_SIZE;
        int triggerY = tileTop + (int) Math.round(GameConfig.TILE_SIZE * CABIN_DOOR_TRIGGER_DEPTH);
        int centerY = player.getY() + player.getHeight() / 2;
        return centerY >= triggerY;
    }

    public void clearCooldown() {
        cooldownRemainingMs = 0;
    }

    private boolean applyTransition(
            GameSession session,
            Player player,
            DroppedItemManager droppedItems,
            String targetMapId,
            int spawnCol,
            int spawnRow
    ) {
        worldMaps.switchToMap(targetMapId);
        TileMap targetMap = worldMaps.getCurrentMap();
        WorldMapManager.placePlayerAtTile(player, targetMap, spawnCol, spawnRow);

        // Keep ground loot on the map you left; show the target map's loot.
        droppedItems.setActiveMap(targetMapId);
        cooldownRemainingMs = COOLDOWN_MS;

        session.onMapChanged();
        return true;
    }
}
