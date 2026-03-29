package com.lowdragmc.lowdraglib2;

import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlock;
import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlockEntity;
import com.lowdragmc.lowdraglib2.gui.factory.LDMenuTypes;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.networking.LDLNetworking;
import com.lowdragmc.lowdraglib2.networking.both.PacketModularUISync;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCBlockEntity;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCPacket;
import com.lowdragmc.lowdraglib2.networking.c2s.CPacketUIRPCEvent;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib2.plugin.LDLibPlugin;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.test.*;
import com.lowdragmc.lowdraglib2.utils.ReflectionUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class CommonProxy {

    public static BlockEntityType<TestBlockEntity> TEST_BE_TYPE;
    public static BlockEntityType<RendererBlockEntity> RENDERER_BE_TYPE;

    public CommonProxy() {
        // Register networking payloads (codecs)
        LDLNetworking.register();

        // Register C2S networking receivers
        ServerPlayNetworking.registerGlobalReceiver(CPacketUIRPCEvent.TYPE, (payload, context) -> {
            context.server().execute(() -> CPacketUIRPCEvent.handle(payload, context.player(), context.player().registryAccess()));
        });
        ServerPlayNetworking.registerGlobalReceiver(PacketRPCBlockEntity.TYPE, (payload, context) -> {
            context.server().execute(() -> PacketRPCBlockEntity.handle(payload, context.player(), context.player().registryAccess()));
        });
        ServerPlayNetworking.registerGlobalReceiver(PacketModularUISync.TYPE, (payload, context) -> {
            context.server().execute(() -> PacketModularUISync.handle(payload, context.player(), context.player().registryAccess()));
        });
        ServerPlayNetworking.registerGlobalReceiver(PacketRPCPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> PacketRPCPacket.handle(payload, context.player(), context.player().registryAccess()));
        });

        // Registration for blocks, items, BEs
        if (Platform.isDevEnv()) {
            register("test", TestBlock.BLOCK);
            register("test", TestItem.ITEM);
            register("test_2", NoRendererTestBlock.BLOCK);
            register("test_2", new BlockItem(NoRendererTestBlock.BLOCK, new Item.Properties()));
            TEST_BE_TYPE = register("test", BlockEntityType.Builder.of(TestBlockEntity::new, TestBlock.BLOCK).build(null));
        }
        register("renderer_block", RendererBlock.BLOCK);
        RENDERER_BE_TYPE = register("renderer_block", BlockEntityType.Builder.of(RendererBlockEntity::new, RendererBlock.BLOCK).build(null));

        // init common features
        CommonProxy.init();
        
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

    public static void init() {
        LDLib2Registries.init();
        AccessorRegistries.init();
        RPCPacketDistributor.init();
        PropertyRegistry.init();
        LDMenuTypes.init();
    }

    private static <T extends Block> T register(String name, T block) {
        return Registry.register(BuiltInRegistries.BLOCK, LDLib2.id(name), block);
    }

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(BuiltInRegistries.ITEM, LDLib2.id(name), item);
    }

    private static <T extends BlockEntityType<?>> T register(String name, T type) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, LDLib2.id(name), type);
    }

}
