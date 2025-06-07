package com.lowdragmc.lowdraglib2;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;

public class Platform {

    private static final RegistryAccess BLANK = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    // This is a helper method to check if the ServerLevel is safe to access.
    // @return true if the ServerLevel is not safe to access, otherwise false.
    public static boolean isServerNotSafe() {
        if (Platform.isClient()) {
            return Minecraft.getInstance().getConnection() == null;
        } else {
            var server = getMinecraftServer();
            return server == null || server.isStopped() || server.isShutdown() || !server.isRunning() || server.isCurrentlySaving();
        }
    }
    @ApiStatus.Internal
    public static RegistryAccess FROZEN_REGISTRY_ACCESS = BLANK;


    public static String platformName() {
        return "NeoForge";
    }

    public static boolean isForge() {
        return true;
    }

    public static boolean isDevEnv() {
        return !FMLLoader.isProduction();
    }

    public static boolean isDatagen() {
        return DatagenModLoader.isRunningDataGen();
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    public static MinecraftServer getMinecraftServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    public static Path getGamePath() {
        return FMLLoader.getGamePath();
    }

    public static RegistryAccess getFrozenRegistry() {
        if (FROZEN_REGISTRY_ACCESS != null) {
            return FROZEN_REGISTRY_ACCESS;
        } else if (LDLib2.isRemote()) {
            if (Minecraft.getInstance().getConnection() != null) {
                return Minecraft.getInstance().getConnection().registryAccess();
            }
        }
        return BLANK;
    }

}
