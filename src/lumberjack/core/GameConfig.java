package lumberjack.core;

/**
 * Engine-level constants that rarely change per content update.
 * Game balance, item stats, and map layouts live in /resources instead.
 */
public final class GameConfig {

    private GameConfig() {
    }

    public static final String GAME_TITLE = "LumberJack Attack";
    /** Shown in window title / playtest branding. */
    public static final String GAME_VERSION = "0.1";
    /**
     * World editor on the main menu. Disabled for playtest builds launched with
     * {@code -Dlumberjack.playtest=true}.
     */
    public static final boolean ENABLE_WORLD_EDIT = !Boolean.getBoolean("lumberjack.playtest");

    public static final int WINDOW_WIDTH = 1920;
    public static final int WINDOW_HEIGHT = 1080;

    /** World / art tile size (Stardew-style 16×16 pixel art). */
    public static final int TILE_SIZE = 16;

    /** Player collision box — feet only, so one-tile gaps stay walkable. */
    public static final int PLAYER_HITBOX_WIDTH = 8;
    public static final int PLAYER_HITBOX_HEIGHT = 6;

    /** Player sprite is drawn bottom-aligned to the hitbox feet. */
    public static final int PLAYER_SPRITE_WIDTH = 16;
    public static final int PLAYER_SPRITE_HEIGHT = 24;

    /** World pickup icon size for dropped items. */
    public static final int DROP_ITEM_SIZE = 12;

    /** Default pickup-magnet radius in tiles (hitbox edge gap). Saved per player. */
    public static final double DEFAULT_PICKUP_MAGNET_TILES = 2.0 / 3.0;

    /** Scatter distance for new drops, in tile fractions. */
    public static final double DROP_SCATTER_MIN_TILES = 0.25;
    public static final double DROP_SCATTER_MAX_TILES = 0.75;
    public static final double DROP_SCATTER_DURATION_MS = 380.0;
    public static final int DROP_BOUNCE_COUNT = 3;
    public static final double DROP_BOUNCE_HEIGHT_PX = 7.0;
    /** How close (px) a magnetized drop must get before inventory pickup. */
    public static final double DROP_COLLECT_DISTANCE_PX = 4.0;
    /** Magnet pull speed in pixels per second. */
    public static final double DROP_MAGNET_SPEED_PX_PER_SEC = 70.0;

    /** Placed machine/furniture sprites and collision on a map tile. */
    public static final int PLACEABLE_SPRITE_SIZE = 16;
    public static final int PLACEABLE_HITBOX_SIZE = 14;

    /** Player animation timing (ms per frame). */
    /** Unique walk art frames on disk (walk_*_0..2). */
    public static final int PLAYER_WALK_FRAMES = 3;
    /**
     * Left/right walk order into walk file indices:
     * sheet poses 1,2,1,3 → files 0,1,0,2 (pose 1 is also idle).
     */
    public static final int[] PLAYER_WALK_SEQUENCE_SIDE = {0, 1, 0, 2};
    /**
     * Up/down walk order: sheet poses 2,3,4 → files 0,1,2 (pose 1 is idle only).
     */
    public static final int[] PLAYER_WALK_SEQUENCE_VERTICAL = {0, 1, 2};
    public static final int PLAYER_AXE_FRAMES = 4;
    public static final int PLAYER_ROD_FRAMES = 4;
    public static final double PLAYER_WALK_FRAME_MS = 120;
    public static final double PLAYER_AXE_FRAME_MS = 100;
    public static final double PLAYER_ROD_FRAME_MS = 100;

    public static final int TARGET_FPS = 60;
    public static final int FRAME_MS = 1000 / TARGET_FPS;

    public static final int INVENTORY_SLOTS = 36;
    public static final int HOTBAR_SLOTS = 9;
    public static final int BACKPACK_SLOTS = INVENTORY_SLOTS - HOTBAR_SLOTS;

    /** Default max stack for stackable inventory items (Stardew-style). */
    public static final int DEFAULT_ITEM_STACK_SIZE = 999;

    /** Wood chipper: one pine log becomes chips after this many in-game minutes. */
    public static final int WOOD_CHIPPER_PROCESS_GAME_MINUTES = 5;

    /** Default tile placed when a choppable tile is removed with no regrowth. */
    public static final int GRASS_TILE_ID = 0;
    public static final int CABIN_FLOOR_TILE_ID = 5;
    public static final int WATER_TILE_ID = 7;
    /**
     * World-editor palette id for the transfer-marker tool. Not painted as terrain —
     * selecting it toggles an invisible-in-game {@code t} marker on the existing cell.
     */
    public static final int TRANSITION_MARKER_TILE_ID = 19;
    /** Walkable overlays painted over water in the world editor (not player-placeable). */
    public static final int BRIDGE_TILE_ID = 8;
    public static final int BRIDGE_H_TILE_ID = 9;
    /** Temporary tile while a chopped pine is regenerating ({@code pine_stump}). */
    public static final int TREE_STUMP_TILE_ID = 10;
    /** Fully grown pine tree tile id ({@code tree1}). */
    public static final int PINE_TREE_TILE_ID = 1;

    /** Tall pine tree art size (base 16px wide, centered in canvas). */
    public static final int PINE_TREE_SPRITE_WIDTH = 48;
    public static final int PINE_TREE_SPRITE_HEIGHT = 72;
    /** Bottom of the tall sprite that matches the stump tile. */
    public static final int PINE_TREE_STUMP_OVERLAP_PX = 16;
    /** Alpha when the player is behind a tree canopy (Stardew-style see-through). */
    public static final float TREE_BEHIND_PLAYER_ALPHA = 0.45f;
    /** Extra canvas above the 24px body on axe / rod sheets. */
    public static final int PLAYER_AXE_TOP_PAD_PX = 6;
    /** Extra canvas beside the body for side-facing axe / rod reach. */
    public static final int PLAYER_AXE_SIDE_EXTEND_PX = 8;

    /** Default axe chop power when item omits tree_damage. */
    public static final int DEFAULT_AXE_TREE_DAMAGE = 1;

    public static final int TREE_DROP_QTY_MIN = 10;
    public static final int TREE_DROP_QTY_MAX = 20;
    public static final double TREE_RAIN_MAX_DELAY_MS = 450.0;
    public static final double TREE_RAIN_FALL_MIN_MS = 280.0;
    public static final double TREE_RAIN_FALL_MAX_MS = 650.0;

    /** Default map loaded when starting a new game. */
    public static final String STARTING_MAP = "cabin_interior";
    public static final String HOMESTEAD_MAP = "homestead";

    public static final int STARTING_SPAWN_COL = 3;
    public static final int STARTING_SPAWN_ROW = 4;

    /** Where the player respawns after death (cabin bed tile). */
    public static final String RESPAWN_MAP = "cabin_interior";
    /** Respawn on the foot tile of the starting cabin bed (top-right). */
    public static final int RESPAWN_BED_COL = 8;
    public static final int RESPAWN_BED_ROW = 2;

    /** Maps reset when starting a new game. */
    public static final String[] GAME_MAPS = { "cabin_interior", "homestead" };

    /** About 2 tiles of reach. */
    public static final int CHOP_REACH_DISTANCE = 32;

    // --- Time system ---
    /** One in-game minute passes every second of real time (24 min per full day). */
    public static final int REAL_MS_PER_GAME_MINUTE = 1_000;

    public static final int MINUTES_PER_HOUR = 60;
    public static final int HOURS_PER_DAY = 24;
    public static final int MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY;

    // --- Combat / player stats ---
    public static final int BASE_DAMAGE = 1;
    public static final double BASE_CRIT_CHANCE = 20.0;
    public static final double BASE_CRIT_MULTIPLIER = 1.5;
    public static final int BASE_MAX_HEALTH = 100;
    /** Starting / base energy cap (food restores up to this unless raised later). */
    public static final int BASE_MAX_ENERGY = 50;
    /** Energy spent per axe swing animation and per fishing cast. */
    public static final int ACTION_ENERGY_COST = 1;
    public static final int FISHING_CAST_ENERGY_COST = 3;
    /** Move at half speed when current energy is below this fraction of max. */
    public static final double LOW_ENERGY_SPEED_THRESHOLD = 0.05;
    public static final double LOW_ENERGY_SPEED_MULTIPLIER = 0.5;
    public static final double BASE_DEFENSE = 0.0;
    public static final double DEFENSE_CAP = 80.0;
    public static final double CRIT_CHANCE_CAP = 100.0;
    public static final double PLAYER_HIT_FLASH_MS = 200.0;

    public static final double FISHING_SPEED_CAP = 80.0;

    // --- Fishing ---
    public static final double FISHING_BITE_MIN_MS = 2_000;
    public static final double FISHING_BITE_MAX_MS = 4_500;
    public static final double FISHING_BITE_FLASH_MS = 700;
    public static final double FISHING_LINE_DESCEND_SPEED = 55.0;
    /** Depth interval at which line descent speed increases (repeats forever). */
    public static final double FISHING_LINE_SPEED_DEPTH_TIER = 350.0;
    /** Extra descend speed added per depth tier. */
    public static final double FISHING_LINE_DESCEND_SPEED_PER_TIER = 12.0;
    public static final double FISHING_HOOK_MOVE_SPEED = 220.0;
    public static final double FISHING_ENTITY_SPAWN_INTERVAL = 75.0;
    public static final double FISHING_SPAWN_LOOKAHEAD = 520.0;
    public static final double FISHING_DESPAWN_BEHIND = 140.0;
    public static final int FISHING_MINIGAME_PANEL_WIDTH = 480;
    public static final int FISHING_MINIGAME_PANEL_HEIGHT = 560;
    public static final int FISHING_PLAY_AREA_TOP = 60;
    public static final int FISHING_HOOK_SCREEN_Y = 200;
    public static final double FISHING_RESULT_DISPLAY_MS = 1_500;
    public static final double FISHING_TRASH_SPAWN_CHANCE = 0.5;
    public static final int FISHING_REFERENCE_SPEED_STAT = 3;
    public static final double FISHING_BASE_SPEED_MIN = 40.0;
    public static final double FISHING_BASE_SPEED_MAX = 90.0;
    public static final int FISHING_MAX_SPEED_STAT = 10;

    // --- Skills ---
    public static final int SKILL_MAX_LEVEL = 25;

    // --- Notifications ---
    public static final double NOTIFICATION_FEED_DURATION_MS = 3_000;
    public static final double NOTIFICATION_XP_DURATION_MS = 2_000;
    public static final double NOTIFICATION_FADE_MS = 450;
    public static final double NOTIFICATION_XP_DRIFT_SPEED = 28.0;
    public static final int NOTIFICATION_MAX_FEED_LINES = 8;
    public static final int NOTIFICATION_MAX_XP_POPUPS = 5;

    // --- Display ---
    /**
     * Render scale at 100% zoom (0% = 1.0).
     * Higher max helps 16×16 tiles feel readable on large monitors.
     */
    public static final double MAX_VIEW_ZOOM_SCALE = 12.0;

    // --- Saves ---
    public static final double AUTO_SAVE_INTERVAL_MS = 120_000;
}
