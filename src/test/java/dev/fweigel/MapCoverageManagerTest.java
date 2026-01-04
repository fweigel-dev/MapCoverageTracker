package dev.fweigel;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MapCoverageManagerTest {
    @Test
    void alignsCoordinatesToExpectedMapCenters() {
        assertThat(MapCoverageManager.alignToMapCenter(10, (byte) 0)).isEqualTo(0);
        assertThat(MapCoverageManager.alignToMapCenter(317, (byte) 0)).isEqualTo(256);
        assertThat(MapCoverageManager.alignToMapCenter(-200, (byte) 0)).isEqualTo(-256);

        assertThat(MapCoverageManager.alignToMapCenter(0, (byte) 1)).isEqualTo(64);
        assertThat(MapCoverageManager.alignToMapCenter(0, (byte) 2)).isEqualTo(192);
        assertThat(MapCoverageManager.alignToMapCenter(0, (byte) 3)).isEqualTo(448);
        assertThat(MapCoverageManager.alignToMapCenter(0, (byte) 4)).isEqualTo(960);
    }

    @Test
    void anchorsTileBoundsToNorthwestOffsetGrid() {
        assertGridAlignment(0, (byte) 0, 0, -64, 63);
        assertGridAlignment(0, (byte) 1, 64, -64, 191);
        assertGridAlignment(0, (byte) 2, 192, -64, 447);
        assertGridAlignment(0, (byte) 3, 448, -64, 959);
        assertGridAlignment(0, (byte) 4, 960, -64, 1983);

        assertGridAlignment(64, (byte) 0, 128, 64, 191);
        assertGridAlignment(64, (byte) 1, 64, -64, 191);
        assertGridAlignment(64, (byte) 2, 192, -64, 447);
        assertGridAlignment(64, (byte) 3, 448, -64, 959);
        assertGridAlignment(64, (byte) 4, 960, -64, 1983);
        assertGridAlignment(-64, (byte) 1, 64, -64, 191);

        assertGridAlignment(-128, (byte) 0, -128, -192, -65);
        assertGridAlignment(-128, (byte) 1, -192, -320, -65);
        assertGridAlignment(-128, (byte) 2, -320, -576, -65);
        assertGridAlignment(-128, (byte) 3, -576, -1088, -65);
        assertGridAlignment(-128, (byte) 4, -1088, -2112, -65);
    }

    private static void assertGridAlignment(int coordinate, byte scale, int expectedCenter, int expectedNorthwest,
                                            int expectedSoutheast) {
        int center = MapCoverageManager.alignToMapCenter(coordinate, scale);
        int halfSize = (MapCoverageManager.MAP_TILE_SIZE << scale) / 2;
        int northwest = center - halfSize;
        int southeast = center + halfSize - 1;

        assertThat(center).isEqualTo(expectedCenter);
        assertThat(northwest).isEqualTo(expectedNorthwest);
        assertThat(southeast).isEqualTo(expectedSoutheast);

        int gridIndex = MapCoverageManager.toGridIndex(center, scale);
        int start = MapCoverageManager.gridIndexToStart(gridIndex, scale);
        assertThat(start).isEqualTo(expectedNorthwest);
    }

    @Test
    void resolvesMissingCentersFromFallbackPositions() {
        assertThat(MapCoverageManager.resolveMapCenter(0, 317, (byte) 0)).isEqualTo(256);
        assertThat(MapCoverageManager.resolveMapCenter(128, 999, (byte) 0)).isEqualTo(128);
        assertThat(MapCoverageManager.resolveMapCenter(0, null, (byte) 0)).isNull();
    }

    @Test
    void retainsStateSnapshotIndependentlyFromManagerCollections() {
        MapCoverageManager.reset(true);
        MapCoverageManager.setTargetArea(0, 0, 128, 128);
        MapCoverageManager.markMapped(0, 0, 42);

        MapCoverageState snapshot = MapCoverageManager.getStateSnapshot();

        MapCoverageManager.reset(true);

        assertThat(snapshot.targetStartPos()).isEqualTo(new BlockPos(0, 0, 0));
        assertThat(snapshot.targetEndPos()).isEqualTo(new BlockPos(128, 0, 128));
        assertThat(snapshot.mappedAreas()).containsExactly(0L);
        assertThat(snapshot.mapIds()).containsEntry(0L, 42);
    }
}
