package lumberjack.save;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain-data snapshot of everything that must persist between sessions.
 * No behavior here — just fields that {@link lumberjack.save.SaveCodec} reads/writes.
 */
public final class SaveData {

    public static final int CURRENT_VERSION = 14;

    private int version = CURRENT_VERSION;
    private String saveTitle = "Untitled";
    private String mapId;
    private int playerX;
    private int playerY;
    private double playerHealth;
    private double playerEnergy = -1;
    private long woodChips;
    /** Pickup magnet radius in tiles. 0 means “use default on load”. */
    private double pickupMagnetTiles;
    private int day;
    private int hour;
    private int minute;
    private int selectedHotbarSlot;
    private int[][] mapTiles;
    private List<MapEntry> mapStates = new ArrayList<>();
    private List<InventoryEntry> inventoryEntries = new ArrayList<>();
    private List<DropEntry> drops = new ArrayList<>();
    /** True when the save file contained an {@code [enemies]} section (even if empty). */
    private boolean enemiesSectionPresent;
    private List<RegrowthEntry> regrowths = new ArrayList<>();
    private List<EnemyEntry> enemies = new ArrayList<>();
    private List<PlacedObjectEntry> placedObjects = new ArrayList<>();
    private List<WoodChipperEntry> woodChippers = new ArrayList<>();
    private List<ChestSlotEntry> chestSlots = new ArrayList<>();
    private List<SkillEntry> skillEntries = new ArrayList<>();
    private List<CollectionEntry> collectionEntries = new ArrayList<>();
    private long savedAtMillis;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getSaveTitle() {
        return saveTitle;
    }

    public void setSaveTitle(String saveTitle) {
        this.saveTitle = saveTitle;
    }

    public String getMapId() {
        return mapId;
    }

    public void setMapId(String mapId) {
        this.mapId = mapId;
    }

    public int getPlayerX() {
        return playerX;
    }

    public void setPlayerX(int playerX) {
        this.playerX = playerX;
    }

    public int getPlayerY() {
        return playerY;
    }

    public void setPlayerY(int playerY) {
        this.playerY = playerY;
    }

    public double getPlayerHealth() {
        return playerHealth;
    }

    public void setPlayerHealth(double playerHealth) {
        this.playerHealth = playerHealth;
    }

    public double getPlayerEnergy() {
        return playerEnergy;
    }

    public void setPlayerEnergy(double playerEnergy) {
        this.playerEnergy = playerEnergy;
    }

    public long getWoodChips() {
        return woodChips;
    }

    public void setWoodChips(long woodChips) {
        this.woodChips = Math.max(0, woodChips);
    }

    public double getPickupMagnetTiles() {
        return pickupMagnetTiles;
    }

    public void setPickupMagnetTiles(double pickupMagnetTiles) {
        this.pickupMagnetTiles = Math.max(0, pickupMagnetTiles);
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public int getSelectedHotbarSlot() {
        return selectedHotbarSlot;
    }

    public void setSelectedHotbarSlot(int selectedHotbarSlot) {
        this.selectedHotbarSlot = selectedHotbarSlot;
    }

    public int[][] getMapTiles() {
        return mapTiles;
    }

    public void setMapTiles(int[][] mapTiles) {
        this.mapTiles = mapTiles;
    }

    public List<MapEntry> getMapStates() {
        return mapStates;
    }

    public void setMapStates(List<MapEntry> mapStates) {
        this.mapStates = mapStates;
    }

    public List<InventoryEntry> getInventoryEntries() {
        return inventoryEntries;
    }

    public void setInventoryEntries(List<InventoryEntry> inventoryEntries) {
        this.inventoryEntries = inventoryEntries;
    }

    public List<DropEntry> getDrops() {
        return drops;
    }

    public void setDrops(List<DropEntry> drops) {
        this.drops = drops;
    }

    public List<RegrowthEntry> getRegrowths() {
        return regrowths;
    }

    public void setRegrowths(List<RegrowthEntry> regrowths) {
        this.regrowths = regrowths;
    }

    public List<EnemyEntry> getEnemies() {
        return enemies;
    }

    public void setEnemies(List<EnemyEntry> enemies) {
        this.enemies = enemies;
    }

    public boolean isEnemiesSectionPresent() {
        return enemiesSectionPresent;
    }

    public void setEnemiesSectionPresent(boolean enemiesSectionPresent) {
        this.enemiesSectionPresent = enemiesSectionPresent;
    }

    public List<PlacedObjectEntry> getPlacedObjects() {
        return placedObjects;
    }

    public void setPlacedObjects(List<PlacedObjectEntry> placedObjects) {
        this.placedObjects = placedObjects;
    }

    public List<WoodChipperEntry> getWoodChippers() {
        return woodChippers;
    }

    public void setWoodChippers(List<WoodChipperEntry> woodChippers) {
        this.woodChippers = woodChippers;
    }

    public List<ChestSlotEntry> getChestSlots() {
        return chestSlots;
    }

    public void setChestSlots(List<ChestSlotEntry> chestSlots) {
        this.chestSlots = chestSlots;
    }

    public List<SkillEntry> getSkillEntries() {
        return skillEntries;
    }

    public void setSkillEntries(List<SkillEntry> skillEntries) {
        this.skillEntries = skillEntries;
    }

    public List<CollectionEntry> getCollectionEntries() {
        return collectionEntries;
    }

    public void setCollectionEntries(List<CollectionEntry> collectionEntries) {
        this.collectionEntries = collectionEntries;
    }

    public long getSavedAtMillis() {
        return savedAtMillis;
    }

    public void setSavedAtMillis(long savedAtMillis) {
        this.savedAtMillis = savedAtMillis;
    }

    public static final class MapEntry {

        private final String mapId;
        private final int[][] tiles;
        private final int[][] rotations;

        public MapEntry(String mapId, int[][] tiles) {
            this(mapId, tiles, null);
        }

        public MapEntry(String mapId, int[][] tiles, int[][] rotations) {
            this.mapId = mapId;
            this.tiles = tiles;
            this.rotations = rotations;
        }

        public String getMapId() {
            return mapId;
        }

        public int[][] getTiles() {
            return tiles;
        }

        public int[][] getRotations() {
            return rotations;
        }
    }

    public static final class InventoryEntry {

        private final int slotIndex;
        private final String itemId;
        private final int quantity;

        public InventoryEntry(int slotIndex, String itemId, int quantity) {
            this.slotIndex = slotIndex;
            this.itemId = itemId;
            this.quantity = quantity;
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        public String getItemId() {
            return itemId;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    public static final class DropEntry {

        private final String mapId;
        private final String itemId;
        private final int quantity;
        private final int centerX;
        private final int centerY;

        public DropEntry(String mapId, String itemId, int quantity, int centerX, int centerY) {
            this.mapId = mapId;
            this.itemId = itemId;
            this.quantity = quantity;
            this.centerX = centerX;
            this.centerY = centerY;
        }

        public String getMapId() {
            return mapId;
        }

        public String getItemId() {
            return itemId;
        }

        public int getQuantity() {
            return quantity;
        }

        public int getCenterX() {
            return centerX;
        }

        public int getCenterY() {
            return centerY;
        }
    }

    public static final class RegrowthEntry {

        private final String mapId;
        private final int col;
        private final int row;
        private final int tileId;
        private final double remainingMs;

        public RegrowthEntry(String mapId, int col, int row, int tileId, double remainingMs) {
            this.mapId = mapId;
            this.col = col;
            this.row = row;
            this.tileId = tileId;
            this.remainingMs = remainingMs;
        }

        public RegrowthEntry(int col, int row, int tileId, double remainingMs) {
            this(null, col, row, tileId, remainingMs);
        }

        public String getMapId() {
            return mapId;
        }

        public int getCol() {
            return col;
        }

        public int getRow() {
            return row;
        }

        public int getTileId() {
            return tileId;
        }

        public double getRemainingMs() {
            return remainingMs;
        }
    }

    public static final class EnemyEntry {

        private final String mapId;
        private final String enemyId;
        private final int x;
        private final int y;
        private final double health;

        public EnemyEntry(String mapId, String enemyId, int x, int y, double health) {
            this.mapId = mapId;
            this.enemyId = enemyId;
            this.x = x;
            this.y = y;
            this.health = health;
        }

        public String getMapId() {
            return mapId;
        }

        public String getEnemyId() {
            return enemyId;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public double getHealth() {
            return health;
        }
    }

    public static final class PlacedObjectEntry {

        private final String mapId;
        private final String itemId;
        private final int col;
        private final int row;

        public PlacedObjectEntry(String mapId, String itemId, int col, int row) {
            this.mapId = mapId;
            this.itemId = itemId;
            this.col = col;
            this.row = row;
        }

        public String getMapId() {
            return mapId;
        }

        public String getItemId() {
            return itemId;
        }

        public int getCol() {
            return col;
        }

        public int getRow() {
            return row;
        }
    }

    public static final class WoodChipperEntry {

        private final String mapId;
        private final int col;
        private final int row;
        private final String inputItemId;
        private final int inputQuantity;
        private final String outputItemId;
        private final int outputQuantity;
        private final double progressMs;
        private final boolean processing;

        public WoodChipperEntry(
                String mapId,
                int col,
                int row,
                String inputItemId,
                int inputQuantity,
                String outputItemId,
                int outputQuantity,
                double progressMs,
                boolean processing
        ) {
            this.mapId = mapId;
            this.col = col;
            this.row = row;
            this.inputItemId = inputItemId;
            this.inputQuantity = inputQuantity;
            this.outputItemId = outputItemId;
            this.outputQuantity = outputQuantity;
            this.progressMs = progressMs;
            this.processing = processing;
        }

        public String getMapId() {
            return mapId;
        }

        public int getCol() {
            return col;
        }

        public int getRow() {
            return row;
        }

        public String getInputItemId() {
            return inputItemId;
        }

        public int getInputQuantity() {
            return inputQuantity;
        }

        public String getOutputItemId() {
            return outputItemId;
        }

        public int getOutputQuantity() {
            return outputQuantity;
        }

        public double getProgressMs() {
            return progressMs;
        }

        public boolean isProcessing() {
            return processing;
        }
    }

    public static final class ChestSlotEntry {

        private final String mapId;
        private final int col;
        private final int row;
        private final int slotIndex;
        private final String itemId;
        private final int quantity;

        public ChestSlotEntry(String mapId, int col, int row, int slotIndex, String itemId, int quantity) {
            this.mapId = mapId;
            this.col = col;
            this.row = row;
            this.slotIndex = slotIndex;
            this.itemId = itemId;
            this.quantity = quantity;
        }

        public String getMapId() {
            return mapId;
        }

        public int getCol() {
            return col;
        }

        public int getRow() {
            return row;
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        public String getItemId() {
            return itemId;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    public static final class SkillEntry {

        private final String skillId;
        private final int totalXp;

        public SkillEntry(String skillId, int totalXp) {
            this.skillId = skillId;
            this.totalXp = totalXp;
        }

        public String getSkillId() {
            return skillId;
        }

        public int getTotalXp() {
            return totalXp;
        }
    }

    public static final class CollectionEntry {

        private final String entryId;

        public CollectionEntry(String entryId) {
            this.entryId = entryId;
        }

        public String getEntryId() {
            return entryId;
        }
    }
}
