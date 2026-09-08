package lumberjack.gdx;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.audio.MusicTrack;

/**
 * Root LibGDX game — owns the shared {@link SpriteBatch}, save controller, and screen stack.
 */
public final class LumberjackGame extends Game {

    private SpriteBatch batch;
    private GdxSaveController saveController;
    private GdxAudioManager audio;

    @Override
    public void create() {
        batch = new SpriteBatch();
        saveController = new GdxSaveController();
        audio = new GdxAudioManager();
        showMainMenu();
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public GdxSaveController getSaveController() {
        return saveController;
    }

    public GdxAudioManager getAudio() {
        return audio;
    }

    public void showMainMenu() {
        switchScreen(new MainMenuScreen(this, batch, saveController));
        // Title theme: loops until the player joins a save.
        audio.playMusic(MusicTrack.LUMBERJACK_ATTACK_OVERTURE, true);
    }

    public void showWorldEdit() {
        // Keep title music playing in the editor (not a joined save).
        switchScreen(new WorldEditScreen(this, batch));
    }

    public void startNewGame(String saveTitle) throws Exception {
        // Construct first so a failure does not tear down the menu screen / music.
        GameplayScreen next = new GameplayScreen(this, batch, saveController, saveTitle, null);
        audio.stopMusic();
        switchScreen(next);
    }

    public void startLoadGame(String fileName) throws Exception {
        GameplayScreen next = new GameplayScreen(this, batch, saveController, null, fileName);
        audio.stopMusic();
        switchScreen(next);
    }

    private void switchScreen(Screen next) {
        Screen previous = getScreen();
        Gdx.input.setInputProcessor(null);
        if (batch.isDrawing()) {
            batch.end();
        }
        batch.setColor(Color.WHITE);
        setScreen(next);
        // Dispose on the following frame so we never free textures the shared batch
        // still considers "last bound" from the previous screen's final draw.
        if (previous != null) {
            Gdx.app.postRunnable(() -> {
                try {
                    previous.dispose();
                } catch (RuntimeException exception) {
                    Gdx.app.error("LumberjackGame", "Failed disposing previous screen", exception);
                }
            });
        }
    }

    @Override
    public void dispose() {
        Screen screen = getScreen();
        if (screen != null) {
            screen.dispose();
            setScreen(null);
        }
        if (audio != null) {
            audio.dispose();
            audio = null;
        }
        if (saveController != null) {
            saveController.shutdown();
        }
        if (batch != null) {
            batch.dispose();
        }
    }
}
