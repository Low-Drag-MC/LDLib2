package com.lowdragmc.lowdraglib.client.forge;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.client.ClientProxy;
import com.lowdragmc.lowdraglib.client.model.custommodel.LDLMetadataSection;
import com.lowdragmc.lowdraglib.client.model.forge.CustomBakedModelImpl;
import com.lowdragmc.lowdraglib.client.model.forge.LDLRendererModel;
import com.lowdragmc.lowdraglib.client.renderer.ATESRRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.block.RendererBlock;
import com.lowdragmc.lowdraglib.client.renderer.block.forge.RendererBlockEntityImpl;
import com.lowdragmc.lowdraglib.client.shader.Shaders;
import com.lowdragmc.lowdraglib.client.utils.WidgetClientTooltipComponent;
import com.lowdragmc.lowdraglib.core.mixins.accessor.ModelBakeryAccessor;
import com.lowdragmc.lowdraglib.forge.CommonProxyImpl;
import com.lowdragmc.lowdraglib.gui.compass.CompassManager;
import com.lowdragmc.lowdraglib.gui.util.WidgetTooltipComponent;
import com.lowdragmc.lowdraglib.test.TestBlock;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.*;
import java.util.function.Consumer;


@OnlyIn(Dist.CLIENT)
public class ClientProxyImpl extends CommonProxyImpl {

    public ClientProxyImpl() {
        super();
    }

    @SubscribeEvent
    public void onRegisterClientTooltipComponentFactoriesEvent(final RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(WidgetTooltipComponent.class, WidgetClientTooltipComponent::new);
    }

    @SubscribeEvent
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RendererBlockEntityImpl.TYPE(), ATESRRendererProvider::new);
    }

    @SubscribeEvent
    public void clientSetup(final FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            ((ReloadableResourceManager) Minecraft.getInstance().getResourceManager()).registerReloadListener(CompassManager.INSTANCE);
            CompassManager.INSTANCE.onResourceManagerReload(Minecraft.getInstance().getResourceManager());
            ClientProxy.init();
            ItemBlockRenderTypes.setRenderLayer(RendererBlock.BLOCK, RenderType.translucent());
            if (Platform.isDevEnv()) {
                ItemBlockRenderTypes.setRenderLayer(TestBlock.BLOCK, RenderType.cutoutMipped());
            }
        });
    }

    @SubscribeEvent
    public void modelRegistry(final ModelEvent.RegisterGeometryLoaders e) {
        e.register("renderer", LDLRendererModel.Loader.INSTANCE);
    }

    @SubscribeEvent
    public void modelBake(final ModelEvent.ModifyBakingResult event) {
        ModelBakery modelBakery = event.getModelBakery();
        for (Map.Entry<ResourceLocation, BakedModel> entry : event.getModels().entrySet()) {
            ResourceLocation rl = entry.getKey();
            UnbakedModel rootModel = ((ModelBakeryAccessor)modelBakery).getTopLevelModels().get(rl);
            if (rootModel != null) {
                BakedModel baked = entry.getValue();
                if (baked instanceof LDLRendererModel) {
                    continue;
                }
                if (baked.isCustomRenderer()) { // Nothing we can add to builtin models
                    continue;
                }
                Deque<ResourceLocation> dependencies = new ArrayDeque<>();
                Set<ResourceLocation> seenModels = new HashSet<>();
                dependencies.push(rl);
                seenModels.add(rl);
                boolean shouldWrap = ClientProxy.WRAPPED_MODELS.getOrDefault(rl, false);
                // Breadth-first loop through dependencies, exiting as soon as a CTM texture is found, and skipping duplicates/cycles
                while (!shouldWrap && !dependencies.isEmpty()) {
                    ResourceLocation dep = dependencies.pop();
                    UnbakedModel model;
                    try {
                        model = dep == rl ? rootModel : modelBakery.getModel(dep);
                    } catch (Exception e) {
                        continue;
                    }

                    try {
                        Set<Material> textures = new HashSet<>(ClientProxy.SCRAPED_TEXTURES.get(dep));
                        for (Material tex : textures) {
                            // Cache all dependent texture metadata
                            // At least one texture has CTM metadata, so we should wrap this model
                            if (!LDLMetadataSection.getMetadata(LDLMetadataSection.spriteToAbsolute(tex.texture())).isMissing()) { // TODO lazy
                                shouldWrap = true;
                                break;
                            }
                        }
                        if (!shouldWrap) {
                            for (ResourceLocation newDep : model.getDependencies()) {
                                if (seenModels.add(newDep)) {
                                    dependencies.push(newDep);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LDLib.LOGGER.error("Error loading model dependency {} for model {}. Skipping...", dep, rl, e);
                    }
                }
                ClientProxy.WRAPPED_MODELS.put(rl, shouldWrap);
                if (shouldWrap) {
                    entry.setValue(new CustomBakedModelImpl(baked));
                }
            }
        }

    }

    @SubscribeEvent
    public void shaderRegistry(RegisterShadersEvent event) {
        for (Pair<ShaderInstance, Consumer<ShaderInstance>> pair : Shaders.registerShaders(event.getResourceProvider())) {
            event.registerShader(pair.getFirst(), pair.getSecond());
        }
    }

//    @SubscribeEvent
//    public void registerTextures(RegisterTextureAtlasSpriteLoadersEvent event) {
//        for (IRenderer renderer : IRenderer.EVENT_REGISTERS) {
//            renderer.onPrepareTextureAtlas(event.getAtlas().location(), location -> event.getAtlas().);
//        }
//    }

    @SubscribeEvent
    public void registerModels(ModelEvent.RegisterAdditional event) {
        // load all models under the ldlib folder
        for (var entry : Minecraft.getInstance().getResourceManager().listResources("models",
                id -> id.getNamespace().equals("ldlib") && id.getPath().endsWith(".json")).entrySet()) {
            if (entry.getValue().sourcePackId().equals("ldlib")) {
                event.register(new ResourceLocation(entry.getKey().getNamespace(),
                        entry.getKey().getPath().replace("models/", "").replace(".json", "")));
            }
        }
        for (IRenderer renderer : IRenderer.EVENT_REGISTERS) {
            renderer.onAdditionalModel(event::register);
        }
    }
}
