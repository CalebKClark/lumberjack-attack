package lumberjack.crafting;

import java.util.List;

/**
 * Immutable template for one craftable recipe.
 */
public final class RecipeDefinition {

    private final String recipeId;
    private final String displayName;
    private final CraftingCategory category;
    private final String outputItemId;
    private final int outputQuantity;
    private final List<RecipeIngredient> ingredients;
    private final int sortOrder;
    /**
     * If set, recipe unlocks when this item is discovered in Collections.
     * If null/blank, unlocks when every ingredient item has been discovered.
     */
    private final String unlockItemId;

    public RecipeDefinition(
            String recipeId,
            String displayName,
            CraftingCategory category,
            String outputItemId,
            int outputQuantity,
            List<RecipeIngredient> ingredients,
            int sortOrder,
            String unlockItemId
    ) {
        this.recipeId = recipeId;
        this.displayName = displayName;
        this.category = category;
        this.outputItemId = outputItemId;
        this.outputQuantity = outputQuantity;
        this.ingredients = List.copyOf(ingredients);
        this.sortOrder = sortOrder;
        this.unlockItemId = unlockItemId == null || unlockItemId.isBlank() ? null : unlockItemId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CraftingCategory getCategory() {
        return category;
    }

    public String getOutputItemId() {
        return outputItemId;
    }

    public int getOutputQuantity() {
        return outputQuantity;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getUnlockItemId() {
        return unlockItemId;
    }
}
