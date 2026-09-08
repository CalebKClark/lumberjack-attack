package lumberjack.save;

/**
 * Converts player-facing save titles into safe file names on disk.
 */
public final class SaveFileNames {

    private SaveFileNames() {
    }

    public static String toFileName(String saveTitle) {
        String slug = saveTitle.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");

        if (slug.isEmpty()) {
            slug = "untitled";
        }

        if (slug.length() > 48) {
            slug = slug.substring(0, 48);
        }

        return slug + ".save";
    }
}
