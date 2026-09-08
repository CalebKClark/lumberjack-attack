package lumberjack.game;

import lumberjack.collection.CollectionCategory;
import lumberjack.collection.CollectionEntryDefinition;
import lumberjack.collection.CollectionRegistry;
import lumberjack.collection.DiscoveryType;
import lumberjack.crafting.RecipeDefinition;
import lumberjack.crafting.RecipeRegistry;
import lumberjack.entity.enemy.EnemyRegistry;
import lumberjack.item.ItemRegistry;

/**
 * Startup validation so broken content fails fast instead of at runtime.
 */
public final class ContentValidator {

    private ContentValidator() {
    }

    public static void validate(
            ItemRegistry itemRegistry,
            RecipeRegistry recipeRegistry,
            CollectionRegistry collectionRegistry,
            EnemyRegistry enemyRegistry
    ) {
        validateRecipes(itemRegistry, recipeRegistry);
        validateCollections(itemRegistry, collectionRegistry, enemyRegistry);
    }

    private static void validateRecipes(ItemRegistry itemRegistry, RecipeRegistry recipeRegistry) {
        for (RecipeDefinition recipe : recipeRegistry.getAllRecipes()) {
            String outputItemId = recipe.getOutputItemId();
            if (!itemRegistry.exists(outputItemId)) {
                throw new IllegalStateException(
                        "Recipe '" + recipe.getRecipeId() + "' output item not found: " + outputItemId
                );
            }
            for (var ingredient : recipe.getIngredients()) {
                if (!itemRegistry.exists(ingredient.itemId())) {
                    throw new IllegalStateException(
                            "Recipe '" + recipe.getRecipeId() + "' ingredient not found: " + ingredient.itemId()
                    );
                }
            }
            String unlockItemId = recipe.getUnlockItemId();
            if (unlockItemId != null && !itemRegistry.exists(unlockItemId)) {
                throw new IllegalStateException(
                        "Recipe '" + recipe.getRecipeId() + "' unlock item not found: " + unlockItemId
                );
            }
        }
    }

    private static void validateCollections(
            ItemRegistry itemRegistry,
            CollectionRegistry collectionRegistry,
            EnemyRegistry enemyRegistry
    ) {
        for (CollectionCategory category : CollectionCategory.values()) {
            for (CollectionEntryDefinition entry : collectionRegistry.getEntries(category)) {
                validateCollectionEntry(itemRegistry, enemyRegistry, entry);
            }
        }
    }

    private static void validateCollectionEntry(
            ItemRegistry itemRegistry,
            EnemyRegistry enemyRegistry,
            CollectionEntryDefinition entry
    ) {
        String discoveryId = entry.getDiscoveryId();
        if (discoveryId == null || discoveryId.isBlank()) {
            return;
        }
        if (entry.getDiscoveryType() == DiscoveryType.ITEM && !itemRegistry.exists(discoveryId)) {
            System.err.println("[content warning] Collection entry references unknown item: " + discoveryId);
        }
        if (entry.getDiscoveryType() == DiscoveryType.ENEMY && !enemyRegistry.exists(discoveryId)) {
            System.err.println("[content warning] Collection entry references unknown enemy: " + discoveryId);
        }
    }
}
