package lumberjack.gdx;

import java.awt.Rectangle;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.collection.PlayerCollections;
import lumberjack.crafting.CraftingCategory;
import lumberjack.crafting.CraftingService;
import lumberjack.crafting.RecipeDefinition;
import lumberjack.crafting.RecipeIngredient;
import lumberjack.game.GameSession;
import lumberjack.item.ItemRegistry;
import lumberjack.ui.hud.CraftingTabLayout;

/**
 * Native Crafting tab.
 */
public final class GdxCraftingTab {

    private static final Color ENOUGH = new Color(0.39f, 0.82f, 0.47f, 1f);
    private static final Color NOT_ENOUGH = new Color(0.9f, 0.37f, 0.37f, 1f);

    private final GdxMenuUi ui = new GdxMenuUi();

    public void draw(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            Rectangle content,
            CraftingCategory focusedCategory,
            String hoveredRecipeId,
            int screenHeight
    ) {
        if (focusedCategory == null) {
            drawCategoryList(batch, font, textures, session, content, screenHeight);
        } else {
            drawCategoryDetail(batch, font, textures, session, content, focusedCategory, hoveredRecipeId, screenHeight);
        }
    }

    private void drawCategoryList(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            Rectangle content,
            int screenHeight
    ) {
        CraftingService craftingService = session.getCraftingService();
        PlayerCollections collections = session.getPlayerCollections();
        float top = GdxUiCoords.bottom(content, screenHeight) + content.height;

        ui.drawText(batch, font, "Crafting", GdxMenuUi.HUD_TEXT, content.x + 24, top - 24);
        ui.drawText(batch, font, "Click a category to view recipes", GdxMenuUi.SUBTEXT, content.x + 24, top - 52);

        CraftingCategory[] categories = CraftingCategory.values();
        for (int index = 0; index < categories.length; index++) {
            CraftingCategory category = categories[index];
            Rectangle row = CraftingTabLayout.getCategoryRowBounds(content, index);
            ui.drawAccentRow(batch, textures, row, accentColor(category), screenHeight);

            int unlocked = craftingService.getUnlockedCount(category, collections);
            int total = craftingService.getTotalCount(category);
            String categoryLabel = category.getDisplayName() + " Recipes";

            float rowBottom = GdxUiCoords.bottom(row, screenHeight);
            ui.drawText(batch, font, categoryLabel, GdxMenuUi.HUD_TEXT, row.x + 20, rowBottom + row.height - 14);
            ui.drawText(
                    batch,
                    font,
                    categoryLabel + " Unlocked: " + unlocked + "/" + total,
                    GdxMenuUi.SUBTEXT,
                    row.x + 20,
                    rowBottom + 16
            );
        }
    }

    private void drawCategoryDetail(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            Rectangle content,
            CraftingCategory category,
            String hoveredRecipeId,
            int screenHeight
    ) {
        CraftingService craftingService = session.getCraftingService();
        PlayerCollections collections = session.getPlayerCollections();
        List<RecipeDefinition> recipes = craftingService.getRecipeRegistry().getRecipes(category);
        String categoryLabel = category.getDisplayName() + " Recipes";
        float top = GdxUiCoords.bottom(content, screenHeight) + content.height;

        ui.drawBackButton(batch, font, textures, CraftingTabLayout.getBackButtonBounds(content), screenHeight);
        ui.drawText(batch, font, categoryLabel, GdxMenuUi.HUD_TEXT, content.x + 130, top - 24);
        ui.drawText(
                batch,
                font,
                "Unlocked: " + craftingService.getUnlockedCount(category, collections)
                        + "/" + craftingService.getTotalCount(category),
                GdxMenuUi.SUBTEXT,
                content.x + 130,
                top - 52
        );

        RecipeDefinition hoveredRecipe = null;
        Rectangle hoveredBounds = null;

        for (int index = 0; index < recipes.size(); index++) {
            RecipeDefinition recipe = recipes.get(index);
            Rectangle row = CraftingTabLayout.getRecipeRowBounds(content, index);
            boolean unlocked = craftingService.isRecipeUnlocked(recipe, collections);

            ui.drawRect(batch, textures, GdxMenuUi.ROW_BG, row, screenHeight);
            ui.drawBorder(batch, textures, GdxMenuUi.ROW_BORDER, row, screenHeight);

            float size = 28;
            float left = row.x + 12;
            float bottom = GdxUiCoords.bottom(row, screenHeight) + (row.height - size) / 2f;
            if (unlocked) {
                ui.drawItemSwatch(
                        batch,
                        textures,
                        session.getItemRegistry(),
                        recipe.getOutputItemId(),
                        left,
                        bottom,
                        size
                );
            } else {
                ui.drawItemSwatch(batch, textures, session.getItemRegistry(), null, left, bottom, size);
            }

            ui.drawText(
                    batch,
                    font,
                    unlocked ? recipe.getDisplayName() : "???",
                    unlocked ? GdxMenuUi.HUD_TEXT : GdxMenuUi.UNKNOWN_TEXT,
                    row.x + 52,
                    GdxUiCoords.bottom(row, screenHeight) + (row.height + ui.measureHeight(font, "A")) / 2f
            );

            if (recipe.getRecipeId().equals(hoveredRecipeId) && unlocked) {
                hoveredRecipe = recipe;
                hoveredBounds = row;
            }
        }

        if (hoveredRecipe != null && hoveredBounds != null) {
            drawIngredientTooltip(batch, font, textures, session, content, hoveredRecipe, hoveredBounds, screenHeight);
        }
    }

    private void drawIngredientTooltip(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            Rectangle content,
            RecipeDefinition recipe,
            Rectangle anchor,
            int screenHeight
    ) {
        ItemRegistry itemRegistry = session.getItemRegistry();
        int padding = CraftingTabLayout.getTooltipPadding();
        int lineHeight = CraftingTabLayout.getTooltipLineHeight();
        int boxWidth = CraftingTabLayout.getTooltipWidth();
        int boxHeight = padding * 2 + recipe.getIngredients().size() * lineHeight;

        Rectangle tooltipBounds = CraftingTabLayout.getIngredientTooltipBounds(
                content,
                anchor,
                recipe.getIngredients().size()
        );

        ui.drawRect(batch, textures, GdxMenuUi.TOOLTIP_BG, tooltipBounds, screenHeight);
        ui.drawBorder(batch, textures, GdxMenuUi.TOOLTIP_BORDER, tooltipBounds, screenHeight);

        float textY = GdxUiCoords.bottom(tooltipBounds, screenHeight) + tooltipBounds.height - padding;
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            int owned = session.getInventory().countItem(ingredient.itemId());
            String itemName = itemRegistry.get(ingredient.itemId()).getName();
            String line = itemName + " " + owned + "/" + ingredient.quantity();
            ui.drawText(
                    batch,
                    font,
                    line,
                    owned >= ingredient.quantity() ? ENOUGH : NOT_ENOUGH,
                    tooltipBounds.x + padding,
                    textY
            );
            textY -= lineHeight;
        }
    }

    private static Color accentColor(CraftingCategory category) {
        return switch (category) {
            case FORAGING -> new Color(0.55f, 0.35f, 0.17f, 1f);
            case COMBAT -> new Color(0.82f, 0.37f, 0.37f, 1f);
            case FISHING -> new Color(0.35f, 0.61f, 0.88f, 1f);
            case MISC -> new Color(0.67f, 0.55f, 0.82f, 1f);
        };
    }
}
