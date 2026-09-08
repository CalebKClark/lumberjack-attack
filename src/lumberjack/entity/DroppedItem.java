package lumberjack.entity;

import lumberjack.core.GameConfig;
import lumberjack.inventory.Inventory;
import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;

/**
 * An item sitting on the ground waiting to be picked up.
 * New drops scatter outward with a short bounce; settled drops can be magnetized.
 * Canopy rain drops fall to stump height first, then bounce-scatter.
 */
public final class DroppedItem extends Entity {

    private enum Phase {
        DELAY,
        FALL,
        SCATTER,
        SETTLED
    }

    private ItemStack stack;

    private float posX;
    private float posY;
    private float bounceOffsetY;

    private float scatterFromX;
    private float scatterFromY;
    private float scatterToX;
    private float scatterToY;
    private double scatterDurationMs;
    private double scatterElapsedMs;

    private float fallFromX;
    private float fallFromY;
    private float fallToX;
    private float fallToY;
    private double fallDurationMs;
    private double fallElapsedMs;
    private double appearDelayMs;
    private double appearElapsedMs;

    private Phase phase;

    public DroppedItem(ItemStack stack, int centerX, int centerY, boolean scatter) {
        super(
                centerX - GameConfig.DROP_ITEM_SIZE / 2,
                centerY - GameConfig.DROP_ITEM_SIZE / 2,
                GameConfig.DROP_ITEM_SIZE,
                GameConfig.DROP_ITEM_SIZE
        );
        this.stack = stack.copy();
        this.posX = centerX - GameConfig.DROP_ITEM_SIZE / 2f;
        this.posY = centerY - GameConfig.DROP_ITEM_SIZE / 2f;
        this.scatterFromX = posX;
        this.scatterFromY = posY;
        this.fallFromX = posX;
        this.fallFromY = posY;
        this.fallToX = posX;
        this.fallToY = posY;
        this.fallDurationMs = 0;
        this.appearDelayMs = 0;

        if (scatter) {
            pickScatterTarget(scatterFromX, scatterFromY);
            this.scatterDurationMs = GameConfig.DROP_SCATTER_DURATION_MS;
            this.phase = Phase.SCATTER;
        } else {
            this.scatterToX = scatterFromX;
            this.scatterToY = scatterFromY;
            this.scatterDurationMs = 0;
            this.phase = Phase.SETTLED;
        }
    }

    /**
     * Spawns high on a tree, waits, falls to stump-level ground, then bounce-scatters.
     */
    public static DroppedItem canopyRain(
            ItemStack stack,
            float startCenterX,
            float startCenterY,
            float landCenterX,
            float landCenterY,
            double appearDelayMs,
            double fallDurationMs
    ) {
        DroppedItem drop = new DroppedItem(stack, Math.round(startCenterX), Math.round(startCenterY), false);
        drop.posX = startCenterX - GameConfig.DROP_ITEM_SIZE / 2f;
        drop.posY = startCenterY - GameConfig.DROP_ITEM_SIZE / 2f;
        drop.fallFromX = drop.posX;
        drop.fallFromY = drop.posY;
        drop.fallToX = landCenterX - GameConfig.DROP_ITEM_SIZE / 2f;
        drop.fallToY = landCenterY - GameConfig.DROP_ITEM_SIZE / 2f;
        drop.appearDelayMs = Math.max(0, appearDelayMs);
        drop.fallDurationMs = Math.max(1, fallDurationMs);
        drop.fallElapsedMs = 0;
        drop.appearElapsedMs = 0;
        drop.phase = drop.appearDelayMs > 0 ? Phase.DELAY : Phase.FALL;
        drop.syncPosition();
        return drop;
    }

    public ItemStack getStack() {
        return stack;
    }

    /** Visual upward bounce offset in world pixels (Y increases downward). */
    public float getBounceOffsetY() {
        return bounceOffsetY;
    }

    public boolean isSettled() {
        return phase == Phase.SETTLED;
    }

    /** Hidden until the rain delay finishes. */
    public boolean isVisible() {
        return phase != Phase.DELAY;
    }

    public float getCenterX() {
        return posX + width / 2f;
    }

    public float getCenterY() {
        return posY + height / 2f;
    }

    public void update(double elapsedMs) {
        if (phase == Phase.DELAY) {
            appearElapsedMs += elapsedMs;
            if (appearElapsedMs < appearDelayMs) {
                return;
            }
            phase = Phase.FALL;
        }

        if (phase == Phase.FALL) {
            fallElapsedMs += elapsedMs;
            double t = Math.min(1.0, fallElapsedMs / fallDurationMs);
            // Ease-in so pieces pick up speed as they drop.
            double ease = t * t;
            posX = fallFromX + (float) ((fallToX - fallFromX) * ease);
            posY = fallFromY + (float) ((fallToY - fallFromY) * ease);
            bounceOffsetY = 0f;
            syncPosition();
            if (t >= 1.0) {
                posX = fallToX;
                posY = fallToY;
                beginScatterFromCurrent();
            }
            return;
        }

        if (phase == Phase.SCATTER) {
            scatterElapsedMs += elapsedMs;
            double t = scatterDurationMs <= 0 ? 1.0 : Math.min(1.0, scatterElapsedMs / scatterDurationMs);
            double ease = 1.0 - Math.pow(1.0 - t, 3.0);
            posX = scatterFromX + (float) ((scatterToX - scatterFromX) * ease);
            posY = scatterFromY + (float) ((scatterToY - scatterFromY) * ease);

            double bounceWave = Math.sin(t * Math.PI * GameConfig.DROP_BOUNCE_COUNT);
            bounceOffsetY = (float) (Math.abs(bounceWave) * GameConfig.DROP_BOUNCE_HEIGHT_PX * (1.0 - t));

            if (t >= 1.0) {
                phase = Phase.SETTLED;
                posX = scatterToX;
                posY = scatterToY;
                bounceOffsetY = 0f;
            }
            syncPosition();
        }
    }

    /**
     * Pulls toward the player when the gap between hitboxes is within magnet range.
     *
     * @return true if close enough to attempt inventory pickup
     */
    public boolean updateMagnet(
            float playerX,
            float playerY,
            float playerW,
            float playerH,
            double magnetRadiusPx,
            double elapsedMs
    ) {
        if (phase != Phase.SETTLED || stack.isEmpty() || magnetRadiusPx <= 0) {
            return false;
        }

        double gap = hitboxGap(playerX, playerY, playerW, playerH, posX, posY, width, height);
        if (gap > magnetRadiusPx) {
            return false;
        }
        if (gap <= GameConfig.DROP_COLLECT_DISTANCE_PX) {
            return true;
        }

        float playerCenterX = playerX + playerW / 2f;
        float playerCenterY = playerY + playerH / 2f;
        float dx = playerCenterX - getCenterX();
        float dy = playerCenterY - getCenterY();
        double centerDistance = Math.hypot(dx, dy);
        if (centerDistance <= 0.0001) {
            return true;
        }

        double step = GameConfig.DROP_MAGNET_SPEED_PX_PER_SEC * (elapsedMs / 1000.0);
        // Mild speed-up as it nears the player — keep the pull smooth, not snappy.
        step *= 1.0 + 0.35 * (1.0 - gap / magnetRadiusPx);
        if (step >= centerDistance) {
            posX = playerCenterX - width / 2f;
            posY = playerCenterY - height / 2f;
            syncPosition();
            return true;
        }

        float nx = (float) (dx / centerDistance);
        float ny = (float) (dy / centerDistance);
        posX += nx * (float) step;
        posY += ny * (float) step;
        syncPosition();
        return hitboxGap(playerX, playerY, playerW, playerH, posX, posY, width, height)
                <= GameConfig.DROP_COLLECT_DISTANCE_PX;
    }

    /** Shortest gap between two axis-aligned boxes; 0 when overlapping. */
    private static double hitboxGap(
            float ax,
            float ay,
            float aw,
            float ah,
            float bx,
            float by,
            float bw,
            float bh
    ) {
        float gapX = Math.max(0f, Math.max(ax - (bx + bw), bx - (ax + aw)));
        float gapY = Math.max(0f, Math.max(ay - (by + bh), by - (ay + ah)));
        return Math.hypot(gapX, gapY);
    }

    /**
     * @return true if the drop was fully picked up and should be removed from the world
     */
    public boolean tryCollect(Inventory inventory, ItemRegistry itemRegistry) {
        if (stack.isEmpty()) {
            return false;
        }

        int remaining = inventory.addItem(stack.getItemId(), stack.getQuantity());
        if (remaining <= 0) {
            stack = ItemStack.empty();
            return true;
        }

        stack.setQuantity(remaining);
        return false;
    }

    private void beginScatterFromCurrent() {
        scatterFromX = posX;
        scatterFromY = posY;
        pickScatterTarget(scatterFromX, scatterFromY);
        scatterDurationMs = GameConfig.DROP_SCATTER_DURATION_MS;
        scatterElapsedMs = 0;
        phase = Phase.SCATTER;
    }

    private void pickScatterTarget(float fromX, float fromY) {
        double min = GameConfig.DROP_SCATTER_MIN_TILES * GameConfig.TILE_SIZE;
        double max = GameConfig.DROP_SCATTER_MAX_TILES * GameConfig.TILE_SIZE;
        double distance = min + Math.random() * (max - min);
        double angle = Math.random() * Math.PI * 2.0;
        scatterToX = fromX + (float) (Math.cos(angle) * distance);
        // Keep bounce-scatter mostly on the ground line (don't climb back up much).
        scatterToY = fromY + (float) (Math.sin(angle) * distance * 0.35);
    }

    private void syncPosition() {
        setPosition(Math.round(posX), Math.round(posY));
    }
}
