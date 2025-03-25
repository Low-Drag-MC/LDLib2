package com.lowdragmc.lowdraglib;

import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
import com.lowdragmc.lowdraglib.gui.factory.*;
import com.lowdragmc.lowdraglib.kjs.ui.BlockUIJSFactory;
import com.lowdragmc.lowdraglib.kjs.ui.ItemUIJSFactory;
import com.lowdragmc.lowdraglib.networking.LDLNetworking;
import com.lowdragmc.lowdraglib.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib.plugin.LDLibPlugin;
import com.lowdragmc.lowdraglib.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib.test.NoRendererTestBlock;
import com.lowdragmc.lowdraglib.test.TestBlock;
import com.lowdragmc.lowdraglib.test.TestBlockEntity;
import com.lowdragmc.lowdraglib.test.TestItem;
import com.lowdragmc.lowdraglib.utils.ReflectionUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CommonProxy {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, LDLib.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, LDLib.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, LDLib.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestBlockEntity>> TEST_BE_TYPE;

    static {
        if (Platform.isDevEnv()) {
            TEST_BE_TYPE = BLOCK_ENTITY_TYPES.register("test", () -> BlockEntityType.Builder.of(TestBlockEntity::new, TestBlock.BLOCK).build(null));
        } else {
            TEST_BE_TYPE = null;
        }
    }

    public CommonProxy(IEventBus eventBus) {
        if (Platform.isDevEnv()) {
            BLOCKS.register("test", () -> TestBlock.BLOCK);
            ITEMS.register("test", () -> TestItem.ITEM);
            BLOCKS.register("test_2", () -> NoRendererTestBlock.BLOCK);
            ITEMS.register("test_2", () -> new BlockItem(NoRendererTestBlock.BLOCK, new Item.Properties()));
        }

        // used for forge events (ClientProxy + CommonProxy)
        // eventBus.register(this); //TODO: uncomment if @SubscribeEvent is used in this class
        eventBus.addListener(FMLLoadCompleteEvent.class, CommonProxy::loadComplete);
        eventBus.addListener(LDLNetworking::registerPayloads);
        // register server commands
        NeoForge.EVENT_BUS.addListener(this::registerCommand);
        // init common features
        CommonProxy.init();
        // load ldlib plugin
        ReflectionUtils.findAnnotationClasses(LDLibPlugin.class, clazz -> {
            try {
                if (clazz.getConstructor().newInstance() instanceof ILDLibPlugin plugin) {
                    plugin.onLoad();
                }
            } catch (Throwable ignored) {}
        }, () -> {});
        AccessorRegistries.postInit();
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITY_TYPES.register(eventBus);
    }

    public static void init() {
        UIFactory.register(BlockEntityUIFactory.INSTANCE);
        UIFactory.register(HeldItemUIFactory.INSTANCE);
        UIFactory.register(UIEditorFactory.INSTANCE);
        if (LDLib.isKubejsLoaded()) {
            UIFactory.register(BlockUIJSFactory.INSTANCE);
            UIFactory.register(ItemUIJSFactory.INSTANCE);
        }
        AnnotationDetector.init();
        AccessorRegistries.init();
    }

    public static void loadComplete(FMLLoadCompleteEvent e) {
        e.enqueueWork(AccessorRegistries::postInit);
    }

    public void registerCommand(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        ServerCommands.createServerCommands().forEach(dispatcher::register);
    }
}
