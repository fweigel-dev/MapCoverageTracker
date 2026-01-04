package dev.fweigel.ui;

import dev.fweigel.MapCoverageManager;
import dev.fweigel.MapCoverageExporter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class MapConfigScreen extends Screen {
    private EditBox x1Field;
    private EditBox z1Field;
    private EditBox x2Field;
    private EditBox z2Field;
    private static final List<String> COORDINATE_LABELS = List.of("Start X", "Start Z", "End X", "End Z");
    private Button overlayToggleButton;

    public MapConfigScreen() {
        super(Component.literal("Map Coverage Configuration"));
    }

    @Override
    protected void init() {
        int formCenterX = this.width / 4;
        int centerY = this.height / 2 - 20;

        x1Field = new EditBox(this.font, formCenterX - 105, centerY - 60, 100, 20, Component.literal("X1"));
        z1Field = new EditBox(this.font, formCenterX + 5, centerY - 60, 100, 20, Component.literal("Z1"));
        x2Field = new EditBox(this.font, formCenterX - 105, centerY - 20, 100, 20, Component.literal("X2"));
        z2Field = new EditBox(this.font, formCenterX + 5, centerY - 20, 100, 20, Component.literal("Z2"));

        configurePlaceholderBehavior(x1Field, COORDINATE_LABELS.get(0));
        configurePlaceholderBehavior(z1Field, COORDINATE_LABELS.get(1));
        configurePlaceholderBehavior(x2Field, COORDINATE_LABELS.get(2));
        configurePlaceholderBehavior(z2Field, COORDINATE_LABELS.get(3));

        this.addRenderableWidget(x1Field);
        this.addRenderableWidget(z1Field);
        this.addRenderableWidget(x2Field);
        this.addRenderableWidget(z2Field);

        if (MapCoverageManager.hasTarget()) {
            BlockPos start = MapCoverageManager.getTargetStart();
            BlockPos end = MapCoverageManager.getTargetEnd();
            if (start != null && end != null) {
                x1Field.setValue(Integer.toString(start.getX()));
                z1Field.setValue(Integer.toString(start.getZ()));
                x2Field.setValue(Integer.toString(end.getX()));
                z2Field.setValue(Integer.toString(end.getZ()));
            }
        }

        this.addRenderableWidget(Button.builder(Component.literal("Cartograph"), button -> {
            applyInputs();
        }).bounds(formCenterX - 50, centerY + 20, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
            MapCoverageManager.reset(true);
            x1Field.setValue("");
            z1Field.setValue("");
            x2Field.setValue("");
            z2Field.setValue("");
            updateOverlayButtonLabel();
        }).bounds(formCenterX - 50, centerY + 50, 100, 20).build());

        overlayToggleButton = Button.builder(Component.literal(getOverlayButtonLabel()), button -> {
            MapCoverageManager.toggleOverlay();
            updateOverlayButtonLabel();
        }).bounds(formCenterX - 50, centerY + 80, 100, 20).build();
        this.addRenderableWidget(overlayToggleButton);

        this.addRenderableWidget(Button.builder(Component.literal("Export HTML"), button -> {
            try {
                notifyExportResult(MapCoverageExporter.exportHtml(this.minecraft));
            } catch (Exception e) {
                notifyExportResult(null);
                dev.fweigel.MapCoverageTracker.LOGGER.error("Failed to export map coverage", e);
            }
        }).bounds(formCenterX - 50, centerY + 110, 100, 20).build());
    }

    private void applyInputs() {
        boolean updated = false;
        try {
            int x1 = Integer.parseInt(x1Field.getValue());
            int z1 = Integer.parseInt(z1Field.getValue());
            int x2 = Integer.parseInt(x2Field.getValue());
            int z2 = Integer.parseInt(z2Field.getValue());
            MapCoverageManager.setTarget(new BlockPos(x1, 0, z1), new BlockPos(x2, 0, z2));
            updated = true;
        } catch (NumberFormatException ignored) {
        }

        if (updated) {
            updateOverlayButtonLabel();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        super.render(graphics, mouseX, mouseY, delta);

        int titleX = this.width / 4;
        graphics.drawCenteredString(this.font, this.title, titleX, 20, 0xFFFFFF);
        graphics.drawString(this.font, "Start (X/Z)", titleX - 105, this.height / 2 - 92, 0xA0A0A0);
        graphics.drawString(this.font, "End (X/Z)", titleX - 105, this.height / 2 - 52, 0xA0A0A0);

        graphics.drawString(this.font, COORDINATE_LABELS.get(0), x1Field.getX(), x1Field.getY() - 10, 0xFFFFFF, true);
        graphics.drawString(this.font, COORDINATE_LABELS.get(1), z1Field.getX(), z1Field.getY() - 10, 0xFFFFFF, true);
        graphics.drawString(this.font, COORDINATE_LABELS.get(2), x2Field.getX(), x2Field.getY() - 10, 0xFFFFFF, true);
        graphics.drawString(this.font, COORDINATE_LABELS.get(3), z2Field.getX(), z2Field.getY() - 10, 0xFFFFFF, true);

        drawPlayerPosition(graphics, titleX);
        drawCoverageGrid(graphics, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawPlayerPosition(GuiGraphics graphics, int labelX) {
        Minecraft mc = this.minecraft;
        if (mc != null) {
            Player player = mc.player;
            if (player != null) {
                String posText = String.format("Current: %.1f, %.1f", player.getX(), player.getZ());
                graphics.drawCenteredString(this.font, posText, labelX, this.height - 30, 0x00FF00);
            }
        }
    }

    private void configurePlaceholderBehavior(EditBox field, String placeholder) {
        field.setSuggestion(placeholder);
        field.setResponder(value -> field.setSuggestion(value.isEmpty() ? placeholder : ""));
    }

    private void drawCoverageGrid(GuiGraphics graphics, int mouseX, int mouseY) {

        int panelLeft = this.width / 2 + CoverageMapRenderer.GRID_MARGIN;
        int panelTop = 40;
        int panelRight = this.width - CoverageMapRenderer.GRID_MARGIN;
        int panelBottom = this.height - CoverageMapRenderer.GRID_MARGIN;

        CoverageMapRenderer.render(graphics, this.minecraft, this.font, panelLeft, panelTop, panelRight, panelBottom,
                mouseX, mouseY, true, true);
    }

    static java.util.List<String> getCoordinateLabelTexts() {
        return COORDINATE_LABELS;
    }

    static java.util.List<Component> buildCellTooltip(int xStart, int zStart, boolean mapped, Integer mapId) {
        return CoverageMapRenderer.buildCellTooltip(xStart, zStart, mapped, mapId);
    }

    private void updateOverlayButtonLabel() {
        if (overlayToggleButton != null) {
            overlayToggleButton.setMessage(Component.literal(getOverlayButtonLabel()));
        }
    }

    private String getOverlayButtonLabel() {
        return MapCoverageManager.isOverlayEnabled() ? "Unpin Overlay" : "Pin Overlay";
    }

    private void notifyExportResult(java.nio.file.Path exportPath) {
        Minecraft mc = this.minecraft;
        if (mc == null || mc.player == null) {
            return;
        }

        if (exportPath == null) {
            mc.player.displayClientMessage(Component.literal("Failed to export map grid. Ensure a target area is set."), false);
            return;
        }

        mc.player.displayClientMessage(Component.literal("Exported map grid to " + exportPath.toAbsolutePath()), false);
    }
}
