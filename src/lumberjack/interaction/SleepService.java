package lumberjack.interaction;

import lumberjack.entity.PlacedObject;
import lumberjack.entity.Player;
import lumberjack.game.GameSession;
import lumberjack.time.GameClock;
import lumberjack.world.map.TileMap;
import lumberjack.world.placeable.BedPlaceable;
import lumberjack.world.tile.TileDefinition;
import lumberjack.world.tile.TileRegistry;

/**
 * Prompts the player to sleep when they step onto a bed (tile or placed).
 */
public final class SleepService {

    private final SleepPrompt sleepPrompt;
    private boolean wasOnBedLastFrame;

    public SleepService(SleepPrompt sleepPrompt) {
        this.sleepPrompt = sleepPrompt;
    }

    /**
     * @return true if the player slept and the clock advanced
     */
    public boolean update(GameSession session) {
        Player player = session.getPlayer();
        TileMap map = session.getCurrentMap();
        TileRegistry tileRegistry = session.getTileRegistry();

        int centerX = player.getX() + player.getWidth() / 2;
        int centerY = player.getY() + player.getHeight() / 2;
        int col = map.getColumnAtWorldX(centerX);
        int row = map.getRowAtWorldY(centerY);

        boolean onBed = false;
        if (map.isInBounds(col, row)) {
            TileDefinition tile = tileRegistry.get(map.getTileIdAt(col, row));
            onBed = tile.isBed();
            if (!onBed) {
                PlacedObject placed = session.getPlacedObjectManager().findAt(session.getCurrentMapId(), col, row);
                onBed = placed != null && BedPlaceable.isBed(placed.getItemId());
            }
        }

        if (!onBed) {
            wasOnBedLastFrame = false;
            sleepPrompt.cancelSleepPrompt();
            return false;
        }

        if (wasOnBedLastFrame) {
            return false;
        }

        Boolean decision = sleepPrompt.confirmSleep();
        if (decision == null) {
            // Deferred UI still open — keep prompting next frame without locking the bed-enter edge.
            return false;
        }

        wasOnBedLastFrame = true;
        if (decision) {
            GameClock clock = session.getClock();
            int skippedMinutes = clock.advanceToNextMorning();
            session.applySkippedGameTime(skippedMinutes);
            session.getEnergyState().resetToFull();
            return true;
        }
        return false;
    }

    public void reset() {
        wasOnBedLastFrame = false;
        sleepPrompt.cancelSleepPrompt();
    }
}
