package lumberjack.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import lumberjack.core.GameConfig;
import lumberjack.engine.DisplaySettings;
import lumberjack.engine.input.InputSettings;
import lumberjack.engine.input.KeyboardInput;
import lumberjack.game.GameSession;
import lumberjack.item.ItemStack;
import lumberjack.world.map.TileMap;

/**
 * LibGDX gameplay screen: world sim, overlays, fishing minigame, HUD, and saves.
 */
public final class GameplayScreen extends ScreenAdapter {

    private static final float MAX_FRAME_MS = 250f;
    /** Ignore leftover menu clicks/keys after entering gameplay. */
    private static final float INPUT_GRACE_MS = 400f;
    private static final Color TILE_HOVER_OUTLINE = new Color(1f, 0.15f, 0.15f, 0.55f);
    private static final float TILE_HOVER_EDGE = 1f;

    private final LumberjackGame game;
    private final SpriteBatch batch;
    private final GdxSaveController saveController;
    private final GameSession session;
    private final GdxSleepPrompt sleepPrompt;
    private final GdxEatPrompt eatPrompt;
    private final InputSettings inputSettings;
    private final DisplaySettings displaySettings;
    private final GdxTextureCache textures;
    private final GdxWorldRenderer worldRenderer;
    private final GdxEntityRenderer entityRenderer;
    private final GdxHudRenderer hudRenderer;
    private final GdxFishingMinigameRenderer fishingMinigameRenderer;
    private final GdxOverlayController overlayController;
    private final BitmapFont hudFont;
    private final ScreenViewport screenViewport;
    private float inputGraceMsRemaining;
    /** Mouse-wheel notches waiting to change the hotbar selection. */
    private float hotbarScrollAccumulator;
    private final InputAdapter hotbarScrollInput = new InputAdapter() {
        @Override
        public boolean scrolled(float amountX, float amountY) {
            hotbarScrollAccumulator += amountY;
            return true;
        }
    };

    public GameplayScreen(
            LumberjackGame game,
            SpriteBatch batch,
            GdxSaveController saveController,
            String newGameTitle,
            String loadFileName
    ) throws Exception {
        this.game = game;
        this.batch = batch;
        this.saveController = saveController;
        this.inputSettings = InputSettings.load();
        this.displaySettings = DisplaySettings.load();
        this.textures = new GdxTextureCache();
        this.sleepPrompt = new GdxSleepPrompt();
        this.eatPrompt = new GdxEatPrompt();
        this.session = GameSession.createNewGame(sleepPrompt, eatPrompt);
        saveController.attach(session);

        if (loadFileName != null) {
            saveController.loadSave(loadFileName);
        } else {
            String title = newGameTitle != null ? newGameTitle : saveController.nextAvailableTitle("Untitled");
            saveController.createNewGame(title);
        }

        this.session.setWorldSimulationActive(true);
        this.worldRenderer = new GdxWorldRenderer(session.getTileRegistry(), textures);
        this.entityRenderer = new GdxEntityRenderer(textures);
        this.hudRenderer = new GdxHudRenderer();
        this.fishingMinigameRenderer = new GdxFishingMinigameRenderer();
        this.overlayController = new GdxOverlayController(inputSettings);
        this.hudFont = new BitmapFont();
        this.hudFont.getData().setScale(1.35f);
        this.screenViewport = new ScreenViewport();

        session.getFishingManager().setMinigameStartListener(() -> session.getKeyboardInput().clearAll());
        session.getFishingManager().setMinigameEndListener(() -> session.getKeyboardInput().clearAll());

        overlayController.setPauseActions(new GdxOverlayController.PauseActions() {
            @Override
            public void onContinue() {
                // pause flag already cleared by overlay
            }

            @Override
            public void onSave() {
                if (!saveController.hasActiveSave()) {
                    session.getNotificationManager().notifyMessage("No active save file");
                    return;
                }
                saveController.saveNow();
            }

            @Override
            public void onSaveAndExit() {
                if (saveController.hasActiveSave()) {
                    saveController.saveNow();
                }
                session.setWorldSimulationActive(false);
                session.setMenuPaused(false);
                saveController.detach();
                Gdx.app.postRunnable(game::showMainMenu);
            }
        });
    }

    @Override
    public void show() {
        session.getKeyboardInput().clearAll();
        inputGraceMsRemaining = INPUT_GRACE_MS;
        hotbarScrollAccumulator = 0f;
        Gdx.input.setInputProcessor(hotbarScrollInput);
    }

    @Override
    public void render(float delta) {
        // Reinstall if a previous screen's deferred dispose cleared the processor.
        if (Gdx.input.getInputProcessor() == null) {
            Gdx.input.setInputProcessor(hotbarScrollInput);
        }
        float elapsedMs = Math.min(delta * 1000f, MAX_FRAME_MS);
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        boolean minigameActive = session.getFishingManager().hasActiveMinigameSession();
        boolean pauseOpen = overlayController.isPauseMenuOpen();
        boolean sleepPromptOpen = sleepPrompt.isSleepPromptVisible();
        boolean eatPromptOpen = eatPrompt.isEatPromptVisible();
        boolean acceptInput = inputGraceMsRemaining <= 0f;
        if (inputGraceMsRemaining > 0f) {
            inputGraceMsRemaining = Math.max(0f, inputGraceMsRemaining - elapsedMs);
            session.getKeyboardInput().clearAll();
        }

        if (acceptInput && eatPromptOpen) {
            eatPrompt.updateInput(screenWidth, screenHeight);
            session.getEatService().updateOpenPrompt(session);
        } else if (acceptInput && sleepPromptOpen) {
            sleepPrompt.updateInput(screenWidth, screenHeight);
        } else if (acceptInput) {
            overlayController.update(session, displaySettings, screenWidth, screenHeight);
        }

        if (acceptInput && !pauseOpen && !minigameActive && !sleepPromptOpen && !eatPromptOpen) {
            pollHotbarKeys();
            pollHotbarScroll();
            handleZoomKeys();
        } else {
            hotbarScrollAccumulator = 0f;
        }

        if (minigameActive) {
            pollMinigameInput();
            if (acceptInput && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                session.getFishingManager().cancelMinigame(session);
                session.getKeyboardInput().clearAll();
            }
        } else if (acceptInput && !pauseOpen && !sleepPromptOpen && !eatPromptOpen
                && !overlayController.hasGameplayOverlay(session)) {
            pollKeyboard();
            handlePrimaryClick(screenWidth, screenHeight);
        } else {
            session.getKeyboardInput().setMovementPressed(false, false, false, false);
        }

        updateFishing(elapsedMs);
        session.getNotificationManager().update(elapsedMs);
        saveController.tickAutoSave(elapsedMs);

        if (!pauseOpen && !minigameActive && !sleepPromptOpen && !eatPromptOpen
                && session.shouldAdvanceWorldTime()) {
            session.updateTime(elapsedMs);
            session.updateMachines(elapsedMs);
            session.updateMovement(elapsedMs);
            session.updateGameplay(elapsedMs);
        } else if (!pauseOpen && !minigameActive && !sleepPromptOpen && !eatPromptOpen
                && session.isWoodChipperOpen()) {
            // Chipper stays live: clock, machines, enemies, and axe resolution continue.
            session.updateTime(elapsedMs);
            session.updateMachines(elapsedMs);
            session.updateMovement(elapsedMs);
            session.updateGameplay(elapsedMs);
        } else if (sleepPromptOpen || eatPromptOpen) {
            session.updateGameplay(0);
        }

        session.updateCamera(screenWidth, screenHeight, displaySettings.getZoomScale());

        float worldViewWidth = displaySettings.getEffectiveViewportWidth(screenWidth);
        float worldViewHeight = displaySettings.getEffectiveViewportHeight(screenHeight);

        Gdx.gl.glClearColor(0.08f, 0.09f, 0.11f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        worldRenderer.draw(batch, session.getCurrentMap(), session.getCamera(), worldViewWidth, worldViewHeight);
        entityRenderer.draw(
                batch,
                session,
                session.getCamera(),
                worldViewWidth,
                worldViewHeight,
                displaySettings.isDebugEnabled()
        );
        if (!minigameActive && !pauseOpen && !sleepPromptOpen && !eatPromptOpen
                && !overlayController.hasGameplayOverlay(session)) {
            drawHoveredTileOutline(worldViewWidth, worldViewHeight);
        }
        if (!minigameActive) {
            GdxBobberRenderer.draw(batch, textures, session.getFishingManager(), session.getCamera(), worldViewWidth, worldViewHeight);
        }
        batch.end();

        drawHud(screenWidth, screenHeight);
        drawOverlays(screenWidth, screenHeight);

        if (minigameActive) {
            drawFishingMinigame(screenWidth, screenHeight);
        }

        if (pauseOpen) {
            drawPauseMenu(screenWidth, screenHeight);
        }

        if (sleepPrompt.isSleepPromptVisible()) {
            drawSleepPrompt(screenWidth, screenHeight);
        }
        if (eatPrompt.isEatPromptVisible()) {
            drawEatPrompt(screenWidth, screenHeight);
        }
    }

    private void pollKeyboard() {
        KeyboardInput input = session.getKeyboardInput();
        boolean up = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean down = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        input.setMovementPressed(up, down, left, right);
    }

    private void pollMinigameInput() {
        session.getKeyboardInput().setMovementPressed(false, false, false, false);
        var minigame = session.getFishingManager().getActiveMinigame();
        if (minigame != null) {
            minigame.setMoveLeft(Gdx.input.isKeyPressed(Input.Keys.A));
            minigame.setMoveRight(Gdx.input.isKeyPressed(Input.Keys.D));
        }
    }

    private void pollHotbarKeys() {
        if (session.getFishingManager().hasActiveMinigameSession() || overlayController.hasGameplayOverlay(session)) {
            return;
        }
        for (int slot = 0; slot < 9; slot++) {
            if (GdxInputHelper.isActionJustPressed(
                    lumberjack.engine.input.GameAction.forHotbarIndex(slot),
                    inputSettings
            )) {
                session.selectHotbarSlot(slot);
            }
        }
    }

    private void pollHotbarScroll() {
        if (session.getFishingManager().hasActiveMinigameSession() || overlayController.hasGameplayOverlay(session)) {
            hotbarScrollAccumulator = 0f;
            return;
        }
        if (hotbarScrollAccumulator == 0f) {
            return;
        }

        int slots = GameConfig.HOTBAR_SLOTS;
        int selected = session.getInventory().getSelectedHotbarSlot();
        int previous = selected;
        // LibGDX: positive amountY is scroll-down → next hotbar slot.
        // Keep fractional remainder so smooth-scroll devices still advance.
        while (hotbarScrollAccumulator >= 1f) {
            hotbarScrollAccumulator -= 1f;
            selected = (selected + 1) % slots;
        }
        while (hotbarScrollAccumulator <= -1f) {
            hotbarScrollAccumulator += 1f;
            selected = (selected - 1 + slots) % slots;
        }
        if (selected != previous) {
            session.selectHotbarSlot(selected);
        }
    }

    private void drawHoveredTileOutline(float worldViewWidth, float worldViewHeight) {
        TileMap map = session.getCurrentMap();
        int worldX = displaySettings.screenToWorldX(Gdx.input.getX(), session.getCamera());
        int worldY = displaySettings.screenToWorldY(Gdx.input.getY(), session.getCamera());
        int col = map.getColumnAtWorldX(worldX);
        int row = map.getRowAtWorldY(worldY);
        if (!map.isInBounds(col, row)) {
            return;
        }

        GdxWorldRenderer.applyWorldProjection(batch, session.getCamera(), worldViewWidth, worldViewHeight);
        drawTileOutline(col, row);
        ItemStack held = session.getInventory().getSelectedHotbarStack();
        if (!held.isEmpty() && lumberjack.world.placeable.BedPlaceable.isBed(held.getItemId())) {
            int headRow = lumberjack.world.placeable.BedPlaceable.headRow(row);
            if (map.isInBounds(col, headRow)) {
                drawTileOutline(col, headRow);
            }
        }
    }

    private void drawTileOutline(int col, int row) {
        float x = col * GameConfig.TILE_SIZE;
        float y = row * GameConfig.TILE_SIZE;
        float size = GameConfig.TILE_SIZE;
        float edge = TILE_HOVER_EDGE;
        GdxBatchUtils.drawSolid(batch, textures, TILE_HOVER_OUTLINE, x, y, size, edge);
        GdxBatchUtils.drawSolid(batch, textures, TILE_HOVER_OUTLINE, x, y + size - edge, size, edge);
        GdxBatchUtils.drawSolid(batch, textures, TILE_HOVER_OUTLINE, x, y, edge, size);
        GdxBatchUtils.drawSolid(batch, textures, TILE_HOVER_OUTLINE, x + size - edge, y, edge, size);
    }

    private void handlePrimaryClick(int screenWidth, int screenHeight) {
        if (!Gdx.input.justTouched() || !session.shouldAdvanceWorldTime()) {
            return;
        }
        if (overlayController.blocksWorldPrimaryClick(session, screenWidth, screenHeight)) {
            return;
        }

        // Hook bite while holding the cast pose (action lock would otherwise block this).
        if (session.getFishingManager().tryHookBite(session)) {
            return;
        }

        if (session.getPlayer().isActionLocked()) {
            return;
        }

        if (session.getEatService().tryBeginEatOnClick(session)) {
            return;
        }

        int screenX = Gdx.input.getX();
        int screenY = Gdx.input.getY();
        int worldX = displaySettings.screenToWorldX(screenX, session.getCamera());
        int worldY = displaySettings.screenToWorldY(screenY, session.getCamera());

        if (!session.getFishingService().tryCastAtWorldPosition(session, worldX, worldY)
                && !session.getCombatService().tryAttackAtWorldPosition(session, worldX, worldY)
                && !session.getPlaceService().tryPlaceAtWorldPosition(session, worldX, worldY)
                && !session.getPlaceableBreakService().tryBreakAtWorldPosition(session, worldX, worldY)) {
            session.getChopService().tryChopAtWorldPosition(session, worldX, worldY);
        }
    }

    private void handleZoomKeys() {
        if (session.getFishingManager().hasActiveMinigameSession() || overlayController.hasGameplayOverlay(session)) {
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_SUBTRACT)) {
            displaySettings.setZoomPercent(displaySettings.getZoomPercent() - 10);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ADD)) {
            displaySettings.setZoomPercent(displaySettings.getZoomPercent() + 10);
        }
    }

    private void updateFishing(double elapsedMs) {
        if (session.getFishingManager().isBobberIdle()) {
            return;
        }
        if (session.getFishingManager().hasActiveMinigameSession()) {
            if (!session.isMenuPaused()) {
                session.getFishingManager().update(session, elapsedMs);
            }
            return;
        }
        if (session.shouldAdvanceWorldTime()
                || session.isChestOpen()
                || session.isWoodChipperOpen()) {
            session.getFishingManager().update(session, elapsedMs);
        }
    }

    private void drawOverlays(int screenWidth, int screenHeight) {
        if (!overlayController.hasGameplayOverlay(session)) {
            return;
        }

        screenViewport.update(screenWidth, screenHeight, true);
        batch.begin();
        batch.setProjectionMatrix(screenViewport.getCamera().combined);
        batch.setColor(1f, 1f, 1f, 1f);
        overlayController.draw(batch, hudFont, textures, session, screenWidth, screenHeight);
        batch.end();
    }

    private void drawHud(int screenWidth, int screenHeight) {
        screenViewport.update(screenWidth, screenHeight, true);
        batch.begin();
        batch.setProjectionMatrix(screenViewport.getCamera().combined);
        batch.setColor(1f, 1f, 1f, 1f);
        hudRenderer.draw(batch, hudFont, textures, session, inputSettings, screenWidth, screenHeight);
        batch.end();
    }

    private void drawFishingMinigame(int screenWidth, int screenHeight) {
        screenViewport.update(screenWidth, screenHeight, true);
        batch.begin();
        batch.setProjectionMatrix(screenViewport.getCamera().combined);
        batch.setColor(1f, 1f, 1f, 1f);
        fishingMinigameRenderer.draw(batch, hudFont, textures, session, screenWidth, screenHeight);
        batch.end();
    }

    private void drawPauseMenu(int screenWidth, int screenHeight) {
        screenViewport.update(screenWidth, screenHeight, true);
        batch.begin();
        batch.setProjectionMatrix(screenViewport.getCamera().combined);
        batch.setColor(1f, 1f, 1f, 1f);
        overlayController.drawPauseMenu(batch, hudFont, textures, displaySettings, screenWidth, screenHeight);
        batch.end();
    }

    private void drawSleepPrompt(int screenWidth, int screenHeight) {
        screenViewport.update(screenWidth, screenHeight, true);
        batch.begin();
        batch.setProjectionMatrix(screenViewport.getCamera().combined);
        batch.setColor(1f, 1f, 1f, 1f);
        sleepPrompt.draw(batch, hudFont, textures, screenWidth, screenHeight);
        batch.end();
    }

    private void drawEatPrompt(int screenWidth, int screenHeight) {
        screenViewport.update(screenWidth, screenHeight, true);
        batch.begin();
        batch.setProjectionMatrix(screenViewport.getCamera().combined);
        batch.setColor(1f, 1f, 1f, 1f);
        eatPrompt.draw(batch, hudFont, textures, screenWidth, screenHeight);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        screenViewport.update(width, height, true);
        session.updateCamera(width, height, displaySettings.getZoomScale());
    }

    @Override
    public void hide() {
        session.setWorldSimulationActive(false);
    }

    @Override
    public void dispose() {
        hudFont.dispose();
        textures.dispose();
        overlayController.dispose();
    }
}
