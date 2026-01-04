package dev.fweigel.ui;

import dev.fweigel.MapCoverageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CoverageMapRenderer {
    public static final int GRID_MARGIN = 16;
    public static final int GRID_MAX_CELL = 28;

    private CoverageMapRenderer() {
    }

    public static void render(GuiGraphics graphics, Minecraft minecraft, Font font, int panelLeft, int panelTop,
                              int panelRight, int panelBottom, int mouseX, int mouseY, boolean drawLegend,
                              boolean enableTooltip) {
        graphics.fill(panelLeft - 2, panelTop - 2, panelRight + 2, panelBottom + 2, 0xAA000000);

        if (!MapCoverageManager.hasTarget()) {
            graphics.drawCenteredString(font,
                    "Enter coordinates and press Cartograph to see coverage",
                    (panelLeft + panelRight) / 2,
                    (panelTop + panelBottom) / 2,
                    0xFFFFFF);
            return;
        }

        BlockPos start = MapCoverageManager.getTargetStart();
        BlockPos end = MapCoverageManager.getTargetEnd();
        if (start == null || end == null) {
            return;
        }

        int xMin = Math.min(start.getX(), end.getX());
        int xMax = Math.max(start.getX(), end.getX());
        int zMin = Math.min(start.getZ(), end.getZ());
        int zMax = Math.max(start.getZ(), end.getZ());

        int gxMin = MapCoverageManager.toGridIndex(xMin);
        int gxMax = MapCoverageManager.toGridIndex(xMax);
        int gzMin = MapCoverageManager.toGridIndex(zMin);
        int gzMax = MapCoverageManager.toGridIndex(zMax);

        int columns = gxMax - gxMin + 1;
        int rows = gzMax - gzMin + 1;
        if (columns <= 0 || rows <= 0) {
            return;
        }

        int availableWidth = panelRight - panelLeft - GRID_MARGIN;
        int availableHeight = panelBottom - panelTop - GRID_MARGIN;
        if (availableWidth <= 0 || availableHeight <= 0) {
            return;
        }

        double fittedCell = Math.min((double) availableWidth / columns, (double) availableHeight / rows);
        int cellSize = (int) Math.floor(Math.min(GRID_MAX_CELL, fittedCell));
        cellSize = Math.max(1, cellSize);

        int gridWidth = cellSize * columns;
        int gridHeight = cellSize * rows;
        int startX = panelLeft + (availableWidth - gridWidth) / 2;
        int startY = panelTop + (availableHeight - gridHeight) / 2;

        List<Component> hoveredTooltip = null;

        for (int gx = 0; gx < columns; gx++) {
            for (int gz = 0; gz < rows; gz++) {
                int worldGX = gxMin + gx;
                int worldGZ = gzMin + gz;
                int xStart = MapCoverageManager.gridIndexToStart(worldGX);
                int zStart = MapCoverageManager.gridIndexToStart(worldGZ);
                boolean mapped = MapCoverageManager.isMapped(xStart, zStart);
                Integer mapId = MapCoverageManager.getMapId(xStart, zStart);
                int color = mapped ? 0xAA00CC66 : 0xAACC1111;

                int cellLeft = startX + gx * cellSize;
                int cellTop = startY + gz * cellSize;
                int cellRight = cellLeft + cellSize;
                int cellBottom = cellTop + cellSize;

                graphics.fill(cellLeft, cellTop, cellRight, cellBottom, color);
                graphics.renderOutline(cellLeft, cellTop, cellSize, cellSize, 0x66000000);

                if (enableTooltip && mouseX >= cellLeft && mouseX < cellRight && mouseY >= cellTop && mouseY < cellBottom) {
                    hoveredTooltip = buildCellTooltip(xStart, zStart, mapped, mapId);
                }
            }
        }

        drawPlayerMarker(graphics, minecraft, startX, startY, cellSize, gxMin, gzMin, columns, rows);

        if (drawLegend) {
            graphics.drawString(font, MapCoverageManager.MAP_TILE_SIZE + "x" + MapCoverageManager.MAP_TILE_SIZE + " block tiles", startX, startY - 12, 0xFFFFFF);
            graphics.drawString(font, "Green = mapped", startX, panelBottom - 24, 0x00FF66);
            graphics.drawString(font, "Red = unmapped", startX, panelBottom - 12, 0xFF5555);
        }

        if (enableTooltip && hoveredTooltip != null) {
            graphics.setTooltipForNextFrame(font, hoveredTooltip, Optional.empty(), mouseX, mouseY);
        }
    }

    private static void drawPlayerMarker(GuiGraphics graphics, Minecraft minecraft, int startX, int startY, int cellSize,
                                         int gxMin, int gzMin, int columns, int rows) {
        if (minecraft == null) {
            return;
        }

        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        int playerGX = MapCoverageManager.toGridIndex((int) Math.floor(player.getX()));
        int playerGZ = MapCoverageManager.toGridIndex((int) Math.floor(player.getZ()));
        int relativeGX = playerGX - gxMin;
        int relativeGZ = playerGZ - gzMin;

        if (relativeGX < 0 || relativeGX >= columns || relativeGZ < 0 || relativeGZ >= rows) {
            return;
        }

        int iconSize = Math.max(6, Math.min(cellSize, 16));
        int drawX = startX + relativeGX * cellSize + (cellSize - iconSize) / 2;
        int drawY = startY + relativeGZ * cellSize + (cellSize - iconSize) / 2;

        var poseStack = graphics.pose();
        poseStack.pushMatrix();
        poseStack.translate(drawX, drawY);
        float scale = iconSize / 16.0f;
        poseStack.scale(scale, scale);
        graphics.renderItem(new ItemStack(Items.COMPASS), 0, 0);
        poseStack.popMatrix();
    }

    public static List<Component> buildCellTooltip(int xStart, int zStart, boolean mapped, Integer mapId) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(String.format("X: %d to %d", xStart, xStart + MapCoverageManager.MAP_TILE_SIZE - 1)));
        tooltip.add(Component.literal(String.format("Z: %d to %d", zStart, zStart + MapCoverageManager.MAP_TILE_SIZE - 1)));

        if (mapped) {
            String status = "Mapped";
            if (mapId != null) {
                status += String.format(" (Map ID: %d)", mapId);
            }
            tooltip.add(Component.literal(status));
        } else {
            tooltip.add(Component.literal("Not mapped"));
        }
        return tooltip;
    }
}
