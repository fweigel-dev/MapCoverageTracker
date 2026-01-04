package dev.fweigel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MapCoverageManager {
    public static final int MAP_TILE_SIZE = 128;
    public static final int MAP_TILE_RADIUS = MAP_TILE_SIZE / 2;
    private static final Set<Long> mappedAreas = new HashSet<>();
    private static final Map<Long, Integer> mapIds = new HashMap<>();
    private static BlockPos targetStart = null;
    private static BlockPos targetEnd = null;
    private static boolean overlayEnabled = false;
    private static boolean isLoadingState = false;

    private MapCoverageManager() {
    }

    public static void setTarget(BlockPos start, BlockPos end) {
        targetStart = start;
        targetEnd = end;
        overlayEnabled = true;
        saveState();
    }

    public static void setTargetArea(int x1, int z1, int x2, int z2) {
        setTarget(new BlockPos(x1, 0, z1), new BlockPos(x2, 0, z2));
    }

    public void addCoveredRegion(int x, int z) {
        markMapped(x, z, null);
    }

    public static boolean hasTarget() {
        return targetStart != null && targetEnd != null;
    }

    public static boolean isOverlayEnabled() {
        return overlayEnabled && hasTarget();
    }

    public static void toggleOverlay() {
        overlayEnabled = !overlayEnabled && hasTarget();
        saveState();
    }

    public static void setOverlayEnabled(boolean enabled) {
        overlayEnabled = enabled && hasTarget();
        saveState();
    }

    public static BlockPos getTargetStart() {
        return targetStart;
    }

    public static BlockPos getTargetEnd() {
        return targetEnd;
    }

    public static void markMapped(int x, int z) {
        markMapped(x, z, null);
    }

    public static void markMapped(int x, int z, Integer mapId) {
        long key = getGridKey(x, z);
        mappedAreas.add(key);
        if (mapId != null) {
            mapIds.putIfAbsent(key, mapId);
        }
        saveState();
    }

    public static boolean isMapped(int x, int z) {
        return mappedAreas.contains(getGridKey(x, z));
    }

    public static Integer getMapId(int x, int z) {
        return mapIds.get(getGridKey(x, z));
    }

    public static int alignToMapCenter(int coordinate, byte scale) {
        int gridSize = MAP_TILE_SIZE << scale;
        int centerOffset = gridSize / 2 - MAP_TILE_RADIUS;

        return Math.floorDiv(coordinate + MAP_TILE_RADIUS, gridSize) * gridSize + centerOffset;
    }

    public static int toGridIndex(int coordinate, byte scale) {
        int gridSize = MAP_TILE_SIZE << scale;
        return Math.floorDiv(coordinate + MAP_TILE_RADIUS, gridSize);
    }

    public static int toGridIndex(int coordinate) {
        return toGridIndex(coordinate, (byte) 0);
    }

    public static int gridIndexToStart(int gridIndex, byte scale) {
        int gridSize = MAP_TILE_SIZE << scale;
        return gridIndex * gridSize - MAP_TILE_RADIUS;
    }

    public static int gridIndexToStart(int gridIndex) {
        return gridIndexToStart(gridIndex, (byte) 0);
    }

    public static Integer resolveMapCenter(int reportedCenter, Integer fallbackCoordinate, byte scale) {
        if (reportedCenter != 0) {
            return reportedCenter;
        }

        if (fallbackCoordinate == null) {
            return null;
        }

        return alignToMapCenter(fallbackCoordinate, scale);
    }

    public static void trackMapData(MapItemSavedData data, BlockPos fallbackCenter, Integer mapId) {
        if ((data == null && fallbackCenter == null) || (data != null && data.scale != 0)) {
            return;
        }

        byte scale = data != null ? data.scale : 0;
        Integer centerX = resolveMapCenter(data != null ? data.centerX : 0,
                fallbackCenter != null ? fallbackCenter.getX() : null, scale);
        Integer centerZ = resolveMapCenter(data != null ? data.centerZ : 0,
                fallbackCenter != null ? fallbackCenter.getZ() : null, scale);
        if (centerX != null && centerZ != null) {
            markMapped(centerX, centerZ, mapId);
        }
    }

    private static long getGridKey(int x, int z) {
        int gx = toGridIndex(x);
        int gz = toGridIndex(z);
        return (((long) gx) << 32) | (gz & 0xFFFFFFFFL);
    }

    public static void reset(boolean clearSavedData) {
        overlayEnabled = false;
        targetStart = null;
        targetEnd = null;
        mappedAreas.clear();
        mapIds.clear();
        MapCreationTracker.reset();

        if (clearSavedData) {
            MapCoverageStorage.clearSavedData();
        }
    }

    public static void reset() {
        reset(false);
    }

    public static void loadState(MapCoverageState state) {
        isLoadingState = true;
        reset(false);

        if (state != null) {
            targetStart = state.targetStartPos();
            targetEnd = state.targetEndPos();
            overlayEnabled = state.overlayEnabled() && hasTarget();
            mappedAreas.addAll(state.mappedAreasCopy());
            mapIds.putAll(state.mapIdsCopy());
        }

        isLoadingState = false;
    }

    public static MapCoverageState getStateSnapshot() {
        return MapCoverageState.snapshotFrom(targetStart, targetEnd, overlayEnabled,
                new HashSet<>(mappedAreas), new HashMap<>(mapIds));
    }

    private static void saveState() {
        if (!isLoadingState) {
            MapCoverageStorage.save(getStateSnapshot());
        }
    }
}
