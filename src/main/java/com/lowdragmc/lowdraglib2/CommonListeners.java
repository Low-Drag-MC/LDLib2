package com.lowdragmc.lowdraglib2;

import com.lowdragmc.lowdraglib2.async.AsyncThreadData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * @author KilaBash
 * @date 2022/11/27
 * @implNote CommonListeners
 */
@EventBusSubscriber(modid = LDLib2.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class CommonListeners {

    @SubscribeEvent
    public static void onWorldUnLoad(LevelEvent.Unload event) {
        LevelAccessor world = event.getLevel();
        if (!world.isClientSide() && world instanceof ServerLevel serverLevel) {
            AsyncThreadData.getOrCreate(serverLevel).releaseExecutorService();
        }
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Platform.FROZEN_REGISTRY_ACCESS = event.getServer().registryAccess();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Platform.FROZEN_REGISTRY_ACCESS = null;
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        var levels = event.getServer().getAllLevels();
        for (var level : levels) {
            if (!level.isClientSide()) {
                AsyncThreadData.getOrCreate(level).releaseExecutorService();
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        ServerCommands.createServerCommands().forEach(dispatcher::register);
    }
}
