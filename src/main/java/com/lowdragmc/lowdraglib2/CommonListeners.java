package com.lowdragmc.lowdraglib2;

import com.lowdragmc.lowdraglib2.async.AsyncThreadData;
import com.lowdragmc.lowdraglib2.editor.resource.PackResourceManager;
import com.lowdragmc.lowdraglib2.gui.event.ContainerMenuEvent;
import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolderMenu;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
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
        Platform.SERVER_REGISTRY_ACCESS = event.getServer().registryAccess();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Platform.SERVER_REGISTRY_ACCESS = null;
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

    @SubscribeEvent
    public static void onAddReloadListenerEvent(AddReloadListenerEvent event) {
        event.addListener(PackResourceManager.INSTANCE);
    }

//    @SubscribeEvent
//    public static void onContainerMenuCreateEvent(ContainerMenuEvent.Create event) {
//        if (event.menu instanceof CraftingMenu craftingMenu && craftingMenu instanceof IModularUIHolderMenu uiHolderMenu) {
//            var player = event.player;
//            var mui = ModularUI.of(UI.of(
//                    // root
//                    new UIElement().layout(l -> l.width(176).height(166)).addChildren(
//                            new ItemSlot().bind(new Slot(event.player.getInventory(), 10, 0, 0)),
//                            new Label().bind(DataBindingBuilder.componentS2C(player::getDisplayName).build()),
//                            new Toggle().bind(DataBindingBuilder.bool(player::isCreative, isCreative -> {
//                                if (player instanceof ServerPlayer serverPlayer) {
//                                    serverPlayer.setGameMode(isCreative ? GameType.CREATIVE : GameType.DEFAULT_MODE);
//                                }
//                            }).build())
//                    )), player);
//            uiHolderMenu.setModularUI(mui);
//        }
//    }
}
