package lumberjack.crafting;

/**
 * One material requirement for a crafting recipe.
 */
public record RecipeIngredient(String itemId, int quantity) {
}
