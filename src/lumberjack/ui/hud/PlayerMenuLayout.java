package lumberjack.ui.hud;

import java.awt.Rectangle;

/**
 * Layout for the tabbed player menu overlay (inventory, crafting, stats, etc.).
 */
public final class PlayerMenuLayout {

    private PlayerMenuLayout() {
    }

    public static Rectangle getMenuPanelBounds(int screenWidth, int screenHeight) {
        int width = Math.min(HudTheme.MENU_PANEL_WIDTH, screenWidth - HudTheme.INVENTORY_PANEL_PADDING * 2);
        int height = Math.min(HudTheme.MENU_PANEL_HEIGHT, screenHeight - HudTheme.INVENTORY_PANEL_PADDING * 2);
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height) / 2;
        return new Rectangle(x, y, width, height);
    }

    public static Rectangle getTabBarBounds(Rectangle menuPanel) {
        return new Rectangle(
                menuPanel.x,
                menuPanel.y,
                HudTheme.MENU_TAB_BAR_WIDTH,
                menuPanel.height
        );
    }

    public static Rectangle getContentBounds(Rectangle menuPanel) {
        return new Rectangle(
                menuPanel.x + HudTheme.MENU_TAB_BAR_WIDTH,
                menuPanel.y,
                menuPanel.width - HudTheme.MENU_TAB_BAR_WIDTH,
                menuPanel.height
        );
    }

    public static Rectangle getTabBounds(Rectangle menuPanel, int tabIndex) {
        Rectangle tabBar = getTabBarBounds(menuPanel);
        int tabHeight = tabBar.height / PlayerMenuTab.MAX_TABS;
        int y = tabBar.y + tabIndex * tabHeight;
        return new Rectangle(tabBar.x, y, tabBar.width, tabHeight);
    }

    /**
     * @return tab index, or -1 if the point is not on a visible tab
     */
    public static int getTabIndexAtPoint(int x, int y, int screenWidth, int screenHeight) {
        Rectangle menuPanel = getMenuPanelBounds(screenWidth, screenHeight);
        int tabCount = PlayerMenuTab.values().length;

        for (int tabIndex = 0; tabIndex < tabCount; tabIndex++) {
            if (getTabBounds(menuPanel, tabIndex).contains(x, y)) {
                return tabIndex;
            }
        }

        return -1;
    }
}
