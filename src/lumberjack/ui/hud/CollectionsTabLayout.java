package lumberjack.ui.hud;

import java.awt.Rectangle;
import java.util.List;

import lumberjack.collection.CollectionCategory;
import lumberjack.collection.CollectionEntryDefinition;

/**
 * Layout helpers for the Collections tab.
 */
public final class CollectionsTabLayout {

    private static final int ROW_HEIGHT = 72;

    private CollectionsTabLayout() {
    }

    public static Rectangle getCategoryRowBounds(Rectangle content, int categoryIndex) {
        int y = content.y + 88 + categoryIndex * ROW_HEIGHT;
        return new Rectangle(content.x + 32, y, content.width - 64, ROW_HEIGHT - 8);
    }

    public static CollectionCategory getCategoryAtPoint(int x, int y, Rectangle content) {
        CollectionCategory[] categories = CollectionCategory.values();
        for (int index = 0; index < categories.length; index++) {
            if (getCategoryRowBounds(content, index).contains(x, y)) {
                return categories[index];
            }
        }
        return null;
    }

    public static Rectangle getBackButtonBounds(Rectangle content) {
        return new Rectangle(content.x + 24, content.y + 20, 90, 32);
    }

    public static Rectangle getEntryRowBounds(Rectangle content, int entryIndex) {
        int y = content.y + 88 + entryIndex * 52;
        return new Rectangle(content.x + 32, y, content.width - 64, 44);
    }

    public static String getEntryIdAtPoint(
            int x,
            int y,
            Rectangle content,
            List<CollectionEntryDefinition> entries
    ) {
        for (int index = 0; index < entries.size(); index++) {
            if (getEntryRowBounds(content, index).contains(x, y)) {
                return entries.get(index).getEntryId();
            }
        }
        return null;
    }
}
