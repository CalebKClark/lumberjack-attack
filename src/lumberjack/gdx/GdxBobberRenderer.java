package lumberjack.gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.engine.Camera;
import lumberjack.fishing.BobberPhase;
import lumberjack.fishing.FishingManager;

/**
 * World-space fishing bobber for the LibGDX renderer.
 */
public final class GdxBobberRenderer {

    private GdxBobberRenderer() {
    }

    public static void draw(
            SpriteBatch batch,
            GdxTextureCache textures,
            FishingManager fishingManager,
            Camera camera,
            float worldViewWidth,
            float worldViewHeight
    ) {
        if (fishingManager.getBobberPhase() == BobberPhase.IDLE) {
            return;
        }

        GdxWorldRenderer.applyWorldProjection(batch, camera, worldViewWidth, worldViewHeight);

        float worldX = fishingManager.getBobberWorldX();
        float worldY = fishingManager.getBobberWorldY();
        Color bobberColor;
        if (fishingManager.getBobberPhase() == BobberPhase.FLASHING && fishingManager.isBobberFlashVisible()) {
            bobberColor = Color.WHITE;
        } else {
            bobberColor = new Color(1f, 0.86f, 0.24f, 1f);
        }

        // Small marker — not near tile-sized (LibGDX solid draw is world pixels).
        float size = 4f;
        float half = size / 2f;
        GdxBatchUtils.drawSolid(batch, textures, bobberColor, worldX - half, worldY - half, size, size);
    }
}
