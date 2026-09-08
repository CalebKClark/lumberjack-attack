package lumberjack.gdx;

import java.io.IOException;

import lumberjack.core.GameConfig;
import lumberjack.game.GameSession;
import lumberjack.save.AsyncSaveManager;
import lumberjack.save.SaveManager;
import lumberjack.save.SaveSummary;

import java.util.List;

/**
 * LibGDX save/load lifecycle for gameplay sessions.
 */
public final class GdxSaveController {

    private final SaveManager saveManager = new SaveManager();
    private final AsyncSaveManager asyncSaveManager = new AsyncSaveManager(saveManager);

    private GameSession session;
    private String activeSaveFileName;
    private double autoSaveElapsedMs;

    public SaveManager getSaveManager() {
        return saveManager;
    }

    public List<SaveSummary> listSaves() {
        return saveManager.listSaves();
    }

    public void attach(GameSession session) {
        this.session = session;
        session.setOnSleptListener(this::saveAfterSleep);
    }

    public void detach() {
        session = null;
        activeSaveFileName = null;
        autoSaveElapsedMs = 0;
    }

    public String getActiveSaveFileName() {
        return activeSaveFileName;
    }

    public boolean hasActiveSave() {
        return activeSaveFileName != null;
    }

    public String nextAvailableTitle(String base) {
        String title = base == null || base.isBlank() ? "Untitled" : base.trim();
        if (!saveManager.existsByTitle(title)) {
            return title;
        }
        int index = 2;
        while (saveManager.existsByTitle(title + " " + index)) {
            index++;
        }
        return title + " " + index;
    }

    public void createNewGame(String saveTitle) throws IOException {
        requireSession();
        session.resetToNewGame();
        session.setSaveTitle(saveTitle);
        activeSaveFileName = saveManager.createNewSave(session.captureState());
        autoSaveElapsedMs = 0;
    }

    public void loadSave(String fileName) throws IOException {
        requireSession();
        session.applyState(saveManager.load(fileName));
        activeSaveFileName = fileName;
        autoSaveElapsedMs = 0;
    }

    public void deleteSave(String fileName) throws IOException {
        saveManager.delete(fileName);
        if (fileName != null && fileName.equals(activeSaveFileName)) {
            activeSaveFileName = null;
        }
    }

    public boolean existsByTitle(String saveTitle) {
        return saveManager.existsByTitle(saveTitle);
    }

    public boolean saveNow() {
        if (session == null || activeSaveFileName == null) {
            return false;
        }
        try {
            saveManager.save(activeSaveFileName, session.captureState());
            session.getNotificationManager().notifyGameSaved();
            autoSaveElapsedMs = 0;
            return true;
        } catch (IOException exception) {
            session.getNotificationManager().notifyMessage("Save failed: " + exception.getMessage());
            return false;
        }
    }

    public void tickAutoSave(double elapsedMs) {
        if (session == null || activeSaveFileName == null || !session.shouldAdvanceWorldTime()) {
            return;
        }

        autoSaveElapsedMs += elapsedMs;
        if (autoSaveElapsedMs < GameConfig.AUTO_SAVE_INTERVAL_MS) {
            return;
        }

        autoSaveElapsedMs = 0;
        String fileName = activeSaveFileName;
        asyncSaveManager.saveAsync(
                fileName,
                session.captureState(),
                null,
                exception -> GdxAppPosts.post(() -> {
                    if (session != null) {
                        session.getNotificationManager().notifyMessage("Auto-save failed");
                    }
                })
        );
    }

    public void saveAfterSleep() {
        if (session == null || activeSaveFileName == null) {
            return;
        }

        String fileName = activeSaveFileName;
        asyncSaveManager.saveAsync(
                fileName,
                session.captureState(),
                () -> GdxAppPosts.post(() -> {
                    if (session != null) {
                        session.getNotificationManager().notifyGameSaved();
                    }
                }),
                exception -> GdxAppPosts.post(() -> {
                    if (session != null) {
                        session.getNotificationManager().notifyMessage("Save failed after sleep");
                    }
                })
        );
    }

    public void shutdown() {
        asyncSaveManager.shutdown();
    }

    private void requireSession() {
        if (session == null) {
            throw new IllegalStateException("Save controller has no attached session.");
        }
    }
}
