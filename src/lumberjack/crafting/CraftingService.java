package lumberjack.crafting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lumberjack.collection.PlayerCollections;
import lumberjack.inventory.Inventory;
import lumberjack.ui.hud.GameNotificationManager;

/**
 * Recipe unlock checks and crafting execution.
 */
public final class CraftingService {

    private final RecipeRegistry recipeRegistry;
    private final Set<String> notifiedRecipeIds = new HashSet<>();

    public CraftingService(RecipeRegistry recipeRegistry) {
        this.recipeRegistry = recipeRegistry;
    }

    public RecipeRegistry getRecipeRegistry() {
        return recipeRegistry;
    }

    public boolean isRecipeUnlocked(RecipeDefinition recipe, PlayerCollections collections) {
        String unlockItemId = recipe.getUnlockItemId();
        if (unlockItemId != null) {
            return collections.hasDiscoveredItem(unlockItemId);
        }
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            if (!collections.hasDiscoveredItem(ingredient.itemId())) {
                return false;
            }
        }
        return true;
    }

    public int getUnlockedCount(CraftingCategory category, PlayerCollections collections) {
        int count = 0;
        for (RecipeDefinition recipe : recipeRegistry.getRecipes(category)) {
            if (isRecipeUnlocked(recipe, collections)) {
                count++;
            }
        }
        return count;
    }

    public int getTotalCount(CraftingCategory category) {
        return recipeRegistry.getTotalCount(category);
    }

    /**
     * Shows feed notifications for recipes that became unlocked since the last check.
     */
    public void checkForNewUnlocks(PlayerCollections collections, GameNotificationManager notifications) {
        for (RecipeDefinition recipe : recipeRegistry.getAllRecipes()) {
            String recipeId = recipe.getRecipeId();
            if (notifiedRecipeIds.contains(recipeId)) {
                continue;
            }
            if (!isRecipeUnlocked(recipe, collections)) {
                continue;
            }

            notifiedRecipeIds.add(recipeId);
            notifications.notifyRecipeUnlocked(recipe.getDisplayName());
        }
    }

    /**
     * Marks currently unlocked recipes as already notified (e.g. after loading a save).
     */
    public void syncNotifiedUnlocks(PlayerCollections collections) {
        for (RecipeDefinition recipe : recipeRegistry.getAllRecipes()) {
            if (isRecipeUnlocked(recipe, collections)) {
                notifiedRecipeIds.add(recipe.getRecipeId());
            }
        }
    }

    public void resetNotifiedUnlocks() {
        notifiedRecipeIds.clear();
    }

    public boolean canCraft(RecipeDefinition recipe, Inventory inventory, PlayerCollections collections) {
        if (!isRecipeUnlocked(recipe, collections)) {
            return false;
        }

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            if (inventory.countItem(ingredient.itemId()) < ingredient.quantity()) {
                return false;
            }
        }

        Map<String, Integer> toRemove = new HashMap<>();
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            toRemove.put(ingredient.itemId(), ingredient.quantity());
        }
        return inventory.canFitAfterRemoving(
                recipe.getOutputItemId(),
                recipe.getOutputQuantity(),
                toRemove
        );
    }

    public boolean tryCraft(RecipeDefinition recipe, Inventory inventory, PlayerCollections collections) {
        if (!canCraft(recipe, inventory, collections)) {
            return false;
        }

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            inventory.removeItem(ingredient.itemId(), ingredient.quantity());
        }

        int leftover = inventory.addItem(recipe.getOutputItemId(), recipe.getOutputQuantity());
        return leftover == 0;
    }
}
