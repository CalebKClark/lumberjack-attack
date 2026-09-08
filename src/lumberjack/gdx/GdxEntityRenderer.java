package lumberjack.gdx;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.core.GameConfig;
import lumberjack.engine.Camera;
import lumberjack.entity.DroppedItem;
import lumberjack.entity.PlacedObject;
import lumberjack.entity.Player;
import lumberjack.entity.PlayerAnimKind;
import lumberjack.entity.PlayerFacing;
import lumberjack.entity.enemy.EnemyDefinition;
import lumberjack.entity.enemy.WorldEnemy;
import lumberjack.game.GameSession;
import lumberjack.item.ItemDefinition;
import lumberjack.item.ItemStack;
import lumberjack.world.building.WorldBuilding;
import lumberjack.world.map.TileMap;
import lumberjack.world.tile.TallTreeSprites;

/**
 * LibGDX renderer for world entities (player, buildings, drops, placeables, enemies).
 * Buildings and the player are Y-sorted so you can walk behind tall sprites.
 */
public final class GdxEntityRenderer {

    private static final Color HITBOX_PLAYER = new Color(0.2f, 1f, 0.35f, 0.45f);
    private static final Color HITBOX_ENEMY = new Color(1f, 0.25f, 0.25f, 0.45f);
    private static final Color HITBOX_DROP = new Color(1f, 0.9f, 0.2f, 0.45f);
    private static final Color HITBOX_PLACEABLE = new Color(0.3f, 0.7f, 1f, 0.45f);
    private static final Color HITBOX_BUILDING = new Color(1f, 0.45f, 0.1f, 0.4f);
    private static final Color HITBOX_DOOR = new Color(0.8f, 0.2f, 1f, 0.4f);
    private static final Color HITBOX_EDGE = new Color(1f, 1f, 1f, 0.85f);

    private final GdxTextureCache textures;

    public GdxEntityRenderer(GdxTextureCache textures) {
        this.textures = textures;
    }

    public void draw(
            SpriteBatch batch,
            GameSession session,
            Camera camera,
            float worldViewWidth,
            float worldViewHeight,
            boolean debugHitboxes
    ) {
        GdxWorldRenderer.applyWorldProjection(batch, camera, worldViewWidth, worldViewHeight);
        batch.setColor(1f, 1f, 1f, 1f);

        String mapId = session.getCurrentMapId();
        drawPlaceables(batch, session, mapId);
        drawDrops(batch, session);
        drawEnemies(batch, session, mapId);
        drawYSorted(batch, session, mapId);

        if (debugHitboxes) {
            drawDebugHitboxes(batch, session, mapId);
        }
    }

    private void drawDebugHitboxes(SpriteBatch batch, GameSession session, String mapId) {
        Player player = session.getPlayer();
        drawHitbox(batch, player.getX(), player.getY(), player.getWidth(), player.getHeight(), HITBOX_PLAYER);

        for (WorldEnemy enemy : session.getEnemyManager().getLivingOnMap(mapId)) {
            drawHitbox(batch, enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight(), HITBOX_ENEMY);
        }

        for (DroppedItem drop : session.getDroppedItems().getDrops()) {
            if (drop.getStack().isEmpty() || !drop.isVisible()) {
                continue;
            }
            drawHitbox(batch, drop.getX(), drop.getY(), drop.getWidth(), drop.getHeight(), HITBOX_DROP);
        }

        for (PlacedObject placed : session.getPlacedObjectManager().getOnMap(mapId)) {
            drawHitbox(batch, placed.getX(), placed.getY(), placed.getWidth(), placed.getHeight(), HITBOX_PLACEABLE);
        }

        for (WorldBuilding building : session.getBuildingManager().getOnMap(mapId)) {
            Rectangle collision = building.getCollisionBounds();
            drawHitbox(batch, collision.x, collision.y, collision.width, collision.height, HITBOX_BUILDING);
            Rectangle door = building.getDoorBounds();
            if (door != null) {
                drawHitbox(batch, door.x, door.y, door.width, door.height, HITBOX_DOOR);
            }
        }
    }

    private void drawHitbox(SpriteBatch batch, float x, float y, float width, float height, Color fill) {
        GdxBatchUtils.drawSolid(batch, textures, fill, x, y, width, height);
        float edge = 1f;
        GdxBatchUtils.drawSolid(batch, textures, HITBOX_EDGE, x, y, width, edge);
        GdxBatchUtils.drawSolid(batch, textures, HITBOX_EDGE, x, y + height - edge, width, edge);
        GdxBatchUtils.drawSolid(batch, textures, HITBOX_EDGE, x, y, edge, height);
        GdxBatchUtils.drawSolid(batch, textures, HITBOX_EDGE, x + width - edge, y, edge, height);
    }

    private void drawYSorted(SpriteBatch batch, GameSession session, String mapId) {
        Player player = session.getPlayer();
        int playerSortY = player.getY() + player.getHeight();

        List<SortableWorldDraw> draws = new ArrayList<>();
        for (WorldBuilding building : session.getBuildingManager().getOnMap(mapId)) {
            draws.add(new SortableWorldDraw(building.getSortY(), () -> drawBuilding(batch, building)));
        }

        TileMap map = session.getCurrentMap();
        for (int row = 0; row < map.getHeightInTiles(); row++) {
            for (int col = 0; col < map.getWidthInTiles(); col++) {
                int tileId = map.getTileIdAt(col, row);
                if (!TallTreeSprites.isTallTree(tileId)) {
                    continue;
                }
                int sortY = TallTreeSprites.sortY(row);
                int treeCol = col;
                int treeRow = row;
                int treeTileId = tileId;
                draws.add(new SortableWorldDraw(
                        sortY,
                        () -> drawTallTree(batch, player, treeCol, treeRow, treeTileId, sortY)
                ));
            }
        }

        draws.sort(Comparator.comparingInt(d -> d.sortY));

        boolean playerDrawn = false;
        for (SortableWorldDraw draw : draws) {
            if (!playerDrawn && playerSortY <= draw.sortY) {
                drawPlayer(batch, session);
                playerDrawn = true;
            }
            draw.drawer.run();
        }
        if (!playerDrawn) {
            drawPlayer(batch, session);
        }
    }

    private void drawTallTree(
            SpriteBatch batch,
            Player player,
            int col,
            int row,
            int tileId,
            int treeSortY
    ) {
        Texture tree = textures.getOptional(TallTreeSprites.spritePath(tileId));
        if (tree == null) {
            return;
        }
        float x = TallTreeSprites.drawX(col, tileId);
        float y = TallTreeSprites.drawY(row, tileId);
        float w = TallTreeSprites.spriteWidth(tileId);
        float h = TallTreeSprites.spriteHeight(tileId);

        float previousR = batch.getColor().r;
        float previousG = batch.getColor().g;
        float previousB = batch.getColor().b;
        float previousA = batch.getColor().a;
        if (shouldFadeTreeForPlayer(player, col, row, tileId, treeSortY)) {
            batch.setColor(previousR, previousG, previousB, GameConfig.TREE_BEHIND_PLAYER_ALPHA);
        }
        GdxBatchUtils.drawWorldTexture(batch, tree, x, y, w, h);
        batch.setColor(previousR, previousG, previousB, previousA);
    }

    /**
     * Fade when the player is depth-sorted behind the tree and their hitbox
     * overlaps the stump column under the canopy (16px wide × full tree height).
     * Uses the hitbox only — sprite overhang beside the tree must not fade it.
     */
    private static boolean shouldFadeTreeForPlayer(
            Player player,
            int col,
            int row,
            int tileId,
            int treeSortY
    ) {
        int playerSortY = player.getY() + player.getHeight();
        if (playerSortY > treeSortY) {
            return false;
        }

        // Stump is solid, so test the walkable column behind it (same X as the
        // stump tile, full canopy height) instead of the wide 48px sprite AABB.
        float columnX = col * GameConfig.TILE_SIZE;
        float columnY = TallTreeSprites.drawY(row, tileId);
        float columnW = GameConfig.TILE_SIZE;
        float columnH = treeSortY - columnY;
        return rectsOverlap(
                player.getX(),
                player.getY(),
                player.getWidth(),
                player.getHeight(),
                columnX,
                columnY,
                columnW,
                columnH
        );
    }

    private static boolean rectsOverlap(
            float ax,
            float ay,
            float aw,
            float ah,
            float bx,
            float by,
            float bw,
            float bh
    ) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private record SortableWorldDraw(int sortY, Runnable drawer) {
    }

    private void drawBuilding(SpriteBatch batch, WorldBuilding building) {
        Texture sprite = textures.getOptional(building.getSpritePath());
        if (sprite != null) {
            GdxBatchUtils.drawWorldTexture(
                    batch,
                    sprite,
                    building.getWorldX(),
                    building.getWorldY(),
                    building.getSpriteWidth(),
                    building.getSpriteHeight()
            );
        } else {
            GdxBatchUtils.drawSolid(
                    batch,
                    textures,
                    new Color(0.45f, 0.28f, 0.12f, 1f),
                    building.getWorldX(),
                    building.getWorldY(),
                    building.getSpriteWidth(),
                    building.getSpriteHeight()
            );
        }
    }

    private void drawPlaceables(SpriteBatch batch, GameSession session, String mapId) {
        for (PlacedObject placed : session.getPlacedObjectManager().getOnMap(mapId)) {
            Texture sprite = textures.getOptional("items/sprites/" + placed.getItemId() + ".png");
            boolean bed = lumberjack.world.placeable.BedPlaceable.isBed(placed.getItemId());
            int drawX = placed.getCol() * GameConfig.TILE_SIZE;
            int drawY = bed
                    ? lumberjack.world.placeable.BedPlaceable.headRow(placed.getRow()) * GameConfig.TILE_SIZE
                    : placed.getRow() * GameConfig.TILE_SIZE;
            int drawW = bed
                    ? lumberjack.world.placeable.BedPlaceable.SPRITE_WIDTH
                    : GameConfig.PLACEABLE_SPRITE_SIZE;
            int drawH = bed
                    ? lumberjack.world.placeable.BedPlaceable.SPRITE_HEIGHT
                    : GameConfig.PLACEABLE_SPRITE_SIZE;
            if (sprite != null) {
                GdxBatchUtils.drawWorldTexture(batch, sprite, drawX, drawY, drawW, drawH);
            } else {
                GdxBatchUtils.drawSolid(
                        batch,
                        textures,
                        Color.GRAY,
                        placed.getX(),
                        placed.getY(),
                        placed.getWidth(),
                        placed.getHeight()
                );
            }
        }
    }

    private void drawDrops(SpriteBatch batch, GameSession session) {
        for (DroppedItem drop : session.getDroppedItems().getDrops()) {
            ItemStack stack = drop.getStack();
            if (stack.isEmpty() || !drop.isVisible()) {
                continue;
            }

            float centerX = drop.getCenterX();
            // Bounce offset is "up" — world Y increases downward.
            float centerY = drop.getCenterY() - drop.getBounceOffsetY();
            float size = GameConfig.DROP_ITEM_SIZE;
            Texture sprite = textures.getOptional("items/sprites/" + stack.getItemId() + ".png");

            if (sprite != null) {
                GdxBatchUtils.drawWorldTexture(batch, sprite, centerX - size / 2f, centerY - size / 2f, size, size);
            } else {
                ItemDefinition item = session.getItemRegistry().get(stack.getItemId());
                GdxBatchUtils.drawSolid(
                        batch,
                        textures,
                        GdxBatchUtils.toGdx(item.getDebugColor()),
                        centerX - size / 2f,
                        centerY - size / 2f,
                        size,
                        size
                );
            }
        }
    }

    private void drawEnemies(SpriteBatch batch, GameSession session, String mapId) {
        for (WorldEnemy enemy : session.getEnemyManager().getLivingOnMap(mapId)) {
            EnemyDefinition definition = session.getEnemyRegistry().get(enemy.getEnemyId());
            Color color = enemy.isFlashing() ? Color.WHITE : GdxBatchUtils.toGdx(definition.getColor());
            GdxBatchUtils.drawSolid(
                    batch,
                    textures,
                    color,
                    enemy.getX(),
                    enemy.getY(),
                    enemy.getWidth(),
                    enemy.getHeight()
            );
        }
    }

    private void drawPlayer(SpriteBatch batch, GameSession session) {
        Player player = session.getPlayer();
        Texture frame = resolvePlayerFrame(player);

        float spriteW = GameConfig.PLAYER_SPRITE_WIDTH;
        float spriteH = GameConfig.PLAYER_SPRITE_HEIGHT;
        if (frame != null) {
            spriteW = frame.getWidth();
            spriteH = frame.getHeight();
        }

        float bodyW = GameConfig.PLAYER_SPRITE_WIDTH;
        float bodyH = GameConfig.PLAYER_SPRITE_HEIGHT;
        float bodyDrawX = player.getX() + (player.getWidth() - bodyW) / 2f;
        float drawX;
        float drawY;

        if (player.getAnimKind() == PlayerAnimKind.AXE_SWING
                || player.getAnimKind() == PlayerAnimKind.ROD_CAST
                || player.getAnimKind() == PlayerAnimKind.ROD_HOLD
                || player.getAnimKind() == PlayerAnimKind.ROD_REEL) {
            // Axe / rod sheets pad above the body (and sideways for left/right) so the
            // physical player stays planted while the tool extends out of frame.
            float topPad = GameConfig.PLAYER_AXE_TOP_PAD_PX;
            drawY = player.getY() + player.getHeight() - bodyH - topPad;
            drawX = switch (player.getFacing()) {
                case RIGHT -> bodyDrawX;
                case LEFT -> bodyDrawX - GameConfig.PLAYER_AXE_SIDE_EXTEND_PX;
                case UP, DOWN -> player.getX() + (player.getWidth() - spriteW) / 2f;
            };
        } else {
            float topPad = Math.max(0f, (spriteH - bodyH) / 2f);
            drawX = player.getX() + (player.getWidth() - spriteW) / 2f;
            drawY = player.getY() + player.getHeight() - bodyH - topPad;
        }

        if (frame == null) {
            GdxBatchUtils.drawSolid(
                    batch,
                    textures,
                    player.isFlashing() ? Color.WHITE : Color.BLUE,
                    drawX,
                    drawY,
                    spriteW,
                    spriteH
            );
            return;
        }

        GdxBatchUtils.drawWorldTexture(batch, frame, drawX, drawY, spriteW, spriteH);

        if (player.isFlashing()) {
            float previousR = batch.getColor().r;
            float previousG = batch.getColor().g;
            float previousB = batch.getColor().b;
            float previousA = batch.getColor().a;
            batch.setColor(1f, 1f, 1f, 0.55f);
            GdxBatchUtils.drawWorldTexture(batch, frame, drawX, drawY, spriteW, spriteH);
            batch.setColor(previousR, previousG, previousB, previousA);
        }
    }

    private Texture resolvePlayerFrame(Player player) {
        PlayerAnimKind kind = player.getAnimKind();
        PlayerFacing facing = player.getFacing();
        String facingKey = facing.getSpriteKey();

        // Idle uses the dedicated standing frame; walk cycles the 3 walk frames only.
        if (kind == PlayerAnimKind.IDLE) {
            Texture idle = textures.getOptional("sprites/player/idle_" + facingKey + "_0.png");
            if (idle != null) {
                return idle;
            }
            return textures.getOptional("sprites/player/idle_down_0.png");
        }

        String prefix = switch (kind) {
            case WALK -> "walk";
            case AXE_SWING -> "axe";
            case ROD_CAST, ROD_HOLD, ROD_REEL -> "rod";
            case IDLE -> "idle";
        };

        int[] walkSequence = walkSequenceFor(facing);
        int frameCount = switch (kind) {
            case WALK -> walkSequence.length;
            case AXE_SWING -> GameConfig.PLAYER_AXE_FRAMES;
            case ROD_CAST, ROD_HOLD, ROD_REEL -> GameConfig.PLAYER_ROD_FRAMES;
            case IDLE -> 1;
        };

        double frameDuration = switch (kind) {
            case WALK -> GameConfig.PLAYER_WALK_FRAME_MS;
            case AXE_SWING -> GameConfig.PLAYER_AXE_FRAME_MS;
            case ROD_CAST, ROD_REEL -> GameConfig.PLAYER_ROD_FRAME_MS;
            case ROD_HOLD, IDLE -> 0;
        };

        boolean loop = kind == PlayerAnimKind.WALK;
        int sequenceIndex;
        if (kind == PlayerAnimKind.ROD_HOLD) {
            sequenceIndex = frameCount - 1;
        } else if (kind == PlayerAnimKind.ROD_REEL) {
            int forward = frameIndexForTime(frameCount, frameDuration, player.getAnimElapsedMs(), false);
            sequenceIndex = frameCount - 1 - forward;
        } else {
            sequenceIndex = frameIndexForTime(frameCount, frameDuration, player.getAnimElapsedMs(), loop);
        }
        int frameIndex = kind == PlayerAnimKind.WALK
                ? walkSequence[sequenceIndex]
                : sequenceIndex;

        String path = "sprites/player/" + prefix + "_" + facingKey + "_" + frameIndex + ".png";
        Texture texture = textures.getOptional(path);
        if (texture == null) {
            // Older facings may have fewer frames — hold the last available pose.
            for (int fallback = frameIndex - 1; fallback >= 0 && texture == null; fallback--) {
                texture = textures.getOptional(
                        "sprites/player/" + prefix + "_" + facingKey + "_" + fallback + ".png"
                );
            }
        }
        if (texture == null) {
            texture = textures.getOptional("sprites/player/idle_down_0.png");
        }
        return texture;
    }

    private static int[] walkSequenceFor(PlayerFacing facing) {
        return switch (facing) {
            case LEFT, RIGHT -> GameConfig.PLAYER_WALK_SEQUENCE_SIDE;
            case UP, DOWN -> GameConfig.PLAYER_WALK_SEQUENCE_VERTICAL;
        };
    }

    private static int frameIndexForTime(int frameCount, double frameDurationMs, double elapsedMs, boolean loop) {
        if (frameCount <= 1) {
            return 0;
        }
        int index = (int) (elapsedMs / frameDurationMs);
        if (loop) {
            return index % frameCount;
        }
        return Math.min(index, frameCount - 1);
    }
}
