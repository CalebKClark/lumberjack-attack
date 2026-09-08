package lumberjack.engine;

/**
 * 2D camera that translates world coordinates into screen space.
 * The player stays centered when possible; otherwise the view clamps to map bounds
 * (Stardew-style camera bounds).
 */
public final class Camera {

    private int x;
    private int y;

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /**
     * Centers the camera on a world position within the given viewport size.
     */
    public void centerOn(int worldX, int worldY, int viewportWidth, int viewportHeight) {
        x = worldX - viewportWidth / 2;
        y = worldY - viewportHeight / 2;
    }

    /**
     * Centers on a world point, then clamps so the view stays inside the map.
     * If the map is smaller than the viewport on an axis, the map is centered on that axis.
     */
    public void centerOnClamped(
            int worldX,
            int worldY,
            int viewportWidth,
            int viewportHeight,
            int mapWidthPx,
            int mapHeightPx
    ) {
        centerOn(worldX, worldY, viewportWidth, viewportHeight);
        x = clampAxis(x, viewportWidth, mapWidthPx);
        y = clampAxis(y, viewportHeight, mapHeightPx);
    }

    private static int clampAxis(int cameraOrigin, int viewportSize, int mapSize) {
        int max = mapSize - viewportSize;
        if (max <= 0) {
            return max / 2;
        }
        if (cameraOrigin < 0) {
            return 0;
        }
        if (cameraOrigin > max) {
            return max;
        }
        return cameraOrigin;
    }
}
