package lumberjack.interaction;

/**
 * UI callback that asks the player whether to eat a held food item.
 */
public interface EatPrompt {

    /**
     * Opens (or keeps open) the eat confirmation for {@code itemName}.
     *
     * @return {@link Boolean#TRUE} to eat, {@link Boolean#FALSE} to decline,
     *         or {@code null} while waiting for input
     */
    Boolean confirmEat(String itemName);

    void cancelEatPrompt();

    boolean isEatPromptVisible();
}
