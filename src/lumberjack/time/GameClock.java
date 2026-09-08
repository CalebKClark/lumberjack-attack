package lumberjack.time;

import java.util.ArrayList;
import java.util.List;

import lumberjack.core.GameConfig;

/**
 * Authoritative in-world clock.
 *
 * Tick model:
 * - Time advances in whole in-game minutes.
 * - One minute of game time passes every {@link GameConfig#REAL_MS_PER_GAME_MINUTE} ms of real time.
 * - A full 24-hour day takes 24 real-time minutes.
 *
 * Other systems should query this class (or listen via {@link TimeListener}) instead of tracking their own timers.
 */
public final class GameClock {

    private final DayCycleConfig config;
    private final List<TimeListener> listeners = new ArrayList<>();

    private int day;
    private int hour;
    private int minute;
    private DayPhase phase;

    private double minuteAccumulatorMs;

    public GameClock() {
        this(new DayCycleConfig());
    }

    public GameClock(DayCycleConfig config) {
        this.config = config;
        this.day = config.getStartingDay();
        this.hour = config.getStartingHour();
        this.minute = config.getStartingMinute();
        this.phase = config.getPhase(hour, minute);
    }

    public void addListener(TimeListener listener) {
        listeners.add(listener);
    }

    /**
     * Advances time based on real milliseconds elapsed this frame.
     * Call once per game loop tick while gameplay time should run.
     */
    public void update(double elapsedMs) {
        if (elapsedMs <= 0) {
            return;
        }

        minuteAccumulatorMs += elapsedMs;

        while (minuteAccumulatorMs >= GameConfig.REAL_MS_PER_GAME_MINUTE) {
            minuteAccumulatorMs -= GameConfig.REAL_MS_PER_GAME_MINUTE;
            advanceOneMinute();
        }
    }

    /**
     * In-game minutes from the current time until the next {@link DayCycleConfig#getDayStartHour()} morning.
     */
    public int getMinutesUntilNextMorning() {
        int minutesUntilMidnight = (GameConfig.HOURS_PER_DAY - hour) * GameConfig.MINUTES_PER_HOUR - minute;
        int minutesFromMidnightToMorning = config.getDayStartHour() * GameConfig.MINUTES_PER_HOUR;
        return minutesUntilMidnight + minutesFromMidnightToMorning;
    }

    /**
     * For sleep / skip-day: jump to the next morning at day-start hour.
     *
     * @return in-game minutes skipped
     */
    public int advanceToNextMorning() {
        int skippedMinutes = getMinutesUntilNextMorning();

        day++;
        hour = config.getDayStartHour();
        minute = 0;
        minuteAccumulatorMs = 0;

        DayPhase previousPhase = phase;
        phase = config.getPhase(hour, minute);

        notifyDayAdvanced();
        if (previousPhase != phase) {
            notifyPhaseChanged(previousPhase, phase);
        }
        notifyMinuteAdvanced();

        return skippedMinutes;
    }

    public void resetToTime(int day, int hour, int minute) {
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.phase = config.getPhase(hour, minute);
        this.minuteAccumulatorMs = 0;
    }

    public void reset() {
        resetToTime(config.getStartingDay(), config.getStartingHour(), config.getStartingMinute());
    }

    public int getDay() {
        return day;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public DayPhase getPhase() {
        return phase;
    }

    public boolean isDaytime() {
        return phase == DayPhase.DAY;
    }

    public boolean isNighttime() {
        return phase == DayPhase.NIGHT;
    }

    /** Minutes since midnight on the current day (0–1439). Useful for scheduling events. */
    public int getMinutesSinceMidnight() {
        return hour * GameConfig.MINUTES_PER_HOUR + minute;
    }

    /** Total in-game minutes since the campaign started — handy for unique timestamps. */
    public int getTotalElapsedMinutes() {
        return (day - 1) * GameConfig.MINUTES_PER_DAY + getMinutesSinceMidnight();
    }

    public DayCycleConfig getConfig() {
        return config;
    }

    public String formatHudText() {
        String period = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) {
            displayHour = 12;
        }

        return String.format(
                "Day %d · %d:%02d %s · %s",
                day,
                displayHour,
                minute,
                period,
                phase.getDisplayName()
        );
    }

    private void advanceOneMinute() {
        minute++;

        if (minute >= GameConfig.MINUTES_PER_HOUR) {
            minute = 0;
            hour++;

            if (hour >= GameConfig.HOURS_PER_DAY) {
                hour = 0;
                day++;
                notifyDayAdvanced();
            }
        }

        updatePhase();
        notifyMinuteAdvanced();
    }

    private void updatePhase() {
        DayPhase newPhase = config.getPhase(hour, minute);

        if (newPhase != phase) {
            DayPhase previous = phase;
            phase = newPhase;
            notifyPhaseChanged(previous, newPhase);
        }
    }

    private void notifyMinuteAdvanced() {
        for (TimeListener listener : listeners) {
            listener.onMinuteAdvanced(this);
        }
    }

    private void notifyDayAdvanced() {
        for (TimeListener listener : listeners) {
            listener.onDayAdvanced(this);
        }
    }

    private void notifyPhaseChanged(DayPhase previousPhase, DayPhase newPhase) {
        for (TimeListener listener : listeners) {
            listener.onPhaseChanged(previousPhase, newPhase, this);
        }
    }
}
