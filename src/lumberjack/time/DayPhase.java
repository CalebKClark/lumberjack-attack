package lumberjack.time;

/**
 * Broad time-of-day bucket used by spawning, shops, lighting, and events.
 * More phases (dawn/dusk) can be added later without changing the clock core.
 */
public enum DayPhase {

    DAY("Day"),
    NIGHT("Night");

    private final String displayName;

    DayPhase(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
