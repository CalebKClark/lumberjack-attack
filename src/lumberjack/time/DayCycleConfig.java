package lumberjack.time;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads day/night boundaries and default start time from /time/day_cycle.csv.
 * Future seasons, festivals, and weather can extend this file or companion data.
 */
public final class DayCycleConfig {

    private static final String DEFAULT_PATH = "/time/day_cycle.csv";

    private final int dayStartHour;
    private final int nightStartHour;
    private final int startingDay;
    private final int startingHour;
    private final int startingMinute;

    public DayCycleConfig() {
        this(DEFAULT_PATH);
    }

    public DayCycleConfig(String resourcePath) {
        Map<String, String> values = loadKeyValueCsv(resourcePath);

        dayStartHour = parseHour(values, "day_start_hour");
        nightStartHour = parseHour(values, "night_start_hour");
        startingDay = Integer.parseInt(values.get("starting_day"));
        startingHour = parseHour(values, "starting_hour");
        startingMinute = Integer.parseInt(values.get("starting_minute"));

        validate();
    }

    public int getDayStartHour() {
        return dayStartHour;
    }

    public int getNightStartHour() {
        return nightStartHour;
    }

    public int getStartingDay() {
        return startingDay;
    }

    public int getStartingHour() {
        return startingHour;
    }

    public int getStartingMinute() {
        return startingMinute;
    }

    /**
     * Resolves whether a 24h clock reading is day or night using configurable boundaries.
     * Supports ranges that wrap midnight (e.g. night 20:00 -> 06:00).
     */
    public DayPhase getPhase(int hour, int minute) {
        int minutesSinceMidnight = hour * 60 + minute;
        int dayStart = dayStartHour * 60;
        int nightStart = nightStartHour * 60;

        if (dayStart < nightStart) {
            boolean isDay = minutesSinceMidnight >= dayStart && minutesSinceMidnight < nightStart;
            return isDay ? DayPhase.DAY : DayPhase.NIGHT;
        }

        // Wrapped schedule (uncommon but supported).
        boolean isDay = minutesSinceMidnight >= dayStart || minutesSinceMidnight < nightStart;
        return isDay ? DayPhase.DAY : DayPhase.NIGHT;
    }

    private void validate() {
        if (startingDay < 1) {
            throw new IllegalStateException("starting_day must be >= 1");
        }
        if (startingMinute < 0 || startingMinute >= 60) {
            throw new IllegalStateException("starting_minute must be 0-59");
        }
    }

    private int parseHour(Map<String, String> values, String key) {
        int hour = Integer.parseInt(values.get(key));
        if (hour < 0 || hour >= 24) {
            throw new IllegalStateException(key + " must be 0-23");
        }
        return hour;
    }

    private Map<String, String> loadKeyValueCsv(String resourcePath) {
        Map<String, String> values = new HashMap<>();

        try (InputStream input = DayCycleConfig.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Day cycle config not found: " + resourcePath);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                reader.readLine(); // header

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }

                    String[] parts = line.split(",");
                    values.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load day cycle config: " + resourcePath, exception);
        }

        return values;
    }
}
