package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderPipelines;
import com.lowdragmc.lowdraglib2.editor.resource.PackResourceManager;
import com.lowdragmc.lowdraglib2.gui.factory.LDMenuTypes;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.utils.ModularUIClientElementComponent;
import com.lowdragmc.lowdraglib2.gui.ui.utils.ModularUITooltipComponent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;

public final class ClientModBusEventListener {
    @SubscribeEvent
    public void onRegisterMenuScreensEvent(final RegisterMenuScreensEvent event) {
        event.register(LDMenuTypes.PLAYER_UI.get(), ModularUIContainerScreen::new);
        event.register(LDMenuTypes.HELD_ITEM_UI.get(), ModularUIContainerScreen::new);
        event.register(LDMenuTypes.BLOCK_UI.get(), ModularUIContainerScreen::new);
        if (LDLib2.isKubejsLoaded()) {
            // todo kjs
//            LDKJSMenuTypes.onRegisterMenuScreensEvent(event);
        }
    }

    @SubscribeEvent
    public void onRegisterClientTooltipComponentFactoriesEvent(final RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ModularUITooltipComponent.class, ModularUIClientElementComponent::new);
    }

    @SubscribeEvent
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // todo renderer
//        if (Platform.isDevEnv()) {
//            event.registerBlockEntityRenderer(CommonProxy.TEST_BE_TYPE.get(), ATESRRendererProvider::new);
//        }
//        event.registerBlockEntityRenderer(CommonProxy.RENDERER_BE_TYPE.get(), ATESRRendererProvider::new);
    }

    @SubscribeEvent
    public void clientSetup(final FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
        });
    }

    // todo renderer
//    @SubscribeEvent
//    public void modelRegistry(final ModelEvent.RegisterGeometryLoaders e) {
//        e.register(LDLib2.id("renderer"), LDLRendererModel.Loader.INSTANCE);
//    }

    @SubscribeEvent
    public void onAddClientReloadListenerEvent(AddClientReloadListenersEvent event) {
        event.addListener(PackResourceManager.RESOURCE_ID, PackResourceManager.INSTANCE);
        event.addListener(StylesheetManager.RESOURCE_ID, StylesheetManager.INSTANCE);
    }

    @SubscribeEvent
    public void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
        LDLibRenderPipelines.register(event);
    }

    @SubscribeEvent
    public void registerPIPRenderers(net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent event) {
        event.register(com.lowdragmc.lowdraglib2.client.scene.SceneRenderState.class, com.lowdragmc.lowdraglib2.client.scene.ScenePIPRenderer::new);
        event.register(com.lowdragmc.lowdraglib2.gui.ui.rendering.VisualLayerPipState.class,
                com.lowdragmc.lowdraglib2.gui.ui.rendering.VisualLayerPipRenderer::new);
    }

    @SubscribeEvent
    public void registerModels(ModelEvent.RegisterStandalone event) {
        // load all models under the ldlib folder
        // TODO RENDERER
//        for (var entry : Minecraft.getInstance().getResourceManager().listResources("models",
//                id -> id.getNamespace().equals(LDLib2.MOD_ID) && id.getPath().endsWith(".json")).entrySet()) {
//            if (entry.getValue().sourcePackId().equals(LDLib2.MOD_ID)) {
//                var modelLocation = Identifier.fromNamespaceAndPath(
//                        entry.getKey().getNamespace(),
//                        entry.getKey().getPath()
//                                .replace("models/", "")
//                                .replace(".json", ""));
//                event.register(ModelResourceLocation.standalone(modelLocation));
//            }
//        }
//        IRendererResource.INSTANCE.onAdditionalModel(event::register);
//        for (IRenderer renderer : IRenderer.EVENT_REGISTERS) {
//            renderer.onAdditionalModel(event::register);
//        }
    }
}
