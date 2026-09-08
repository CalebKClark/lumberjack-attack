package lumberjack.gdx;

import java.awt.Rectangle;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.collection.CollectionCategory;
import lumberjack.collection.CollectionEntryDefinition;
import lumberjack.collection.DiscoveryType;
import lumberjack.collection.PlayerCollections;
import lumberjack.game.GameSession;
import lumberjack.ui.hud.CollectionsTabLayout;

/**
 * Native Collections tab.
 */
public final class GdxCollectionsTab {

    private static final Color TOOLTIP_TEXT = new Color(0.86f, 0.9f, 0.94f, 1f);
    private static final Color SLIME = new Color(0.2f, 0.78f, 0.2f, 1f);

    private final GdxMenuUi ui = new GdxMenuUi();

    public void draw(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            Rectangle content,
            CollectionCategory focusedCategory,
            String hoveredEntryId,
            int screenHeight
    ) {
        if (focusedCategory == null) {
            drawCategoryList(batch, font, textures, session, content, screenHeight);
        } else {
            drawCategoryDetail(batch, font, textures, session, content, focusedCategory, hoveredEntryId, screenHeight);
        }
    }

    private void drawCategoryList(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            Rectangle content,
            int screenHeight
    ) {
        PlayerCollections collections = session.getPlayerCollections();
        float top = GdxUiCoords.bottom(content, screenHeight) + content.height;

        ui.drawText(batch, font, "Collections", GdxMenuUi.HUD_TEXT, content.x + 24, top - 24);
        ui.drawText(batch, font, "Click a category to view discoveries", GdxMenuUi.SUBTEXT, content.x + 24, top - 52);

        CollectionCategory[] categories = CollectionCategory.values();
        for (int index = 0; index < categories.length; index++) {
            CollectionCategory category = categories[index];
            Rectangle row = CollectionsTabLayout.getCategoryRowBounds(content, index);
            ui.drawAccentRow(batch, textures, row, accentColor(category), screenHeight);

            float rowBottom = GdxUiCoords.bottom(row, screenHeight);
            ui.drawText(batch, font, category.getDisplayName(), GdxMenuUi.HUD_TEXT, row.x + 20, rowBottom + row.height - 14);
            ui.drawText(
                    batch,
                    font,
                    category.getDisplayName() + " Unlocked: "
                            + collections.getDiscoveredCount(category) + "/"
                            + collections.getTotalCount(category),
                    GdxMenuUi.SUBTEXT,
                    row.x + 20,
                    rowBottom + 16
            );
        }
    }

    private void drawCategoryDetail(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            Rectangle content,
            CollectionCategory category,
            String hoveredEntryId,
            int screenHeight
    ) {
        PlayerCollections collections = session.getPlayerCollections();
        List<CollectionEntryDefinition> entries = session.getCollectionRegistry().getEntries(category);
        float top = GdxUiCoords.bottom(content, screenHeight) + content.height;

        ui.drawBackButton(batch, font, textures, CollectionsTabLayout.getBackButtonBounds(content), screenHeight);
        ui.drawText(batch, font, category.getDisplayName(), GdxMenuUi.HUD_TEXT, content.x + 130, top - 24);
        ui.drawText(
                batch,
                font,
                "Unlocked: " + collections.getDiscoveredCount(category) + "/" + collections.getTotalCount(category),
                GdxMenuUi.SUBTEXT,
                content.x + 130,
                top - 52
        );

        CollectionEntryDefinition hoveredEntry = null;
        Rectangle hoveredBounds = null;

        for (int index = 0; index < entries.size(); index++) {
            CollectionEntryDefinition entry = entries.get(index);
            Rectangle row = CollectionsTabLayout.getEntryRowBounds(content, index);
            boolean discovered = collections.isDiscovered(entry.getEntryId());

            ui.drawRect(batch, textures, GdxMenuUi.ROW_BG, row, screenHeight);
            ui.drawBorder(batch, textures, GdxMenuUi.ROW_BORDER, row, screenHeight);
            drawEntrySwatch(batch, textures, session, entry, discovered, row, screenHeight);

            float rowBottom = GdxUiCoords.bottom(row, screenHeight);
            ui.drawText(
                    batch,
                    font,
                    discovered ? entry.getDisplayName() : "???",
                    discovered ? GdxMenuUi.HUD_TEXT : GdxMenuUi.UNKNOWN_TEXT,
                    row.x + 52,
                    rowBottom + (row.height + ui.measureHeight(font, "A")) / 2f
            );

            if (entry.getEntryId().equals(hoveredEntryId) && discovered) {
                hoveredEntry = entry;
                hoveredBounds = row;
            }
        }

        if (hoveredEntry != null && hoveredBounds != null) {
            String tooltip = collections.getTooltipText(hoveredEntry, session.getFishSpawnRegistry());
            if (!tooltip.isBlank()) {
                drawEntryTooltip(batch, font, textures, content, tooltip, hoveredBounds, screenHeight);
            }
        }
    }

    private void drawEntrySwatch(
            SpriteBatch batch,
            GdxTextureCache textures,
            GameSession session,
            CollectionEntryDefinition entry,
            boolean discovered,
            Rectangle row,
            int screenHeight
    ) {
        float size = 28;
        float left = row.x + 12;
        float bottom = GdxUiCoords.bottom(row, screenHeight) + (row.height - size) / 2f;

        if (discovered && entry.getDiscoveryType() == DiscoveryType.ITEM
                && entry.getDiscoveryId() != null
                && session.getItemRegistry().exists(entry.getDiscoveryId())) {
            ui.drawItemSwatch(batch, textures, session.getItemRegistry(), entry.getDiscoveryId(), left, bottom, size);
        } else if (discovered && entry.getDiscoveryType() == DiscoveryType.ENEMY
                && "slime".equals(entry.getDiscoveryId())) {
            GdxBatchUtils.drawSolid(batch, textures, SLIME, left, bottom, size, size);
            GdxBatchUtils.drawSolid(batch, textures, GdxMenuUi.ROW_BORDER, left, bottom, size, 1);
        } else {
            ui.drawItemSwatch(batch, textures, session.getItemRegistry(), null, left, bottom, size);
        }
    }

    private void drawEntryTooltip(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            Rectangle content,
            String tooltip,
            Rectangle anchor,
            int screenHeight
    ) {
        int padding = 10;
        float maxWidth = Math.min(320, content.width - 48);
        List<String> lines = ui.wrapText(font, tooltip, maxWidth);
        float lineH = ui.measureHeight(font, "Ag") + 4;
        float boxWidth = maxWidth + padding * 2;
        float boxHeight = padding * 2 + lines.size() * lineH;

        float boxX = anchor.x + anchor.width + 12;
        float boxTopDownY = anchor.y;
        if (boxX + boxWidth > content.x + content.width - 8) {
            boxX = anchor.x - boxWidth - 12;
        }
        if (boxTopDownY + boxHeight > content.y + content.height - 8) {
            boxTopDownY = content.y + content.height - boxHeight - 8;
        }
        float boxBottom = GdxUiCoords.bottom((int) boxTopDownY, (int) boxHeight, screenHeight);

        GdxBatchUtils.drawSolid(batch, textures, GdxMenuUi.TOOLTIP_BG, boxX, boxBottom, boxWidth, boxHeight);
        GdxBatchUtils.drawSolid(batch, textures, GdxMenuUi.TOOLTIP_BORDER, boxX, boxBottom + boxHeight - 2, boxWidth, 2);

        float textY = boxBottom + boxHeight - padding;
        for (String line : lines) {
            ui.drawText(batch, font, line, TOOLTIP_TEXT, boxX + padding, textY);
            textY -= lineH;
        }
    }

    private static Color accentColor(CollectionCategory category) {
        return switch (category) {
            case WOOD -> new Color(0.55f, 0.35f, 0.17f, 1f);
            case COMBAT -> new Color(0.82f, 0.37f, 0.37f, 1f);
            case FISH -> new Color(0.35f, 0.61f, 0.88f, 1f);
        };
    }
}
