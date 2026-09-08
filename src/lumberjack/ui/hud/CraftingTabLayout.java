package lumberjack.ui.hud;

import java.awt.Rectangle;
import java.util.List;

import lumberjack.crafting.CraftingCategory;
import lumberjack.crafting.RecipeDefinition;

/**
 * Layout helpers for the Crafting tab.
 */
public final class CraftingTabLayout {

    private static final int ROW_HEIGHT = 72;
    private static final int TOOLTIP_LINE_HEIGHT = 18;
    private static final int TOOLTIP_PADDING = 10;
    private static final int TOOLTIP_WIDTH = 220;

    private CraftingTabLayout() {
    }

    public static Rectangle getCategoryRowBounds(Rectangle content, int categoryIndex) {
        int y = content.y + 88 + categoryIndex * ROW_HEIGHT;
        return new Rectangle(content.x + 32, y, content.width - 64, ROW_HEIGHT - 8);
    }

    public static CraftingCategory getCategoryAtPoint(int x, int y, Rectangle content) {
        CraftingCategory[] categories = CraftingCategory.values();
        for (int index = 0; index < categories.length; index++) {
            if (getCategoryRowBounds(content, index).contains(x, y)) {
                return categories[index];
            }
        }
        return null;
    }

    public static Rectangle getBackButtonBounds(Rectangle content) {
        return new Rectangle(content.x + 24, content.y + 20, 90, 32);
    }

    public static Rectangle getRecipeRowBounds(Rectangle content, int recipeIndex) {
        int y = content.y + 88 + recipeIndex * 52;
        return new Rectangle(content.x + 32, y, content.width - 64, 44);
    }

    public static Rectangle getIngredientTooltipBounds(Rectangle content, Rectangle anchorRow, int ingredientCount) {
        int boxHeight = TOOLTIP_PADDING * 2 + ingredientCount * TOOLTIP_LINE_HEIGHT;

        int boxX = anchorRow.x + anchorRow.width + 12;
        int boxY = anchorRow.y;
        if (boxX + TOOLTIP_WIDTH > content.x + content.width - 8) {
            boxX = anchorRow.x - TOOLTIP_WIDTH - 12;
        }
        if (boxY + boxHeight > content.y + content.height - 8) {
            boxY = content.y + content.height - boxHeight - 8;
        }

        return new Rectangle(boxX, boxY, TOOLTIP_WIDTH, boxHeight);
    }

    public static int getTooltipPadding() {
        return TOOLTIP_PADDING;
    }

    public static int getTooltipLineHeight() {
        return TOOLTIP_LINE_HEIGHT;
    }

    public static int getTooltipWidth() {
        return TOOLTIP_WIDTH;
    }

    public static String getRecipeIdAtPoint(
            int x,
            int y,
            Rectangle content,
            List<RecipeDefinition> recipes
    ) {
        for (int index = 0; index < recipes.size(); index++) {
            RecipeDefinition recipe = recipes.get(index);
            Rectangle row = getRecipeRowBounds(content, index);
            if (row.contains(x, y)) {
                return recipe.getRecipeId();
            }

            Rectangle tooltip = getIngredientTooltipBounds(content, row, recipe.getIngredients().size());
            if (tooltip.contains(x, y)) {
                return recipe.getRecipeId();
            }
        }
        return null;
    }
}
