package dev.fweigel.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class MapItemIdOverlayMixin {
    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
    private void renderMapIdOverlay(Font font, ItemStack stack, int x, int y, String countLabel, CallbackInfo ci) {
        if (!stack.is(Items.FILLED_MAP)) {
            return;
        }

        MapId mapId = stack.get(DataComponents.MAP_ID);
        if (mapId == null) {
            return;
        }

        String idText = String.valueOf(mapId.id());
        float scale = idText.length() > 3 ? 0.7F : 1.0F;
        int textWidth = font.width(idText);
        int textHeight = font.lineHeight;
        int scaledTextWidth = (int) Math.ceil(textWidth * scale);
        int scaledTextHeight = (int) Math.ceil(textHeight * scale);

        int padding = 2;
        int boxLeft = x - 1;
        int boxTop = y - 1;
        int boxRight = x + scaledTextWidth + padding - 1;
        int boxBottom = y + scaledTextHeight + padding - 1;

        GuiGraphics graphics = (GuiGraphics) (Object) this;
        var poseStack = graphics.pose();
        poseStack.pushMatrix();

        graphics.fill(boxLeft, boxTop, boxRight, boxBottom, 0xAA000000);
        poseStack.translate(x + 1, y);
        poseStack.scale(scale, scale);
        graphics.drawString(font, idText, 0, 0, 0xFFFFFFFF, true);

        poseStack.popMatrix();
    }
}
