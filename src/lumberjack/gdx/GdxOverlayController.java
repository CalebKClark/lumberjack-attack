package lumberjack.gdx;

import java.awt.Rectangle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

import lumberjack.collection.CollectionCategory;
import lumberjack.crafting.CraftingCategory;
import lumberjack.crafting.RecipeDefinition;
import lumberjack.engine.DisplaySettings;
import lumberjack.engine.input.GameAction;
import lumberjack.engine.input.InputSettings;
import lumberjack.game.GameSession;
import lumberjack.interaction.PlaceableAccessService;
import lumberjack.inventory.InventoryInteraction;
import lumberjack.inventory.ItemContainer;
import lumberjack.inventory.SlotTransferPolicies;
import lumberjack.interaction.WoodChipperSlotInteraction;
import lumberjack.item.ItemRegistry;
import lumberjack.item.ItemStack;
import lumberjack.skills.SkillType;
import lumberjack.ui.hud.CollectionsTabLayout;
import lumberjack.ui.hud.CraftingTabLayout;
import lumberjack.ui.hud.HotbarLayout;
import lumberjack.ui.hud.InventoryLayout;
import lumberjack.ui.hud.InventoryUiClicks;
import lumberjack.ui.hud.PlayerMenuLayout;
import lumberjack.ui.hud.PlayerMenuTab;
import lumberjack.ui.hud.ProcessingMachineUiLayout;
import lumberjack.ui.hud.SkillSnakeLayout;
import lumberjack.ui.hud.StorageOverlayLayout;
import lumberjack.world.placeable.ChestManager;
import lumberjack.world.placeable.PlacedObjectKey;
import lumberjack.world.placeable.WoodChipperMachine;

/**
 * LibGDX overlay controller — inventory, chest, chipper, and pause UI.
 */
public final class GdxOverlayController implements Disposable {

    public interface PauseActions {
        void onContinue();

        void onSave();

        void onSaveAndExit();
    }

    private enum PauseView {
        MENU,
        SETTINGS
    }

    private final InputSettings inputSettings;
    private final GdxPauseMenuRenderer pauseMenuRenderer = new GdxPauseMenuRenderer();
    private final GdxSettingsPanel settingsPanel = new GdxSettingsPanel();
    private final GdxItemSlotDrawer itemSlotDrawer = new GdxItemSlotDrawer();
    private final GdxProcessingMachineOverlay machineOverlay = new GdxProcessingMachineOverlay();
    private final GdxStorageOverlay storageOverlay = new GdxStorageOverlay();
    private final GdxPlayerMenuOverlay playerMenuOverlay = new GdxPlayerMenuOverlay();
    private PauseActions pauseActions;
    private PauseView pauseView = PauseView.MENU;

    private boolean pauseMenuOpen;
    private PlayerMenuTab selectedTab = PlayerMenuTab.INVENTORY;
    private SkillType focusedSkill;
    private Integer hoveredSkillLevel;
    private CollectionCategory focusedCollectionCategory;
    private String hoveredCollectionEntryId;
    private CraftingCategory focusedCraftingCategory;
    private String hoveredRecipeId;
    private Integer hoveredInventorySlot;
    private Integer hoveredContainerSlot;
    private Integer hoveredMachineSlot;
    private int mouseX;
    private int mouseY;

    public GdxOverlayController(InputSettings inputSettings) {
        this.inputSettings = inputSettings;
    }

    public void setPauseActions(PauseActions pauseActions) {
        this.pauseActions = pauseActions;
    }

    public boolean isPauseMenuOpen() {
        return pauseMenuOpen;
    }

    public void update(GameSession session, DisplaySettings displaySettings, int screenWidth, int screenHeight) {
        mouseX = GdxInputHelper.mouseX();
        mouseY = GdxInputHelper.mouseYTopDown();

        if (GdxInputHelper.isActionJustPressed(GameAction.INVENTORY, inputSettings)) {
            handleInventoryKey(session, displaySettings);
        }

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            handleEscape(session);
        }

        if (pauseMenuOpen) {
            if (pauseView == PauseView.SETTINGS) {
                if (settingsPanel.update(inputSettings, displaySettings, screenWidth, screenHeight)) {
                    pauseView = PauseView.MENU;
                }
            } else {
                handlePauseMenuClick(session);
            }
            return;
        }

        if (session.getFishingManager().hasActiveMinigameSession()) {
            return;
        }

        if (session.isInventoryOpen()) {
            updateInventoryHover(session, screenWidth, screenHeight);
            handleInventoryClick(session, screenWidth, screenHeight);
            return;
        }

        if (session.isChestOpen()) {
            updateChestHover(session, screenWidth, screenHeight);
            handleChestClick(session, screenWidth, screenHeight);
            return;
        }

        if (session.isWoodChipperOpen()) {
            updateChipperHover(session, screenWidth, screenHeight);
            handleChipperClick(session, screenWidth, screenHeight);
        }
    }

    public boolean blocksWorldPrimaryClick(GameSession session, int screenWidth, int screenHeight) {
        if (hasGameplayOverlay(session)) {
            return true;
        }
        return HotbarLayout.getSlotAtPoint(mouseX, mouseY, screenWidth, screenHeight) >= 0;
    }

    public boolean isModalOpen() {
        return pauseMenuOpen;
    }

    public boolean hasGameplayOverlay(GameSession session) {
        return pauseMenuOpen
                || session.isInventoryOpen()
                || session.isChestOpen()
                || session.isWoodChipperOpen();
    }

    public void draw(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            int screenWidth,
            int screenHeight
    ) {
        if (pauseMenuOpen) {
            return;
        }

        if (session.isInventoryOpen()) {
            drawInventoryOverlay(batch, font, textures, session, screenWidth, screenHeight);
            return;
        }

        if (session.isChestOpen()) {
            drawChestOverlay(batch, font, textures, session, screenWidth, screenHeight);
            return;
        }

        if (session.isWoodChipperOpen()) {
            drawChipperOverlay(batch, font, textures, session, screenWidth, screenHeight);
        }
    }

    public void drawPauseMenu(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            DisplaySettings displaySettings,
            int screenWidth,
            int screenHeight
    ) {
        if (!pauseMenuOpen) {
            return;
        }
        if (pauseView == PauseView.SETTINGS) {
            settingsPanel.draw(batch, font, textures, inputSettings, displaySettings, screenWidth, screenHeight);
        } else {
            pauseMenuRenderer.draw(batch, font, textures, screenWidth, screenHeight);
        }
    }

    private void handleInventoryKey(GameSession session, DisplaySettings displaySettings) {
        if (pauseMenuOpen) {
            return;
        }

        if (session.isChestOpen()) {
            tryCloseChest(session);
            return;
        }

        if (session.isWoodChipperOpen()) {
            tryCloseWoodChipper(session);
            return;
        }

        if (session.isInventoryOpen()) {
            tryCloseInventory(session);
            return;
        }

        int pointerX = Gdx.input.getX();
        int pointerY = Gdx.input.getY();

        PlacedObjectKey chestKey = PlaceableAccessService.findInteractable(
                session,
                displaySettings,
                pointerX,
                pointerY,
                ChestManager.CHEST_ITEM_ID
        );
        if (chestKey != null) {
            openChest(session, chestKey);
            return;
        }

        PlacedObjectKey chipperKey = PlaceableAccessService.findInteractable(
                session,
                displaySettings,
                pointerX,
                pointerY,
                WoodChipperMachine.MACHINE_ITEM_ID
        );
        if (chipperKey != null) {
            openWoodChipper(session, chipperKey);
            return;
        }

        session.getInventoryInteraction().clearCarried();
        resetToInventoryTab();
        session.setInventoryOpen(true);
    }

    private void handleEscape(GameSession session) {
        if (pauseMenuOpen) {
            if (pauseView == PauseView.SETTINGS) {
                if (settingsPanel.isListening()) {
                    settingsPanel.cancelListening();
                } else {
                    pauseView = PauseView.MENU;
                }
                return;
            }
            setPauseMenuOpen(session, false);
            return;
        }

        if (session.getFishingManager().hasActiveMinigameSession()) {
            session.getFishingManager().cancelMinigame(session);
            session.getKeyboardInput().clearAll();
            return;
        }

        if (session.getFishingManager().isLineOut() || session.getPlayer().isRodBusy()) {
            session.getFishingManager().cancelLine(session);
            session.getKeyboardInput().clearAll();
            return;
        }

        if (session.isChestOpen()) {
            tryCloseChest(session);
            return;
        }

        if (session.isWoodChipperOpen()) {
            tryCloseWoodChipper(session);
            return;
        }

        if (session.isInventoryOpen()) {
            tryCloseInventory(session);
            return;
        }

        setPauseMenuOpen(session, true);
    }

    private void handlePauseMenuClick(GameSession session) {
        if (!GdxInputHelper.isPrimaryMouseJustPressed()) {
            return;
        }

        int button = pauseMenuRenderer.getClickedButton(
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );
        switch (button) {
            case 0 -> {
                setPauseMenuOpen(session, false);
                if (pauseActions != null) {
                    pauseActions.onContinue();
                }
            }
            case 1 -> pauseView = PauseView.SETTINGS;
            case 2 -> {
                if (pauseActions != null) {
                    pauseActions.onSave();
                }
            }
            case 3 -> {
                if (pauseActions != null) {
                    pauseActions.onSaveAndExit();
                }
            }
            default -> {
            }
        }
    }

    private void setPauseMenuOpen(GameSession session, boolean open) {
        pauseMenuOpen = open;
        pauseView = PauseView.MENU;
        settingsPanel.cancelListening();
        session.setMenuPaused(open);
        if (open) {
            session.setInventoryOpen(false);
            session.closeChest();
            session.closeWoodChipper();
            session.getFishingManager().cancelMinigame(session);
            session.getKeyboardInput().clearAll();
        }
    }

    private void openChest(GameSession session, PlacedObjectKey key) {
        session.getInventoryInteraction().clearCarried();
        session.openChest(key);
    }

    private void openWoodChipper(GameSession session, PlacedObjectKey key) {
        session.getInventoryInteraction().clearCarried();
        session.openWoodChipper(key);
    }

    private boolean tryCloseChest(GameSession session) {
        if (!stowCarried(session)) {
            return false;
        }
        session.closeChest();
        return true;
    }

    private boolean tryCloseWoodChipper(GameSession session) {
        if (!stowCarried(session)) {
            return false;
        }
        session.closeWoodChipper();
        return true;
    }

    private boolean tryCloseInventory(GameSession session) {
        if (!stowCarried(session)) {
            return false;
        }
        session.setInventoryOpen(false);
        return true;
    }

    private boolean stowCarried(GameSession session) {
        return session.getInventoryInteraction().stowCarriedStack(
                session.getInventory(),
                session.getItemRegistry(),
                session.getPlayerBank()
        );
    }

    private void resetToInventoryTab() {
        selectedTab = PlayerMenuTab.INVENTORY;
        focusedSkill = null;
        hoveredSkillLevel = null;
        focusedCollectionCategory = null;
        hoveredCollectionEntryId = null;
        focusedCraftingCategory = null;
        hoveredRecipeId = null;
    }

    private void handleInventoryClick(GameSession session, int screenWidth, int screenHeight) {
        if (!GdxInputHelper.isPrimaryMouseJustPressed() && !GdxInputHelper.isSecondaryMouseJustPressed()) {
            return;
        }
        boolean rightClick = GdxInputHelper.isSecondaryMouseJustPressed();

        int tabIndex = PlayerMenuLayout.getTabIndexAtPoint(mouseX, mouseY, screenWidth, screenHeight);
        if (tabIndex >= 0) {
            selectTab(session, PlayerMenuTab.fromIndex(tabIndex));
            return;
        }

        if (selectedTab == PlayerMenuTab.SKILLS) {
            handleSkillsClick(screenWidth, screenHeight);
            return;
        }
        if (selectedTab == PlayerMenuTab.COLLECTIONS) {
            handleCollectionsClick(screenWidth, screenHeight);
            return;
        }
        if (selectedTab == PlayerMenuTab.CRAFTING) {
            handleCraftingClick(session, screenWidth, screenHeight);
            return;
        }
        if (selectedTab != PlayerMenuTab.INVENTORY) {
            return;
        }

        Rectangle content = PlayerMenuLayout.getContentBounds(
                PlayerMenuLayout.getMenuPanelBounds(screenWidth, screenHeight)
        );
        int slotIndex = InventoryLayout.getSlotAtPoint(mouseX, mouseY, content);
        if (slotIndex >= 0) {
            InventoryUiClicks.handlePlayerInventorySlot(session, slotIndex, rightClick);
        }
    }

    private void selectTab(GameSession session, PlayerMenuTab tab) {
        if (tab == selectedTab) {
            return;
        }

        if (selectedTab == PlayerMenuTab.INVENTORY && session.getInventoryInteraction().isCarrying()) {
            if (!stowCarried(session)) {
                return;
            }
            session.getInventoryInteraction().clearCarried();
        }

        selectedTab = tab;
        if (tab != PlayerMenuTab.SKILLS) {
            focusedSkill = null;
            hoveredSkillLevel = null;
        }
        if (tab != PlayerMenuTab.COLLECTIONS) {
            focusedCollectionCategory = null;
            hoveredCollectionEntryId = null;
        }
        if (tab != PlayerMenuTab.CRAFTING) {
            focusedCraftingCategory = null;
            hoveredRecipeId = null;
        }
    }

    private void handleSkillsClick(int screenWidth, int screenHeight) {
        Rectangle content = PlayerMenuLayout.getContentBounds(
                PlayerMenuLayout.getMenuPanelBounds(screenWidth, screenHeight)
        );
        if (focusedSkill == null) {
            SkillType clickedSkill = SkillSnakeLayout.getSkillAtPoint(mouseX, mouseY, content);
            if (clickedSkill != null) {
                focusedSkill = clickedSkill;
                hoveredSkillLevel = null;
            }
            return;
        }
        if (SkillSnakeLayout.getBackButtonBounds(content).contains(mouseX, mouseY)) {
            focusedSkill = null;
            hoveredSkillLevel = null;
        }
    }

    private void handleCollectionsClick(int screenWidth, int screenHeight) {
        Rectangle content = PlayerMenuLayout.getContentBounds(
                PlayerMenuLayout.getMenuPanelBounds(screenWidth, screenHeight)
        );
        if (focusedCollectionCategory == null) {
            CollectionCategory clicked = CollectionsTabLayout.getCategoryAtPoint(mouseX, mouseY, content);
            if (clicked != null) {
                focusedCollectionCategory = clicked;
                hoveredCollectionEntryId = null;
            }
            return;
        }
        if (CollectionsTabLayout.getBackButtonBounds(content).contains(mouseX, mouseY)) {
            focusedCollectionCategory = null;
            hoveredCollectionEntryId = null;
        }
    }

    private void handleCraftingClick(GameSession session, int screenWidth, int screenHeight) {
        Rectangle content = PlayerMenuLayout.getContentBounds(
                PlayerMenuLayout.getMenuPanelBounds(screenWidth, screenHeight)
        );
        if (focusedCraftingCategory == null) {
            CraftingCategory clicked = CraftingTabLayout.getCategoryAtPoint(mouseX, mouseY, content);
            if (clicked != null) {
                focusedCraftingCategory = clicked;
                hoveredRecipeId = null;
            }
            return;
        }
        if (CraftingTabLayout.getBackButtonBounds(content).contains(mouseX, mouseY)) {
            focusedCraftingCategory = null;
            hoveredRecipeId = null;
            return;
        }

        String recipeId = CraftingTabLayout.getRecipeIdAtPoint(
                mouseX,
                mouseY,
                content,
                session.getCraftingService().getRecipeRegistry().getRecipes(focusedCraftingCategory)
        );
        if (recipeId == null && hoveredRecipeId != null) {
            recipeId = hoveredRecipeId;
        }
        if (recipeId == null) {
            return;
        }

        RecipeDefinition recipe = session.getCraftingService().getRecipeRegistry().get(recipeId);
        if (!session.getCraftingService().isRecipeUnlocked(recipe, session.getPlayerCollections())) {
            return;
        }
        session.getCraftingService().tryCraft(
                recipe,
                session.getInventory(),
                session.getPlayerCollections()
        );
    }

    private void updateInventoryHover(GameSession session, int screenWidth, int screenHeight) {
        Rectangle content = PlayerMenuLayout.getContentBounds(
                PlayerMenuLayout.getMenuPanelBounds(screenWidth, screenHeight)
        );

        Integer newHover = null;
        Integer newSkillLevelHover = null;
        String newCollectionEntryHover = null;
        String newRecipeHover = null;

        if (selectedTab == PlayerMenuTab.INVENTORY) {
            int slotIndex = InventoryLayout.getSlotAtPoint(mouseX, mouseY, content);
            if (slotIndex >= 0 && !session.getInventory().getSlot(slotIndex).isEmpty()) {
                newHover = slotIndex;
            }
        } else if (selectedTab == PlayerMenuTab.SKILLS && focusedSkill != null) {
            int level = SkillSnakeLayout.getLevelAtPoint(mouseX, mouseY, content);
            if (level > 0) {
                newSkillLevelHover = level;
            }
        } else if (selectedTab == PlayerMenuTab.COLLECTIONS && focusedCollectionCategory != null) {
            newCollectionEntryHover = CollectionsTabLayout.getEntryIdAtPoint(
                    mouseX,
                    mouseY,
                    content,
                    session.getCollectionRegistry().getEntries(focusedCollectionCategory)
            );
            if (newCollectionEntryHover != null
                    && !session.getPlayerCollections().isDiscovered(newCollectionEntryHover)) {
                newCollectionEntryHover = null;
            }
        } else if (selectedTab == PlayerMenuTab.CRAFTING && focusedCraftingCategory != null) {
            newRecipeHover = CraftingTabLayout.getRecipeIdAtPoint(
                    mouseX,
                    mouseY,
                    content,
                    session.getCraftingService().getRecipeRegistry().getRecipes(focusedCraftingCategory)
            );
            if (newRecipeHover != null) {
                RecipeDefinition recipe = session.getCraftingService().getRecipeRegistry().get(newRecipeHover);
                if (!session.getCraftingService().isRecipeUnlocked(recipe, session.getPlayerCollections())) {
                    newRecipeHover = null;
                }
            }
        }

        hoveredInventorySlot = newHover;
        hoveredSkillLevel = newSkillLevelHover;
        hoveredCollectionEntryId = newCollectionEntryHover;
        hoveredRecipeId = newRecipeHover;
    }

    private void handleChestClick(GameSession session, int screenWidth, int screenHeight) {
        if (!GdxInputHelper.isPrimaryMouseJustPressed() && !GdxInputHelper.isSecondaryMouseJustPressed()) {
            return;
        }
        boolean rightClick = GdxInputHelper.isSecondaryMouseJustPressed();

        ItemContainer container = getActiveContainer(session);
        if (container == null) {
            return;
        }

        Rectangle panel = StorageOverlayLayout.getPanelBounds(screenWidth, screenHeight);
        InventoryInteraction cursor = session.getInventoryInteraction();
        ItemRegistry itemRegistry = session.getItemRegistry();

        int containerSlot = StorageOverlayLayout.getContainerSlotAtPoint(mouseX, mouseY, panel);
        if (containerSlot >= 0) {
            ItemStack updated = cursor.handleSlotClick(
                    container.getSlot(containerSlot),
                    itemRegistry,
                    SlotTransferPolicies.playerInventory(),
                    rightClick
            );
            container.setSlot(containerSlot, updated);
            return;
        }

        int inventorySlot = StorageOverlayLayout.getInventorySlotAtPoint(mouseX, mouseY, panel);
        if (inventorySlot >= 0) {
            InventoryUiClicks.handlePlayerInventorySlot(session, inventorySlot, rightClick);
        }
    }

    private void updateChestHover(GameSession session, int screenWidth, int screenHeight) {
        Rectangle panel = StorageOverlayLayout.getPanelBounds(screenWidth, screenHeight);

        Integer newContainerHover = null;
        int containerSlot = StorageOverlayLayout.getContainerSlotAtPoint(mouseX, mouseY, panel);
        if (containerSlot >= 0) {
            newContainerHover = containerSlot;
        }

        Integer newInventoryHover = null;
        if (newContainerHover == null) {
            int inventorySlot = StorageOverlayLayout.getInventorySlotAtPoint(mouseX, mouseY, panel);
            if (inventorySlot >= 0 && !session.getInventory().getSlot(inventorySlot).isEmpty()) {
                newInventoryHover = inventorySlot;
            }
        }

        hoveredContainerSlot = newContainerHover;
        hoveredInventorySlot = newInventoryHover;
    }

    private void handleChipperClick(GameSession session, int screenWidth, int screenHeight) {
        if (!GdxInputHelper.isPrimaryMouseJustPressed() && !GdxInputHelper.isSecondaryMouseJustPressed()) {
            return;
        }
        boolean rightClick = GdxInputHelper.isSecondaryMouseJustPressed();

        WoodChipperMachine machine = session.getWoodChipperManager().get(session.getActiveWoodChipperKey());
        if (machine == null) {
            return;
        }

        Rectangle panel = ProcessingMachineUiLayout.getPanelBounds(screenWidth, screenHeight);
        int machineSlot = ProcessingMachineUiLayout.getMachineSlotAtPoint(mouseX, mouseY, panel);
        if (machineSlot == ProcessingMachineUiLayout.INPUT_SLOT) {
            WoodChipperSlotInteraction.handleInputClick(
                    machine,
                    session.getInventoryInteraction(),
                    session.getItemRegistry(),
                    rightClick
            );
            return;
        }
        if (machineSlot == ProcessingMachineUiLayout.OUTPUT_SLOT) {
            WoodChipperSlotInteraction.handleOutputClick(
                    machine,
                    session.getPlayerBank(),
                    rightClick
            );
            return;
        }

        int inventorySlot = ProcessingMachineUiLayout.getInventorySlotAtPoint(mouseX, mouseY, panel);
        if (inventorySlot >= 0) {
            InventoryUiClicks.handlePlayerInventorySlot(session, inventorySlot, rightClick);
        }
    }

    private void updateChipperHover(GameSession session, int screenWidth, int screenHeight) {
        Rectangle panel = ProcessingMachineUiLayout.getPanelBounds(screenWidth, screenHeight);

        Integer newMachineHover = null;
        int machineSlot = ProcessingMachineUiLayout.getMachineSlotAtPoint(mouseX, mouseY, panel);
        if (machineSlot >= 0) {
            newMachineHover = machineSlot;
        }

        Integer newInventoryHover = null;
        if (newMachineHover == null) {
            int inventorySlot = ProcessingMachineUiLayout.getInventorySlotAtPoint(mouseX, mouseY, panel);
            if (inventorySlot >= 0 && !session.getInventory().getSlot(inventorySlot).isEmpty()) {
                newInventoryHover = inventorySlot;
            }
        }

        hoveredMachineSlot = newMachineHover;
        hoveredInventorySlot = newInventoryHover;
    }

    private ItemContainer getActiveContainer(GameSession session) {
        PlacedObjectKey key = session.getActiveChestKey();
        if (key == null) {
            return null;
        }
        return session.getChestManager().get(key);
    }

    private void drawInventoryOverlay(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            int screenWidth,
            int screenHeight
    ) {
        playerMenuOverlay.draw(
                batch,
                font,
                textures,
                session,
                inputSettings,
                selectedTab,
                hoveredInventorySlot,
                focusedSkill,
                hoveredSkillLevel,
                focusedCollectionCategory,
                hoveredCollectionEntryId,
                focusedCraftingCategory,
                hoveredRecipeId,
                screenWidth,
                screenHeight
        );
        if (selectedTab == PlayerMenuTab.INVENTORY && session.getInventoryInteraction().isCarrying()) {
            itemSlotDrawer.drawCarried(
                    batch,
                    font,
                    textures,
                    session.getItemRegistry(),
                    session.getInventoryInteraction().getCarriedStack(),
                    mouseX,
                    mouseY,
                    screenHeight
            );
        }
    }

    private void drawChestOverlay(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            int screenWidth,
            int screenHeight
    ) {
        ItemContainer container = getActiveContainer(session);
        if (container == null) {
            return;
        }

        storageOverlay.draw(
                batch,
                font,
                textures,
                "Chest",
                container,
                session.getInventory(),
                session.getItemRegistry(),
                screenWidth,
                screenHeight,
                hoveredContainerSlot,
                hoveredInventorySlot
        );
        drawCarriedStackNative(batch, font, textures, session, screenHeight);
    }

    private void drawChipperOverlay(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            int screenWidth,
            int screenHeight
    ) {
        WoodChipperMachine machine = session.getWoodChipperManager().get(session.getActiveWoodChipperKey());
        if (machine == null) {
            return;
        }

        machineOverlay.draw(
                batch,
                font,
                textures,
                "Wood Chipper",
                machine.getInputStack(),
                machine.getOutputStack(),
                machine.getProgressRatio(),
                machine.isProcessing(),
                "Click to deposit (right-click: 1)",
                session.getItemRegistry(),
                session.getInventory(),
                screenWidth,
                screenHeight,
                hoveredMachineSlot,
                hoveredInventorySlot
        );
        drawCarriedStackNative(batch, font, textures, session, screenHeight);
    }

    private void drawCarriedStackNative(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            int screenHeight
    ) {
        if (session.getInventoryInteraction().isCarrying()) {
            itemSlotDrawer.drawCarried(
                    batch,
                    font,
                    textures,
                    session.getItemRegistry(),
                    session.getInventoryInteraction().getCarriedStack(),
                    mouseX,
                    mouseY,
                    screenHeight
            );
        }
    }

    @Override
    public void dispose() {
        // Native overlays hold no disposable GPU resources of their own.
    }
}
