package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.CommonProxy;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.model.fabric.LDLRendererModel;
import com.lowdragmc.lowdraglib2.client.model.fabric.OBJModelLoader;
import com.lowdragmc.lowdraglib2.client.renderer.ATESRRendererProvider;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.networking.both.PacketModularUISync;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCBlockEntity;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCPacket;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketUIRPCEventReturn;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import com.lowdragmc.lowdraglib2.editor.resource.PackResourceManager;
import com.lowdragmc.lowdraglib2.gui.factory.LDMenuTypes;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class ClientProxy {

    public static void register() {
        // Register Screens
        MenuScreens.register(LDMenuTypes.PLAYER_UI, ModularUIContainerScreen::new);
        MenuScreens.register(LDMenuTypes.HELD_ITEM_UI, ModularUIContainerScreen::new);
        MenuScreens.register(LDMenuTypes.BLOCK_UI, ModularUIContainerScreen::new);

        // Register Entity Renderers
        if (Platform.isDevEnv() && CommonProxy.TEST_BE_TYPE != null) {
            BlockEntityRendererRegistry.register(CommonProxy.TEST_BE_TYPE, ATESRRendererProvider::new);
        }
        if (CommonProxy.RENDERER_BE_TYPE != null) {
            BlockEntityRendererRegistry.register(CommonProxy.RENDERER_BE_TYPE, ATESRRendererProvider::new);
        }

        // Networking receivers (S2C)
        ClientPlayNetworking.registerGlobalReceiver(SPacketAutoSyncBlockEntity.TYPE, (payload, context) -> {
            context.client().execute(() -> SPacketAutoSyncBlockEntity.handle(payload, context.player(), context.player().registryAccess()));
        });
        ClientPlayNetworking.registerGlobalReceiver(SPacketUIRPCEventReturn.TYPE, (payload, context) -> {
            context.client().execute(() -> SPacketUIRPCEventReturn.handle(payload, context.player(), context.player().registryAccess()));
        });
        ClientPlayNetworking.registerGlobalReceiver(PacketRPCBlockEntity.TYPE, (payload, context) -> {
            context.client().execute(() -> PacketRPCBlockEntity.handle(payload, context.player(), context.player().registryAccess()));
        });
        ClientPlayNetworking.registerGlobalReceiver(PacketModularUISync.TYPE, (payload, context) -> {
            context.client().execute(() -> PacketModularUISync.handle(payload, context.player(), context.player().registryAccess()));
        });
        ClientPlayNetworking.registerGlobalReceiver(PacketRPCPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> PacketRPCPacket.handle(payload, context.player(), context.player().registryAccess()));
        });

        // Lifecycle Events
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> LDLibShaders.init());

        // Model Loading
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.resolveModel().register(OBJModelLoader.INSTANCE);
            pluginContext.modifyModelOnLoad().register((model, context) -> {
                if (LDLib2.id("renderer").equals(context.topLevelId())) {
                    return LDLRendererModel.INSTANCE;
                }
                return model;
            });
            registerModels(pluginContext);
            IRenderer.EVENT_REGISTERS.forEach(r -> r.onAdditionalModel(pluginContext::addModels));
        });

        // Shaders
        CoreShaderRegistrationCallback.EVENT.register(LDLibShaders::registerCoreShaders);

        // Resource Listeners
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(PackResourceManager.INSTANCE);
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(StylesheetManager.INSTANCE);

        ClientEventListener.register();
        ClientEventListener.init();
    }

    public static void registerModels(ModelLoadingPlugin.Context event) {
        // load all models under the ldlib and photon folder
        for (var entry : Minecraft.getInstance().getResourceManager().listResources("models",
                id -> (id.getNamespace().equals(LDLib2.MOD_ID) || id.getNamespace().equals("photon")) && 
                      (id.getPath().endsWith(".json") || id.getPath().endsWith(".obj"))).entrySet()) {
            var modelLocation = ResourceLocation.fromNamespaceAndPath(
                    entry.getKey().getNamespace(),
                    entry.getKey().getPath()
                            .replace("models/", "")
                            .replace(".json", "")
                            .replace(".obj", ""));
            event.addModels(modelLocation);
        }
        for (IRenderer renderer : IRenderer.EVENT_REGISTERS) {
            renderer.onAdditionalModel(event::addModels);
        }
    }

    public static ParticleProvider getProvider(ParticleType<?> type) {
        if (Minecraft.getInstance().particleEngine instanceof com.lowdragmc.lowdraglib2.core.mixins.ParticleEngineAccessor accessor) {
            return accessor.getProviders().get(BuiltInRegistries.PARTICLE_TYPE.getId(type));
        }
        return null;
    }

}
