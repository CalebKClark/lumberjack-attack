package lumberjack.entity;

/**
 * High-level player sprite animation set.
 */
public enum PlayerAnimKind {
    IDLE,
    WALK,
    AXE_SWING,
    /** Cast frames 0→last, then auto-enters {@link #ROD_HOLD}. */
    ROD_CAST,
    /** Hold last cast frame while the line is out / minigame runs. */
    ROD_HOLD,
    /** Reverse cast frames last→0 after a catch (or successful reel). */
    ROD_REEL
}
