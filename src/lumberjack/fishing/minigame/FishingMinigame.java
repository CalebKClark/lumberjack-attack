package lumberjack.fishing.minigame;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import lumberjack.core.GameConfig;
import lumberjack.fishing.FishSpawnDefinition;
import lumberjack.fishing.FishSpawnRegistry;
import lumberjack.item.ItemRegistry;

/**
 * Underwater dodge minigame: descending hook, A/D to move, catch what you touch.
 * World Y increases with depth; the view scrolls so the hook stays on screen.
 */
public final class FishingMinigame {

    private static final int HOOK_WIDTH = 14;
    private static final int HOOK_HEIGHT = 14;
    private static final int ENTITY_WIDTH = 36;
    private static final int ENTITY_HEIGHT = 22;

    private final int panelWidth;
    private final int panelHeight;
    private final int playAreaTop;
    private final int hookScreenY;
    private final ItemRegistry itemRegistry;
    private final FishSpawnRegistry fishSpawnRegistry;

    private double lineX;
    private double depth;
    private double nextSpawnDepth;
    private boolean moveLeft;
    private boolean moveRight;
    private boolean finished;
    private String caughtItemId;
    private final List<MinigameEntity> entities = new ArrayList<>();

    public FishingMinigame(ItemRegistry itemRegistry, FishSpawnRegistry fishSpawnRegistry) {
        this.itemRegistry = itemRegistry;
        this.fishSpawnRegistry = fishSpawnRegistry;
        panelWidth = GameConfig.FISHING_MINIGAME_PANEL_WIDTH;
        panelHeight = GameConfig.FISHING_MINIGAME_PANEL_HEIGHT;
        playAreaTop = GameConfig.FISHING_PLAY_AREA_TOP;
        hookScreenY = GameConfig.FISHING_HOOK_SCREEN_Y;
        lineX = panelWidth / 2.0;
        depth = 0;
        nextSpawnDepth = GameConfig.FISHING_ENTITY_SPAWN_INTERVAL;
    }

    public void setMoveLeft(boolean moveLeft) {
        this.moveLeft = moveLeft;
    }

    public void setMoveRight(boolean moveRight) {
        this.moveRight = moveRight;
    }

    public boolean isFinished() {
        return finished;
    }

    public String getCaughtItemId() {
        return caughtItemId;
    }

    public double getLineX() {
        return lineX;
    }

    public double getDepth() {
        return depth;
    }

    public int getHookScreenY() {
        return hookScreenY;
    }

    public int getPlayAreaTop() {
        return playAreaTop;
    }

    /**
     * Converts a world-space depth to a Y coordinate inside the panel (before panel offset).
     */
    public double worldYToScreenY(double worldY) {
        return hookScreenY + (worldY - depth);
    }

    public List<MinigameEntity> getEntities() {
        return List.copyOf(entities);
    }

    public int getPanelWidth() {
        return panelWidth;
    }

    public int getPanelHeight() {
        return panelHeight;
    }

    public void update(double elapsedMs) {
        if (finished || elapsedMs <= 0) {
            return;
        }

        double seconds = elapsedMs / 1000.0;

        if (moveLeft) {
            lineX -= GameConfig.FISHING_HOOK_MOVE_SPEED * seconds;
        }
        if (moveRight) {
            lineX += GameConfig.FISHING_HOOK_MOVE_SPEED * seconds;
        }
        lineX = Math.max(HOOK_WIDTH / 2.0, Math.min(panelWidth - HOOK_WIDTH / 2.0, lineX));

        depth += resolveDescendSpeed() * seconds;

        while (nextSpawnDepth <= depth + GameConfig.FISHING_SPAWN_LOOKAHEAD) {
            spawnEntity(nextSpawnDepth);
            nextSpawnDepth += GameConfig.FISHING_ENTITY_SPAWN_INTERVAL;
        }

        for (MinigameEntity entity : entities) {
            if (!entity.isStationary()) {
                entity.update(seconds, panelWidth);
            }
        }

        Rectangle hookBounds = getHookBounds();
        for (MinigameEntity entity : entities) {
            if (hookBounds.intersects(entity.getBounds())) {
                caughtItemId = entity.getItemId();
                finished = true;
                return;
            }
        }

        entities.removeIf(entity -> entity.getY() < depth - GameConfig.FISHING_DESPAWN_BEHIND);
    }

    private double resolveDescendSpeed() {
        int tier = (int) (depth / GameConfig.FISHING_LINE_SPEED_DEPTH_TIER);
        return GameConfig.FISHING_LINE_DESCEND_SPEED + tier * GameConfig.FISHING_LINE_DESCEND_SPEED_PER_TIER;
    }

    private Rectangle getHookBounds() {
        int x = (int) Math.round(lineX - HOOK_WIDTH / 2.0);
        int y = (int) Math.round(depth - HOOK_HEIGHT / 2.0);
        return new Rectangle(x, y, HOOK_WIDTH, HOOK_HEIGHT);
    }

    private void spawnEntity(double spawnDepth) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        SpawnRoll spawn = rollSpawn(random, spawnDepth);
        double x = random.nextDouble(ENTITY_WIDTH, panelWidth - ENTITY_WIDTH);

        double velocityX = 0;
        if (!spawn.stationary()) {
            double referenceSpeed = random.nextDouble(
                    GameConfig.FISHING_BASE_SPEED_MIN,
                    GameConfig.FISHING_BASE_SPEED_MAX
            );
            double speedScale = spawn.speedStat() / (double) GameConfig.FISHING_REFERENCE_SPEED_STAT;
            int direction = random.nextBoolean() ? 1 : -1;
            velocityX = referenceSpeed * speedScale * direction;
        }

        entities.add(new MinigameEntity(
                spawn.itemId(),
                x,
                spawnDepth,
                ENTITY_WIDTH,
                ENTITY_HEIGHT,
                velocityX,
                spawn.stationary()
        ));
    }

    private SpawnRoll rollSpawn(ThreadLocalRandom random, double spawnDepth) {
        if (random.nextDouble() < GameConfig.FISHING_TRASH_SPAWN_CHANCE) {
            FishSpawnDefinition trash = fishSpawnRegistry.rollTrash(random);
            return new SpawnRoll(trash.getItemId(), true, trash.getSpeed());
        }

        FishSpawnDefinition fish = fishSpawnRegistry.rollFishAtDepth(spawnDepth, random);
        return new SpawnRoll(fish.getItemId(), false, fish.getSpeed());
    }

    private record SpawnRoll(String itemId, boolean stationary, int speedStat) {
    }

    public static final class MinigameEntity {

        private final String itemId;
        private double x;
        private final double y;
        private final int width;
        private final int height;
        private double velocityX;
        private final boolean stationary;

        public MinigameEntity(
                String itemId,
                double x,
                double y,
                int width,
                int height,
                double velocityX,
                boolean stationary
        ) {
            this.itemId = itemId;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.velocityX = velocityX;
            this.stationary = stationary;
        }

        public void update(double seconds, int panelWidth) {
            x += velocityX * seconds;
            if (x <= 0) {
                x = 0;
                velocityX = Math.abs(velocityX);
            } else if (x + width >= panelWidth) {
                x = panelWidth - width;
                velocityX = -Math.abs(velocityX);
            }
        }

        public boolean isStationary() {
            return stationary;
        }

        public String getItemId() {
            return itemId;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public Rectangle getBounds() {
            return new Rectangle((int) Math.round(x), (int) Math.round(y), width, height);
        }
    }
}
