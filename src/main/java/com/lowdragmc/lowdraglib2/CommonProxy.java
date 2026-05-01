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
import com.lowdragmc.lowdraglib2.utils.ReflectionUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CommonProxy {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, LDLib2.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, LDLib2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, LDLib2.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestBlockEntity>> TEST_BE_TYPE;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RendererBlockEntity>> RENDERER_BE_TYPE;

    static {
        TEST_BE_TYPE = Platform.isDevEnv() ? BLOCK_ENTITY_TYPES.register("test", () -> BlockEntityType.Builder.of(TestBlockEntity::new, TestBlock.BLOCK).build(null)) : null;
        RENDERER_BE_TYPE = BLOCK_ENTITY_TYPES.register("renderer_block", () -> BlockEntityType.Builder.of(RendererBlockEntity::new, RendererBlock.BLOCK).build(null));
    }

    public CommonProxy(IEventBus eventBus) {
        if (Platform.isDevEnv()) {
            BLOCKS.register("test", () -> TestBlock.BLOCK);
            ITEMS.register("test", () -> TestItem.ITEM);
            BLOCKS.register("test_2", () -> NoRendererTestBlock.BLOCK);
            ITEMS.register("test_2", () -> new BlockItem(NoRendererTestBlock.BLOCK, new Item.Properties()));
        }
        BLOCKS.register("renderer_block", () -> RendererBlock.BLOCK);

        // used for forge events (ClientProxy + CommonProxy)
        eventBus.addListener(LDLNetworking::registerPayloads);
        // init common features
        CommonProxy.init(eventBus);
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

    public static void init(IEventBus eventBus) {
        LDLib2Registries.init();
        AccessorRegistries.init();
        RPCPacketDistributor.init();
        PropertyRegistry.init();
        LDMenuTypes.init(eventBus);
        TypeHandles.init();

        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITY_TYPES.register(eventBus);
    }

}
