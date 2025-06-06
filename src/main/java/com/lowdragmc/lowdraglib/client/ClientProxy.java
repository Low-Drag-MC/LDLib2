package com.lowdragmc.lowdraglib.client;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.lowdragmc.lowdraglib.CommonProxy;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.client.model.custommodel.CustomBakedModel;
import com.lowdragmc.lowdraglib.client.model.custommodel.LDLMetadataSection;
import com.lowdragmc.lowdraglib.client.model.forge.LDLRendererModel;
import com.lowdragmc.lowdraglib.client.renderer.ATESRRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.shader.Shaders;
import com.lowdragmc.lowdraglib.client.utils.WidgetClientTooltipComponent;
import com.lowdragmc.lowdraglib.core.mixins.ParticleEngineAccessor;
import com.lowdragmc.lowdraglib.core.mixins.accessor.ModelBakeryAccessor;
import com.lowdragmc.lowdraglib.compass.CompassManager;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.util.WidgetTooltipComponent;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ClientProxy extends CommonProxy {

    public ClientProxy(IEventBus eventBus) {
        super(eventBus);
        eventBus.register(this);
    }


    /**
     * should be called when Minecraft is prepared.
     */
    public static void init() {
        Shaders.init();
        DrawerHelper.init();
        CompassManager.INSTANCE.init();
    }

    @SubscribeEvent
    public void onRegisterClientTooltipComponentFactoriesEvent(final RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(WidgetTooltipComponent.class, WidgetClientTooltipComponent::new);
    }

    @SubscribeEvent
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (Platform.isDevEnv()) {
            event.registerBlockEntityRenderer(CommonProxy.TEST_BE_TYPE.get(), ATESRRendererProvider::new);
        }
    }

    @SubscribeEvent
    public void clientSetup(final FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            ((ReloadableResourceManager) Minecraft.getInstance().getResourceManager()).registerReloadListener(CompassManager.INSTANCE);
            CompassManager.INSTANCE.onResourceManagerReload(Minecraft.getInstance().getResourceManager());
            ClientProxy.init();
        });
    }

    @ApiStatus.Internal
    public static final Multimap<ResourceLocation, Material> SCRAPED_TEXTURES = HashMultimap.create();
    @ApiStatus.Internal
    public static final Object2BooleanMap<ModelResourceLocation> WRAPPED_MODELS = new Object2BooleanLinkedOpenHashMap<>();

    @ApiStatus.Internal
    public static void textureScraped(ResourceLocation modelLocation, Material material) {
        SCRAPED_TEXTURES.put(modelLocation, material);
    }

    @SubscribeEvent
    public void modelRegistry(final ModelEvent.RegisterGeometryLoaders e) {
        e.register(LDLib.id("renderer"), LDLRendererModel.Loader.INSTANCE);
    }

    @SubscribeEvent
    public void modelBake(final ModelEvent.ModifyBakingResult event) {
        ModelBakery modelBakery = event.getModelBakery();
        for (Map.Entry<ModelResourceLocation, BakedModel> entry : event.getModels().entrySet()) {
            ModelResourceLocation mrl = entry.getKey();
            UnbakedModel rootModel = ((ModelBakeryAccessor)modelBakery).getTopLevelModels().get(mrl);
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
                ResourceLocation rl = mrl.id();
                dependencies.push(rl);
                seenModels.add(rl);
                boolean shouldWrap = ClientProxy.WRAPPED_MODELS.getOrDefault(mrl, false);
                // Breadth-first loop through dependencies, exiting as soon as a CTM texture is found, and skipping duplicates/cycles
                while (!shouldWrap && !dependencies.isEmpty()) {
                    ResourceLocation dep = dependencies.pop();
                    UnbakedModel model;
                    try {
                        model = dep == rl ? rootModel : ((ModelBakeryAccessor) modelBakery).invokeGetModel(dep);
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
                        LDLib.LOGGER.error("Error loading model dependency {} for model {}. Skipping...", dep, mrl, e);
                    }
                }
                ClientProxy.WRAPPED_MODELS.put(mrl, shouldWrap);
                if (shouldWrap) {
                    entry.setValue(new CustomBakedModel<>(baked));
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
                var modelLocation = ResourceLocation.fromNamespaceAndPath(
                        entry.getKey().getNamespace(),
                        entry.getKey().getPath()
                                .replace("models/", "")
                                .replace(".json", ""));
                event.register(ModelResourceLocation.standalone(modelLocation));
            }
        }
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
