package com.lowdragmc.lowdraglib2;

import com.lowdragmc.lowdraglib2.async.AsyncThreadData;
import com.lowdragmc.lowdraglib2.editor.resource.PackResourceManager;
import com.lowdragmc.lowdraglib2.test.NoRendererTestBlock;
import com.lowdragmc.lowdraglib2.test.TestItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class CommonListeners {

    public static void register() {
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            AsyncThreadData.getOrCreate(world).releaseExecutorService();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (var level : server.getAllLevels()) {
                AsyncThreadData.getOrCreate(level).releaseExecutorService();
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ServerCommands.createServerCommands().forEach(dispatcher::register);
        });

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(PackResourceManager.INSTANCE);

        if (Platform.isDevEnv()) {
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, LDLib2.id("ldlib2_dev_tab"),
                    FabricItemGroup.builder()
                            .title(Component.translatable("itemGroup.ldlib2.dev_tab"))
                            .icon(() -> new ItemStack(TestItem.ITEM.getBlock()))
                            .displayItems((parameters, output) -> {
                                output.accept(TestItem.ITEM.getBlock());
                                output.accept(NoRendererTestBlock.BLOCK);
                            })
                            .build());
        }
    }
}
