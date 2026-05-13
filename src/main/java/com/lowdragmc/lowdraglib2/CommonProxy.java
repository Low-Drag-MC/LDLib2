package com.lowdragmc.lowdraglib2;

import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlock;
import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlockEntity;
import com.lowdragmc.lowdraglib2.gui.factory.LDMenuTypes;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.networking.LDLNetworking;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib2.plugin.LDLibPlugin;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.test.*;
import com.lowdragmc.lowdraglib2.test.gametest.nodegraph.NodeGraphGameTests;
import com.lowdragmc.lowdraglib2.test.gametest.registry.RegistryGameTests;
import com.lowdragmc.lowdraglib2.test.gametest.syncdata.SyncDataGameTests;
import com.lowdragmc.lowdraglib2.test.gametest.ui.UIGameTests;
import com.lowdragmc.lowdraglib2.utils.ReflectionUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CommonProxy {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LDLib2.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LDLib2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, LDLib2.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestBlockEntity>> TEST_BE_TYPE;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RendererBlockEntity>> RENDERER_BE_TYPE;

    static {
        TEST_BE_TYPE = Platform.isDevEnv() ? BLOCK_ENTITY_TYPES.register("test", () -> new BlockEntityType<>(TestBlockEntity::new, TestBlock.BLOCK)) : null;
        RENDERER_BE_TYPE = BLOCK_ENTITY_TYPES.register("renderer_block", () -> new BlockEntityType<>(RendererBlockEntity::new, RendererBlock.BLOCK));
    }

    public CommonProxy(IEventBus eventBus) {
        if (Platform.isDevEnv()) {
            BLOCKS.registerBlock("test", TestBlock::new);
            ITEMS.registerItem("test", TestItem::new);
        }
        BLOCKS.registerBlock("renderer_block", RendererBlock::new);

        // used for forge events (ClientProxy + CommonProxy)
        eventBus.addListener(LDLNetworking::registerPayloads);
        // init common features
        init(eventBus);
        // load ldlib2 plugin
        ReflectionUtils.findAnnotationClasses(LDLibPlugin.class, data -> true, clazz -> {
            try {
                if (clazz.getConstructor().newInstance() instanceof ILDLibPlugin plugin) {
                    plugin.onLoad();
                }
            } catch (Throwable throwable) {
                LDLib2.LOGGER.error("Failed to load plugin {}", clazz.getName(), throwable);
            }
        }, () -> {});
    }

    public void init(IEventBus eventBus) {
        LDLib2Registries.init();
        AccessorRegistries.init();
        RPCPacketDistributor.init();
        PropertyRegistry.init();
        LDMenuTypes.init(eventBus);
        TypeHandles.init();
        if (Platform.isDevEnv()) {
            UIGameTests.init(eventBus);
            NodeGraphGameTests.init(eventBus);
            RegistryGameTests.init(eventBus);
            SyncDataGameTests.init(eventBus);
        }

        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITY_TYPES.register(eventBus);
    }

}
