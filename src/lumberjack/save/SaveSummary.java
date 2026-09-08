package lumberjack.save;

/**
 * Summary of one save file for the scrollable saves menu.
 */
public final class SaveSummary {

    private final String fileName;
    private final String saveTitle;
    private final String detailText;
    private final long savedAtMillis;
    private final boolean loadable;

    public SaveSummary(
            String fileName,
            String saveTitle,
            String detailText,
            long savedAtMillis,
            boolean loadable
    ) {
        this.fileName = fileName;
        this.saveTitle = saveTitle;
        this.detailText = detailText;
        this.savedAtMillis = savedAtMillis;
        this.loadable = loadable;
    }

    public static SaveSummary fromLoadedSave(String fileName, SaveData data) {
        String title = formatTitle(data.getSaveTitle());
        String period = data.getHour() >= 12 ? "PM" : "AM";
        int displayHour = toDisplayHour(data.getHour());
        String detail = String.format("Day %d · %d:%02d %s", data.getDay(), displayHour, data.getMinute(), period);

        return new SaveSummary(fileName, title, detail, data.getSavedAtMillis(), true);
    }

    public static SaveSummary corrupt(String fileName) {
        return new SaveSummary(fileName, fileName, "Corrupt save", 0L, false);
    }

    public String getFileName() {
        return fileName;
    }

    public String getSaveTitle() {
        return saveTitle;
    }

    public String getDetailText() {
        return detailText;
    }

    public long getSavedAtMillis() {
        return savedAtMillis;
    }

    public boolean isLoadable() {
        return loadable;
    }

    public String getButtonLabel() {
        return saveTitle + " — " + detailText;
    }

    private static String formatTitle(String saveTitle) {
        if (saveTitle == null || saveTitle.isBlank()) {
            return "Untitled";
        }
        return saveTitle.trim();
    }

    private static int toDisplayHour(int hour) {
        int displayHour = hour % 12;
        return displayHour == 0 ? 12 : displayHour;
    }
}
