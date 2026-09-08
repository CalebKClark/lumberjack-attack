package lumberjack.interaction;

import lumberjack.core.GameConfig;
import lumberjack.game.GameSession;
import lumberjack.world.map.WorldMapManager;

/**
 * Handles player death and respawn at the cabin bed.
 */
public final class DeathService {

    public boolean checkAndRespawn(GameSession session) {
        if (!session.getCombatState().isDead()) {
            return false;
        }

        respawnAtBed(session);
        return true;
    }

    private void respawnAtBed(GameSession session) {
        session.switchToMap(GameConfig.RESPAWN_MAP);
        WorldMapManager.placePlayerAtTile(
                session.getPlayer(),
                session.getCurrentMap(),
                GameConfig.RESPAWN_BED_COL,
                GameConfig.RESPAWN_BED_ROW
        );
        session.getCombatState().resetToFullHealth(
                session.getInventory(),
                session.getItemRegistry()
        );
        session.getEnergyState().resetToFull();
        session.clearTransitionCooldown();
        session.onMapChanged();
    }
}
