package com.lowdragmc.lowdraglib2;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.RandomSource;
import net.minecraft.resources.ResourceLocation;
import com.lowdragmc.lowdraglib2.client.ClientProxy;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;

public class LDLib2 implements ModInitializer {
    public static final String MOD_ID = "ldlib2";
    public static final String NAME = "LowDragLib2";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    public static final String MODID_RUBIDIUM = "rubidium";
    public static final RandomSource RANDOM = RandomSource.createThreadSafe();
    public static final Gson GSON = new GsonBuilder().create();
    private static File assetsLocation;

    @Override
    public void onInitialize() {
        LDLib2.init();
        
        // Track the server instance in Platform
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Platform.SERVER = server;
            Platform.SERVER_REGISTRY_ACCESS = server.registryAccess();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            Platform.SERVER = null;
            Platform.SERVER_REGISTRY_ACCESS = null;
        });

        CommonListeners.register();
        new CommonProxy();
        

    }

    public static void init() {
        LOGGER.info("{} is initializing on platform: {}", NAME, Platform.platformName());
        getAssetsDir();
    }

    public static File getAssetsDir() {
        if (assetsLocation == null) {
            assetsLocation = new File(Platform.getGamePath().toFile(), "ldlib2/assets");
            if (assetsLocation.mkdirs()) {
                LOGGER.info("Created assets folder {}", assetsLocation.getPath());
            }
            if (new File(assetsLocation, "ldlib2").mkdirs()) {
                LOGGER.info("Created ldlib2 assets folder {}", assetsLocation.getPath());
            }
        }
        return assetsLocation;
    }

    public static boolean isValidResourceLocation(String string) {
        int i = string.indexOf(":");
        if (i == -1) {
            for (int j = 0; j < string.length(); j++) {
                if (!ResourceLocation.isAllowedInResourceLocation(string.charAt(j))) {
                    return false;
                }
            }
        } else {
            var namespace = string.substring(0, i);
            var path = string.substring(i + 1);
            return ResourceLocation.isValidNamespace(namespace) && ResourceLocation.isValidPath(path);
        }
        return true;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean isClient() {
        return Platform.isClient();
    }

    public static boolean isRemote() {
        // Simple heuristic for Fabric
        return FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT;
    }

    public static boolean isServer() {
        return FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER;
    }

    public static boolean isModLoaded(String mod) {
        return Platform.isModLoaded(mod);
    }
}
