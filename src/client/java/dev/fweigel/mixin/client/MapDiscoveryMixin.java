package dev.fweigel.mixin.client;

import dev.fweigel.MapCoverageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MapDiscoveryMixin {
    @Inject(method = "handleMapItemData", at = @At("RETURN"))
    private void onMapUpdate(ClientboundMapItemDataPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        MapItemSavedData state = client.level.getMapData(packet.mapId());
        MapCoverageManager.trackMapData(state, null, packet.mapId().id());
    }
}
