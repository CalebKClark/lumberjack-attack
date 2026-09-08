package lumberjack.world.building;

import java.awt.Rectangle;

/**
 * A multi-tile world building (Stardew-style): tall sprite, base collision, optional door.
 * Position is the top-left of the sprite in world pixels.
 */
public final class WorldBuilding {

    private final String id;
    private final String mapId;
    private final String spritePath;
    private final int worldX;
    private final int worldY;
    private final int spriteWidth;
    private final int spriteHeight;
    private final Rectangle collisionBounds;
    private final Rectangle doorBounds;
    private final String doorTargetMapId;
    private final int doorSpawnCol;
    private final int doorSpawnRow;

    public WorldBuilding(
            String id,
            String mapId,
            String spritePath,
            int worldX,
            int worldY,
            int spriteWidth,
            int spriteHeight,
            Rectangle collisionBounds,
            Rectangle doorBounds,
            String doorTargetMapId,
            int doorSpawnCol,
            int doorSpawnRow
    ) {
        this.id = id;
        this.mapId = mapId;
        this.spritePath = spritePath;
        this.worldX = worldX;
        this.worldY = worldY;
        this.spriteWidth = spriteWidth;
        this.spriteHeight = spriteHeight;
        this.collisionBounds = new Rectangle(collisionBounds);
        this.doorBounds = doorBounds == null ? null : new Rectangle(doorBounds);
        this.doorTargetMapId = doorTargetMapId;
        this.doorSpawnCol = doorSpawnCol;
        this.doorSpawnRow = doorSpawnRow;
    }

    public String getId() {
        return id;
    }

    public String getMapId() {
        return mapId;
    }

    public String getSpritePath() {
        return spritePath;
    }

    public int getWorldX() {
        return worldX;
    }

    public int getWorldY() {
        return worldY;
    }

    public int getSpriteWidth() {
        return spriteWidth;
    }

    public int getSpriteHeight() {
        return spriteHeight;
    }

    /** Bottom edge of the sprite — used for Y-sorting (walk behind). */
    public int getSortY() {
        return worldY + spriteHeight;
    }

    public Rectangle getCollisionBounds() {
        return new Rectangle(collisionBounds);
    }

    public Rectangle getDoorBounds() {
        return doorBounds == null ? null : new Rectangle(doorBounds);
    }

    public boolean collidesWith(Rectangle bounds) {
        return collisionBounds.intersects(bounds);
    }

    public boolean hasDoor() {
        return doorBounds != null && doorTargetMapId != null;
    }

    public boolean isPlayerInDoor(int centerX, int centerY) {
        return hasDoor() && doorBounds.contains(centerX, centerY);
    }

    public String getDoorTargetMapId() {
        return doorTargetMapId;
    }

    public int getDoorSpawnCol() {
        return doorSpawnCol;
    }

    public int getDoorSpawnRow() {
        return doorSpawnRow;
    }
}
