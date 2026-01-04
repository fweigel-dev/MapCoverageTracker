package dev.fweigel.ui;

import dev.fweigel.MapCoverageManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class MapOverlayRenderer {
    private MapOverlayRenderer() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!MapCoverageManager.isOverlayEnabled()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getWindow() == null) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int panelMargin = CoverageMapRenderer.GRID_MARGIN;
        int panelWidth = Math.min(240, screenWidth - panelMargin * 2);
        int panelHeight = Math.min(200, screenHeight / 2);

        int panelRight = screenWidth - panelMargin;
        int panelLeft = panelRight - panelWidth;
        int panelTop = panelMargin;
        int panelBottom = Math.min(screenHeight - panelMargin, panelTop + panelHeight);

        CoverageMapRenderer.render(graphics, minecraft, minecraft.font, panelLeft, panelTop, panelRight, panelBottom,
                -1, -1, true, false);
    }
}
