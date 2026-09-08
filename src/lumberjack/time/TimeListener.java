package lumberjack.time;

/**
 * Optional hook for systems that react to the clock (shops, spawns, crops, quests).
 * Register listeners on {@link GameClock} — nothing is required to listen yet.
 */
public interface TimeListener {

    /** Called every in-game minute (once per real-time second at default speed). */
    void onMinuteAdvanced(GameClock clock);

    /** Called when the calendar day increments (midnight rollover). */
    void onDayAdvanced(GameClock clock);

    /** Called when day/night phase changes (e.g. 8:00 PM becomes night). */
    void onPhaseChanged(DayPhase previousPhase, DayPhase newPhase, GameClock clock);
}
