package lumberjack.crafting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads crafting recipes from /crafting/recipes.csv.
 */
public final class RecipeRegistry {

    private static final String DEFAULT_PATH = "/crafting/recipes.csv";

    private final Map<String, RecipeDefinition> recipesById = new HashMap<>();
    private final Map<CraftingCategory, List<RecipeDefinition>> recipesByCategory = new EnumMap<>(CraftingCategory.class);

    public RecipeRegistry() {
        this(DEFAULT_PATH);
    }

    public RecipeRegistry(String resourcePath) {
        for (CraftingCategory category : CraftingCategory.values()) {
            recipesByCategory.put(category, new ArrayList<>());
        }
        load(resourcePath);
    }

    public RecipeDefinition get(String recipeId) {
        RecipeDefinition recipe = recipesById.get(recipeId);
        if (recipe == null) {
            throw new IllegalArgumentException("Unknown recipe: " + recipeId);
        }
        return recipe;
    }

    public List<RecipeDefinition> getRecipes(CraftingCategory category) {
        return List.copyOf(recipesByCategory.get(category));
    }

    public int getTotalCount(CraftingCategory category) {
        return recipesByCategory.get(category).size();
    }

    public List<RecipeDefinition> getAllRecipes() {
        List<RecipeDefinition> recipes = new ArrayList<>(recipesById.values());
        recipes.sort(Comparator.comparingInt(RecipeDefinition::getSortOrder));
        return List.copyOf(recipes);
    }

    private void load(String resourcePath) {
        try (InputStream input = RecipeRegistry.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Recipe data not found: " + resourcePath);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                reader.readLine(); // header

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }

                    String[] parts = parseCsvLine(line);
                    String recipeId = parts[0].trim();
                    String displayName = parts[1].trim();
                    CraftingCategory category = CraftingCategory.fromId(parts[2].trim());
                    String outputItemId = parts[3].trim();
                    int outputQuantity = Integer.parseInt(parts[4].trim());
                    List<RecipeIngredient> ingredients = parseIngredients(parts[5].trim());
                    int sortOrder = Integer.parseInt(parts[6].trim());
                    String unlockItemId = parts.length > 7 ? parts[7].trim() : "";

                    RecipeDefinition recipe = new RecipeDefinition(
                            recipeId,
                            displayName,
                            category,
                            outputItemId,
                            outputQuantity,
                            ingredients,
                            sortOrder,
                            unlockItemId
                    );

                    recipesById.put(recipeId, recipe);
                    recipesByCategory.get(category).add(recipe);
                }
            }

            for (CraftingCategory category : CraftingCategory.values()) {
                recipesByCategory.get(category).sort(Comparator.comparingInt(RecipeDefinition::getSortOrder));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load recipe data: " + resourcePath, exception);
        }
    }

    private static List<RecipeIngredient> parseIngredients(String raw) {
        List<RecipeIngredient> ingredients = new ArrayList<>();
        if (raw.isEmpty()) {
            return ingredients;
        }

        for (String part : raw.split(";")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String[] itemParts = trimmed.split(":");
            if (itemParts.length != 2) {
                throw new IllegalArgumentException("Invalid ingredient format: " + trimmed);
            }

            ingredients.add(new RecipeIngredient(itemParts[0].trim(), Integer.parseInt(itemParts[1].trim())));
        }
        return ingredients;
    }

    private String[] parseCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (character == ',' && !inQuotes) {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(character);
        }
        parts.add(current.toString());
        return parts.toArray(String[]::new);
    }
}
