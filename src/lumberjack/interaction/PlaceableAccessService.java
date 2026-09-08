package lumberjack.interaction;

import lumberjack.combat.ReachUtils;
import lumberjack.engine.Camera;
import lumberjack.engine.DisplaySettings;
import lumberjack.entity.PlacedObject;
import lumberjack.game.GameSession;
import lumberjack.world.placeable.PlacedObjectKey;

/**
 * Finds a placed object the player can interact with from a screen-space cursor position.
 */
public final class PlaceableAccessService {

    private PlaceableAccessService() {
    }

    public static PlacedObjectKey findInteractable(
            GameSession session,
            DisplaySettings displaySettings,
            int screenX,
            int screenY,
            String... itemIds
    ) {
        if (itemIds.length == 0) {
            return null;
        }

        Camera camera = session.getCamera();
        int worldX = displaySettings.screenToWorldX(screenX, camera);
        int worldY = displaySettings.screenToWorldY(screenY, camera);
        String mapId = session.getCurrentMapId();

        PlacedObject placedObject = session.getPlacedObjectManager().findAtWorldPosition(mapId, worldX, worldY);
        if (placedObject == null || !matchesItemId(placedObject.getItemId(), itemIds)) {
            return null;
        }

        int centerX = placedObject.getX() + placedObject.getWidth() / 2;
        int centerY = placedObject.getY() + placedObject.getHeight() / 2;
        if (!ReachUtils.isWithinReach(session.getPlayer(), centerX, centerY)) {
            return null;
        }

        return new PlacedObjectKey(mapId, placedObject.getCol(), placedObject.getRow());
    }

    private static boolean matchesItemId(String itemId, String... allowedIds) {
        for (String allowedId : allowedIds) {
            if (allowedId.equals(itemId)) {
                return true;
            }
        }
        return false;
    }
}
