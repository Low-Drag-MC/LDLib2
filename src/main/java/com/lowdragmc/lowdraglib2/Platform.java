package com.lowdragmc.lowdraglib2;

import com.lowdragmc.lowdraglib2.utils.ResourceHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class Platform {

    private static final RegistryAccess BLANK_REGISTRY_ACCESS = getBlankRegistryAccess();

    @ApiStatus.Internal
    public static MinecraftServer SERVER = null;

    @ApiStatus.Internal
    public static RegistryAccess SERVER_REGISTRY_ACCESS = null;

    // This is a helper method to check if the ServerLevel is safe to access.
    // @return true if the ServerLevel is not safe to access, otherwise false.
    public static boolean isServerNotSafe() {
        if (Platform.isClient()) {
            return Minecraft.getInstance().getConnection() == null;
        } else {
            var server = getMinecraftServer();
            return server == null || server.isStopped() || !server.isRunning();
        }
    }

    public static String platformName() {
        return "Fabric";
    }

    public static boolean isForge() {
        return false;
    }

    public static boolean isFabric() {
        return true;
    }

    public static boolean isDevEnv() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }



    public static boolean isDatagen() {
        return System.getProperty("fabric-api.datagen") != null;
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    public static MinecraftServer getMinecraftServer() {
        return SERVER;
    }



    public ResourceManager getResourceProvider() {
        return ResourceHelper.getResourceManager();
    }

    public static Path getGamePath() {
        return FabricLoader.getInstance().getGameDir();
    }

    private static RegistryAccess getBlankRegistryAccess() {
        try {
            return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        } catch (Throwable e) {
            return new RegistryAccess.Frozen() {
                @Override
                public <T> @NotNull Optional<Registry<T>> registry(ResourceKey<? extends Registry<? extends T>> p_206220_) {
                    return Optional.empty();
                }

                @Override
                public @NotNull Stream<RegistryEntry<?>> registries() {
                    return Stream.empty();
                }

                @Override
                public @NotNull RegistryAccess.Frozen freeze() {
                    return this;
                }
            };
        }
    }

    public static RegistryAccess getFrozenRegistry() {
        RegistryAccess serverRegistryAccess = SERVER_REGISTRY_ACCESS;
        if (LDLib2.isServer()) {
            RegistryAccess access = serverRegistryAccess == null ? BLANK_REGISTRY_ACCESS : serverRegistryAccess;
            return access == null ? BLANK_REGISTRY_ACCESS : access;
        } else if (LDLib2.isRemote()) {
            if (Minecraft.getInstance().getConnection() != null) {
                return getRegistryFromMultipleSources(Minecraft.getInstance().getConnection().registryAccess(), serverRegistryAccess);
            }
        }
        RegistryAccess access = serverRegistryAccess == null ? getClientRegistryAccess() : serverRegistryAccess;
        return access == null ? BLANK_REGISTRY_ACCESS : access;
    }

    public static RegistryAccess getServerRegistryAccess() {
        return SERVER_REGISTRY_ACCESS == null ? BLANK_REGISTRY_ACCESS : SERVER_REGISTRY_ACCESS;
    }

    public static RegistryAccess getClientRegistryAccess() {
        if (LDLib2.isClient()) {
            if (Minecraft.getInstance().getConnection() != null) {
                return Minecraft.getInstance().getConnection().registryAccess();
            }
        }
        return SERVER_REGISTRY_ACCESS == null ? BLANK_REGISTRY_ACCESS : SERVER_REGISTRY_ACCESS;
    }

    private static RegistryAccess getRegistryFromMultipleSources(RegistryAccess... accesses) {
        return new RegistryAccess() {
            @Override
            public <E> Optional<Registry<E>> registry(ResourceKey<? extends Registry<? extends E>> registryKey) {
                for (RegistryAccess access : accesses) {
                    if (access == null) continue;
                    Optional<Registry<E>> registry = access.registry(registryKey);
                    if (registry.isPresent()) {
                        return registry;
                    }
                }
                return Optional.empty();
            }

            @Override
            public Stream<RegistryEntry<?>> registries() {
                return Arrays.stream(accesses).filter(java.util.Objects::nonNull).flatMap(RegistryAccess::registries);
            }
        };
    }
}
