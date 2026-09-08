package lumberjack.interaction;

/**
 * UI callback that asks the player whether to sleep.
 * Deferred LibGDX prompts may return {@code null} while waiting for a click.
 */
public interface SleepPrompt {

    /**
     * @return {@link Boolean#TRUE} to sleep, {@link Boolean#FALSE} to decline,
     *         or {@code null} if the UI is still waiting for a decision
     */
    Boolean confirmSleep();

    /** Clears any deferred sleep UI (e.g. player walked off the bed). */
    default void cancelSleepPrompt() {
    }

    /** @return true when a deferred sleep dialog is on screen */
    default boolean isSleepPromptVisible() {
        return false;
    }
}
