package lumberjack.entity;

import java.awt.Rectangle;

/**
 * Base class for anything that exists in the world with a position and hitbox.
 * Combat stats, AI, and inventory will attach to subclasses later.
 */
public abstract class Entity {

    protected int x;
    protected int y;
    protected final int width;
    protected final int height;
    protected final Rectangle bounds;

    protected Entity(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bounds = new Rectangle(x, y, width, height);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public Rectangle getBoundsAtOffset(int dx, int dy) {
        return new Rectangle(x + dx, y + dy, width, height);
    }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;
        syncBounds();
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        syncBounds();
    }

    protected void syncBounds() {
        bounds.x = x;
        bounds.y = y;
    }
}
