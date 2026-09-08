package lumberjack.gdx;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import lumberjack.core.GameConfig;
import lumberjack.world.map.EditableTileMap;
import lumberjack.world.map.MapFileIo;
import lumberjack.world.placeable.BedPlaceable;
import lumberjack.world.tile.OverlayTiles;
import lumberjack.world.tile.TallTreeSprites;
import lumberjack.world.tile.TileDefinition;
import lumberjack.world.tile.TileRegistry;
import lumberjack.world.tile.WaterTileSprites;

/**
 * Built-in tile map editor. Saves to {@code resources/maps/{id}.map}.
 * Removable later — not part of the shipped game loop.
 */
public final class WorldEditScreen extends ScreenAdapter {

    private static final Color BG = new Color(0.1f, 0.11f, 0.13f, 1f);
    private static final Color PANEL = new Color(0.16f, 0.17f, 0.2f, 1f);
    private static final Color BUTTON = new Color(0.28f, 0.3f, 0.36f, 1f);
    private static final Color BUTTON_HOVER = new Color(0.4f, 0.42f, 0.5f, 1f);
    private static final Color BUTTON_ACTIVE = new Color(0.45f, 0.55f, 0.35f, 1f);
    private static final Color TEXT = new Color(0.95f, 0.95f, 0.95f, 1f);
    private static final Color GRID = new Color(1f, 1f, 1f, 0.22f);
    private static final Color SELECT = new Color(1f, 0.9f, 0.2f, 0.9f);
    private static final Color STATUS = new Color(1f, 0.85f, 0.45f, 1f);

    private static final int TOP_BAR = 64;
    private static final int BOTTOM_BAR = 130;
    private static final int SIDE_PAD = 56;
    private static final int BASE_TILE_DRAW = 32;
    private static final int PALETTE_SLOT = 52;
    private static final int PALETTE_GAP = 8;
    private static final int MAX_UNDO = 40;
    private static final int ERASE_TILE = 0;

    private final LumberjackGame game;
    private final SpriteBatch batch;
    private final GdxTextureCache textures;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private final ScreenViewport viewport;
    private final TileRegistry tileRegistry;
    private final List<TileDefinition> paletteTiles;
    private final Deque<EditSnapshot> undoStack = new ArrayDeque<>();
    private final GdxTextInputOverlay namePrompt = new GdxTextInputOverlay();

    private EditableTileMap map;
    private int selectedTileId;
    /** Paint rotation in clockwise 90° steps (0–3). */
    private int paintRotation;
    private int paletteScroll;
    private float cameraX;
    private float cameraY;
    private float zoom = 1f;
    private boolean painting;
    private boolean erasing;
    private boolean strokeActive;
    private boolean dragPanning;
    private int dragLastMouseX;
    private int dragLastMouseY;
    private boolean loadPanelOpen;
    private List<String> loadIds = List.of();
    private int loadScroll;
    private String statusMessage = "LMB paint · Ctrl+LMB drag pan · RMB erase · Q/E rotate · Scroll zoom · WASD pan · Z undo";
    private NamePromptMode namePromptMode = NamePromptMode.NONE;
    private float scrollAccumulator;

    private static final class EditSnapshot {
        private final int[][] tiles;
        private final int[][] rotations;
        private final boolean[][] transitionMarkers;

        private EditSnapshot(int[][] tiles, int[][] rotations, boolean[][] transitionMarkers) {
            this.tiles = tiles;
            this.rotations = rotations;
            this.transitionMarkers = transitionMarkers;
        }
    }

    private enum NamePromptMode {
        NONE,
        SAVE_AS,
        NEW_MAP
    }

    public WorldEditScreen(LumberjackGame game, SpriteBatch batch) {
        this.game = game;
        this.batch = batch;
        this.textures = new GdxTextureCache();
        this.font = new BitmapFont();
        this.font.getData().setScale(1.2f);
        this.viewport = new ScreenViewport();
        this.tileRegistry = new TileRegistry();
        this.paletteTiles = tileRegistry.getAllSorted();
        this.selectedTileId = paletteTiles.isEmpty() ? 0 : paletteTiles.get(0).getId();
        this.map = EditableTileMap.createDefault("untitled");
        centerCamera();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                scrollAccumulator += amountY;
                return true;
            }
        });
        namePrompt.close();
        loadPanelOpen = false;
        namePromptMode = NamePromptMode.NONE;
    }

    @Override
    public void render(float delta) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        handleUi(delta, screenWidth, screenHeight);

        Gdx.gl.glClearColor(BG.r, BG.g, BG.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.update(screenWidth, screenHeight, true);
        batch.begin();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.setColor(1f, 1f, 1f, 1f);

        drawMap(screenWidth, screenHeight);
        drawExtendButtons(screenWidth, screenHeight);
        drawTopBar(screenWidth, screenHeight);
        drawPalette(screenWidth, screenHeight);
        drawStatus(screenWidth);

        if (loadPanelOpen) {
            drawLoadPanel(screenWidth, screenHeight);
        }
        namePrompt.draw(batch, font, textures, screenWidth, screenHeight);

        batch.end();
    }

    private void handleUi(float delta, int screenWidth, int screenHeight) {
        if (namePrompt.isVisible()) {
            namePrompt.updateInput(screenWidth, screenHeight);
            String result = namePrompt.pollResult();
            if (result != null) {
                handleNamePromptResult(result);
            }
            return;
        }

        if (loadPanelOpen) {
            handleLoadPanelInput(screenWidth, screenHeight);
            return;
        }

        handleHotkeys();
        handlePanZoom(delta);
        handleToolbarClicks(screenWidth, screenHeight);
        handleExtendClicks(screenWidth, screenHeight);
        handlePaletteClicks(screenWidth, screenHeight);
        handleDragPan(screenWidth, screenHeight);
        handleMapPaint(screenWidth, screenHeight);
    }

    private void handleDragPan(int screenWidth, int screenHeight) {
        boolean ctrl = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        boolean left = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        int mouseX = GdxInputHelper.mouseX();
        int mouseY = GdxInputHelper.mouseYBottomUp();

        if (!ctrl || !left) {
            dragPanning = false;
            return;
        }

        if (!dragPanning) {
            // Start a drag even if the press began over the map area.
            if (!pointInMapArea(mouseX, mouseY, screenWidth, screenHeight)
                    && !GdxInputHelper.isPrimaryMouseJustPressed()) {
                return;
            }
            dragPanning = true;
            dragLastMouseX = mouseX;
            dragLastMouseY = mouseY;
            painting = false;
            erasing = false;
            strokeActive = false;
            return;
        }

        int tileDraw = Math.max(8, Math.round(BASE_TILE_DRAW * zoom));
        float scale = tileDraw / (float) GameConfig.TILE_SIZE;
        float dx = mouseX - dragLastMouseX;
        float dy = mouseY - dragLastMouseY;
        // Grab-drag: content follows the cursor.
        cameraX -= dx / scale;
        cameraY += dy / scale;
        dragLastMouseX = mouseX;
        dragLastMouseY = mouseY;
    }

    private void handleHotkeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.postRunnable(game::showMainMenu);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            undo();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            rotatePaint(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            rotatePaint(1);
        }
    }

    private void rotatePaint(int clockwiseSteps) {
        paintRotation = (paintRotation + clockwiseSteps) & 3;
        statusMessage = "Paint rotation: " + (paintRotation * 90) + "°";
    }

    private void handlePanZoom(float delta) {
        float panSpeed = 280f * delta / Math.max(0.35f, zoom);
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            cameraX -= panSpeed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            cameraX += panSpeed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            cameraY -= panSpeed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            cameraY += panSpeed;
        }

        if (scrollAccumulator != 0f) {
            float oldZoom = zoom;
            zoom = Math.max(0.4f, Math.min(3.5f, zoom - scrollAccumulator * 0.12f));
            int mouseX = GdxInputHelper.mouseX();
            int mouseY = GdxInputHelper.mouseYBottomUp();
            float[] worldBefore = screenToWorld(mouseX, mouseY, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), oldZoom);
            float[] worldAfter = screenToWorld(mouseX, mouseY, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), zoom);
            cameraX += worldBefore[0] - worldAfter[0];
            cameraY += worldBefore[1] - worldAfter[1];
            scrollAccumulator = 0f;
        }
    }

    private void handleToolbarClicks(int screenWidth, int screenHeight) {
        if (!GdxInputHelper.isPrimaryMouseJustPressed()) {
            return;
        }
        int mouseX = GdxInputHelper.mouseX();
        int mouseY = GdxInputHelper.mouseYBottomUp();
        int y = screenHeight - 52;
        int x = 16;
        int btnW = 90;
        int gap = 10;

        if (hit(mouseX, mouseY, x, y, btnW, 36)) {
            namePromptMode = NamePromptMode.NEW_MAP;
            namePrompt.open("New map id", "map_" + (MapFileIo.listMapIds().size() + 1));
            return;
        }
        x += btnW + gap;
        if (hit(mouseX, mouseY, x, y, btnW, 36)) {
            openLoadPanel();
            return;
        }
        x += btnW + gap;
        if (hit(mouseX, mouseY, x, y, btnW, 36)) {
            beginSave();
            return;
        }
        x += btnW + gap;
        if (hit(mouseX, mouseY, x, y, btnW, 36)) {
            undo();
            return;
        }
        x += btnW + gap;
        int rotW = 70;
        if (hit(mouseX, mouseY, x, y, rotW, 36)) {
            rotatePaint(-1);
            return;
        }
        x += rotW + gap;
        if (hit(mouseX, mouseY, x, y, rotW, 36)) {
            rotatePaint(1);
            return;
        }
        x += rotW + gap;
        if (hit(mouseX, mouseY, x, y, btnW, 36)) {
            Gdx.app.postRunnable(game::showMainMenu);
        }
    }

    private void handleExtendClicks(int screenWidth, int screenHeight) {
        if (!GdxInputHelper.isPrimaryMouseJustPressed()) {
            return;
        }
        int mouseX = GdxInputHelper.mouseX();
        int mouseY = GdxInputHelper.mouseYBottomUp();
        if (hit(mouseX, mouseY, edgeButtonBounds("up", true, screenWidth, screenHeight))) {
            pushUndo();
            map.extendUp(ERASE_TILE);
            statusMessage = "Extended up (" + map.getWidth() + "x" + map.getHeight() + ")";
        } else if (hit(mouseX, mouseY, edgeButtonBounds("down", true, screenWidth, screenHeight))) {
            pushUndo();
            map.extendDown(ERASE_TILE);
            statusMessage = "Extended down (" + map.getWidth() + "x" + map.getHeight() + ")";
        } else if (hit(mouseX, mouseY, edgeButtonBounds("left", true, screenWidth, screenHeight))) {
            pushUndo();
            map.extendLeft(ERASE_TILE);
            cameraX += GameConfig.TILE_SIZE;
            statusMessage = "Extended left (" + map.getWidth() + "x" + map.getHeight() + ")";
        } else if (hit(mouseX, mouseY, edgeButtonBounds("right", true, screenWidth, screenHeight))) {
            pushUndo();
            map.extendRight(ERASE_TILE);
            statusMessage = "Extended right (" + map.getWidth() + "x" + map.getHeight() + ")";
        } else if (hit(mouseX, mouseY, edgeButtonBounds("up", false, screenWidth, screenHeight))) {
            pushUndo();
            if (map.shrinkUp()) {
                cameraY -= GameConfig.TILE_SIZE;
                statusMessage = "Removed top row (" + map.getWidth() + "x" + map.getHeight() + ")";
            } else {
                undoStack.pop();
                statusMessage = "Map already at minimum height";
            }
        } else if (hit(mouseX, mouseY, edgeButtonBounds("down", false, screenWidth, screenHeight))) {
            pushUndo();
            if (map.shrinkDown()) {
                statusMessage = "Removed bottom row (" + map.getWidth() + "x" + map.getHeight() + ")";
            } else {
                undoStack.pop();
                statusMessage = "Map already at minimum height";
            }
        } else if (hit(mouseX, mouseY, edgeButtonBounds("left", false, screenWidth, screenHeight))) {
            pushUndo();
            if (map.shrinkLeft()) {
                cameraX -= GameConfig.TILE_SIZE;
                statusMessage = "Removed left column (" + map.getWidth() + "x" + map.getHeight() + ")";
            } else {
                undoStack.pop();
                statusMessage = "Map already at minimum width";
            }
        } else if (hit(mouseX, mouseY, edgeButtonBounds("right", false, screenWidth, screenHeight))) {
            pushUndo();
            if (map.shrinkRight()) {
                statusMessage = "Removed right column (" + map.getWidth() + "x" + map.getHeight() + ")";
            } else {
                undoStack.pop();
                statusMessage = "Map already at minimum width";
            }
        }
    }

    private void handlePaletteClicks(int screenWidth, int screenHeight) {
        int mouseX = GdxInputHelper.mouseX();
        int mouseY = GdxInputHelper.mouseYBottomUp();
        int paletteBottom = 24;
        int arrowY = paletteBottom + 30;
        if (GdxInputHelper.isPrimaryMouseJustPressed()) {
            if (hit(mouseX, mouseY, 16, arrowY, 40, 40)) {
                paletteScroll = Math.max(0, paletteScroll - 1);
                return;
            }
            if (hit(mouseX, mouseY, screenWidth - 56, arrowY, 40, 40)) {
                paletteScroll = Math.min(Math.max(0, paletteTiles.size() - 1), paletteScroll + 1);
                return;
            }
        }

        int startX = 70;
        int visible = Math.max(1, (screenWidth - 140) / (PALETTE_SLOT + PALETTE_GAP));
        for (int i = 0; i < visible; i++) {
            int index = paletteScroll + i;
            if (index >= paletteTiles.size()) {
                break;
            }
            int slotX = startX + i * (PALETTE_SLOT + PALETTE_GAP);
            if (GdxInputHelper.isPrimaryMouseJustPressed()
                    && hit(mouseX, mouseY, slotX, paletteBottom + 16, PALETTE_SLOT, PALETTE_SLOT)) {
                selectedTileId = paletteTiles.get(index).getId();
                if (selectedTileId == GameConfig.TRANSITION_MARKER_TILE_ID) {
                    statusMessage = "Transfer marker (T): LMB place on tile · RMB clear · invisible in-game";
                } else {
                    statusMessage = "Selected: " + paletteTiles.get(index).getName() + " (" + selectedTileId + ")";
                }
            }
        }
    }

    private void handleMapPaint(int screenWidth, int screenHeight) {
        boolean ctrl = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        boolean left = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean right = Gdx.input.isButtonPressed(Input.Buttons.RIGHT);
        int mouseX = GdxInputHelper.mouseX();
        int mouseY = GdxInputHelper.mouseYBottomUp();

        // Ctrl+LMB is reserved for drag-panning the viewport.
        if (ctrl || dragPanning) {
            painting = false;
            erasing = false;
            strokeActive = false;
            return;
        }

        if (!pointInMapArea(mouseX, mouseY, screenWidth, screenHeight)) {
            painting = false;
            erasing = false;
            strokeActive = false;
            return;
        }

        // Don't paint when clicking UI chrome edges (extend buttons)
        if (overExtendButton(mouseX, mouseY, screenWidth, screenHeight)) {
            return;
        }

        if (left || right) {
            if (!strokeActive) {
                pushUndo();
                strokeActive = true;
            }
            painting = left;
            erasing = right;
            float[] world = screenToWorld(mouseX, mouseY, screenWidth, screenHeight, zoom);
            int col = (int) Math.floor(world[0] / GameConfig.TILE_SIZE);
            int row = (int) Math.floor(world[1] / GameConfig.TILE_SIZE);
            if (map.inBounds(col, row)) {
                if (selectedTileId == GameConfig.TRANSITION_MARKER_TILE_ID) {
                    // Overlay tool: toggle / clear transfer marker without changing terrain.
                    if (erasing) {
                        map.setTransitionMarker(col, row, false);
                    } else {
                        map.setTransitionMarker(col, row, true);
                    }
                } else if (erasing) {
                    map.setTile(col, row, ERASE_TILE, 0);
                    map.setTransitionMarker(col, row, false);
                } else {
                    // Water auto-tiler owns orientation — never store paint rotation on water.
                    int rotation = selectedTileId == GameConfig.WATER_TILE_ID ? 0 : paintRotation;
                    map.setTile(col, row, selectedTileId, rotation);
                    // Bed tile is 2 tall: brush = foot, also paint the head above.
                    if (BedPlaceable.isBedTile(selectedTileId)) {
                        int headRow = BedPlaceable.headRow(row);
                        if (map.inBounds(col, headRow)) {
                            map.setTile(col, headRow, selectedTileId, rotation);
                        }
                    }
                }
            }
        } else {
            painting = false;
            erasing = false;
            strokeActive = false;
        }
    }

    private void handleLoadPanelInput(int screenWidth, int screenHeight) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            loadPanelOpen = false;
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            loadScroll = Math.max(0, loadScroll - 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            loadScroll = Math.min(Math.max(0, loadIds.size() - 1), loadScroll + 1);
        }
        if (!GdxInputHelper.isPrimaryMouseJustPressed()) {
            return;
        }
        int mouseX = GdxInputHelper.mouseX();
        int mouseY = GdxInputHelper.mouseYBottomUp();
        int panelW = 420;
        int panelH = 420;
        int left = screenWidth / 2 - panelW / 2;
        int bottom = screenHeight / 2 - panelH / 2;
        if (!hit(mouseX, mouseY, left, bottom, panelW, panelH)) {
            loadPanelOpen = false;
            return;
        }
        if (hit(mouseX, mouseY, left + panelW - 110, bottom + 16, 90, 36)) {
            loadPanelOpen = false;
            return;
        }
        int listTop = bottom + panelH - 70;
        for (int i = 0; i < 8; i++) {
            int index = loadScroll + i;
            if (index >= loadIds.size()) {
                break;
            }
            int rowBottom = listTop - (i + 1) * 40;
            if (hit(mouseX, mouseY, left + 20, rowBottom, panelW - 40, 34)) {
                loadMap(loadIds.get(index));
                loadPanelOpen = false;
                return;
            }
        }
    }

    private void openLoadPanel() {
        loadIds = MapFileIo.listMapIds();
        loadScroll = 0;
        loadPanelOpen = true;
        if (loadIds.isEmpty()) {
            statusMessage = "No maps found in resources/maps/";
            loadPanelOpen = false;
        }
    }

    private void beginSave() {
        if (MapFileIo.isValidMapId(map.getMapId()) && !"untitled".equals(map.getMapId())) {
            saveCurrent();
            return;
        }
        namePromptMode = NamePromptMode.SAVE_AS;
        namePrompt.open("Save map as id", map.getMapId());
    }

    private void handleNamePromptResult(String result) {
        NamePromptMode mode = namePromptMode;
        namePromptMode = NamePromptMode.NONE;
        if (result.isEmpty()) {
            return;
        }
        String id = MapFileIo.normalizeMapId(result);
        if (!MapFileIo.isValidMapId(id)) {
            statusMessage = "Invalid map id. Use letters/numbers/underscore, start with a letter.";
            return;
        }
        if (mode == NamePromptMode.NEW_MAP) {
            pushUndo();
            map = EditableTileMap.createDefault(id);
            undoStack.clear();
            centerCamera();
            statusMessage = "New map: " + id;
            return;
        }
        if (mode == NamePromptMode.SAVE_AS) {
            map.setMapId(id);
            saveCurrent();
        }
    }

    private void saveCurrent() {
        try {
            MapFileIo.save(map);
            statusMessage = "Saved resources/maps/" + map.getMapId() + ".map ("
                    + map.getWidth() + "x" + map.getHeight() + ")";
        } catch (Exception exception) {
            statusMessage = "Save failed: " + exception.getMessage();
        }
    }

    private void loadMap(String mapId) {
        try {
            map = MapFileIo.load(mapId);
            undoStack.clear();
            centerCamera();
            statusMessage = "Loaded " + mapId + " (" + map.getWidth() + "x" + map.getHeight() + ")";
        } catch (Exception exception) {
            statusMessage = "Load failed: " + exception.getMessage();
        }
    }

    private void pushUndo() {
        undoStack.push(new EditSnapshot(
                map.copyTiles(),
                map.copyRotations(),
                map.copyTransitionMarkers()
        ));
        while (undoStack.size() > MAX_UNDO) {
            undoStack.removeLast();
        }
    }

    private void undo() {
        if (undoStack.isEmpty()) {
            statusMessage = "Nothing to undo";
            return;
        }
        EditSnapshot snapshot = undoStack.pop();
        map.restore(snapshot.tiles, snapshot.rotations, snapshot.transitionMarkers);
        statusMessage = "Undo (" + undoStack.size() + " left)";
    }

    private void centerCamera() {
        cameraX = map.getWidth() * GameConfig.TILE_SIZE / 2f;
        cameraY = map.getHeight() * GameConfig.TILE_SIZE / 2f;
        zoom = 1f;
    }

    private void drawMap(int screenWidth, int screenHeight) {
        int tileDraw = Math.max(8, Math.round(BASE_TILE_DRAW * zoom));
        float worldTile = GameConfig.TILE_SIZE;
        float scale = tileDraw / worldTile;

        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                // mouseYBottomUp: 0 at bottom. world Y grows downward.
                float drawBottom = screenHeight / 2f - ((row + 1) * worldTile - cameraY) * scale;
                float drawLeft = (col * worldTile - cameraX) * scale + screenWidth / 2f;

                if (drawLeft + tileDraw < SIDE_PAD || drawLeft > screenWidth - SIDE_PAD
                        || drawBottom + tileDraw < BOTTOM_BAR || drawBottom > screenHeight - TOP_BAR) {
                    continue;
                }

                int tileId = map.getTile(col, row);
                if (BedPlaceable.isBedTile(tileId)) {
                    drawBedEditorTile(col, row, drawLeft, drawBottom, tileDraw);
                } else {
                    drawTile(tileId, map.getRotation(col, row), drawLeft, drawBottom, tileDraw);
                }
                if (map.hasTransitionMarker(col, row)) {
                    drawTransitionMarkerOverlay(drawLeft, drawBottom, tileDraw);
                }
                GdxBatchUtils.drawSolid(batch, textures, GRID, drawLeft, drawBottom, tileDraw, 1);
                GdxBatchUtils.drawSolid(batch, textures, GRID, drawLeft, drawBottom, 1, tileDraw);
            }
        }
    }

    private void drawBedEditorTile(int col, int row, float drawLeft, float drawBottom, int tileDraw) {
        drawTile(GameConfig.CABIN_FLOOR_TILE_ID, 0, drawLeft, drawBottom, tileDraw);
        // Head of a stacked pair — foot cell draws the full 2-tile sprite.
        int footRow = row + 1;
        if (map.inBounds(col, footRow) && BedPlaceable.isBedTile(map.getTile(col, footRow))) {
            return;
        }
        Texture bed = textures.getOptional("tiles/sprites/bed.png");
        if (bed == null) {
            drawTile(BedPlaceable.TILE_ID, 0, drawLeft, drawBottom, tileDraw);
            return;
        }
        GdxBatchUtils.drawScreenTexture(
                batch,
                bed,
                drawLeft,
                drawBottom,
                tileDraw,
                tileDraw * BedPlaceable.HEIGHT_TILES
        );
    }

    private void drawTransitionMarkerOverlay(float drawLeft, float drawBottom, int tileDraw) {
        Texture marker = textures.getOptional("tiles/sprites/transition_marker.png");
        if (marker != null) {
            GdxBatchUtils.drawScreenTexture(batch, marker, drawLeft, drawBottom, tileDraw, tileDraw);
            return;
        }
        GdxBatchUtils.drawSolid(
                batch,
                textures,
                new Color(1f, 0.85f, 0.15f, 0.55f),
                drawLeft + tileDraw * 0.2f,
                drawBottom + tileDraw * 0.2f,
                tileDraw * 0.6f,
                tileDraw * 0.6f
        );
    }

    private void drawTile(int tileId, float x, float y, float size) {
        drawTile(tileId, 0, x, y, size);
    }

    private void drawTile(int tileId, int rotation, float x, float y, float size) {
        TileDefinition tile;
        try {
            tile = tileRegistry.get(tileId);
        } catch (RuntimeException exception) {
            GdxBatchUtils.drawSolid(batch, textures, Color.MAGENTA, x, y, size, size);
            return;
        }
        if (WaterTileSprites.isBridgeTileId(tile.getId())) {
            Texture water = textures.getOptional("tiles/sprites/water.png");
            if (water != null) {
                GdxBatchUtils.drawScreenTexture(batch, water, x, y, size, size);
            } else {
                GdxBatchUtils.drawSolid(
                        batch,
                        textures,
                        GdxBatchUtils.toGdx(tileRegistry.get(GameConfig.WATER_TILE_ID).getDebugColor()),
                        x,
                        y,
                        size,
                        size
                );
            }
            Texture bridge = textures.getOptional("tiles/sprites/" + tile.getName() + ".png");
            if (bridge != null) {
                GdxBatchUtils.drawScreenTexture(batch, bridge, x, y, size, size, rotation);
            }
            return;
        }

        if (OverlayTiles.isGrassOverlay(tile.getId())) {
            Texture grass = textures.getOptional("tiles/sprites/grass.png");
            if (grass != null) {
                GdxBatchUtils.drawScreenTexture(batch, grass, x, y, size, size);
            } else {
                GdxBatchUtils.drawSolid(
                        batch,
                        textures,
                        GdxBatchUtils.toGdx(tileRegistry.get(GameConfig.GRASS_TILE_ID).getDebugColor()),
                        x,
                        y,
                        size,
                        size
                );
            }
            if (TallTreeSprites.isTallTree(tile.getId())) {
                Texture tree = textures.getOptional(TallTreeSprites.spritePath(tile.getId()));
                if (tree != null) {
                    float scale = size / GameConfig.TILE_SIZE;
                    float tw = TallTreeSprites.spriteWidth(tile.getId()) * scale;
                    float th = TallTreeSprites.spriteHeight(tile.getId()) * scale;
                    float tx = x + (size - tw) / 2f;
                    float ty = y + size - th;
                    GdxBatchUtils.drawScreenTexture(batch, tree, tx, ty, tw, th);
                }
            } else {
                Texture overlay = textures.getOptional("tiles/sprites/" + tile.getName() + ".png");
                if (overlay != null) {
                    GdxBatchUtils.drawScreenTexture(batch, overlay, x, y, size, size, rotation);
                }
            }
            return;
        }

        Texture sprite = textures.getOptional("tiles/sprites/" + tile.getName() + ".png");
        if ("water".equals(tile.getName())) {
            Texture water = textures.getOptional("tiles/sprites/water.png");
            if (water != null) {
                sprite = water;
            }
        }
        if (sprite != null) {
            GdxBatchUtils.drawScreenTexture(batch, sprite, x, y, size, size, rotation);
        } else {
            GdxBatchUtils.drawSolid(batch, textures, GdxBatchUtils.toGdx(tile.getDebugColor()), x, y, size, size);
        }
    }

    private void drawExtendButtons(int screenWidth, int screenHeight) {
        drawButton(edgeButtonBounds("up", true, screenWidth, screenHeight), "+Up", false);
        drawButton(edgeButtonBounds("up", false, screenWidth, screenHeight), "-Up", false);
        drawButton(edgeButtonBounds("down", true, screenWidth, screenHeight), "+Down", false);
        drawButton(edgeButtonBounds("down", false, screenWidth, screenHeight), "-Down", false);
        drawButton(edgeButtonBounds("left", true, screenWidth, screenHeight), "+L", false);
        drawButton(edgeButtonBounds("left", false, screenWidth, screenHeight), "-L", false);
        drawButton(edgeButtonBounds("right", true, screenWidth, screenHeight), "+R", false);
        drawButton(edgeButtonBounds("right", false, screenWidth, screenHeight), "-R", false);
    }

    /**
     * Edge resize controls. {@code grow=true} adds a row/col; {@code grow=false} removes one.
     */
    private int[] edgeButtonBounds(String dir, boolean grow, int screenWidth, int screenHeight) {
        int cx = screenWidth / 2;
        int cy = (screenHeight - TOP_BAR + BOTTOM_BAR) / 2;
        int w = 70;
        int h = 32;
        int gap = 6;
        return switch (dir) {
            case "up" -> grow
                    ? new int[] {cx - w - gap / 2, screenHeight - TOP_BAR - 44, w, h}
                    : new int[] {cx + gap / 2, screenHeight - TOP_BAR - 44, w, h};
            case "down" -> grow
                    ? new int[] {cx - w - gap / 2, BOTTOM_BAR + 8, w, h}
                    : new int[] {cx + gap / 2, BOTTOM_BAR + 8, w, h};
            case "left" -> grow
                    ? new int[] {8, cy - h - gap / 2, 44, h}
                    : new int[] {8, cy + gap / 2, 44, h};
            case "right" -> grow
                    ? new int[] {screenWidth - 52, cy - h - gap / 2, 44, h}
                    : new int[] {screenWidth - 52, cy + gap / 2, 44, h};
            default -> new int[] {0, 0, 0, 0};
        };
    }

    private void drawTopBar(int screenWidth, int screenHeight) {
        GdxBatchUtils.drawSolid(batch, textures, PANEL, 0, screenHeight - TOP_BAR, screenWidth, TOP_BAR);
        int y = screenHeight - 52;
        int x = 16;
        int btnW = 90;
        int rotW = 70;
        int gap = 10;
        drawButton(x, y, btnW, 36, "New", false);
        x += btnW + gap;
        drawButton(x, y, btnW, 36, "Load", false);
        x += btnW + gap;
        drawButton(x, y, btnW, 36, "Save", false);
        x += btnW + gap;
        drawButton(x, y, btnW, 36, "Undo", false);
        x += btnW + gap;
        drawButton(x, y, rotW, 36, "RotL", paintRotation != 0);
        x += rotW + gap;
        drawButton(x, y, rotW, 36, "RotR", paintRotation != 0);
        x += rotW + gap;
        drawButton(x, y, btnW, 36, "Back", false);

        font.setColor(TEXT);
        String info = "Map: " + map.getMapId() + "   " + map.getWidth() + "x" + map.getHeight()
                + "   rot " + (paintRotation * 90) + "°"
                + "   zoom " + String.format("%.0f%%", zoom * 100f);
        layout.setText(font, info);
        font.draw(batch, info, screenWidth - layout.width - 20, screenHeight - 28);
    }

    private void drawPalette(int screenWidth, int screenHeight) {
        GdxBatchUtils.drawSolid(batch, textures, PANEL, 0, 0, screenWidth, BOTTOM_BAR);
        drawButton(16, 54, 40, 40, "<", false);
        drawButton(screenWidth - 56, 54, 40, 40, ">", false);

        int startX = 70;
        int visible = Math.max(1, (screenWidth - 140) / (PALETTE_SLOT + PALETTE_GAP));
        for (int i = 0; i < visible; i++) {
            int index = paletteScroll + i;
            if (index >= paletteTiles.size()) {
                break;
            }
            TileDefinition tile = paletteTiles.get(index);
            int slotX = startX + i * (PALETTE_SLOT + PALETTE_GAP);
            int slotY = 40;
            GdxBatchUtils.drawSolid(batch, textures, BUTTON, slotX, slotY, PALETTE_SLOT, PALETTE_SLOT);
            int previewRot = tile.getId() == selectedTileId ? paintRotation : 0;
            drawTile(tile.getId(), previewRot, slotX + 6, slotY + 6, PALETTE_SLOT - 12);
            if (tile.getId() == selectedTileId) {
                GdxBatchUtils.drawSolid(batch, textures, SELECT, slotX, slotY, PALETTE_SLOT, 3);
                GdxBatchUtils.drawSolid(batch, textures, SELECT, slotX, slotY + PALETTE_SLOT - 3, PALETTE_SLOT, 3);
            }
            font.setColor(TEXT);
            font.getData().setScale(0.9f);
            layout.setText(font, tile.getName());
            font.draw(batch, tile.getName(), slotX + (PALETTE_SLOT - layout.width) / 2f, slotY - 4);
            font.getData().setScale(1.2f);
        }
    }

    private void drawLoadPanel(int screenWidth, int screenHeight) {
        GdxBatchUtils.drawSolid(batch, textures, new Color(0f, 0f, 0f, 0.55f), 0, 0, screenWidth, screenHeight);
        int panelW = 420;
        int panelH = 420;
        int left = screenWidth / 2 - panelW / 2;
        int bottom = screenHeight / 2 - panelH / 2;
        GdxBatchUtils.drawSolid(batch, textures, PANEL, left, bottom, panelW, panelH);
        font.setColor(TEXT);
        font.getData().setScale(1.5f);
        layout.setText(font, "Load Map");
        font.draw(batch, "Load Map", left + (panelW - layout.width) / 2f, bottom + panelH - 28);
        font.getData().setScale(1.2f);
        drawButton(left + panelW - 110, bottom + 16, 90, 36, "Close", false);

        int listTop = bottom + panelH - 70;
        for (int i = 0; i < 8; i++) {
            int index = loadScroll + i;
            if (index >= loadIds.size()) {
                break;
            }
            int rowBottom = listTop - (i + 1) * 40;
            drawButton(left + 20, rowBottom, panelW - 40, 34, loadIds.get(index), false);
        }
    }

    private void drawStatus(int screenWidth) {
        if (statusMessage == null || statusMessage.isBlank()) {
            return;
        }
        font.setColor(STATUS);
        font.getData().setScale(1.0f);
        layout.setText(font, statusMessage);
        font.draw(batch, statusMessage, 16, BOTTOM_BAR + 18);
        font.getData().setScale(1.2f);
    }

    private void drawButton(int[] bounds, String label, boolean active) {
        drawButton(bounds[0], bounds[1], bounds[2], bounds[3], label, active);
    }

    private void drawButton(int x, int y, int w, int h, String label, boolean active) {
        int mouseX = GdxInputHelper.mouseX();
        int mouseY = GdxInputHelper.mouseYBottomUp();
        boolean hovered = hit(mouseX, mouseY, x, y, w, h);
        Color color = active ? BUTTON_ACTIVE : (hovered ? BUTTON_HOVER : BUTTON);
        GdxBatchUtils.drawSolid(batch, textures, color, x, y, w, h);
        font.setColor(TEXT);
        layout.setText(font, label);
        font.draw(batch, label, x + (w - layout.width) / 2f, y + (h + layout.height) / 2f - 2);
    }

    private float[] screenToWorld(int mouseX, int mouseY, int screenWidth, int screenHeight, float zoomValue) {
        int tileDraw = Math.max(8, Math.round(BASE_TILE_DRAW * zoomValue));
        float scale = tileDraw / (float) GameConfig.TILE_SIZE;
        float worldX = (mouseX - screenWidth / 2f) / scale + cameraX;
        // mouseY is bottom-up; world Y grows downward
        float worldY = cameraY - (mouseY - screenHeight / 2f) / scale;
        return new float[] {worldX, worldY};
    }

    private boolean pointInMapArea(int x, int y, int screenWidth, int screenHeight) {
        return x >= SIDE_PAD && x < screenWidth - SIDE_PAD
                && y >= BOTTOM_BAR && y < screenHeight - TOP_BAR;
    }

    private boolean overExtendButton(int x, int y, int screenWidth, int screenHeight) {
        return hit(x, y, edgeButtonBounds("up", true, screenWidth, screenHeight))
                || hit(x, y, edgeButtonBounds("up", false, screenWidth, screenHeight))
                || hit(x, y, edgeButtonBounds("down", true, screenWidth, screenHeight))
                || hit(x, y, edgeButtonBounds("down", false, screenWidth, screenHeight))
                || hit(x, y, edgeButtonBounds("left", true, screenWidth, screenHeight))
                || hit(x, y, edgeButtonBounds("left", false, screenWidth, screenHeight))
                || hit(x, y, edgeButtonBounds("right", true, screenWidth, screenHeight))
                || hit(x, y, edgeButtonBounds("right", false, screenWidth, screenHeight));
    }

    private static boolean hit(int mx, int my, int[] b) {
        return hit(mx, my, b[0], b[1], b[2], b[3]);
    }

    private static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void dispose() {
        textures.dispose();
        font.dispose();
    }
}
