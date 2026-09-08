package lumberjack.world.map;

/**
 * Encodes tile id, optional rotation, and optional transition marker in map files.
 * <ul>
 *   <li>{@code 12} — tile 12 at 0°</li>
 *   <li>{@code 12:1} — 90° clockwise ({@code :2}=180°, {@code :3}=270°)</li>
 *   <li>{@code 12t} / {@code 12:1t} — same, with an editor-only transfer marker</li>
 * </ul>
 */
public final class MapCellCodec {

    private MapCellCodec() {
    }

    public static int parseTileId(String token) {
        String body = stripMarkerSuffix(token);
        int colon = body.indexOf(':');
        return Integer.parseInt(colon < 0 ? body : body.substring(0, colon));
    }

    public static int parseRotation(String token) {
        String body = stripMarkerSuffix(token);
        int colon = body.indexOf(':');
        if (colon < 0 || colon == body.length() - 1) {
            return 0;
        }
        return Integer.parseInt(body.substring(colon + 1)) & 3;
    }

    public static boolean parseTransitionMarker(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        char last = token.charAt(token.length() - 1);
        return last == 't' || last == 'T';
    }

    public static String format(int tileId, int rotation) {
        return format(tileId, rotation, false);
    }

    public static String format(int tileId, int rotation, boolean transitionMarker) {
        int rotated = rotation & 3;
        String body = rotated == 0
                ? Integer.toString(tileId)
                : tileId + ":" + rotated;
        return transitionMarker ? body + "t" : body;
    }

    private static String stripMarkerSuffix(String token) {
        return parseTransitionMarker(token) ? token.substring(0, token.length() - 1) : token;
    }
}
