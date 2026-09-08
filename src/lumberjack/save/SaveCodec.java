package lumberjack.save;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import lumberjack.world.map.MapCellCodec;

/**
 * Reads and writes {@link SaveData} as a human-readable text file.
 * Format is custom but stable — bump {@link SaveData#CURRENT_VERSION} when it changes.
 */
public final class SaveCodec {

    private SaveCodec() {
    }

    public static void write(Path file, SaveData data) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("version=" + data.getVersion());
            writer.newLine();
            writer.write("title=" + data.getSaveTitle());
            writer.newLine();
            writer.write("map_id=" + data.getMapId());
            writer.newLine();
            writer.write("saved_at=" + data.getSavedAtMillis());
            writer.newLine();

            writer.write("[player]");
            writer.newLine();
            writer.write("x=" + data.getPlayerX());
            writer.newLine();
            writer.write("y=" + data.getPlayerY());
            writer.newLine();
            writer.write("health=" + data.getPlayerHealth());
            writer.newLine();
            writer.write("energy=" + data.getPlayerEnergy());
            writer.newLine();
            writer.write("wood_chips=" + data.getWoodChips());
            writer.newLine();
            writer.write("pickup_magnet_tiles=" + data.getPickupMagnetTiles());
            writer.newLine();

            writer.write("[clock]");
            writer.newLine();
            writer.write("day=" + data.getDay());
            writer.newLine();
            writer.write("hour=" + data.getHour());
            writer.newLine();
            writer.write("minute=" + data.getMinute());
            writer.newLine();

            writer.write("[inventory]");
            writer.newLine();
            writer.write("selected_hotbar=" + data.getSelectedHotbarSlot());
            writer.newLine();
            for (SaveData.InventoryEntry entry : data.getInventoryEntries()) {
                writer.write("slot=" + entry.getSlotIndex() + "," + entry.getItemId() + "," + entry.getQuantity());
                writer.newLine();
            }

            writer.write("[maps]");
            writer.newLine();
            for (SaveData.MapEntry mapEntry : data.getMapStates()) {
                writeMapBlock(writer, mapEntry.getMapId(), mapEntry.getTiles(), mapEntry.getRotations());
            }

            writer.write("[drops]");
            writer.newLine();
            for (SaveData.DropEntry drop : data.getDrops()) {
                String mapId = drop.getMapId() == null ? data.getMapId() : drop.getMapId();
                writer.write("drop="
                        + mapId + ","
                        + drop.getItemId() + ","
                        + drop.getQuantity() + ","
                        + drop.getCenterX() + ","
                        + drop.getCenterY());
                writer.newLine();
            }

            writer.write("[regrowth]");
            writer.newLine();
            for (SaveData.RegrowthEntry regrowth : data.getRegrowths()) {
                String mapId = regrowth.getMapId() == null ? "homestead" : regrowth.getMapId();
                writer.write("regrow="
                        + mapId + ","
                        + regrowth.getCol() + ","
                        + regrowth.getRow() + ","
                        + regrowth.getTileId() + ","
                        + regrowth.getRemainingMs());
                writer.newLine();
            }

            writer.write("[enemies]");
            writer.newLine();
            for (SaveData.EnemyEntry enemy : data.getEnemies()) {
                writer.write("enemy="
                        + enemy.getMapId() + ","
                        + enemy.getEnemyId() + ","
                        + enemy.getX() + ","
                        + enemy.getY() + ","
                        + enemy.getHealth());
                writer.newLine();
            }

            writer.write("[placed_objects]");
            writer.newLine();
            for (SaveData.PlacedObjectEntry placed : data.getPlacedObjects()) {
                writer.write("placed="
                        + placed.getMapId() + ","
                        + placed.getItemId() + ","
                        + placed.getCol() + ","
                        + placed.getRow());
                writer.newLine();
            }

            writer.write("[wood_chippers]");
            writer.newLine();
            for (SaveData.WoodChipperEntry chipper : data.getWoodChippers()) {
                writer.write("chipper="
                        + chipper.getMapId() + ","
                        + chipper.getCol() + ","
                        + chipper.getRow() + ","
                        + chipper.getInputItemId() + ","
                        + chipper.getInputQuantity() + ","
                        + chipper.getOutputItemId() + ","
                        + chipper.getOutputQuantity() + ","
                        + chipper.getProgressMs() + ","
                        + chipper.isProcessing());
                writer.newLine();
            }

            writer.write("[chests]");
            writer.newLine();
            for (SaveData.ChestSlotEntry chestSlot : data.getChestSlots()) {
                writer.write("slot="
                        + chestSlot.getMapId() + ","
                        + chestSlot.getCol() + ","
                        + chestSlot.getRow() + ","
                        + chestSlot.getSlotIndex() + ","
                        + chestSlot.getItemId() + ","
                        + chestSlot.getQuantity());
                writer.newLine();
            }

            writer.write("[skills]");
            writer.newLine();
            for (SaveData.SkillEntry skill : data.getSkillEntries()) {
                writer.write("skill=" + skill.getSkillId() + "," + skill.getTotalXp());
                writer.newLine();
            }

            writer.write("[collections]");
            writer.newLine();
            for (SaveData.CollectionEntry entry : data.getCollectionEntries()) {
                writer.write("discovered=" + entry.getEntryId());
                writer.newLine();
            }
        }
    }

    public static SaveData read(Path file) throws IOException {
        SaveData data = new SaveData();
        List<SaveData.InventoryEntry> inventoryEntries = new ArrayList<>();
        List<SaveData.DropEntry> drops = new ArrayList<>();
        List<SaveData.RegrowthEntry> regrowths = new ArrayList<>();
        List<SaveData.EnemyEntry> enemies = new ArrayList<>();
        List<SaveData.PlacedObjectEntry> placedObjects = new ArrayList<>();
        List<SaveData.WoodChipperEntry> woodChippers = new ArrayList<>();
        List<SaveData.ChestSlotEntry> chestSlots = new ArrayList<>();
        List<SaveData.SkillEntry> skillEntries = new ArrayList<>();
        List<SaveData.CollectionEntry> collectionEntries = new ArrayList<>();
        List<SaveData.MapEntry> mapStates = new ArrayList<>();

        String section = "";
        boolean enemiesSectionPresent = false;
        MapBlockReader legacyMap = new MapBlockReader();
        MapBlockReader mapsSectionReader = new MapBlockReader();

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("[") && line.endsWith("]")) {
                    if ("[maps]".equals(section)) {
                        mapsSectionReader.flush(mapStates);
                    } else if ("[map]".equals(section)) {
                        legacyMap.flush(mapStates, data.getMapId());
                    }

                    section = line;
                    if ("[enemies]".equals(section)) {
                        enemiesSectionPresent = true;
                    }
                    continue;
                }

                switch (section) {
                    case "" -> parseHeader(data, line);
                    case "[player]" -> parsePlayer(data, line);
                    case "[clock]" -> parseClock(data, line);
                    case "[inventory]" -> parseInventory(data, line, inventoryEntries);
                    case "[map]" -> legacyMap.readLine(line);
                    case "[maps]" -> mapsSectionReader.readLine(line, mapStates);
                    case "[drops]" -> drops.add(parseDrop(line));
                    case "[regrowth]" -> regrowths.add(parseRegrowth(line));
                    case "[enemies]" -> enemies.add(parseEnemy(line));
                    case "[placed_objects]" -> placedObjects.add(parsePlacedObject(line));
                    case "[wood_chippers]" -> woodChippers.add(parseWoodChipper(line));
                    case "[chests]" -> chestSlots.add(parseChestSlot(line));
                    case "[skills]" -> skillEntries.add(parseSkill(line));
                    case "[collections]" -> collectionEntries.add(parseCollection(line));
                    default -> {
                    }
                }
            }
        }

        if ("[maps]".equals(section)) {
            mapsSectionReader.flush(mapStates);
        } else if ("[map]".equals(section)) {
            legacyMap.flush(mapStates, data.getMapId());
        }

        if (mapStates.isEmpty() && legacyMap.hasTiles()) {
            legacyMap.flush(mapStates, data.getMapId());
        }

        data.setInventoryEntries(inventoryEntries);
        data.setDrops(drops);
        data.setRegrowths(regrowths);
        data.setEnemies(enemies);
        data.setEnemiesSectionPresent(enemiesSectionPresent);
        data.setPlacedObjects(placedObjects);
        data.setWoodChippers(woodChippers);
        data.setChestSlots(chestSlots);
        data.setSkillEntries(skillEntries);
        data.setCollectionEntries(collectionEntries);
        data.setMapStates(mapStates);

        if (!mapStates.isEmpty()) {
            SaveData.MapEntry activeMap = mapStates.stream()
                    .filter(entry -> entry.getMapId().equals(data.getMapId()))
                    .findFirst()
                    .orElse(mapStates.get(0));
            data.setMapTiles(activeMap.getTiles());
        }

        return data;
    }

    private static void writeMapBlock(
            BufferedWriter writer,
            String mapId,
            int[][] tiles,
            int[][] rotations
    ) throws IOException {
        writer.write("map_id=" + mapId);
        writer.newLine();
        writer.write("height=" + tiles.length);
        writer.newLine();
        writer.write("width=" + tiles[0].length);
        writer.newLine();

        for (int row = 0; row < tiles.length; row++) {
            StringBuilder line = new StringBuilder("row=");
            for (int col = 0; col < tiles[row].length; col++) {
                if (col > 0) {
                    line.append(',');
                }
                int rotation = rotations == null ? 0 : rotations[row][col];
                line.append(MapCellCodec.format(tiles[row][col], rotation));
            }
            writer.write(line.toString());
            writer.newLine();
        }
    }

    private static void parseHeader(SaveData data, String line) {
        if (line.startsWith("version=")) {
            data.setVersion(Integer.parseInt(line.substring("version=".length())));
        } else if (line.startsWith("title=")) {
            data.setSaveTitle(line.substring("title=".length()));
        } else if (line.startsWith("map_id=")) {
            data.setMapId(line.substring("map_id=".length()));
        } else if (line.startsWith("saved_at=")) {
            data.setSavedAtMillis(Long.parseLong(line.substring("saved_at=".length())));
        }
    }

    private static void parsePlayer(SaveData data, String line) {
        if (line.startsWith("x=")) {
            data.setPlayerX(Integer.parseInt(line.substring("x=".length())));
        } else if (line.startsWith("y=")) {
            data.setPlayerY(Integer.parseInt(line.substring("y=".length())));
        } else if (line.startsWith("health=")) {
            data.setPlayerHealth(Double.parseDouble(line.substring("health=".length())));
        } else if (line.startsWith("energy=")) {
            data.setPlayerEnergy(Double.parseDouble(line.substring("energy=".length())));
        } else if (line.startsWith("wood_chips=")) {
            data.setWoodChips(Long.parseLong(line.substring("wood_chips=".length())));
        } else if (line.startsWith("pickup_magnet_tiles=")) {
            data.setPickupMagnetTiles(Double.parseDouble(line.substring("pickup_magnet_tiles=".length())));
        }
    }

    private static void parseClock(SaveData data, String line) {
        if (line.startsWith("day=")) {
            data.setDay(Integer.parseInt(line.substring("day=".length())));
        } else if (line.startsWith("hour=")) {
            data.setHour(Integer.parseInt(line.substring("hour=".length())));
        } else if (line.startsWith("minute=")) {
            data.setMinute(Integer.parseInt(line.substring("minute=".length())));
        }
    }

    private static void parseInventory(
            SaveData data,
            String line,
            List<SaveData.InventoryEntry> inventoryEntries
    ) {
        if (line.startsWith("selected_hotbar=")) {
            data.setSelectedHotbarSlot(Integer.parseInt(line.substring("selected_hotbar=".length())));
            return;
        }

        if (!line.startsWith("slot=")) {
            return;
        }

        String[] parts = line.substring("slot=".length()).split(",");
        inventoryEntries.add(new SaveData.InventoryEntry(
                Integer.parseInt(parts[0]),
                parts[1],
                Integer.parseInt(parts[2])
        ));
    }

    private static int[][] parseMapRowCells(String line) {
        if (!line.startsWith("row=")) {
            throw new IllegalArgumentException("Invalid map row: " + line);
        }

        String[] parts = line.substring("row=".length()).split(",");
        int[] tiles = new int[parts.length];
        int[] rotations = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            tiles[i] = MapCellCodec.parseTileId(parts[i]);
            rotations[i] = MapCellCodec.parseRotation(parts[i]);
        }
        return new int[][] { tiles, rotations };
    }

    private static SaveData.DropEntry parseDrop(String line) {
        if (!line.startsWith("drop=")) {
            throw new IllegalArgumentException("Invalid drop line: " + line);
        }

        String[] parts = line.substring("drop=".length()).split(",");
        // v13+: mapId,itemId,qty,x,y — older saves: itemId,qty,x,y
        if (parts.length >= 5) {
            return new SaveData.DropEntry(
                    parts[0],
                    parts[1],
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4])
            );
        }
        if (parts.length >= 4) {
            return new SaveData.DropEntry(
                    null,
                    parts[0],
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
            );
        }
        throw new IllegalArgumentException("Invalid drop line: " + line);
    }

    private static SaveData.RegrowthEntry parseRegrowth(String line) {
        if (!line.startsWith("regrow=")) {
            throw new IllegalArgumentException("Invalid regrowth line: " + line);
        }

        String[] parts = line.substring("regrow=".length()).split(",");
        if (parts.length >= 5) {
            return new SaveData.RegrowthEntry(
                    parts[0],
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    Double.parseDouble(parts[4])
            );
        }

        return new SaveData.RegrowthEntry(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Double.parseDouble(parts[3])
        );
    }

    private static SaveData.EnemyEntry parseEnemy(String line) {
        if (!line.startsWith("enemy=")) {
            throw new IllegalArgumentException("Invalid enemy line: " + line);
        }

        String[] parts = line.substring("enemy=".length()).split(",");
        return new SaveData.EnemyEntry(
                parts[0],
                parts[1],
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]),
                Double.parseDouble(parts[4])
        );
    }

    private static SaveData.PlacedObjectEntry parsePlacedObject(String line) {
        if (!line.startsWith("placed=")) {
            throw new IllegalArgumentException("Invalid placed object line: " + line);
        }

        String[] parts = line.substring("placed=".length()).split(",");
        return new SaveData.PlacedObjectEntry(
                parts[0],
                parts[1],
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3])
        );
    }

    private static SaveData.WoodChipperEntry parseWoodChipper(String line) {
        if (!line.startsWith("chipper=")) {
            throw new IllegalArgumentException("Invalid wood chipper line: " + line);
        }

        String[] parts = line.substring("chipper=".length()).split(",");
        return new SaveData.WoodChipperEntry(
                parts[0],
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                parts[3],
                Integer.parseInt(parts[4]),
                parts[5],
                Integer.parseInt(parts[6]),
                Double.parseDouble(parts[7]),
                Boolean.parseBoolean(parts[8])
        );
    }

    private static SaveData.ChestSlotEntry parseChestSlot(String line) {
        if (!line.startsWith("slot=")) {
            throw new IllegalArgumentException("Invalid chest slot line: " + line);
        }

        String[] parts = line.substring("slot=".length()).split(",");
        return new SaveData.ChestSlotEntry(
                parts[0],
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]),
                parts[4],
                Integer.parseInt(parts[5])
        );
    }

    private static SaveData.SkillEntry parseSkill(String line) {
        if (!line.startsWith("skill=")) {
            throw new IllegalArgumentException("Invalid skill line: " + line);
        }

        String[] parts = line.substring("skill=".length()).split(",");
        return new SaveData.SkillEntry(parts[0], Integer.parseInt(parts[1]));
    }

    private static SaveData.CollectionEntry parseCollection(String line) {
        if (!line.startsWith("discovered=")) {
            throw new IllegalArgumentException("Invalid collection line: " + line);
        }

        return new SaveData.CollectionEntry(line.substring("discovered=".length()).trim());
    }

    private static final class MapBlockReader {

        private String mapId;
        private int mapWidth;
        private int mapHeight;
        private final List<int[]> mapRows = new ArrayList<>();
        private final List<int[]> rotationRows = new ArrayList<>();

        private void readLine(String line) {
            if (line.startsWith("width=")) {
                mapWidth = Integer.parseInt(line.substring("width=".length()));
            } else if (line.startsWith("height=")) {
                mapHeight = Integer.parseInt(line.substring("height=".length()));
            } else if (line.startsWith("row=")) {
                int[][] cells = parseMapRowCells(line);
                mapRows.add(cells[0]);
                rotationRows.add(cells[1]);
            }
        }

        private void readLine(String line, List<SaveData.MapEntry> mapStates) {
            if (line.startsWith("map_id=")) {
                flush(mapStates);
                mapId = line.substring("map_id=".length());
                return;
            }

            readLine(line);
        }

        private void flush(List<SaveData.MapEntry> mapStates) {
            if (mapId == null || mapRows.isEmpty()) {
                reset();
                return;
            }

            if (mapHeight != mapRows.size()) {
                throw new IllegalStateException("Map row count does not match saved height for map: " + mapId);
            }

            int[][] tiles = new int[mapHeight][mapWidth];
            int[][] rotations = new int[mapHeight][mapWidth];
            for (int row = 0; row < mapRows.size(); row++) {
                tiles[row] = mapRows.get(row);
                rotations[row] = rotationRows.get(row);
            }

            mapStates.add(new SaveData.MapEntry(mapId, tiles, rotations));
            reset();
        }

        private void flush(List<SaveData.MapEntry> mapStates, String fallbackMapId) {
            if (mapId == null) {
                mapId = fallbackMapId;
            }
            flush(mapStates);
        }

        private boolean hasTiles() {
            return !mapRows.isEmpty();
        }

        private void reset() {
            mapId = null;
            mapWidth = 0;
            mapHeight = 0;
            mapRows.clear();
            rotationRows.clear();
        }
    }
}
