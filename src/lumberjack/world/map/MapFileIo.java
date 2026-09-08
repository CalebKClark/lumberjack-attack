package lumberjack.world.map;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Disk I/O for {@code resources/maps/*.map} used by the world editor.
 */
public final class MapFileIo {

    private static final Path MAPS_DIR = Paths.get("resources", "maps");
    private static final Path ASSETS_MAPS_DIR = Paths.get("assets", "maps");

    private MapFileIo() {
    }

    public static Path mapsDirectory() {
        return MAPS_DIR;
    }

    public static List<String> listMapIds() {
        if (!Files.isDirectory(MAPS_DIR)) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(MAPS_DIR, "*.map")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                ids.add(fileName.substring(0, fileName.length() - 4));
            }
        } catch (IOException exception) {
            return List.of();
        }
        Collections.sort(ids);
        return ids;
    }

    public static EditableTileMap load(String mapId) throws IOException {
        Path path = MAPS_DIR.resolve(mapId + ".map");
        if (!Files.exists(path)) {
            throw new IOException("Map not found: " + path);
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IOException("Map file is empty: " + path);
        }
        String[] dimensions = lines.get(0).trim().split("\\s+");
        int width = Integer.parseInt(dimensions[0]);
        int height = Integer.parseInt(dimensions[1]);
                int[][] tiles = new int[height][width];
                int[][] rotations = new int[height][width];
                boolean[][] markers = new boolean[height][width];
                for (int row = 0; row < height; row++) {
                    if (row + 1 >= lines.size()) {
                        throw new IOException("Map row missing at row " + row);
                    }
                    String[] values = lines.get(row + 1).trim().split("\\s+");
                    if (values.length != width) {
                        throw new IOException("Invalid row width at row " + row);
                    }
                    for (int col = 0; col < width; col++) {
                        tiles[row][col] = MapCellCodec.parseTileId(values[col]);
                        rotations[row][col] = MapCellCodec.parseRotation(values[col]);
                        markers[row][col] = MapCellCodec.parseTransitionMarker(values[col]);
                    }
                }
                return new EditableTileMap(mapId, tiles, rotations, markers);
    }

    public static void save(EditableTileMap map) throws IOException {
        Files.createDirectories(MAPS_DIR);
        String content = map.toFileContents();
        Path primary = MAPS_DIR.resolve(map.getMapId() + ".map");
        Files.writeString(primary, content, StandardCharsets.UTF_8);
        if (Files.isDirectory(ASSETS_MAPS_DIR) || Files.exists(Paths.get("assets"))) {
            Files.createDirectories(ASSETS_MAPS_DIR);
            Files.writeString(ASSETS_MAPS_DIR.resolve(map.getMapId() + ".map"), content, StandardCharsets.UTF_8);
        }
    }

    public static boolean isValidMapId(String mapId) {
        if (mapId == null || mapId.isBlank()) {
            return false;
        }
        String trimmed = mapId.trim();
        if (trimmed.length() > 40) {
            return false;
        }
        return trimmed.matches("[a-zA-Z][a-zA-Z0-9_]*");
    }

    public static String normalizeMapId(String mapId) {
        return mapId == null ? "" : mapId.trim().toLowerCase(Locale.ROOT);
    }
}
