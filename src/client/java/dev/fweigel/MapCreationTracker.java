package dev.fweigel;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class MapCreationTracker {
    private static final MapCreationTracker INSTANCE = new MapCreationTracker();
    private static final long MAX_QUEUE_AGE_TICKS = 100;
    private final Deque<PendingCreation> pendingCenters = new ArrayDeque<>();

    public static void recordCreation(int x, int z, byte scale, ResourceKey<Level> dimension, long gameTime) {
        int alignedX = MapCoverageManager.alignToMapCenter(x, scale);
        int alignedZ = MapCoverageManager.alignToMapCenter(z, scale);
        INSTANCE.pendingCenters.addLast(new PendingCreation(new BlockPos(alignedX, 0, alignedZ), dimension, gameTime));
    }

    public static BlockPos consumeNextCenter(ResourceKey<Level> dimension, long currentGameTime) {
        INSTANCE.dropExpired(currentGameTime);

        Iterator<PendingCreation> iterator = INSTANCE.pendingCenters.iterator();
        while (iterator.hasNext()) {
            PendingCreation creation = iterator.next();
            if (creation.dimension().equals(dimension) && isFresh(creation, currentGameTime)) {
                iterator.remove();
                return creation.center();
            }
        }

        return null;
    }

    public static BlockPos peekNextCenter() {
        PendingCreation creation = INSTANCE.pendingCenters.peekFirst();
        return creation != null ? creation.center() : null;
    }

    public static void reset() {
        INSTANCE.pendingCenters.clear();
    }

    public static boolean hasPending() {
        return !INSTANCE.pendingCenters.isEmpty();
    }

    private void dropExpired(long currentGameTime) {
        while (!pendingCenters.isEmpty() && !isFresh(pendingCenters.peekFirst(), currentGameTime)) {
            pendingCenters.pollFirst();
        }
    }

    private static boolean isFresh(PendingCreation creation, long currentGameTime) {
        return currentGameTime - creation.recordedAt() <= MAX_QUEUE_AGE_TICKS;
    }

    private record PendingCreation(BlockPos center, ResourceKey<Level> dimension, long recordedAt) {
    }
}
