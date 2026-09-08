package lumberjack.fishing;

import lumberjack.combat.EffectivePlayerStats;
import lumberjack.core.GameConfig;
import lumberjack.fishing.minigame.FishingMinigame;
import lumberjack.game.GameSession;
import lumberjack.skills.SkillType;

/**
 * Tracks bobber state, bite timing, and the active fishing minigame.
 */
public final class FishingManager {

    private BobberPhase bobberPhase = BobberPhase.IDLE;
    private int bobberWorldX;
    private int bobberWorldY;
    private double waitRemainingMs;
    private double flashRemainingMs;
    private boolean flashVisible = true;
    private double flashToggleMs;

    /** Cast click accepted; bobber appears when the cast anim reaches hold. */
    private boolean pendingCast;
    private int pendingCastWorldX;
    private int pendingCastWorldY;

    private FishingMinigame activeMinigame;
    private double resultDisplayRemainingMs;
    private Runnable minigameStartListener;
    private Runnable minigameEndListener;

    public void setMinigameStartListener(Runnable minigameStartListener) {
        this.minigameStartListener = minigameStartListener;
    }

    public void setMinigameEndListener(Runnable minigameEndListener) {
        this.minigameEndListener = minigameEndListener;
    }

    public boolean isBobberIdle() {
        return bobberPhase == BobberPhase.IDLE && activeMinigame == null && !pendingCast;
    }

    public boolean isLineOut() {
        return pendingCast || bobberPhase != BobberPhase.IDLE || activeMinigame != null;
    }

    public boolean isFishingMinigameOpen() {
        return activeMinigame != null && !activeMinigame.isFinished();
    }

    public boolean hasActiveMinigameSession() {
        return activeMinigame != null;
    }

    public FishingMinigame getActiveMinigame() {
        return activeMinigame;
    }

    public BobberPhase getBobberPhase() {
        return bobberPhase;
    }

    public int getBobberWorldX() {
        return bobberWorldX;
    }

    public int getBobberWorldY() {
        return bobberWorldY;
    }

    public boolean isBobberFlashVisible() {
        return flashVisible;
    }

    /**
     * Starts the cast animation; bobber/wait begins when {@link #onRodCastHoldReached} fires.
     */
    public void beginPendingCast(int worldX, int worldY) {
        pendingCast = true;
        pendingCastWorldX = worldX;
        pendingCastWorldY = worldY;
        bobberPhase = BobberPhase.IDLE;
        flashRemainingMs = 0;
        flashVisible = true;
    }

    public void onRodCastHoldReached(GameSession session) {
        if (!pendingCast) {
            return;
        }
        pendingCast = false;
        castAt(session, pendingCastWorldX, pendingCastWorldY);
    }

    public void castAt(GameSession session, int worldX, int worldY) {
        bobberWorldX = worldX;
        bobberWorldY = worldY;
        bobberPhase = BobberPhase.WAITING;
        waitRemainingMs = randomBiteDelayMs(session);
        flashRemainingMs = 0;
        flashVisible = true;
    }

    public boolean update(GameSession session, double elapsedMs) {
        if (activeMinigame != null) {
            return updateMinigame(session, elapsedMs);
        }

        if (bobberPhase == BobberPhase.IDLE) {
            return false;
        }

        boolean changed = false;

        if (bobberPhase == BobberPhase.WAITING) {
            waitRemainingMs -= elapsedMs;
            if (waitRemainingMs <= 0) {
                bobberPhase = BobberPhase.FLASHING;
                flashRemainingMs = GameConfig.FISHING_BITE_FLASH_MS;
                flashToggleMs = 0;
                flashVisible = true;
                changed = true;
            }
        } else if (bobberPhase == BobberPhase.FLASHING) {
            flashRemainingMs -= elapsedMs;
            flashToggleMs -= elapsedMs;
            if (flashToggleMs <= 0) {
                flashVisible = !flashVisible;
                flashToggleMs = 100;
                changed = true;
            }

            if (flashRemainingMs <= 0) {
                // Missed the bite window — wait for another nibble.
                bobberPhase = BobberPhase.WAITING;
                waitRemainingMs = randomBiteDelayMs(session);
                flashRemainingMs = 0;
                flashVisible = true;
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Player must click during {@link BobberPhase#FLASHING} to start the minigame.
     *
     * @return true if a bite was hooked and the minigame started
     */
    public boolean tryHookBite(GameSession session) {
        if (activeMinigame != null || bobberPhase != BobberPhase.FLASHING) {
            return false;
        }

        bobberPhase = BobberPhase.IDLE;
        flashRemainingMs = 0;
        startMinigame(session);
        return true;
    }

    private boolean updateMinigame(GameSession session, double elapsedMs) {
        if (!activeMinigame.isFinished()) {
            activeMinigame.update(elapsedMs);
        }

        if (!activeMinigame.isFinished()) {
            return true;
        }

        if (resultDisplayRemainingMs <= 0) {
            resultDisplayRemainingMs = GameConfig.FISHING_RESULT_DISPLAY_MS;
        }
        resultDisplayRemainingMs -= elapsedMs;
        if (resultDisplayRemainingMs <= 0) {
            finishMinigame(session);
        }
        return true;
    }

    public void startMinigame(GameSession session) {
        activeMinigame = new FishingMinigame(session.getItemRegistry(), session.getFishSpawnRegistry());
        resultDisplayRemainingMs = 0;
        if (minigameStartListener != null) {
            minigameStartListener.run();
        }
    }

    public void finishMinigame(GameSession session) {
        if (activeMinigame == null) {
            return;
        }

        String caughtItemId = activeMinigame.getCaughtItemId();
        activeMinigame = null;
        resultDisplayRemainingMs = 0;
        bobberPhase = BobberPhase.IDLE;
        pendingCast = false;

        if (caughtItemId != null) {
            session.getInventory().addItem(caughtItemId, 1);
            int fishingXp = session.getItemRegistry().get(caughtItemId).getFishingXp();
            if (fishingXp > 0) {
                session.getPlayerSkills().grantXp(SkillType.FISHING, fishingXp);
            }
        }

        session.getPlayer().triggerRodReel();

        if (minigameEndListener != null) {
            minigameEndListener.run();
        }
    }

    public void cancelMinigame(GameSession session) {
        if (activeMinigame == null) {
            return;
        }

        activeMinigame = null;
        resultDisplayRemainingMs = 0;
        bobberPhase = BobberPhase.IDLE;
        pendingCast = false;
        session.getPlayer().triggerRodReel();

        if (minigameEndListener != null) {
            minigameEndListener.run();
        }
    }

    /**
     * Cancels a pending cast or waiting bobber and reels the line back in.
     *
     * @return true if fishing was cancelled
     */
    public boolean cancelLine(GameSession session) {
        if (activeMinigame != null) {
            cancelMinigame(session);
            return true;
        }
        if (!pendingCast && bobberPhase == BobberPhase.IDLE) {
            return false;
        }
        pendingCast = false;
        bobberPhase = BobberPhase.IDLE;
        waitRemainingMs = 0;
        flashRemainingMs = 0;
        session.getPlayer().triggerRodReel();
        return true;
    }

    public void reset() {
        bobberPhase = BobberPhase.IDLE;
        waitRemainingMs = 0;
        flashRemainingMs = 0;
        resultDisplayRemainingMs = 0;
        pendingCast = false;
        activeMinigame = null;
    }

    private double randomBiteDelayMs(GameSession session) {
        EffectivePlayerStats stats = EffectivePlayerStats.resolve(
                session.getInventory(),
                session.getItemRegistry()
        );
        return stats.getBiteWaitMinMs()
                + Math.random() * (stats.getBiteWaitMaxMs() - stats.getBiteWaitMinMs());
    }
}
