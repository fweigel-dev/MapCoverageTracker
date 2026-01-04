package dev.fweigel;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MapCoverageExporter {
    private MapCoverageExporter() {
    }

    public static Path exportHtml(Minecraft minecraft) throws IOException {
        if (minecraft == null || !MapCoverageManager.hasTarget()) {
            return null;
        }

        Path exportPath = MapCoverageStorage.getExportFile("html");
        if (exportPath == null) {
            return null;
        }

        BlockPos start = MapCoverageManager.getTargetStart();
        BlockPos end = MapCoverageManager.getTargetEnd();
        if (start == null || end == null) {
            return null;
        }

        Files.createDirectories(exportPath.getParent());
        String htmlContent = buildHtml(start, end);
        Files.writeString(exportPath, htmlContent, StandardCharsets.UTF_8);
        return exportPath;
    }

    private static String buildHtml(BlockPos start, BlockPos end) {
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

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">");
        html.append("<title>Map Coverage Export</title>");
        html.append("<style>body{font-family:Arial,sans-serif;background:#111;color:#eee;padding:16px;}");
        html.append(".meta{margin-bottom:12px;}");
        html.append(".grid{display:grid;grid-template-columns:repeat(").append(columns)
                .append(",minmax(80px,1fr));gap:4px;}");
        html.append(".cell{border:1px solid #333;border-radius:4px;padding:6px;text-align:center;}");
        html.append(".mapped{background:#145a32;color:#f0fff4;}");
        html.append(".unmapped{background:#7b241c;color:#fff5f5;}");
        html.append(".coords{font-size:0.8em;opacity:0.85;margin-top:4px;}");
        html.append(".legend{margin-top:12px;display:flex;gap:12px;align-items:center;}");
        html.append(".legend-item{display:flex;gap:6px;align-items:center;}");
        html.append(".swatch{width:16px;height:16px;border-radius:3px;border:1px solid #222;}");
        html.append("</style></head><body>");

        html.append("<h1>Map Coverage Export</h1>");
        html.append("<div class=\"meta\">Target area: X ").append(xMin).append(" to ").append(xMax)
                .append(", Z ").append(zMin).append(" to ").append(zMax).append("</div>");
        html.append("<div class=\"meta\">Each cell represents a ")
                .append(MapCoverageManager.MAP_TILE_SIZE).append("x")
                .append(MapCoverageManager.MAP_TILE_SIZE).append(" block map.</div>");
        html.append("<div class=\"grid\">");

        for (int gz = 0; gz < rows; gz++) {
            for (int gx = 0; gx < columns; gx++) {
                int worldGX = gxMin + gx;
                int worldGZ = gzMin + gz;
                int cellXStart = MapCoverageManager.gridIndexToStart(worldGX);
                int cellZStart = MapCoverageManager.gridIndexToStart(worldGZ);

                boolean mapped = MapCoverageManager.isMapped(cellXStart, cellZStart);
                Integer mapId = MapCoverageManager.getMapId(cellXStart, cellZStart);
                String cssClass = mapped ? "mapped" : "unmapped";
                String status = mapId != null ? "Map ID: " + mapId : (mapped ? "Mapped" : "Unmapped");

                html.append("<div class=\"cell ").append(cssClass).append("\">");
                html.append(status);
                html.append("<div class=\"coords\">X: ").append(cellXStart).append(" to ")
                        .append(cellXStart + MapCoverageManager.MAP_TILE_SIZE - 1);
                html.append("<br>Z: ").append(cellZStart).append(" to ")
                        .append(cellZStart + MapCoverageManager.MAP_TILE_SIZE - 1);
                html.append("</div></div>");
            }
        }

        html.append("</div>");
        html.append("<div class=\"legend\">");
        html.append("<div class=\"legend-item\"><div class=\"swatch mapped\"></div><span>Mapped</span></div>");
        html.append("<div class=\"legend-item\"><div class=\"swatch unmapped\"></div><span>Unmapped</span></div>");
        html.append("</div>");
        html.append("</body></html>");
        return html.toString();
    }
}
