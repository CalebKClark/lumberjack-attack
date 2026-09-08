package lumberjack.world.building;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import lumberjack.core.GameConfig;

/**
 * Static world buildings (cabin, etc.) — not inventory placeables.
 */
public final class BuildingManager {

    private final List<WorldBuilding> buildings = new ArrayList<>();

    public BuildingManager() {
        buildings.add(createHomesteadCabin());
    }

    /**
     * Cabin is 80×96 (5×6 tiles). Base sits on the mountain_wall_bottom row
     * (cols 14–18, row 10) so the cabin nests under the cliff face.
     */
    private static WorldBuilding createHomesteadCabin() {
        int spriteW = 80;
        int spriteH = 96;
        int footprintCol = 14;
        int footprintBottomRow = 10; // mountain_wall_bottom under the cliff
        int worldX = footprintCol * GameConfig.TILE_SIZE;
        // Bottom of sprite aligns with bottom edge of footprintBottomRow.
        int worldY = (footprintBottomRow + 1) * GameConfig.TILE_SIZE - spriteH;

        // Collision: lower 2 tiles (the void marker), door gap in the center.
        int baseHeight = 2 * GameConfig.TILE_SIZE;
        int doorGapW = GameConfig.TILE_SIZE;
        int doorGapX = worldX + (spriteW - doorGapW) / 2;
        int baseY = worldY + spriteH - baseHeight;

        Rectangle door = new Rectangle(
                doorGapX,
                worldY + spriteH - 24,
                doorGapW,
                36 // includes a little grass in front of the door for easier entry
        );

        return new WorldBuilding(
                "cabin",
                GameConfig.HOMESTEAD_MAP,
                "sprites/buildings/cabin.png",
                worldX,
                worldY,
                spriteW,
                spriteH,
                new Rectangle(worldX, baseY, spriteW, baseHeight), // full base; door gap handled in collidesWith
                door,
                "cabin_interior",
                GameConfig.STARTING_SPAWN_COL,
                GameConfig.STARTING_SPAWN_ROW
        );
    }

    public List<WorldBuilding> getOnMap(String mapId) {
        List<WorldBuilding> results = new ArrayList<>();
        for (WorldBuilding building : buildings) {
            if (mapId.equals(building.getMapId())) {
                results.add(building);
            }
        }
        return results;
    }

    public boolean collidesWith(Rectangle bounds, String mapId) {
        for (WorldBuilding building : buildings) {
            if (!mapId.equals(building.getMapId())) {
                continue;
            }
            if (!building.collidesWith(bounds)) {
                continue;
            }
            // Allow walking through the door opening.
            if (building.hasDoor() && building.isPlayerInDoor(
                    bounds.x + bounds.width / 2,
                    bounds.y + bounds.height / 2
            )) {
                continue;
            }
            // Also allow if the bounds only overlap the door gap strip.
            if (building.hasDoor() && overlapsOnlyDoorGap(bounds, building)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static boolean overlapsOnlyDoorGap(Rectangle bounds, WorldBuilding building) {
        // If intersection with full base is entirely within the door's x-range and bottom area, allow.
        Rectangle base = building.getCollisionBounds();
        Rectangle hit = bounds.intersection(base);
        if (hit.isEmpty()) {
            return true;
        }
        Rectangle door = new Rectangle(
                building.getWorldX() + (building.getSpriteWidth() - 16) / 2,
                base.y,
                16,
                base.height
        );
        return door.contains(hit);
    }

    public WorldBuilding findDoorAt(String mapId, int centerX, int centerY) {
        for (WorldBuilding building : buildings) {
            if (mapId.equals(building.getMapId()) && building.isPlayerInDoor(centerX, centerY)) {
                return building;
            }
        }
        return null;
    }
}
