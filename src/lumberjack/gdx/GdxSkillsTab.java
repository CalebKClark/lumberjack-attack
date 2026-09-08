package lumberjack.gdx;

import java.awt.Rectangle;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lumberjack.game.GameSession;
import lumberjack.skills.PlayerSkills;
import lumberjack.skills.SkillLevelRegistry;
import lumberjack.skills.SkillType;
import lumberjack.skills.SkillXpTable;
import lumberjack.ui.hud.SkillSnakeLayout;

/**
 * Native Skills tab (list + level snake).
 */
public final class GdxSkillsTab {

    private static final Color LOCKED_BAR = new Color(0.23f, 0.24f, 0.27f, 1f);
    private static final Color LOCKED_MILESTONE = new Color(0.31f, 0.27f, 0.2f, 1f);
    private static final Color UNLOCKED_TEXT = new Color(0.47f, 0.86f, 0.55f, 1f);
    private static final Color LOCKED_TEXT = new Color(0.86f, 0.47f, 0.47f, 1f);

    private final GdxMenuUi ui = new GdxMenuUi();

    public void draw(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            Rectangle content,
            SkillType focusedSkill,
            Integer hoveredSkillLevel,
            int screenHeight
    ) {
        if (focusedSkill == null) {
            drawSkillList(batch, font, textures, session, content, screenHeight);
        } else {
            drawSkillSnake(batch, font, textures, session, content, focusedSkill, hoveredSkillLevel, screenHeight);
        }
    }

    private void drawSkillList(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            Rectangle content,
            int screenHeight
    ) {
        PlayerSkills skills = session.getPlayerSkills();
        float top = GdxUiCoords.bottom(content, screenHeight) + content.height;

        ui.drawText(batch, font, "Skills", GdxMenuUi.HUD_TEXT, content.x + 24, top - 24);
        ui.drawText(batch, font, "Click a skill to view its level path", GdxMenuUi.SUBTEXT, content.x + 24, top - 52);

        SkillType[] skillTypes = SkillType.values();
        for (int index = 0; index < skillTypes.length; index++) {
            SkillType skillType = skillTypes[index];
            Rectangle row = SkillSnakeLayout.getSkillRowBounds(content, index);
            ui.drawAccentRow(batch, textures, row, accentColor(skillType), screenHeight);

            float rowBottom = GdxUiCoords.bottom(row, screenHeight);
            ui.drawText(batch, font, skillType.getDisplayName(), GdxMenuUi.HUD_TEXT, row.x + 20, rowBottom + row.height - 14);

            int level = skills.getLevel(skillType);
            int xpToNext = skills.getXpToNextLevel(skillType);
            String progress = level >= SkillXpTable.getMaxLevel()
                    ? "Level " + level + " (Max)"
                    : "Level " + level + "  |  " + xpToNext + " XP to next";
            ui.drawText(batch, font, progress, GdxMenuUi.SUBTEXT, row.x + 20, rowBottom + 16);
        }
    }

    private void drawSkillSnake(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            GameSession session,
            Rectangle content,
            SkillType skillType,
            Integer hoveredSkillLevel,
            int screenHeight
    ) {
        PlayerSkills skills = session.getPlayerSkills();
        SkillLevelRegistry levelRegistry = session.getSkillLevelRegistry();
        int playerLevel = skills.getLevel(skillType);
        Color accent = accentColor(skillType);
        Color milestoneAccent = milestoneColor(skillType);
        float top = GdxUiCoords.bottom(content, screenHeight) + content.height;

        ui.drawBackButton(batch, font, textures, SkillSnakeLayout.getBackButtonBounds(content), screenHeight);
        ui.drawText(batch, font, skillType.getDisplayName(), GdxMenuUi.HUD_TEXT, content.x + 130, top - 24);
        ui.drawText(
                batch,
                font,
                "Level " + playerLevel + " / " + SkillXpTable.getMaxLevel(),
                GdxMenuUi.SUBTEXT,
                content.x + 130,
                top - 52
        );

        for (int level = 1; level <= SkillSnakeLayout.LEVEL_COUNT; level++) {
            Rectangle bar = SkillSnakeLayout.getBarBounds(content, level);
            boolean unlocked = playerLevel >= level;
            boolean milestone = level % 5 == 0;
            Color fill = unlocked
                    ? (milestone ? milestoneAccent : accent)
                    : (milestone ? LOCKED_MILESTONE : LOCKED_BAR);
            ui.drawRect(batch, textures, fill, bar, screenHeight);
            ui.drawBorder(batch, textures, new Color(1f, 1f, 1f, unlocked ? 0.35f : 0.14f), bar, screenHeight);

            String label = String.valueOf(level);
            float textW = ui.measureWidth(font, label);
            float textH = ui.measureHeight(font, label);
            ui.drawText(
                    batch,
                    font,
                    label,
                    GdxMenuUi.HUD_TEXT,
                    bar.x + (bar.width - textW) / 2f,
                    GdxUiCoords.bottom(bar, screenHeight) + (bar.height + textH) / 2f
            );
        }

        if (hoveredSkillLevel != null && hoveredSkillLevel > 0) {
            drawLevelTooltip(
                    batch,
                    font,
                    textures,
                    content,
                    skillType,
                    hoveredSkillLevel,
                    playerLevel,
                    levelRegistry.getRewardText(skillType, hoveredSkillLevel),
                    SkillSnakeLayout.getBarBounds(content, hoveredSkillLevel),
                    screenHeight
            );
        }
    }

    private void drawLevelTooltip(
            SpriteBatch batch,
            BitmapFont font,
            GdxTextureCache textures,
            Rectangle content,
            SkillType skillType,
            int level,
            int playerLevel,
            String reward,
            Rectangle anchor,
            int screenHeight
    ) {
        boolean unlocked = playerLevel >= level;
        String status = unlocked ? "Unlocked" : "Locked";
        String title = "Level " + level;

        int padding = 10;
        float maxTextWidth = Math.max(220, content.width / 2f);
        List<String> rewardLines = ui.wrapText(font, reward, maxTextWidth);
        float boxWidth = maxTextWidth + padding * 2;
        for (String line : rewardLines) {
            boxWidth = Math.max(boxWidth, ui.measureWidth(font, line) + padding * 2);
        }
        boxWidth = Math.max(boxWidth, ui.measureWidth(font, title) + padding * 2);
        boxWidth = Math.max(boxWidth, ui.measureWidth(font, status) + padding * 2);

        float lineH = ui.measureHeight(font, "Ag") + 4;
        float boxHeight = padding * 2 + lineH * (2 + rewardLines.size());

        float boxX = anchor.x + anchor.width + 12;
        float boxTopDownY = anchor.y;
        if (boxX + boxWidth > content.x + content.width - 8) {
            boxX = anchor.x - boxWidth - 12;
        }
        if (boxTopDownY + boxHeight > content.y + content.height - 8) {
            boxTopDownY = content.y + content.height - boxHeight - 8;
        }
        if (boxTopDownY < content.y + 8) {
            boxTopDownY = content.y + 8;
        }
        float boxBottom = GdxUiCoords.bottom((int) boxTopDownY, (int) boxHeight, screenHeight);

        GdxBatchUtils.drawSolid(batch, textures, GdxMenuUi.TOOLTIP_BG, boxX, boxBottom, boxWidth, boxHeight);
        GdxBatchUtils.drawSolid(batch, textures, GdxMenuUi.TOOLTIP_BORDER, boxX, boxBottom + boxHeight - 2, boxWidth, 2);

        float textY = boxBottom + boxHeight - padding;
        ui.drawText(batch, font, title, accentColor(skillType), boxX + padding, textY);
        textY -= lineH;
        for (String line : rewardLines) {
            ui.drawText(batch, font, line, GdxMenuUi.HUD_TEXT, boxX + padding, textY);
            textY -= lineH;
        }
        ui.drawText(batch, font, status, unlocked ? UNLOCKED_TEXT : LOCKED_TEXT, boxX + padding, textY);
    }

    private static Color accentColor(SkillType skillType) {
        return switch (skillType) {
            case FORAGING -> new Color(0.35f, 0.71f, 0.37f, 1f);
            case COMBAT -> new Color(0.82f, 0.37f, 0.37f, 1f);
            case FISHING -> new Color(0.35f, 0.61f, 0.88f, 1f);
        };
    }

    private static Color milestoneColor(SkillType skillType) {
        return switch (skillType) {
            case FORAGING -> new Color(0.86f, 0.75f, 0.31f, 1f);
            case COMBAT -> new Color(0.9f, 0.59f, 0.27f, 1f);
            case FISHING -> new Color(0.51f, 0.82f, 0.9f, 1f);
        };
    }
}
