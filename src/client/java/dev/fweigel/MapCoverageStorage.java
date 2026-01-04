package dev.fweigel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MapCoverageStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String STORAGE_FOLDER = "mapcoveragetracker";
    private static Path activeSaveFile;

    private MapCoverageStorage() {
    }

    public static void loadForWorld(Minecraft client) {
        activeSaveFile = resolveSaveFile(client);
        MapCoverageState loadedState = readState();
        MapCoverageManager.loadState(loadedState);
    }

    public static void save(MapCoverageState state) {
        if (activeSaveFile == null || state == null) {
            return;
        }

        try {
            Files.createDirectories(activeSaveFile.getParent());
            try (Writer writer = Files.newBufferedWriter(activeSaveFile)) {
                GSON.toJson(state, writer);
            }
        } catch (IOException e) {
            MapCoverageTracker.LOGGER.error("Failed to save map coverage data to {}", activeSaveFile, e);
        }
    }

    public static void clearSavedData() {
        if (activeSaveFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(activeSaveFile);
        } catch (IOException e) {
            MapCoverageTracker.LOGGER.error("Failed to delete map coverage save {}", activeSaveFile, e);
        }
    }

    public static Path getExportFile(String extension) {
        if (activeSaveFile == null) {
            return null;
        }

        Path parent = activeSaveFile.getParent();
        if (parent == null) {
            return null;
        }

        String baseName = stripExtension(activeSaveFile.getFileName().toString());
        return parent.resolve(baseName + "." + extension);
    }

    private static MapCoverageState readState() {
        if (activeSaveFile == null || !Files.exists(activeSaveFile)) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(activeSaveFile)) {
            return GSON.fromJson(reader, MapCoverageState.class);
        } catch (IOException | JsonSyntaxException e) {
            MapCoverageTracker.LOGGER.error("Failed to read map coverage data from {}", activeSaveFile, e);
            return null;
        }
    }

    private static Path resolveSaveFile(Minecraft client) {
        String identifier = getWorldIdentifier(client);
        if (identifier == null || identifier.isEmpty()) {
            return null;
        }

        Path baseDir = FabricLoader.getInstance().getGameDir().resolve(STORAGE_FOLDER);
        return baseDir.resolve(identifier + ".json");
    }

    private static String getWorldIdentifier(Minecraft client) {
        if (client.getSingleplayerServer() != null) {
            Path worldDirectory = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT);
            Path worldFolder = normalizeWorldFolder(worldDirectory);

            if (worldFolder != null) {
                return sanitizeFileName(worldFolder.toString());
            }
        }

        ServerData serverData = client.getCurrentServer();
        if (serverData != null) {
            return "Multiplayer_" + sanitizeFileName(serverData.ip);
        }

        return null;
    }

    private static Path normalizeWorldFolder(Path worldDirectory) {
        if (worldDirectory == null) {
            return null;
        }

        Path normalized = worldDirectory.normalize();
        Path worldFolder = normalized.getFileName();

        if (worldFolder == null || worldFolder.toString().isBlank() || ".".equals(worldFolder.toString())) {
            Path parent = normalized.getParent();
            if (parent != null) {
                worldFolder = parent.getFileName();
            }
        }

        return worldFolder;
    }

    private static String sanitizeFileName(String name) {
        if (name == null) {
            return "";
        }

        String sanitized = name.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        return sanitized.isEmpty() ? "world" : sanitized;
    }

    private static String stripExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.') + 1;
        return lastDot > 0 ? fileName.substring(0, lastDot - 1) : fileName;
    }
}
