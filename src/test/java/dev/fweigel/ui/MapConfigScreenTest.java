package dev.fweigel.ui;

import dev.fweigel.MapCoverageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MapConfigScreenTest {
    @AfterEach
    void resetCoverageManager() {
        MapCoverageManager.reset();
    }

    @Test
    void coordinateLabelsExposeAllFields() {
        assertThat(MapConfigScreen.getCoordinateLabelTexts())
                .containsExactly("Start X", "Start Z", "End X", "End Z");
    }

    @Test
    void tooltipMatchesMappingState() {
        List<Component> mappedTooltip = MapConfigScreen.buildCellTooltip(0, 0, true, 42);
        assertThat(mappedTooltip)
                .extracting(Component::getString)
                .containsExactly("X: 0 to 127", "Z: 0 to 127", "Mapped (Map ID: 42)");

        List<Component> unmappedTooltip = MapConfigScreen.buildCellTooltip(128, -256, false, null);
        assertThat(unmappedTooltip)
                .extracting(Component::getString)
                .containsExactly("X: 128 to 255", "Z: -256 to -129", "Not mapped");
    }

    @Test
    void settingTargetAreaTracksRequestedBounds() {
        MapCoverageManager.setTargetArea(-64, 32, 192, 160);
        BlockPos start = MapCoverageManager.getTargetStart();
        BlockPos end = MapCoverageManager.getTargetEnd();

        assertThat(MapCoverageManager.hasTarget()).isTrue();
        assertThat(start).isEqualTo(new BlockPos(-64, 0, 32));
        assertThat(end).isEqualTo(new BlockPos(192, 0, 160));

        MapCoverageManager.markMapped(-64, 32, 7);
        assertThat(MapCoverageManager.isMapped(-64, 32)).isTrue();
        assertThat(MapCoverageManager.getMapId(-64, 32)).isEqualTo(7);
    }
}
