package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.CommonProxy;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.model.forge.LDLRendererModel;
import com.lowdragmc.lowdraglib2.client.renderer.ATESRRendererProvider;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.core.mixins.ParticleEngineAccessor;
import com.lowdragmc.lowdraglib2.editor.resource.IRendererResource;
import com.lowdragmc.lowdraglib2.editor.resource.PackResourceManager;
import com.lowdragmc.lowdraglib2.gui.factory.LDMenuTypes;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.utils.ModularUIClientElementComponent;
import com.lowdragmc.lowdraglib2.gui.ui.utils.ModularUITooltipComponent;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.integration.kjs.ui.LDKJSMenuTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.*;

@OnlyIn(Dist.CLIENT)
public class ClientProxy {

    public ClientProxy(IEventBus eventBus) {
        eventBus.register(this);
    }

    @SubscribeEvent
    public void onRegisterMenuScreensEvent(final RegisterMenuScreensEvent event) {
        event.register(LDMenuTypes.PLAYER_UI.get(), ModularUIContainerScreen::new);
        event.register(LDMenuTypes.HELD_ITEM_UI.get(), ModularUIContainerScreen::new);
        event.register(LDMenuTypes.BLOCK_UI.get(), ModularUIContainerScreen::new);
        if (LDLib2.isKubejsLoaded()) {
            LDKJSMenuTypes.onRegisterMenuScreensEvent(event);
        }
    }

    @SubscribeEvent
    public void onRegisterClientTooltipComponentFactoriesEvent(final RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ModularUITooltipComponent.class, ModularUIClientElementComponent::new);
    }

    @SubscribeEvent
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (Platform.isDevEnv()) {
            event.registerBlockEntityRenderer(CommonProxy.TEST_BE_TYPE.get(), ATESRRendererProvider::new);
        }
        event.registerBlockEntityRenderer(CommonProxy.RENDERER_BE_TYPE.get(), ATESRRendererProvider::new);
    }

    @SubscribeEvent
    public void clientSetup(final FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            LDLibShaders.init();
        });
    }

    @SubscribeEvent
    public void modelRegistry(final ModelEvent.RegisterGeometryLoaders e) {
        e.register(LDLib2.id("renderer"), LDLRendererModel.Loader.INSTANCE);
    }

    @SubscribeEvent
    public void shaderRegistry(RegisterShadersEvent event) {
        LDLibShaders.registerShaders(event);
    }

    @SubscribeEvent
    public void onRegisterClientReloadListenersEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(PackResourceManager.INSTANCE);
        event.registerReloadListener(StylesheetManager.INSTANCE);
    }

    @SubscribeEvent
    public void registerModels(ModelEvent.RegisterAdditional event) {
        // load all models under the ldlib folder
        for (var entry : Minecraft.getInstance().getResourceManager().listResources("models",
                id -> id.getNamespace().equals(LDLib2.MOD_ID) && id.getPath().endsWith(".json")).entrySet()) {
            if (entry.getValue().sourcePackId().equals(LDLib2.MOD_ID)) {
                var modelLocation = ResourceLocation.fromNamespaceAndPath(
                        entry.getKey().getNamespace(),
                        entry.getKey().getPath()
                                .replace("models/", "")
                                .replace(".json", ""));
                event.register(ModelResourceLocation.standalone(modelLocation));
            }
        }
        IRendererResource.INSTANCE.onAdditionalModel(event::register);
        for (IRenderer renderer : IRenderer.EVENT_REGISTERS) {
            renderer.onAdditionalModel(event::register);
        }
    }

    public static ParticleProvider getProvider(ParticleType<?> type) {
        if (Minecraft.getInstance().particleEngine instanceof ParticleEngineAccessor accessor) {
            return accessor.getProviders().get(BuiltInRegistries.PARTICLE_TYPE.getKey(type));
        }
        return null;
    }

}
