package lumberjack.gdx;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;

import lumberjack.core.GameConfig;
import lumberjack.engine.Camera;
import lumberjack.world.map.TileMap;
import lumberjack.world.placeable.BedPlaceable;
import lumberjack.world.tile.OverlayTiles;
import lumberjack.world.tile.TallTreeSprites;
import lumberjack.world.tile.TileDefinition;
import lumberjack.world.tile.TileRegistry;
import lumberjack.world.tile.WaterTileSprites;

/**
 * LibGDX tile map renderer.
 */
public final class GdxWorldRenderer {

    private static final Matrix4 PROJECTION = new Matrix4();

    private final TileRegistry tileRegistry;
    private final GdxTextureCache textures;

    public GdxWorldRenderer(TileRegistry tileRegistry, GdxTextureCache textures) {
        this.tileRegistry = tileRegistry;
        this.textures = textures;
    }

    public void draw(
            SpriteBatch batch,
            TileMap map,
            Camera camera,
            float worldViewWidth,
            float worldViewHeight
    ) {
        applyWorldProjection(batch, camera, worldViewWidth, worldViewHeight);
        batch.setColor(1f, 1f, 1f, 1f);

        int tileSize = GameConfig.TILE_SIZE;
        int startCol = Math.max(0, camera.getX() / tileSize);
        int endCol = Math.min(map.getWidthInTiles(), (int) ((camera.getX() + worldViewWidth) / tileSize) + 1);
        int startRow = Math.max(0, camera.getY() / tileSize);
        int endRow = Math.min(map.getHeightInTiles(), (int) ((camera.getY() + worldViewHeight) / tileSize) + 1);

        for (int row = startRow; row < endRow; row++) {
            for (int col = startCol; col < endCol; col++) {
                int tileId = map.getTileIdAt(col, row);
                int rotation = map.getRotationAt(col, row);
                TileDefinition tile = tileRegistry.get(tileId);
                float worldX = col * tileSize;
                float worldY = row * tileSize;

                // Auto-tiled water picks its own shore orientation — never apply paint rotation.
                if (tile.getId() == GameConfig.WATER_TILE_ID) {
                    Texture water = resolveWaterTexture(map, col, row);
                    if (water != null) {
                        GdxBatchUtils.drawWorldTexture(batch, water, worldX, worldY, tileSize, tileSize);
                    } else {
                        GdxBatchUtils.drawSolid(
                                batch,
                                textures,
                                GdxBatchUtils.toGdx(tile.getDebugColor()),
                                worldX,
                                worldY,
                                tileSize,
                                tileSize
                        );
                    }
                    continue;
                }

                if (WaterTileSprites.isBridgeTileId(tile.getId())) {
                    // Bridge is an overlay: water underneath, bridge sprite on top.
                    Texture water = resolveWaterTexture(map, col, row);
                    if (water != null) {
                        GdxBatchUtils.drawWorldTexture(batch, water, worldX, worldY, tileSize, tileSize);
                    } else {
                        GdxBatchUtils.drawSolid(
                                batch,
                                textures,
                                GdxBatchUtils.toGdx(tileRegistry.get(GameConfig.WATER_TILE_ID).getDebugColor()),
                                worldX,
                                worldY,
                                tileSize,
                                tileSize
                        );
                    }
                    Texture bridge = textures.getOptional("tiles/sprites/" + tile.getName() + ".png");
                    if (bridge != null) {
                        GdxBatchUtils.drawWorldTexture(batch, bridge, worldX, worldY, tileSize, tileSize, rotation);
                    }
                    continue;
                }

                if (BedPlaceable.isBedTile(tile.getId())) {
                    Texture floor = textures.getOptional("tiles/sprites/cabin_floor.png");
                    if (floor != null) {
                        GdxBatchUtils.drawWorldTexture(batch, floor, worldX, worldY, tileSize, tileSize);
                    } else {
                        GdxBatchUtils.drawSolid(
                                batch,
                                textures,
                                GdxBatchUtils.toGdx(tileRegistry.get(GameConfig.CABIN_FLOOR_TILE_ID).getDebugColor()),
                                worldX,
                                worldY,
                                tileSize,
                                tileSize
                        );
                    }
                    // Stacked head cell: foot draws the full 2-tile sprite.
                    if (BedPlaceable.isStackedHead(map, col, row)) {
                        continue;
                    }
                    Texture bed = textures.getOptional("tiles/sprites/bed.png");
                    if (bed != null) {
                        float bedY = BedPlaceable.isStackedFoot(map, col, row)
                                ? BedPlaceable.headRow(row) * tileSize
                                : worldY;
                        GdxBatchUtils.drawWorldTexture(
                                batch,
                                bed,
                                worldX,
                                bedY,
                                BedPlaceable.SPRITE_WIDTH,
                                BedPlaceable.SPRITE_HEIGHT
                        );
                    }
                    continue;
                }

                if (OverlayTiles.isGrassOverlay(tile.getId())) {
                    Texture grass = textures.getOptional("tiles/sprites/grass.png");
                    if (grass != null) {
                        GdxBatchUtils.drawWorldTexture(batch, grass, worldX, worldY, tileSize, tileSize);
                    } else {
                        GdxBatchUtils.drawSolid(
                                batch,
                                textures,
                                GdxBatchUtils.toGdx(tileRegistry.get(GameConfig.GRASS_TILE_ID).getDebugColor()),
                                worldX,
                                worldY,
                                tileSize,
                                tileSize
                        );
                    }
                    // Living trees only need grass under the canopy; stump appears after felling.
                    if (!TallTreeSprites.isTallTree(tile.getId())) {
                        Texture overlay = textures.getOptional("tiles/sprites/" + tile.getName() + ".png");
                        if (overlay != null) {
                            GdxBatchUtils.drawWorldTexture(batch, overlay, worldX, worldY, tileSize, tileSize, rotation);
                        }
                    }
                    continue;
                }

                Texture sprite = resolveTexture(map, col, row, tile);
                if (sprite != null) {
                    GdxBatchUtils.drawWorldTexture(batch, sprite, worldX, worldY, tileSize, tileSize, rotation);
                } else {
                    GdxBatchUtils.drawSolid(
                            batch,
                            textures,
                            GdxBatchUtils.toGdx(tile.getDebugColor()),
                            worldX,
                            worldY,
                            tileSize,
                            tileSize
                    );
                }
            }
        }
    }

    private Texture resolveTexture(TileMap map, int col, int row, TileDefinition tile) {
        if (tile.getId() == GameConfig.WATER_TILE_ID) {
            return resolveWaterTexture(map, col, row);
        }
        return textures.getOptional("tiles/sprites/" + tile.getName() + ".png");
    }

    private Texture resolveWaterTexture(TileMap map, int col, int row) {
        String baseName = WaterTileSprites.spriteBaseName(map, col, row);
        Texture variant = textures.getOptional("tiles/sprites/" + baseName + ".png");
        if (variant != null) {
            return variant;
        }
        int mask = WaterTileSprites.buildMask(map, col, row);
        variant = textures.getOptional("tiles/sprites/water_" + mask + ".png");
        if (variant != null) {
            return variant;
        }
        return textures.getOptional("tiles/sprites/water.png");
    }

    static void applyWorldProjection(
            SpriteBatch batch,
            Camera camera,
            float worldViewWidth,
            float worldViewHeight
    ) {
        PROJECTION.setToOrtho2D(
                camera.getX(),
                camera.getY() + worldViewHeight,
                worldViewWidth,
                -worldViewHeight
        );
        batch.setProjectionMatrix(PROJECTION);
    }
}
