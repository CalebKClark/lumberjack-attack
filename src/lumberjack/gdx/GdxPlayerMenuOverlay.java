package lumberjack.gdx;

import java.awt.Rectangle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.collection.CollectionCategory;
import lumberjack.core.GameConfig;
import lumberjack.crafting.CraftingCategory;
import lumberjack.engine.input.InputSettings;
import lumberjack.game.GameSession;
import lumberjack.inventory.Inventory;
import lumberjack.item.ItemStack;
import lumberjack.skills.SkillType;
import lumberjack.ui.hud.InventoryLayout;
import lumberjack.ui.hud.PlayerMenuLayout;
import lumberjack.ui.hud.PlayerMenuTab;

/**
 * Native LibGDX player menu (all tabs).
 */
public final class GdxPlayerMenuOverlay {

    private static final Color DIM = new Color(0f, 0f, 0f, 0.55f);
    private static final Color MENU_PANEL_BG = new Color(0.12f, 0.12f, 0.14f, 0.96f);
    private static final Color MENU_PANEL_BORDER = new Color(0.55f, 0.55f, 0.6f, 1f);
    private static final Color TAB_ACTIVE = new Color(0.28f, 0.28f, 0.34f, 1f);
    private static final Color TAB_INACTIVE = new Color(0.16f, 0.16f, 0.18f, 1f);
    private static final Color TAB_BORDER = new Color(0.4f, 0.4f, 0.45f, 1f);
    private static final Color HUD_TEXT = new Color(0.96f, 0.96f, 0.96f, 1f);
    private static final Color MUTED = new Color(0.7f, 0.72f, 0.78f, 1f);
    private static final Color KEY_LABEL = new Color(1f, 1f, 1f, 0.7f);

    private final GdxItemSlotDrawer slots = new GdxItemSlotDrawer();
    private final GdxStatsTab statsTab = new GdxStatsTab();
    private final GdxSkillsTab skillsTab = new GdxSkillsTab();
    private final GdxCollectionsTab collectionsTab = new GdxCollectionsTab();
    private final GdxCraftingTab craftingTab = new GdxCraftingTab();
    private final GlyphLayout layout = new GlyphLayout();

    public void draw(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            InputSettings inputSettings,
            PlayerMenuTab selectedTab,
            Integer hoveredInventorySlot,
            SkillType focusedSkill,
            Integer hoveredSkillLevel,
            CollectionCategory focusedCollectionCategory,
            String hoveredCollectionEntryId,
            CraftingCategory focusedCraftingCategory,
            String hoveredRecipeId,
            int screenWidth,
            int screenHeight
    ) {
        GdxBatchUtils.drawSolid(batch, textures, DIM, 0, 0, screenWidth, screenHeight);

        Rectangle menuPanel = PlayerMenuLayout.getMenuPanelBounds(screenWidth, screenHeight);
        drawRect(batch, textures, MENU_PANEL_BG, menuPanel, screenHeight);
        drawBorder(batch, textures, MENU_PANEL_BORDER, menuPanel, screenHeight);
        drawTabs(batch, font, textures, menuPanel, selectedTab, screenHeight);

        Rectangle content = PlayerMenuLayout.getContentBounds(menuPanel);
        switch (selectedTab) {
            case INVENTORY -> drawInventoryTab(
                    batch,
                    font,
                    textures,
                    session,
                    inputSettings,
                    content,
                    hoveredInventorySlot,
                    screenWidth,
                    screenHeight
            );
            case STATS -> statsTab.draw(batch, font, textures, session, content, screenHeight);
            case SKILLS -> skillsTab.draw(
                    batch,
                    font,
                    textures,
                    session,
                    content,
                    focusedSkill,
                    hoveredSkillLevel,
                    screenHeight
            );
            case COLLECTIONS -> collectionsTab.draw(
                    batch,
                    font,
                    textures,
                    session,
                    content,
                    focusedCollectionCategory,
                    hoveredCollectionEntryId,
                    screenHeight
            );
            case CRAFTING -> craftingTab.draw(
                    batch,
                    font,
                    textures,
                    session,
                    content,
                    focusedCraftingCategory,
                    hoveredRecipeId,
                    screenHeight
            );
        }
    }

    private void drawTabs(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            Rectangle menuPanel,
            PlayerMenuTab selectedTab,
            int screenHeight
    ) {
        for (int tabIndex = 0; tabIndex < PlayerMenuTab.values().length; tabIndex++) {
            PlayerMenuTab tab = PlayerMenuTab.fromIndex(tabIndex);
            Rectangle bounds = PlayerMenuLayout.getTabBounds(menuPanel, tabIndex);
            boolean active = tab == selectedTab;

            drawRect(batch, textures, active ? TAB_ACTIVE : TAB_INACTIVE, bounds, screenHeight);
            drawBorder(batch, textures, TAB_BORDER, bounds, screenHeight);

            font.setColor(active ? HUD_TEXT : MUTED);
            layout.setText(font, tab.getLabel());
            float textX = bounds.x + (bounds.width - layout.width) / 2f;
            float textY = GdxUiCoords.bottom(bounds, screenHeight) + (bounds.height + layout.height) / 2f;
            font.draw(batch, tab.getLabel(), textX, textY);
        }
    }

    private void drawInventoryTab(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            InputSettings inputSettings,
            Rectangle content,
            Integer hoveredInventorySlot,
            int screenWidth,
            int screenHeight
    ) {
        float contentBottom = GdxUiCoords.bottom(content, screenHeight);
        font.setColor(HUD_TEXT);
        font.draw(batch, "Inventory", content.x + 24, contentBottom + content.height - 24);
        font.setColor(MUTED);
        font.draw(
                batch,
                "Left click: move stacks · Right click: half / one",
                content.x + 24,
                contentBottom + content.height - 52
        );

        Inventory inventory = session.getInventory();
        for (int slotIndex = 0; slotIndex < GameConfig.INVENTORY_SLOTS; slotIndex++) {
            Rectangle bounds = InventoryLayout.getSlotBounds(slotIndex, content);
            boolean highlightHotbar = inventory.isHotbarSlot(slotIndex)
                    && slotIndex == inventory.getSelectedHotbarSlot();
            slots.drawTopDownSlot(
                    batch,
                    font,
                    textures,
                    session.getItemRegistry(),
                    inventory.getSlot(slotIndex),
                    bounds,
                    screenHeight,
                    highlightHotbar
            );

            if (inventory.isHotbarSlot(slotIndex)) {
                String keyLabel = inputSettings.getHotbarKeyLabel(slotIndex);
                if (keyLabel != null && !keyLabel.isEmpty()) {
                    font.setColor(KEY_LABEL);
                    font.draw(
                            batch,
                            keyLabel,
                            bounds.x + 6,
                            GdxUiCoords.bottom(bounds, screenHeight) + bounds.height - 8
                    );
                }
            }
        }

        if (hoveredInventorySlot != null && hoveredInventorySlot >= 0) {
            ItemStack hoveredStack = inventory.getSlot(hoveredInventorySlot);
            if (!hoveredStack.isEmpty()) {
                slots.drawTooltipForItem(
                        batch,
                        font,
                        textures,
                        session.getItemRegistry().get(hoveredStack.getItemId()),
                        InventoryLayout.getSlotBounds(hoveredInventorySlot, content),
                        screenWidth,
                        screenHeight
                );
            }
        }
    }

    private static void drawRect(
            SpriteBatch batch,
            GdxTextureCache textures,
            Color color,
            Rectangle topDown,
            int screenHeight
    ) {
        GdxBatchUtils.drawSolid(
                batch,
                textures,
                color,
                topDown.x,
                GdxUiCoords.bottom(topDown, screenHeight),
                topDown.width,
                topDown.height
        );
    }

    private static void drawBorder(
            SpriteBatch batch,
            GdxTextureCache textures,
            Color color,
            Rectangle topDown,
            int screenHeight
    ) {
        float left = topDown.x;
        float bottom = GdxUiCoords.bottom(topDown, screenHeight);
        GdxBatchUtils.drawSolid(batch, textures, color, left, bottom, topDown.width, 2);
        GdxBatchUtils.drawSolid(batch, textures, color, left, bottom + topDown.height - 2, topDown.width, 2);
        GdxBatchUtils.drawSolid(batch, textures, color, left, bottom, 2, topDown.height);
        GdxBatchUtils.drawSolid(batch, textures, color, left + topDown.width - 2, bottom, 2, topDown.height);
    }
}
