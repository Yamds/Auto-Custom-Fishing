package com.autofishing;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoFishingMod implements ModInitializer {
    public static final String MOD_ID = "autofishing";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Auto Fishing Mod initialized!");
    }
}
