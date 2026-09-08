package lumberjack.audio;

/**
 * Catalog of streamed music tracks. Add new songs here as files land under
 * {@code resources/audio/music/}.
 */
public enum MusicTrack {

    LUMBERJACK_ATTACK_OVERTURE(
            "LumberJack_Attack_Overture",
            "audio/music/LumberJack_Attack_Overture.wav"
    );

    private final String id;
    private final String assetPath;

    MusicTrack(String id, String assetPath) {
        this.id = id;
        this.assetPath = assetPath;
    }

    /** Stable in-memory / content id. */
    public String getId() {
        return id;
    }

    /** Path under {@code resources/} (synced to {@code assets/}). */
    public String getAssetPath() {
        return assetPath;
    }

    public static MusicTrack byId(String id) {
        for (MusicTrack track : values()) {
            if (track.id.equals(id)) {
                return track;
            }
        }
        throw new IllegalArgumentException("Unknown music track id: " + id);
    }
}
