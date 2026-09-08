package lumberjack.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared helper for loading comma-separated content files from the classpath.
 * All game content registries should use this instead of hand-rolling readers.
 */
public final class CsvLoader {

    private CsvLoader() {
    }

    public static List<String[]> readRows(String resourcePath) {
        return readRows(resourcePath, true);
    }

    /**
     * @param skipHeader when true, the first non-blank row is discarded
     */
    public static List<String[]> readRows(String resourcePath, boolean skipHeader) {
        try (InputStream input = openStream(resourcePath);
                BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            List<String[]> rows = new ArrayList<>();
            String line;
            boolean headerSkipped = !skipHeader;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                rows.add(line.split(",", -1));
            }

            return rows;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load CSV: " + resourcePath, exception);
        }
    }

    public static InputStream openStream(String resourcePath) {
        InputStream input = CsvLoader.class.getResourceAsStream(resourcePath);
        if (input == null) {
            throw new IllegalStateException("Resource not found: " + resourcePath);
        }
        return input;
    }

    public static String cell(String[] row, int index) {
        if (row.length <= index) {
            return "";
        }
        return row[index].trim();
    }

    public static boolean parseBoolean(String[] row, int index, boolean defaultValue) {
        String value = cell(row, index);
        if (value.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public static int parseInt(String[] row, int index, int defaultValue) {
        String value = cell(row, index);
        if (value.isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public static double parseDouble(String[] row, int index, double defaultValue) {
        String value = cell(row, index);
        if (value.isEmpty()) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }
}
