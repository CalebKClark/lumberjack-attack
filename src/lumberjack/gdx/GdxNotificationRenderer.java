package lumberjack.gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.ui.hud.GameNotificationManager;
import lumberjack.ui.hud.GameNotificationManager.FeedLine;
import lumberjack.ui.hud.GameNotificationManager.XpPopup;
import lumberjack.ui.hud.HudTheme;

/**
 * In-game notification feed and XP popups.
 */
public final class GdxNotificationRenderer {

    private static final Color NOTIFICATION_TEXT = new Color(1f, 0.9f, 0.27f, 1f);
    private static final Color NOTIFICATION_SHADOW = new Color(0f, 0f, 0f, 0.63f);

    private final GlyphLayout layout = new GlyphLayout();

    public void draw(
            SpriteBatch batch,
            BitmapFont font,
            GameNotificationManager notifications,
            int screenWidth,
            int screenHeight
    ) {
        if (notifications == null) {
            return;
        }

        drawFeed(batch, font, notifications);
        drawXpPopups(batch, font, notifications, screenWidth);
    }

    private void drawFeed(SpriteBatch batch, BitmapFont font, GameNotificationManager notifications) {
        if (notifications.getFeedLines().isEmpty()) {
            return;
        }

        int lineHeight = (int) font.getLineHeight() + 4;
        int x = 24;
        int y = HudTheme.HOTBAR_BOTTOM_MARGIN + HudTheme.SLOT_SIZE + 12;

        for (int index = notifications.getFeedLines().size() - 1; index >= 0; index--) {
            FeedLine line = notifications.getFeedLines().get(index);
            drawShadowedText(batch, font, line.getText(), x, y, line.getAlpha());
            y += lineHeight;
        }
    }

    private void drawXpPopups(
            SpriteBatch batch,
            BitmapFont font,
            GameNotificationManager notifications,
            int screenWidth
    ) {
        if (notifications.getXpPopups().isEmpty()) {
            return;
        }

        int centerX = screenWidth / 2;
        int baseY = HudTheme.HOTBAR_BOTTOM_MARGIN + HudTheme.SLOT_SIZE + 18;
        int stackIndex = 0;

        for (XpPopup popup : notifications.getXpPopups()) {
            int y = (int) Math.round(baseY + popup.getOffsetY() + stackIndex * (font.getLineHeight() + 6));
            layout.setText(font, popup.getText());
            drawShadowedText(batch, font, popup.getText(), centerX - (int) layout.width / 2, y, popup.getAlpha());
            stackIndex++;
        }
    }

    private void drawShadowedText(SpriteBatch batch, BitmapFont font, String text, int x, int y, float alpha) {
        font.setColor(NOTIFICATION_SHADOW.r, NOTIFICATION_SHADOW.g, NOTIFICATION_SHADOW.b, alpha);
        font.draw(batch, text, x + 1, y + 1);
        font.setColor(NOTIFICATION_TEXT.r, NOTIFICATION_TEXT.g, NOTIFICATION_TEXT.b, alpha);
        font.draw(batch, text, x, y);
    }
}
