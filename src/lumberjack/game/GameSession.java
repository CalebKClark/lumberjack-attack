package lumberjack.game;

import lumberjack.combat.PlayerCombatState;
import lumberjack.combat.PlayerEnergyState;
import lumberjack.collection.CollectionRegistry;
import lumberjack.collection.PlayerCollections;
import lumberjack.crafting.CraftingService;
import lumberjack.crafting.RecipeDefinition;
import lumberjack.crafting.RecipeRegistry;
import lumberjack.core.GameConfig;
import lumberjack.economy.PlayerBank;
import lumberjack.engine.Camera;
import lumberjack.engine.input.KeyboardInput;
import lumberjack.entity.Player;
import lumberjack.entity.PlayerAttributes;
import lumberjack.entity.enemy.EnemyManager;
import lumberjack.entity.enemy.EnemyRegistry;
import lumberjack.entity.enemy.EnemySpawnRegistry;
import lumberjack.fishing.FishingManager;
import lumberjack.fishing.FishSpawnRegistry;
import lumberjack.interaction.ChopService;
import lumberjack.interaction.CombatService;
import lumberjack.interaction.DeathService;
import lumberjack.interaction.FishingService;
import lumberjack.interaction.PlaceService;
import lumberjack.interaction.PlaceableBreakService;
import lumberjack.interaction.EatPrompt;
import lumberjack.interaction.EatService;
import lumberjack.interaction.SleepPrompt;
import lumberjack.interaction.SleepService;
import lumberjack.inventory.Inventory;
import lumberjack.inventory.InventoryInteraction;
import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;
import lumberjack.save.SaveData;
import lumberjack.skills.PlayerSkills;
import lumberjack.skills.SkillLevelRegistry;
import lumberjack.skills.SkillType;
import lumberjack.time.GameClock;
import lumberjack.ui.hud.GameNotificationManager;
import lumberjack.ui.hud.InventoryListener;
import lumberjack.world.building.BuildingManager;
import lumberjack.world.drop.DroppedItemManager;
import lumberjack.world.map.MapTransitionRegistry;
import lumberjack.world.map.MapTransitionService;
import lumberjack.world.map.TileMap;
import lumberjack.world.map.WorldMapManager;
import lumberjack.world.placeable.PlacedObjectManager;
import lumberjack.world.placeable.ChestManager;
import lumberjack.world.placeable.PlacedObjectKey;
import lumberjack.world.placeable.WoodChipperManager;
import lumberjack.world.regrowth.TreeHitTracker;
import lumberjack.world.regrowth.TreeRegrowthManager;
import lumberjack.world.tile.TileRegistry;

/**
 * Runtime state for an active play session.
 * Holds references to world, player, and camera so UI/systems can share one source of truth.
 */
public final class GameSession {

    private final TileRegistry tileRegistry;
    private final ItemRegistry itemRegistry;
    private final WorldMapManager worldMaps;
    private final MapTransitionRegistry transitionRegistry;
    private final MapTransitionService transitionService;
    private final Player player;
    private final Camera camera;
    private final KeyboardInput keyboardInput;
    private final Inventory inventory;
    private final InventoryInteraction inventoryInteraction;
    private final GameClock clock;
    private final DroppedItemManager droppedItems;
    private final TreeRegrowthManager regrowthManager;
    private final TreeHitTracker treeHitTracker;
    private final ChopService chopService;
    private final PlaceableBreakService placeableBreakService;
    private final CombatService combatService;
    private final DeathService deathService;
    private final SleepService sleepService;
    private final EatService eatService;
    private final PlayerCombatState combatState;
    private final PlayerEnergyState energyState;
    private final EnemyRegistry enemyRegistry;
    private final EnemySpawnRegistry enemySpawnRegistry;
    private final EnemyManager enemyManager;
    private final FishingManager fishingManager;
    private final FishingService fishingService;
    private final FishSpawnRegistry fishSpawnRegistry;
    private final PlayerSkills playerSkills;
    private final SkillLevelRegistry skillLevelRegistry;
    private final CollectionRegistry collectionRegistry;
    private final PlayerCollections playerCollections;
    private final RecipeRegistry recipeRegistry;
    private final CraftingService craftingService;
    private final PlacedObjectManager placedObjectManager;
    private final BuildingManager buildingManager;
    private final WoodChipperManager woodChipperManager;
    private final ChestManager chestManager;
    private final PlaceService placeService;
    private final PlayerBank playerBank;
    private final PlayerAttributes playerAttributes;
    private final GameNotificationManager notificationManager;

    private boolean inventoryOpen;
    private boolean woodChipperOpen;
    private boolean chestOpen;
    private PlacedObjectKey activeWoodChipperKey;
    private PlacedObjectKey activeChestKey;
    private boolean menuPaused;
    private boolean worldSimulationActive;
    private Runnable onSleptListener;
    private String saveTitle = "Untitled";

    public GameSession(String mapId, SleepPrompt sleepPrompt, EatPrompt eatPrompt) {
        tileRegistry = new TileRegistry();
        itemRegistry = new ItemRegistry();
        worldMaps = new WorldMapManager(mapId);

        for (String gameMapId : GameConfig.GAME_MAPS) {
            worldMaps.ensureMapLoaded(gameMapId);
        }

        transitionRegistry = new MapTransitionRegistry();
        buildingManager = new BuildingManager();
        transitionService = new MapTransitionService(transitionRegistry, worldMaps, buildingManager);
        player = new Player();
        camera = new Camera();
        keyboardInput = new KeyboardInput();
        inventory = new Inventory(itemRegistry);
        inventoryInteraction = new InventoryInteraction();
        notificationManager = new GameNotificationManager(itemRegistry);
        collectionRegistry = new CollectionRegistry();
        playerCollections = new PlayerCollections(collectionRegistry);
        recipeRegistry = new RecipeRegistry();
        craftingService = new CraftingService(recipeRegistry);
        placedObjectManager = new PlacedObjectManager();
        // buildingManager created earlier for transition service
        woodChipperManager = new WoodChipperManager();
        chestManager = new ChestManager(itemRegistry);
        placeService = new PlaceService();
        playerBank = new PlayerBank();
        playerAttributes = new PlayerAttributes();
        inventory.setInventoryListener(createInventoryListener());
        clock = new GameClock();
        droppedItems = new DroppedItemManager();
        droppedItems.setActiveMap(worldMaps.getCurrentMapId());
        regrowthManager = new TreeRegrowthManager();
        treeHitTracker = new TreeHitTracker();
        chopService = new ChopService(regrowthManager, treeHitTracker);
        placeableBreakService = new PlaceableBreakService();
        combatService = new CombatService();
        deathService = new DeathService();
        sleepService = new SleepService(sleepPrompt);
        eatService = new EatService(eatPrompt);
        combatState = new PlayerCombatState();
        energyState = new PlayerEnergyState();
        enemyRegistry = new EnemyRegistry();
        validateContent();
        enemySpawnRegistry = new EnemySpawnRegistry();
        enemyManager = new EnemyManager();
        fishingManager = new FishingManager();
        fishingService = new FishingService();
        fishSpawnRegistry = new FishSpawnRegistry();
        playerSkills = new PlayerSkills();
        skillLevelRegistry = new SkillLevelRegistry();
        playerSkills.setProgressListener(notificationManager);

        placePlayerAtStartingSpawn();
        giveStarterItems();
        seedStartingCabinBed();
        craftingService.syncNotifiedUnlocks(playerCollections);
        combatState.resetToFullHealth(inventory, itemRegistry);
        enemyManager.resetFromSpawns(enemySpawnRegistry, enemyRegistry, worldMaps);
    }

    public static GameSession createNewGame(SleepPrompt sleepPrompt, EatPrompt eatPrompt) {
        return new GameSession(GameConfig.STARTING_MAP, sleepPrompt, eatPrompt);
    }

    public SaveData captureState() {
        SaveData data = new SaveData();
        data.setSaveTitle(saveTitle);
        data.setMapId(worldMaps.getCurrentMapId());
        data.setPlayerX(player.getX());
        data.setPlayerY(player.getY());
        data.setPlayerHealth(combatState.getCurrentHealth());
        data.setPlayerEnergy(energyState.getCurrentEnergy());
        data.setWoodChips(playerBank.getWoodChips());
        data.setPickupMagnetTiles(playerAttributes.getPickupMagnetTiles());
        data.setDay(clock.getDay());
        data.setHour(clock.getHour());
        data.setMinute(clock.getMinute());
        data.setSelectedHotbarSlot(inventory.getSelectedHotbarSlot());

        java.util.List<SaveData.MapEntry> mapStates = new java.util.ArrayList<>();
        for (TileMap map : worldMaps.getAllMaps()) {
            mapStates.add(new SaveData.MapEntry(map.getMapId(), map.copyTiles(), map.copyRotations()));
        }
        data.setMapStates(mapStates);
        data.setMapTiles(worldMaps.getCurrentMap().copyTiles());

        java.util.List<SaveData.InventoryEntry> inventoryEntries = new java.util.ArrayList<>();
        for (int slot = 0; slot < inventory.getSlotCount(); slot++) {
            ItemStack stack = inventory.getSlot(slot);
            if (!stack.isEmpty()) {
                inventoryEntries.add(new SaveData.InventoryEntry(
                        slot,
                        stack.getItemId(),
                        stack.getQuantity()
                ));
            }
        }
        data.setInventoryEntries(inventoryEntries);
        data.setDrops(droppedItems.exportDrops());
        data.setRegrowths(regrowthManager.exportRegrowths());
        data.setEnemies(enemyManager.exportEnemies());
        data.setEnemiesSectionPresent(true);
        data.setPlacedObjects(placedObjectManager.exportPlacedObjects());
        data.setWoodChippers(woodChipperManager.exportStates());
        data.setChestSlots(chestManager.exportStates());
        playerSkills.exportTo(data);
        playerCollections.exportTo(data);
        return data;
    }

    public void applyState(SaveData data) {
        saveTitle = normalizeSaveTitle(data.getSaveTitle());

        if (!data.getMapStates().isEmpty()) {
            for (SaveData.MapEntry mapEntry : data.getMapStates()) {
                worldMaps.replaceMapTiles(mapEntry.getMapId(), mapEntry.getTiles(), mapEntry.getRotations());
            }
        } else if (data.getMapTiles() != null) {
            worldMaps.replaceMapTiles(data.getMapId(), data.getMapTiles());
        } else {
            throw new IllegalArgumentException("Save file does not contain map data.");
        }

        worldMaps.switchToMap(data.getMapId());
        player.setPosition(data.getPlayerX(), data.getPlayerY());
        clock.resetToTime(data.getDay(), data.getHour(), data.getMinute());

        inventory.clearAllSlots();
        runWithoutNotifications(() -> {
            for (SaveData.InventoryEntry entry : data.getInventoryEntries()) {
                inventory.setSlot(entry.getSlotIndex(), ItemStack.of(entry.getItemId(), entry.getQuantity()));
            }
        });
        selectHotbarSlot(data.getSelectedHotbarSlot());
        playerBank.setWoodChips(data.getWoodChips());
        if (data.getPickupMagnetTiles() > 0) {
            playerAttributes.setPickupMagnetTiles(data.getPickupMagnetTiles());
        } else {
            playerAttributes.resetDefaults();
        }
        depositInventoryCurrencyToBank();

        combatState.setCurrentHealth(data.getPlayerHealth() > 0
                ? data.getPlayerHealth()
                : GameConfig.BASE_MAX_HEALTH);
        combatState.clampHealthToMax(inventory, itemRegistry);
        if (data.getPlayerEnergy() >= 0) {
            energyState.setCurrentEnergy(data.getPlayerEnergy());
        } else {
            energyState.resetToFull();
        }

        droppedItems.clear();
        droppedItems.setActiveMap(worldMaps.getCurrentMapId());
        for (SaveData.DropEntry drop : data.getDrops()) {
            String dropMapId = drop.getMapId() != null ? drop.getMapId() : data.getMapId();
            droppedItems.restoreDrop(
                    dropMapId,
                    drop.getItemId(),
                    drop.getQuantity(),
                    drop.getCenterX(),
                    drop.getCenterY()
            );
        }
        droppedItems.setActiveMap(worldMaps.getCurrentMapId());

        regrowthManager.restoreRegrowths(data.getRegrowths());
        regrowthManager.applyStumpTiles(worldMaps);
        treeHitTracker.clearAll();

        if (data.isEnemiesSectionPresent()) {
            enemyManager.restoreEnemies(data.getEnemies(), enemyRegistry);
        } else {
            enemyManager.resetFromSpawns(enemySpawnRegistry, enemyRegistry, worldMaps);
        }

        placedObjectManager.restoreAll(data.getPlacedObjects(), worldMaps::getMapOrNull);
        woodChipperManager.restoreAll(data.getWoodChippers());
        chestManager.restoreAll(data.getChestSlots());

        transitionService.clearCooldown();
        sleepService.reset();

        inventoryInteraction.clearCarried();
        inventoryOpen = false;
        woodChipperOpen = false;
        chestOpen = false;
        activeWoodChipperKey = null;
        activeChestKey = null;
        menuPaused = false;
        fishingManager.reset();
        player.cancelRodAnimation();
        if (data.getSkillEntries().isEmpty()) {
            playerSkills.reset();
        } else {
            playerSkills.importFrom(data);
        }
        if (data.getCollectionEntries().isEmpty()) {
            playerCollections.reset();
        } else {
            playerCollections.importFrom(data);
        }
        craftingService.syncNotifiedUnlocks(playerCollections);
        notificationManager.clear();
    }

    public void resetToNewGame() {
        worldMaps.resetMaps(GameConfig.GAME_MAPS);
        placePlayerAtStartingSpawn();
        clock.reset();
        inventory.clearAllSlots();
        giveStarterItems();
        playerBank.reset();
        playerAttributes.resetDefaults();
        combatState.resetToFullHealth(inventory, itemRegistry);
        energyState.resetToFull();
        droppedItems.clear();
        droppedItems.setActiveMap(worldMaps.getCurrentMapId());
        placedObjectManager.clear();
        eatService.reset();
        woodChipperManager.clear();
        chestManager.clear();
        seedStartingCabinBed();
        regrowthManager.clear();
        treeHitTracker.clearAll();
        enemyManager.resetFromSpawns(enemySpawnRegistry, enemyRegistry, worldMaps);
        transitionService.clearCooldown();
        sleepService.reset();
        inventoryInteraction.clearCarried();
        inventoryOpen = false;
        woodChipperOpen = false;
        chestOpen = false;
        activeWoodChipperKey = null;
        activeChestKey = null;
        menuPaused = false;
        fishingManager.reset();
        player.cancelRodAnimation();
        playerSkills.reset();
        playerCollections.reset();
        craftingService.resetNotifiedUnlocks();
        notificationManager.clear();
        craftingService.syncNotifiedUnlocks(playerCollections);
    }

    public void onMapChanged() {
        fishingManager.reset();
        player.cancelRodAnimation();
        keyboardInput.clearAll();
        droppedItems.setActiveMap(worldMaps.getCurrentMapId());
    }

    public void switchToMap(String mapId) {
        worldMaps.switchToMap(mapId);
        droppedItems.setActiveMap(mapId);
    }

    public void clearTransitionCooldown() {
        transitionService.clearCooldown();
    }

    /**
     * Fast-forwards regrowth and machine timers by the in-game minutes skipped (e.g. via sleep).
     */
    public void applySkippedGameTime(int skippedGameMinutes) {
        if (skippedGameMinutes <= 0) {
            return;
        }

        double skippedMs = skippedGameMinutes * (double) GameConfig.REAL_MS_PER_GAME_MINUTE;
        regrowthManager.updateAll(skippedMs, worldMaps);
        woodChipperManager.updateAll(skippedMs, itemRegistry);
    }

    public String getSaveTitle() {
        return saveTitle;
    }

    public void setSaveTitle(String saveTitle) {
        this.saveTitle = normalizeSaveTitle(saveTitle);
    }

    private void placePlayerAtStartingSpawn() {
        WorldMapManager.placePlayerAtTile(
                player,
                worldMaps.getCurrentMap(),
                GameConfig.STARTING_SPAWN_COL,
                GameConfig.STARTING_SPAWN_ROW
        );
    }

    private void seedStartingCabinBed() {
        placedObjectManager.place(
                GameConfig.STARTING_MAP,
                lumberjack.world.placeable.BedPlaceable.ITEM_ID,
                GameConfig.RESPAWN_BED_COL,
                GameConfig.RESPAWN_BED_ROW
        );
    }

    private String normalizeSaveTitle(String title) {
        if (title == null) {
            return "Untitled";
        }

        String trimmed = title.trim();
        if (trimmed.isEmpty()) {
            return "Untitled";
        }

        if (trimmed.length() > 40) {
            return trimmed.substring(0, 40);
        }

        return trimmed;
    }

    private void giveStarterItems() {
        StarterLoadout loadout = new StarterLoadout();
        runWithoutNotifications(() -> {
            for (StarterLoadout.Entry entry : loadout.getEntries()) {
                inventory.setSlot(entry.slot(), entry.toStack());
            }
        });
        selectHotbarSlot(loadout.getDefaultHotbarSlot());
    }

    public void checkForNewRecipeUnlocks() {
        craftingService.checkForNewUnlocks(playerCollections, notificationManager);
    }

    private void depositInventoryCurrencyToBank() {
        int found = inventory.removeAllOfItem(PlayerBank.WOOD_CHIPS_ID);
        if (found > 0) {
            playerBank.depositWoodChips(found);
        }
    }

    private void validateContent() {
        ContentValidator.validate(itemRegistry, recipeRegistry, collectionRegistry, enemyRegistry);
    }

    private InventoryListener createInventoryListener() {
        return new InventoryListener() {
            @Override
            public void onItemsAdded(String itemId, int quantity) {
                notificationManager.onItemsAdded(itemId, quantity);
                playerCollections.onItemAcquired(itemId);
                checkForNewRecipeUnlocks();
            }

            @Override
            public void onItemsRemoved(String itemId, int quantity) {
                notificationManager.onItemsRemoved(itemId, quantity);
            }
        };
    }

    private void runWithoutNotifications(Runnable action) {
        notificationManager.setSuppressed(true);
        inventory.setSuppressNotifications(true);
        try {
            action.run();
        } finally {
            inventory.setSuppressNotifications(false);
            notificationManager.setSuppressed(false);
        }
    }

    public String getCurrentMapId() {
        return worldMaps.getCurrentMapId();
    }

    public TileMap getCurrentMap() {
        return worldMaps.getCurrentMap();
    }

    public void selectHotbarSlot(int slotIndex) {
        inventory.selectHotbarSlot(slotIndex);
        combatState.clampHealthToMax(inventory, itemRegistry);
    }

    public Player getPlayer() {
        return player;
    }

    public Camera getCamera() {
        return camera;
    }

    public KeyboardInput getKeyboardInput() {
        return keyboardInput;
    }

    public TileRegistry getTileRegistry() {
        return tileRegistry;
    }

    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public InventoryInteraction getInventoryInteraction() {
        return inventoryInteraction;
    }

    public GameClock getClock() {
        return clock;
    }

    public DroppedItemManager getDroppedItems() {
        return droppedItems;
    }

    public ChopService getChopService() {
        return chopService;
    }

    public PlaceableBreakService getPlaceableBreakService() {
        return placeableBreakService;
    }

    public CombatService getCombatService() {
        return combatService;
    }

    public EnemyRegistry getEnemyRegistry() {
        return enemyRegistry;
    }

    public EnemyManager getEnemyManager() {
        return enemyManager;
    }

    public FishingManager getFishingManager() {
        return fishingManager;
    }

    public FishingService getFishingService() {
        return fishingService;
    }

    public FishSpawnRegistry getFishSpawnRegistry() {
        return fishSpawnRegistry;
    }

    public CollectionRegistry getCollectionRegistry() {
        return collectionRegistry;
    }

    public PlayerCollections getPlayerCollections() {
        return playerCollections;
    }

    public RecipeRegistry getRecipeRegistry() {
        return recipeRegistry;
    }

    public CraftingService getCraftingService() {
        return craftingService;
    }

    public PlacedObjectManager getPlacedObjectManager() {
        return placedObjectManager;
    }

    public BuildingManager getBuildingManager() {
        return buildingManager;
    }

    public WoodChipperManager getWoodChipperManager() {
        return woodChipperManager;
    }

    public ChestManager getChestManager() {
        return chestManager;
    }

    public PlaceService getPlaceService() {
        return placeService;
    }

    public PlayerAttributes getPlayerAttributes() {
        return playerAttributes;
    }

    public PlayerBank getPlayerBank() {
        return playerBank;
    }

    public PlayerSkills getPlayerSkills() {
        return playerSkills;
    }

    public SkillLevelRegistry getSkillLevelRegistry() {
        return skillLevelRegistry;
    }

    public GameNotificationManager getNotificationManager() {
        return notificationManager;
    }

    public PlayerCombatState getCombatState() {
        return combatState;
    }

    public PlayerEnergyState getEnergyState() {
        return energyState;
    }

    public EatService getEatService() {
        return eatService;
    }

    /** Spends energy for an axe swing. */
    public boolean trySpendActionEnergy() {
        return trySpendEnergy(GameConfig.ACTION_ENERGY_COST);
    }

    /** Spends energy for a fishing cast. */
    public boolean trySpendCastEnergy() {
        return trySpendEnergy(GameConfig.FISHING_CAST_ENERGY_COST);
    }

    private boolean trySpendEnergy(int amount) {
        if (energyState.trySpend(amount)) {
            return true;
        }
        notificationManager.notifyMessage("Too tired");
        return false;
    }

    public boolean isInventoryOpen() {
        return inventoryOpen;
    }

    public void setInventoryOpen(boolean inventoryOpen) {
        this.inventoryOpen = inventoryOpen;
    }

    public boolean isWoodChipperOpen() {
        return woodChipperOpen;
    }

    public PlacedObjectKey getActiveWoodChipperKey() {
        return activeWoodChipperKey;
    }

    public void openWoodChipper(PlacedObjectKey key) {
        woodChipperManager.getOrCreate(key);
        activeWoodChipperKey = key;
        woodChipperOpen = true;
    }

    public void closeWoodChipper() {
        woodChipperOpen = false;
        activeWoodChipperKey = null;
    }

    public boolean isChestOpen() {
        return chestOpen;
    }

    public PlacedObjectKey getActiveChestKey() {
        return activeChestKey;
    }

    public void openChest(PlacedObjectKey key) {
        chestManager.getOrCreate(key);
        activeChestKey = key;
        chestOpen = true;
    }

    public void closeChest() {
        chestOpen = false;
        activeChestKey = null;
    }

    public void setMenuPaused(boolean menuPaused) {
        this.menuPaused = menuPaused;
    }

    public boolean isMenuPaused() {
        return menuPaused;
    }

    public void setWorldSimulationActive(boolean worldSimulationActive) {
        this.worldSimulationActive = worldSimulationActive;
    }

    public void setOnSleptListener(Runnable onSleptListener) {
        this.onSleptListener = onSleptListener;
    }

    /**
     * World time (clock, regrowth, machines, fishing bobber, etc.) only advances while actively playing.
     * Inventory and chest UIs pause the world; wood chipper stays live so processing continues.
     */
    public boolean shouldAdvanceWorldTime() {
        return worldSimulationActive
                && !menuPaused
                && !inventoryOpen
                && !chestOpen
                && !fishingManager.hasActiveMinigameSession();
    }

    public void updateMachines(double elapsedMs) {
        woodChipperManager.updateAll(elapsedMs, itemRegistry);
    }

    public void updateTime(double elapsedMs) {
        clock.update(elapsedMs);
    }

    public boolean updateGameplay(double elapsedMs) {
        droppedItems.update(player, playerAttributes, inventory, itemRegistry, elapsedMs);

        boolean mapChanged = transitionService.update(this, player, droppedItems, elapsedMs);
        boolean regrowthChanged = regrowthManager.updateAll(elapsedMs, worldMaps);
        boolean enemyChanged = enemyManager.update(this, elapsedMs);
        boolean respawned = deathService.checkAndRespawn(this);
        boolean slept = sleepService.update(this);
        if (slept && onSleptListener != null) {
            onSleptListener.run();
        }
        return mapChanged || regrowthChanged || enemyChanged || respawned || slept;
    }

    public void updateMovement() {
        updateMovement(GameConfig.FRAME_MS);
    }

    public void updateMovement(double elapsedMs) {
        if (inventoryOpen || woodChipperOpen || chestOpen || fishingManager.hasActiveMinigameSession()) {
            player.setMovementIntent(0, 0);
            player.update(elapsedMs);
            resolveAxeSwingImpacts();
            resolveRodCastHold();
            return;
        }

        Player activePlayer = player;
        if (activePlayer.isActionLocked()) {
            activePlayer.setMovementIntent(0, 0);
            activePlayer.consumeMoveStepX(0);
            activePlayer.consumeMoveStepY(0);
            activePlayer.update(elapsedMs);
            resolveAxeSwingImpacts();
            resolveRodCastHold();
            return;
        }

        double moveScale = elapsedMs / GameConfig.FRAME_MS;
        double axisX = keyboardInput.getHorizontalAxis();
        double axisY = keyboardInput.getVerticalAxis();
        double axisLength = Math.hypot(axisX, axisY);
        if (axisLength > 0.0) {
            // Normalize so W+D (etc.) is not faster than a single key.
            axisX /= axisLength;
            axisY /= axisLength;
        }
        double speed = activePlayer.getSpeed() * moveScale * energyState.getMoveSpeedMultiplier();
        int dx = activePlayer.consumeMoveStepX(axisX * speed);
        int dy = activePlayer.consumeMoveStepY(axisY * speed);

        activePlayer.setMovementIntent(dx, dy);

        if (dx == 0 && dy == 0) {
            activePlayer.update(elapsedMs);
            resolveAxeSwingImpacts();
            resolveRodCastHold();
            return;
        }

        TileMap currentMap = worldMaps.getCurrentMap();
        String mapId = currentMap.getMapId();
        if (!tryMovePlayer(activePlayer, dx, dy, currentMap, mapId)) {
            if (dx != 0) {
                tryMovePlayer(activePlayer, dx, 0, currentMap, mapId);
            }
            if (dy != 0) {
                tryMovePlayer(activePlayer, 0, dy, currentMap, mapId);
            }
        }

        activePlayer.update(elapsedMs);
        resolveAxeSwingImpacts();
        resolveRodCastHold();
    }

    private void resolveAxeSwingImpacts() {
        boolean swingCompleted = player.pollAxeSwingCompleted();
        chopService.onAxeSwingCompleted(this, swingCompleted);
        placeableBreakService.onAxeSwingCompleted(this, swingCompleted);
    }

    private void resolveRodCastHold() {
        if (player.pollRodCastCompleted()) {
            fishingManager.onRodCastHoldReached(this);
        }
    }

    private boolean tryMovePlayer(Player player, int dx, int dy, TileMap map, String mapId) {
        if (dx == 0 && dy == 0) {
            return false;
        }

        java.awt.Rectangle targetBounds = player.getBoundsAtOffset(dx, dy);
        if (map.collidesWith(targetBounds, tileRegistry)
                || placedObjectManager.collidesWith(targetBounds, mapId)
                || buildingManager.collidesWith(targetBounds, mapId)) {
            return false;
        }

        player.move(dx, dy);
        return true;
    }

    public void updateCamera(int viewportWidth, int viewportHeight, double zoomScale) {
        int effectiveWidth = Math.max(1, (int) Math.round(viewportWidth / zoomScale));
        int effectiveHeight = Math.max(1, (int) Math.round(viewportHeight / zoomScale));
        TileMap map = worldMaps.getCurrentMap();
        camera.centerOnClamped(
                player.getX() + player.getWidth() / 2,
                player.getY() + player.getHeight() / 2,
                effectiveWidth,
                effectiveHeight,
                map.getWidthInPixels(),
                map.getHeightInPixels()
        );
    }
}
