package lumberjack.world.map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Loads map grid data from plain-text files in /maps/.
 *
 * Format:
 * first line: width height
 * following lines: space-separated cells ({@code id}, {@code id:rotation}, optional {@code t} marker)
 */
public final class MapLoader {

    private MapLoader() {
    }

    public static TileMap load(String mapId) {
        String resourcePath = "/maps/" + mapId + ".map";

        try (InputStream input = MapLoader.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Map not found: " + resourcePath);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                String header = reader.readLine();
                if (header == null) {
                    throw new IllegalStateException("Map file is empty: " + resourcePath);
                }

                String[] dimensions = header.trim().split("\\s+");
                int width = Integer.parseInt(dimensions[0]);
                int height = Integer.parseInt(dimensions[1]);
                int[][] tiles = new int[height][width];
                int[][] rotations = new int[height][width];

                for (int row = 0; row < height; row++) {
                    String line = reader.readLine();
                    if (line == null) {
                        throw new IllegalStateException("Map row missing at row " + row + ": " + resourcePath);
                    }

                    String[] values = line.trim().split("\\s+");
                    if (values.length != width) {
                        throw new IllegalStateException(
                                "Invalid row width at row " + row + " in map " + mapId
                        );
                    }

                    for (int col = 0; col < width; col++) {
                        tiles[row][col] = MapCellCodec.parseTileId(values[col]);
                        rotations[row][col] = MapCellCodec.parseRotation(values[col]);
                    }
                }

                return new TileMap(mapId, tiles, rotations);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load map: " + mapId, exception);
        }
    }
}
