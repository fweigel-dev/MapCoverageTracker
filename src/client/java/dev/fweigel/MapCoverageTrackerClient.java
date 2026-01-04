package dev.fweigel;

import dev.fweigel.ui.MapConfigScreen;
import dev.fweigel.ui.MapOverlayRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public class MapCoverageTrackerClient implements ClientModInitializer {
    private static KeyMapping configKey;
    private static final Set<Integer> seenMapIds = new HashSet<>();

    @Override
    public void onInitializeClient() {
        configKey = registerConfigKey();

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() && stack.is(Items.MAP)) {
                BlockPos position = player.blockPosition();
                MapCreationTracker.recordCreation(position.getX(), position.getZ(), (byte) 0,
                        world.dimension(), world.getGameTime());
            }
            return InteractionResult.PASS;
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            seenMapIds.clear();
            MapCoverageStorage.loadForWorld(client);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            seenMapIds.clear();
            MapCoverageManager.reset(false);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configKey.consumeClick()) {
                client.setScreen(new MapConfigScreen());
            }

            if (client.player == null || client.level == null) {
                return;
            }

            boolean shouldScan = MapCreationTracker.hasPending() || client.player.tickCount % 5 == 0;
            if (shouldScan) {
                handleInventoryMaps(client);
            }
        });

        HudRenderCallback.EVENT.register(MapOverlayRenderer::render);
    }

    private static KeyMapping registerConfigKey() {
        KeyMapping.Category keyCategory = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MapCoverageTracker.MOD_ID, "general"));
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mapcoveragetracker.config",
                GLFW.GLFW_KEY_M,
                keyCategory));
    }

    private static void handleInventoryMaps(net.minecraft.client.Minecraft client) {
        Level level = client.level;
        for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (!stack.is(Items.FILLED_MAP)) {
                continue;
            }

            MapId mapId = stack.get(DataComponents.MAP_ID);
            if (mapId == null) {
                continue;
            }

            boolean isNewMap = seenMapIds.add(mapId.id());
            BlockPos fallbackCenter = isNewMap ? MapCreationTracker.consumeNextCenter(level.dimension(),
                    level.getGameTime()) : null;

            MapItemSavedData data = level.getMapData(mapId);
            MapCoverageManager.trackMapData(data, fallbackCenter, mapId.id());
        }
    }
}
