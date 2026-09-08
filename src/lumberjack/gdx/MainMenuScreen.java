package lumberjack.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import lumberjack.core.GameConfig;
import lumberjack.engine.DisplaySettings;
import lumberjack.engine.input.InputSettings;
import lumberjack.save.SaveSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Title screen: New Game, Load Game, Settings, Quit — with name prompt and delete.
 */
public final class MainMenuScreen extends ScreenAdapter {

    private enum View {
        HOME,
        LOAD,
        SETTINGS
    }

    private enum ConfirmMode {
        NONE,
        OVERWRITE,
        DELETE
    }

    private final LumberjackGame game;
    private final SpriteBatch batch;
    private final GdxSaveController saveController;
    private final GdxTextureCache textures;
    private final BitmapFont font;
    private final ScreenViewport viewport;
    private final GdxMainMenuRenderer renderer = new GdxMainMenuRenderer();
    private final GdxTextInputOverlay namePrompt = new GdxTextInputOverlay();
    private final GdxConfirmOverlay confirmOverlay = new GdxConfirmOverlay();
    private final GdxSettingsPanel settingsPanel = new GdxSettingsPanel();
    private final InputSettings inputSettings;
    private final DisplaySettings displaySettings;

    private View view = View.HOME;
    private ConfirmMode confirmMode = ConfirmMode.NONE;
    private List<SaveSummary> saves = List.of();
    private String statusMessage = "";
    private String pendingNewGameTitle = "";
    private SaveSummary pendingDelete;
    private boolean transitionQueued;
    private int loadScrollOffset;

    public MainMenuScreen(LumberjackGame game, SpriteBatch batch, GdxSaveController saveController) {
        this.game = game;
        this.batch = batch;
        this.saveController = saveController;
        this.textures = new GdxTextureCache();
        this.font = new BitmapFont();
        this.font.getData().setScale(1.4f);
        this.viewport = new ScreenViewport();
        this.inputSettings = InputSettings.load();
        this.displaySettings = DisplaySettings.load();
    }

    @Override
    public void show() {
        view = View.HOME;
        statusMessage = "";
        confirmMode = ConfirmMode.NONE;
        pendingDelete = null;
        pendingNewGameTitle = "";
        transitionQueued = false;
        loadScrollOffset = 0;
        settingsPanel.cancelListening();
        namePrompt.close();
        confirmOverlay.close();
        Gdx.input.setInputProcessor(null);
        refreshSaves();
    }

    @Override
    public void render(float delta) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        if (view == View.SETTINGS) {
            handleSettings(screenWidth, screenHeight);
        } else {
            handleDialogs(screenWidth, screenHeight);
            if (!namePrompt.isVisible() && !confirmOverlay.isVisible() && !transitionQueued) {
                handleLoadScrollKeys();
                handleInput(screenWidth, screenHeight);
            }
        }

        Gdx.gl.glClearColor(0.07f, 0.08f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.update(screenWidth, screenHeight, true);
        batch.begin();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.setColor(1f, 1f, 1f, 1f);

        if (view == View.SETTINGS) {
            settingsPanel.draw(
                    batch,
                    font,
                    textures,
                    inputSettings,
                    displaySettings,
                    screenWidth,
                    screenHeight
            );
        } else if (view == View.HOME) {
            renderer.drawHome(batch, font, textures, screenWidth, screenHeight, statusMessage);
        } else {
            renderer.drawLoadList(
                    batch,
                    font,
                    textures,
                    screenWidth,
                    screenHeight,
                    saves,
                    loadScrollOffset,
                    statusMessage
            );
        }

        namePrompt.draw(batch, font, textures, screenWidth, screenHeight);
        confirmOverlay.draw(batch, font, textures, screenWidth, screenHeight);

        batch.end();
    }

    private void handleSettings(int screenWidth, int screenHeight) {
        if (settingsPanel.isListening()) {
            settingsPanel.update(inputSettings, displaySettings, screenWidth, screenHeight);
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            settingsPanel.cancelListening();
            view = View.HOME;
            statusMessage = "";
            return;
        }

        if (settingsPanel.update(inputSettings, displaySettings, screenWidth, screenHeight)) {
            view = View.HOME;
            statusMessage = "";
        }
    }

    private void handleDialogs(int screenWidth, int screenHeight) {
        if (namePrompt.isVisible()) {
            namePrompt.updateInput(screenWidth, screenHeight);
            String result = namePrompt.pollResult();
            if (result == null) {
                return;
            }
            if (result.isEmpty()) {
                statusMessage = "";
                return;
            }
            beginNewGameWithTitle(result);
            return;
        }

        if (confirmOverlay.isVisible()) {
            confirmOverlay.updateInput(screenWidth, screenHeight);
            Boolean confirmed = confirmOverlay.pollResult();
            if (confirmed == null) {
                return;
            }
            ConfirmMode mode = confirmMode;
            confirmMode = ConfirmMode.NONE;
            if (!confirmed) {
                if (mode == ConfirmMode.OVERWRITE) {
                    namePrompt.open("Name your save", pendingNewGameTitle);
                }
                pendingDelete = null;
                pendingNewGameTitle = "";
                return;
            }
            if (mode == ConfirmMode.OVERWRITE) {
                launchNewGame(pendingNewGameTitle);
                pendingNewGameTitle = "";
            } else if (mode == ConfirmMode.DELETE && pendingDelete != null) {
                deleteSave(pendingDelete);
                pendingDelete = null;
            }
        }
    }

    private void handleLoadScrollKeys() {
        if (view != View.LOAD) {
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)
                || Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) {
            loadScrollOffset = GdxMainMenuRenderer.clampScroll(loadScrollOffset - 1, saves.size());
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)
                || Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) {
            loadScrollOffset = GdxMainMenuRenderer.clampScroll(loadScrollOffset + 1, saves.size());
        }
    }

    private void handleInput(int screenWidth, int screenHeight) {
        if (!GdxInputHelper.isPrimaryMouseJustPressed()) {
            return;
        }

        if (view == View.HOME) {
            int button = renderer.getHomeButtonAt(
                    screenWidth,
                    screenHeight,
                    GdxInputHelper.mouseX(),
                    GdxInputHelper.mouseYBottomUp()
            );
            if (button == GdxMainMenuRenderer.WORLD_EDIT_ACTION && GameConfig.ENABLE_WORLD_EDIT) {
                game.showWorldEdit();
                return;
            }
            switch (button) {
                case 0 -> namePrompt.open("Name your save", "");
                case 1 -> {
                    refreshSaves();
                    view = View.LOAD;
                    loadScrollOffset = 0;
                    statusMessage = saves.isEmpty() ? "No saves found." : "";
                }
                case 2 -> {
                    settingsPanel.cancelListening();
                    view = View.SETTINGS;
                    statusMessage = "";
                }
                case 3 -> Gdx.app.exit();
                default -> {
                }
            }
            return;
        }

        int loadAction = renderer.getLoadActionAt(
                screenWidth,
                screenHeight,
                GdxInputHelper.mouseX(),
                GdxInputHelper.mouseYBottomUp(),
                saves.size(),
                loadScrollOffset
        );
        if (loadAction == GdxMainMenuRenderer.BACK_ACTION) {
            view = View.HOME;
            statusMessage = "";
            return;
        }
        if (loadAction == GdxMainMenuRenderer.SCROLL_UP_ACTION) {
            loadScrollOffset = GdxMainMenuRenderer.clampScroll(loadScrollOffset - 1, saves.size());
            return;
        }
        if (loadAction == GdxMainMenuRenderer.SCROLL_DOWN_ACTION) {
            loadScrollOffset = GdxMainMenuRenderer.clampScroll(loadScrollOffset + 1, saves.size());
            return;
        }
        if (GdxMainMenuRenderer.isDeleteAction(loadAction)) {
            int index = GdxMainMenuRenderer.deleteIndex(loadAction);
            if (index >= 0 && index < saves.size()) {
                promptDelete(saves.get(index));
            }
            return;
        }
        if (loadAction >= 0 && loadAction < saves.size()) {
            SaveSummary summary = saves.get(loadAction);
            if (!summary.isLoadable()) {
                statusMessage = "That save is corrupt.";
                return;
            }
            launchLoadGame(summary.getFileName());
        }
    }

    private void beginNewGameWithTitle(String saveTitle) {
        pendingNewGameTitle = saveTitle;
        if (saveController.existsByTitle(saveTitle)) {
            confirmMode = ConfirmMode.OVERWRITE;
            confirmOverlay.open(
                    "A save named \"" + saveTitle + "\" already exists.\nOverwrite it?"
            );
            return;
        }
        launchNewGame(saveTitle);
        pendingNewGameTitle = "";
    }

    /**
     * Screen switches must not happen mid-{@code render()} — LibGDX treats that as undefined
     * behavior (save could already be written, then the menu frame finishes on a dead GL state).
     */
    private void launchNewGame(String saveTitle) {
        if (transitionQueued) {
            return;
        }
        transitionQueued = true;
        namePrompt.close();
        confirmOverlay.close();
        Gdx.input.setInputProcessor(null);
        final String title = saveTitle;
        Gdx.app.postRunnable(() -> {
            try {
                game.startNewGame(title);
            } catch (Exception exception) {
                transitionQueued = false;
                statusMessage = "Could not create save: " + exception.getMessage();
                Gdx.app.error("MainMenu", "New game failed", exception);
            }
        });
    }

    private void launchLoadGame(String fileName) {
        if (transitionQueued) {
            return;
        }
        transitionQueued = true;
        namePrompt.close();
        confirmOverlay.close();
        Gdx.input.setInputProcessor(null);
        final String saveFile = fileName;
        Gdx.app.postRunnable(() -> {
            try {
                game.startLoadGame(saveFile);
            } catch (Exception exception) {
                transitionQueued = false;
                statusMessage = "Load failed: " + exception.getMessage();
                Gdx.app.error("MainMenu", "Load failed", exception);
            }
        });
    }

    private void promptDelete(SaveSummary save) {
        pendingDelete = save;
        confirmMode = ConfirmMode.DELETE;
        confirmOverlay.open(
                "Delete save \"" + save.getSaveTitle() + "\"?\nThis cannot be undone."
        );
    }

    private void deleteSave(SaveSummary save) {
        try {
            saveController.deleteSave(save.getFileName());
            refreshSaves();
            statusMessage = saves.isEmpty() ? "No saves found." : "Deleted \"" + save.getSaveTitle() + "\".";
        } catch (Exception exception) {
            statusMessage = "Could not delete save: " + exception.getMessage();
        }
    }

    private void refreshSaves() {
        try {
            saves = new ArrayList<>(saveController.listSaves());
        } catch (RuntimeException exception) {
            saves = List.of();
            statusMessage = "Could not list saves.";
        }
        loadScrollOffset = GdxMainMenuRenderer.clampScroll(loadScrollOffset, saves.size());
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void hide() {
        settingsPanel.cancelListening();
        namePrompt.close();
        confirmOverlay.close();
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        settingsPanel.cancelListening();
        namePrompt.close();
        confirmOverlay.close();
        // Do not clear the input processor here — dispose runs on a later frame after the
        // next screen's show() has already installed its processor (e.g. hotbar scroll).
        font.dispose();
        textures.dispose();
    }
}
