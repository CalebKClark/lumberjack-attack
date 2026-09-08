package lumberjack.ui.hud;

import java.awt.Rectangle;

import lumberjack.core.GameConfig;
import lumberjack.skills.SkillType;

/**
 * Layout helpers for the skills tab list and level snake.
 */
public final class SkillSnakeLayout {

    public static final int LEVEL_COUNT = GameConfig.SKILL_MAX_LEVEL;
    private static final int LEVELS_PER_ROW = 5;
    private static final int NORMAL_WIDTH = 44;
    private static final int NORMAL_HEIGHT = 32;
    private static final int MILESTONE_WIDTH = 60;
    private static final int MILESTONE_HEIGHT = 42;
    private static final int GAP_X = 12;
    private static final int GAP_Y = 18;

    private SkillSnakeLayout() {
    }

    public static Rectangle getSkillRowBounds(Rectangle content, int skillIndex) {
        int rowHeight = 72;
        int y = content.y + 88 + skillIndex * rowHeight;
        return new Rectangle(content.x + 32, y, content.width - 64, rowHeight - 8);
    }

    public static SkillType getSkillAtPoint(int x, int y, Rectangle content) {
        SkillType[] skills = SkillType.values();
        for (int index = 0; index < skills.length; index++) {
            if (getSkillRowBounds(content, index).contains(x, y)) {
                return skills[index];
            }
        }
        return null;
    }

    public static Rectangle getBackButtonBounds(Rectangle content) {
        return new Rectangle(content.x + 24, content.y + 20, 90, 32);
    }

    public static Rectangle getBarBounds(Rectangle content, int level) {
        int index = level - 1;
        int row = index / LEVELS_PER_ROW;
        int colInRow = index % LEVELS_PER_ROW;
        boolean rightToLeft = row % 2 == 1;
        int col = rightToLeft ? (LEVELS_PER_ROW - 1 - colInRow) : colInRow;

        boolean milestone = level % 5 == 0;
        int barWidth = milestone ? MILESTONE_WIDTH : NORMAL_WIDTH;
        int barHeight = milestone ? MILESTONE_HEIGHT : NORMAL_HEIGHT;
        int cellWidth = NORMAL_WIDTH + GAP_X;

        int gridWidth = LEVELS_PER_ROW * cellWidth - GAP_X;
        int startX = content.x + (content.width - gridWidth) / 2;
        int startY = content.y + 88 + row * (NORMAL_HEIGHT + GAP_Y);

        int x = startX + col * cellWidth + (milestone ? (NORMAL_WIDTH - barWidth) / 2 : 0);
        int y = startY + (milestone ? (NORMAL_HEIGHT - barHeight) / 2 : 0);
        return new Rectangle(x, y, barWidth, barHeight);
    }

    public static int getLevelAtPoint(int x, int y, Rectangle content) {
        for (int level = 1; level <= LEVEL_COUNT; level++) {
            if (getBarBounds(content, level).contains(x, y)) {
                return level;
            }
        }
        return -1;
    }
}
