package lumberjack.entity;

import lumberjack.core.GameConfig;

/**
 * The player-controlled lumberjack.
 */
public final class Player extends Entity {

    /** Pixels advanced per engine frame ({@link GameConfig#FRAME_MS}). */
    private static final double DEFAULT_SPEED = 1.5;
    private static final int START_X = 32;
    private static final int START_Y = 32;

    private final double speed;
    private double hitFlashRemainingMs;

    private PlayerFacing facing = PlayerFacing.DOWN;
    private PlayerAnimKind animKind = PlayerAnimKind.IDLE;
    private double animElapsedMs;
    private boolean moving;
    /** Set when an axe swing finishes; consumed by {@link #pollAxeSwingCompleted()}. */
    private boolean axeSwingCompletedPulse;
    /** Set when cast reaches hold pose; consumed by {@link #pollRodCastCompleted()}. */
    private boolean rodCastCompletedPulse;

    /** Sub-pixel leftovers so diagonal moves stay the same speed as cardinal. */
    private double moveCarryX;
    private double moveCarryY;

    public Player() {
        this(START_X, START_Y, DEFAULT_SPEED);
    }

    public Player(int startX, int startY, double speed) {
        super(startX, startY, GameConfig.PLAYER_HITBOX_WIDTH, GameConfig.PLAYER_HITBOX_HEIGHT);
        this.speed = speed;
    }

    public double getSpeed() {
        return speed;
    }

    /**
     * Accumulates velocity and returns whole-pixel steps for this frame.
     * Clears carry when velocity is zero so leftovers don't fire after release.
     */
    public int consumeMoveStepX(double velocityX) {
        if (velocityX == 0.0) {
            moveCarryX = 0.0;
            return 0;
        }
        moveCarryX += velocityX;
        int step = (int) moveCarryX;
        moveCarryX -= step;
        return step;
    }

    public int consumeMoveStepY(double velocityY) {
        if (velocityY == 0.0) {
            moveCarryY = 0.0;
            return 0;
        }
        moveCarryY += velocityY;
        int step = (int) moveCarryY;
        moveCarryY -= step;
        return step;
    }

    public PlayerFacing getFacing() {
        return facing;
    }

    public PlayerAnimKind getAnimKind() {
        return animKind;
    }

    public double getAnimElapsedMs() {
        return animElapsedMs;
    }

    public void setMovementIntent(int dx, int dy) {
        moving = dx != 0 || dy != 0;

        if (isRodBusy() || (isActionAnimation(animKind) && animElapsedMs < getCurrentAnimationDurationMs())) {
            return;
        }

        if (moving) {
            facing = PlayerFacing.fromMovement(dx, dy);
            animKind = PlayerAnimKind.WALK;
        } else {
            animKind = PlayerAnimKind.IDLE;
        }
    }

    public void faceToward(int targetX, int targetY) {
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        facing = PlayerFacing.toward(centerX, centerY, targetX, targetY);
    }

    /** True while an action animation (axe/rod) must play out — no movement or new actions. */
    public boolean isActionLocked() {
        if (isRodBusy()) {
            return true;
        }
        return isActionAnimation(animKind) && animElapsedMs < getCurrentAnimationDurationMs();
    }

    public boolean isRodBusy() {
        return animKind == PlayerAnimKind.ROD_CAST
                || animKind == PlayerAnimKind.ROD_HOLD
                || animKind == PlayerAnimKind.ROD_REEL;
    }

    public void triggerAxeSwing() {
        if (isActionLocked()) {
            return;
        }
        startAction(PlayerAnimKind.AXE_SWING);
        axeSwingCompletedPulse = false;
    }

    public void triggerRodCast() {
        if (isActionLocked()) {
            return;
        }
        startAction(PlayerAnimKind.ROD_CAST);
        rodCastCompletedPulse = false;
    }

    /** Play cast frames in reverse after a catch / successful reel-in. */
    public void triggerRodReel() {
        if (animKind != PlayerAnimKind.ROD_HOLD && animKind != PlayerAnimKind.ROD_CAST) {
            return;
        }
        startAction(PlayerAnimKind.ROD_REEL);
    }

    /** Cancel fishing pose immediately (no reverse reel). */
    public void cancelRodAnimation() {
        if (!isRodBusy()) {
            return;
        }
        animKind = moving ? PlayerAnimKind.WALK : PlayerAnimKind.IDLE;
        animElapsedMs = 0;
        rodCastCompletedPulse = false;
    }

    /**
     * Returns true once when an axe swing finishes (last frame done).
     * Facing-agnostic — works for down/up/left/right axe sheets.
     */
    public boolean pollAxeSwingCompleted() {
        if (!axeSwingCompletedPulse) {
            return false;
        }
        axeSwingCompletedPulse = false;
        return true;
    }

    /** Returns true once when cast reaches the hold pose (bobber can appear). */
    public boolean pollRodCastCompleted() {
        if (!rodCastCompletedPulse) {
            return false;
        }
        rodCastCompletedPulse = false;
        return true;
    }

    private void startAction(PlayerAnimKind actionKind) {
        animKind = actionKind;
        animElapsedMs = 0;
    }

    public void update(double elapsedMs) {
        if (hitFlashRemainingMs > 0) {
            hitFlashRemainingMs = Math.max(0, hitFlashRemainingMs - elapsedMs);
        }

        animElapsedMs += elapsedMs;

        if (animKind == PlayerAnimKind.ROD_CAST) {
            if (animElapsedMs >= getCurrentAnimationDurationMs()) {
                animKind = PlayerAnimKind.ROD_HOLD;
                animElapsedMs = 0;
                rodCastCompletedPulse = true;
            }
        } else if (animKind == PlayerAnimKind.ROD_HOLD) {
            animElapsedMs = 0;
        } else if (animKind == PlayerAnimKind.ROD_REEL) {
            if (animElapsedMs >= getCurrentAnimationDurationMs()) {
                animKind = moving ? PlayerAnimKind.WALK : PlayerAnimKind.IDLE;
                animElapsedMs = 0;
            }
        } else if (isActionAnimation(animKind)) {
            if (animElapsedMs >= getCurrentAnimationDurationMs()) {
                if (animKind == PlayerAnimKind.AXE_SWING) {
                    axeSwingCompletedPulse = true;
                }
                animKind = moving ? PlayerAnimKind.WALK : PlayerAnimKind.IDLE;
                animElapsedMs = 0;
            }
        } else if (animKind == PlayerAnimKind.WALK) {
            double walkDurationMs = GameConfig.PLAYER_WALK_FRAME_MS * walkSequenceLength();
            if (animElapsedMs >= walkDurationMs) {
                animElapsedMs = animElapsedMs % walkDurationMs;
            }
        } else {
            animElapsedMs = 0;
        }
    }

    public void triggerHitFlash(double durationMs) {
        hitFlashRemainingMs = Math.max(hitFlashRemainingMs, durationMs);
    }

    public boolean isFlashing() {
        return hitFlashRemainingMs > 0;
    }

    private int walkSequenceLength() {
        return switch (facing) {
            case LEFT, RIGHT -> GameConfig.PLAYER_WALK_SEQUENCE_SIDE.length;
            case UP, DOWN -> GameConfig.PLAYER_WALK_SEQUENCE_VERTICAL.length;
        };
    }

    private double getCurrentAnimationDurationMs() {
        return switch (animKind) {
            case AXE_SWING -> GameConfig.PLAYER_AXE_FRAME_MS * GameConfig.PLAYER_AXE_FRAMES;
            case ROD_CAST, ROD_REEL -> GameConfig.PLAYER_ROD_FRAME_MS * GameConfig.PLAYER_ROD_FRAMES;
            case ROD_HOLD -> Double.POSITIVE_INFINITY;
            case WALK -> GameConfig.PLAYER_WALK_FRAME_MS * walkSequenceLength();
            case IDLE -> 0;
        };
    }

    private static boolean isActionAnimation(PlayerAnimKind kind) {
        return kind == PlayerAnimKind.AXE_SWING;
    }
}
