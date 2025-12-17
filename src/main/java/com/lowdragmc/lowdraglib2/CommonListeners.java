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
import com.lowdragmc.lowdraglib2.test.NoRendererTestBlock;
import com.lowdragmc.lowdraglib2.test.TestItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/11/27
 * @implNote CommonListeners
 */
@EventBusSubscriber(modid = LDLib2.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class CommonListeners {

    public static class ModCreativeModeTab {
        // Deferred register for creative tabs
        public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
                DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LDLib2.MOD_ID);

        // Supplier for your dev-only tab
        public static final Supplier<CreativeModeTab> LDLIB2_DEV_TAB =
                CREATIVE_MODE_TABS.register("ldlib2_dev_tab", () -> {
                    // Only create the tab in dev environment
                    if (!Platform.isDevEnv()) return null;

                    return CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.ldlib2.dev_tab"))
                            .icon(() -> new ItemStack(TestItem.ITEM.getBlock()))
                            .displayItems((parameters, output) -> {
                                // Add dev-only items here
                                output.accept(TestItem.ITEM.getBlock());
                                output.accept(NoRendererTestBlock.BLOCK);
                            })
                            .build();
                });

        // Method to hook the deferred register to the event bus
        public static void register(IEventBus eventBus) {
            CREATIVE_MODE_TABS.register(eventBus);
        }
    }

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

    @SubscribeEvent
    public static void onAddReloadListenerEvent(AddReloadListenerEvent event) {
        event.addListener(PackResourceManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onContainerMenuCreateEvent(ContainerMenuEvent.Create event) {
        if (event.menu instanceof CraftingMenu craftingMenu && craftingMenu instanceof IModularUIHolderMenu uiHolderMenu) {
            var player = event.player;
            var mui = ModularUI.of(UI.of(
                    // root
                    new UIElement().layout(l -> l.width(176).height(166)).addChildren(
                            new ItemSlot().bind(new Slot(event.player.getInventory(), 10, 0, 0)),
                            new Label().bind(DataBindingBuilder.componentS2C(player::getDisplayName).build()),
                            new Toggle().bind(DataBindingBuilder.bool(player::isCreative, isCreative -> {
                                if (player instanceof ServerPlayer serverPlayer) {
                                    serverPlayer.setGameMode(isCreative ? GameType.CREATIVE : GameType.DEFAULT_MODE);
                                }
                            }).build())
                    )), player);
            uiHolderMenu.setModularUI(mui);
        }
    }
}
