package lumberjack.ui.hud;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lumberjack.core.GameConfig;
import lumberjack.item.ItemRegistry;
import lumberjack.skills.SkillProgressListener;
import lumberjack.skills.SkillType;

/**
 * Transient on-screen messages for item changes, XP gains, and skill level-ups.
 */
public final class GameNotificationManager implements InventoryListener, SkillProgressListener {

    private final ItemRegistry itemRegistry;
    private final List<FeedLine> feedLines = new ArrayList<>();
    private final List<XpPopup> xpPopups = new ArrayList<>();

    private boolean suppressed;

    public GameNotificationManager(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public void setSuppressed(boolean suppressed) {
        this.suppressed = suppressed;
    }

    public boolean isSuppressed() {
        return suppressed;
    }

    public void clear() {
        feedLines.clear();
        xpPopups.clear();
    }

    public boolean update(double elapsedMs) {
        if (elapsedMs <= 0) {
            return false;
        }

        boolean changed = false;

        Iterator<FeedLine> feedIterator = feedLines.iterator();
        while (feedIterator.hasNext()) {
            FeedLine line = feedIterator.next();
            line.remainingMs -= elapsedMs;
            if (line.remainingMs <= 0) {
                feedIterator.remove();
            }
            changed = true;
        }

        Iterator<XpPopup> xpIterator = xpPopups.iterator();
        while (xpIterator.hasNext()) {
            XpPopup popup = xpIterator.next();
            popup.remainingMs -= elapsedMs;
            popup.offsetY += GameConfig.NOTIFICATION_XP_DRIFT_SPEED * (elapsedMs / 1000.0);
            if (popup.remainingMs <= 0) {
                xpIterator.remove();
            }
            changed = true;
        }

        return changed;
    }

    public boolean hasVisibleNotifications() {
        return !feedLines.isEmpty() || !xpPopups.isEmpty();
    }

    public List<FeedLine> getFeedLines() {
        return List.copyOf(feedLines);
    }

    public List<XpPopup> getXpPopups() {
        return List.copyOf(xpPopups);
    }

    @Override
    public void onItemsAdded(String itemId, int quantity) {
        if (suppressed || quantity <= 0) {
            return;
        }

        String itemName = itemRegistry.get(itemId).getName();
        addFeedLine(formatItemChange("+", itemName, quantity));
    }

    @Override
    public void onItemsRemoved(String itemId, int quantity) {
        if (suppressed || quantity <= 0) {
            return;
        }

        String itemName = itemRegistry.get(itemId).getName();
        addFeedLine(formatItemChange("-", itemName, quantity));
    }

    @Override
    public void onXpGained(SkillType skillType, int amount) {
        if (suppressed || amount <= 0) {
            return;
        }

        addXpPopup("+" + amount + " XP");
    }

    @Override
    public void onSkillLevelUp(SkillType skillType, int newLevel) {
        if (suppressed) {
            return;
        }

        addFeedLine("You are now " + skillType.getDisplayName() + " level " + newLevel);
    }

    public void notifyItemCatch(String itemName, int quantity) {
        if (suppressed || quantity <= 0) {
            return;
        }

        addFeedLine(formatItemChange("+", itemName, quantity));
    }

    public void notifyGameSaved() {
        if (suppressed) {
            return;
        }

        addFeedLine("Game saved");
    }

    public void notifyMessage(String text) {
        if (suppressed || text == null || text.isBlank()) {
            return;
        }

        addFeedLine(text);
    }

    public void notifyRecipeUnlocked(String recipeName) {
        if (suppressed) {
            return;
        }

        addFeedLine("New recipe unlocked: " + recipeName);
    }

    private void addFeedLine(String text) {
        feedLines.add(new FeedLine(text, GameConfig.NOTIFICATION_FEED_DURATION_MS));
        while (feedLines.size() > GameConfig.NOTIFICATION_MAX_FEED_LINES) {
            feedLines.remove(0);
        }
    }

    private void addXpPopup(String text) {
        xpPopups.add(new XpPopup(text, GameConfig.NOTIFICATION_XP_DURATION_MS));
        while (xpPopups.size() > GameConfig.NOTIFICATION_MAX_XP_POPUPS) {
            xpPopups.remove(0);
        }
    }

    private static String formatItemChange(String sign, String itemName, int quantity) {
        return sign + " " + itemName + " x" + quantity;
    }

    public static final class FeedLine {

        private final String text;
        private double remainingMs;

        public FeedLine(String text, double durationMs) {
            this.text = text;
            this.remainingMs = durationMs;
        }

        public String getText() {
            return text;
        }

        public double getRemainingMs() {
            return remainingMs;
        }

        public float getAlpha() {
            double fadeWindow = GameConfig.NOTIFICATION_FADE_MS;
            if (remainingMs >= fadeWindow) {
                return 1f;
            }
            return (float) Math.max(0, remainingMs / fadeWindow);
        }
    }

    public static final class XpPopup {

        private final String text;
        private double remainingMs;
        private double offsetY;

        public XpPopup(String text, double durationMs) {
            this.text = text;
            this.remainingMs = durationMs;
        }

        public String getText() {
            return text;
        }

        public double getRemainingMs() {
            return remainingMs;
        }

        public double getOffsetY() {
            return offsetY;
        }

        public float getAlpha() {
            double fadeWindow = GameConfig.NOTIFICATION_FADE_MS;
            if (remainingMs >= fadeWindow) {
                return 1f;
            }
            return (float) Math.max(0, remainingMs / fadeWindow);
        }
    }
}
