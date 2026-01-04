package dev.fweigel;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapCoverageTracker implements ModInitializer {
    public static final String MOD_ID = "mapcoveragetracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Map Coverage Tracker initialized");
    }
}