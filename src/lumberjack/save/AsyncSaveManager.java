package lumberjack.save;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Writes save snapshots on a background thread so gameplay never hitches.
 */
public final class AsyncSaveManager {

    private final SaveManager saveManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "game-save");
        thread.setDaemon(true);
        return thread;
    });

    public AsyncSaveManager(SaveManager saveManager) {
        this.saveManager = saveManager;
    }

    public void saveAsync(String fileName, SaveData data, Consumer<IOException> onFailure) {
        saveAsync(fileName, data, null, onFailure);
    }

    public void saveAsync(String fileName, SaveData data, Runnable onSuccess, Consumer<IOException> onFailure) {
        executor.execute(() -> {
            try {
                saveManager.save(fileName, data);
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } catch (IOException exception) {
                if (onFailure != null) {
                    onFailure.accept(exception);
                }
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
