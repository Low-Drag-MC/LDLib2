package com.lowdragmc.lowdraglib2.client.renderer.impl;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.model.ModelFactory;
import com.lowdragmc.lowdraglib2.client.renderer.IBlockRendererProvider;
import com.lowdragmc.lowdraglib2.client.renderer.IItemRendererProvider;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.editor.resource.IRendererResource;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.vfyjxf.taffy.style.AlignItems;
import lombok.Getter;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@LDLRegisterClient(name = "json_model", registry = "ldlib2:renderer")
public class IModelRenderer implements IRenderer {
    @Getter
    @Configurable
    protected ResourceLocation modelLocation;

    @OnlyIn(Dist.CLIENT)
    @Nullable
    protected volatile BakedModel itemModel;
    @OnlyIn(Dist.CLIENT)
    private volatile boolean itemModelInitialized;

    // resolved once per renderer to avoid re-acquiring the bakery lock on every bake
    @OnlyIn(Dist.CLIENT)
    @Nullable
    protected volatile UnbakedModel cachedUnbakedModel;
    @OnlyIn(Dist.CLIENT)
    private volatile boolean unbakedModelInitialized;

    @OnlyIn(Dist.CLIENT)
    protected volatile Map<ModelStateCacheKey, BakedModel> modelCaches;

    protected IModelRenderer() {
        this(ResourceLocation.withDefaultNamespace("block/furnace"));
    }

    public IModelRenderer(ResourceLocation modelLocation) {
        this.modelLocation = modelLocation;
        if (LDLib2.isClient()) {
            modelCaches = new ConcurrentHashMap<>();
            registerEvent();
        }
    }

    @Override
    public synchronized void clearCache() {
        if (LDLib2.isClient()) {
            itemModel = null;
            itemModelInitialized = false;
            cachedUnbakedModel = null;
            unbakedModelInitialized = false;
            if (modelCaches != null) modelCaches = new ConcurrentHashMap<>();
        }
    }

    @Override
    public IModelRenderer copy() {
        return new IModelRenderer(modelLocation);
    }

    @Override
    public void afterDeserialize() {
        clearCache();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @Nonnull
    public TextureAtlasSprite getParticleTexture(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, ModelData modelData) {
        BakedModel model = getItemBakedModel();
        if (model == null) {
            return IRenderer.super.getParticleTexture(level, pos, modelData);
        }
        return model.getParticleIcon(modelData);
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    protected UnbakedModel getModel() {
        if (!unbakedModelInitialized) {
            synchronized (this) {
                if (!unbakedModelInitialized) {
                    // fast path: models registered through the RegisterAdditional pipeline
                    var model = ModelFactory.getTopLevelModel(ModelResourceLocation.standalone(modelLocation));
                    if (model == null) {
                        // renderer created after the initial reload: dynamically load & resolve
                        // the model under the bakery lock (see ModelFactory#loadUnbakedModelDynamically)
                        model = ModelFactory.loadUnbakedModelDynamically(modelLocation);
                    }
                    cachedUnbakedModel = model;
                    unbakedModelInitialized = true;
                }
            }
        }
        return cachedUnbakedModel;
    }

    @OnlyIn(Dist.CLIENT)
    protected boolean isTopLevelModelMissing() {
        return ModelFactory.getTopLevelModel(ModelResourceLocation.standalone(modelLocation)) == null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderItem(ItemStack stack,
                           ItemDisplayContext transformType,
                           boolean leftHand, PoseStack poseStack,
                           MultiBufferSource buffer, int combinedLight,
                           int combinedOverlay, BakedModel model) {
        IItemRendererProvider.disabled.set(true);
        try {
            model = getItemBakedModel(stack);
            if (model != null) {
                Minecraft.getInstance().getItemRenderer().render(stack, transformType, leftHand, poseStack, buffer, combinedLight, combinedOverlay, model);
            }
        } finally {
            IItemRendererProvider.disabled.set(false);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean useBlockLight(ItemStack stack) {
        var model = getItemBakedModel(stack);
        if (model != null) {
            return model.usesBlockLight();
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public TriState useAO() {
        var model = getItemBakedModel();
        if (model != null) {
            return model.useAmbientOcclusion() ? TriState.DEFAULT : TriState.FALSE;
        }
        return TriState.FALSE;
    }

    @Override
    public TriState useAO(BlockState state, ModelData modelData, RenderType renderType) {
        return IRenderer.super.useAO(state, modelData, renderType);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<BakedQuad> renderModel(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        var ibakedmodel = getBlockBakedModel(level, pos, state);
        if (ibakedmodel == null) return Collections.emptyList();
        return ibakedmodel.getQuads(state, side, rand, data, renderType);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ChunkRenderTypeSet getRenderTypes(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource rand, ModelData modelData) {
        var ibakedmodel = getBlockBakedModel(level, pos, state);
        if (ibakedmodel != null) return ibakedmodel.getRenderTypes(state, rand, modelData);
        return IRenderer.super.getRenderTypes(level, pos, state, rand, modelData);
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    protected BakedModel getItemBakedModel() {
        if (!itemModelInitialized) {
            synchronized (this) {
                if (!itemModelInitialized) {
                    var model = getModel();
                    if (model != null) {
                        itemModel = model.bake(
                                ModelFactory.getModelBaker(),
                                this::materialMapping,
                                BlockModelRotation.X0_Y0);
                    }
                    itemModelInitialized = true;
                }
            }
        }
        return itemModel;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    protected BakedModel getItemBakedModel(ItemStack itemStack) {
        return getItemBakedModel();
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    protected BakedModel getBlockBakedModel(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state) {
        if (level != null && pos != null && state != null && state.getBlock() instanceof IBlockRendererProvider provider) {
            var modelState = provider.getModelState(level, pos, state);
            if (modelState != null) {
                return modelCaches.computeIfAbsent(ModelStateCacheKey.from(modelState), key -> bakeBlockModel(modelState));
            }
        }
        return modelCaches.computeIfAbsent(ModelStateCacheKey.from(BlockModelRotation.X0_Y0), key -> bakeBlockModel(BlockModelRotation.X0_Y0));
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    private BakedModel bakeBlockModel(ModelState modelState) {
        var model = getModel();
        if (model == null) return null;
        return model.bake(
                ModelFactory.getModelBaker(),
                this::materialMapping,
                modelState);
    }


    @OnlyIn(Dist.CLIENT)
    protected TextureAtlasSprite materialMapping(Material material) {
        return material.sprite();
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void onAdditionalModel(Consumer<ModelResourceLocation> registry) {
        registry.accept(ModelResourceLocation.standalone(modelLocation));
        clearCache();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isGui3d() {
        var model = getItemBakedModel();
        if (model == null) {
            return IRenderer.super.isGui3d();
        }
        return model.isGui3d();
    }

    @ConfigSetter(field = "modelLocation")
    public void updateModelWithoutReloadingResource(ResourceLocation modelLocation) {
        this.modelLocation = modelLocation;
        clearCache();
    }

    @OnlyIn(Dist.CLIENT)
    public void updateModelWithReloadingResource(ResourceLocation modelLocation) {
        updateModelWithoutReloadingResource(modelLocation);
        if (isTopLevelModelMissing()) {
            reloadResourcesAndRefreshRendererContainers();
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void reloadResourcesAndRefreshRendererContainers() {
        IRendererResource.INSTANCE.reloadResourcesAndRefreshOpenedContainers();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void buildConfigurator(ConfiguratorGroup father) {
        IRenderer.super.buildConfigurator(father);
        var buttonConfigurator = new Configurator();
        Button reloadButton = new Button().setText("ldlib.gui.editor.menu.reload_resource")
                .setOnClick(e -> {
                    clearCache();
                    reloadResourcesAndRefreshRendererContainers();
                    e.currentElement.setActive(false);
                });
        reloadButton.layout(layout -> layout.alignSelf(AlignItems.CENTER));
        reloadButton.setActive(isTopLevelModelMissing());
        Button selectButton = new Button().setText("ldlib.gui.editor.tips.select_model").setOnClick(e -> {
            Dialog.showFileDialog("ldlib.gui.editor.tips.select_model", LDLib2.getAssetsDir(), true, node -> {
                if (!node.getKey().isFile() || node.getKey().getName().toLowerCase().endsWith(".json".toLowerCase())) {
                    if (node.getKey().isFile()) {
                        return getModelFromFile(node.getKey()) != null;
                    }
                    return true; // allow directories
                }
                return false;
            }, r -> {
                if (r != null && r.isFile()) {
                    var newModel = getModelFromFile(r);
                    if (newModel == null) return;
                    if (newModel.equals(modelLocation)) return;
                    updateModelWithoutReloadingResource(newModel);
                    reloadButton.setActive(isTopLevelModelMissing());
                    buttonConfigurator.notifyChanges();
                }
            }).show(e.currentElement.getModularUI());
        });
        selectButton.layout(layout -> layout.alignSelf(AlignItems.CENTER));
        father.addConfigurators(buttonConfigurator.addInlineChildren(selectButton, reloadButton));
    }

    @OnlyIn(Dist.CLIENT)
    protected record ModelStateCacheKey(TransformationKey rotation, boolean uvLocked) {
        static ModelStateCacheKey from(ModelState modelState) {
            return new ModelStateCacheKey(TransformationKey.from(modelState.getRotation()), modelState.isUvLocked());
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected record TransformationKey(
            float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33) {
        static TransformationKey from(com.mojang.math.Transformation transformation) {
            var matrix = transformation.getMatrix();
            return new TransformationKey(
                    matrix.m00(), matrix.m01(), matrix.m02(), matrix.m03(),
                    matrix.m10(), matrix.m11(), matrix.m12(), matrix.m13(),
                    matrix.m20(), matrix.m21(), matrix.m22(), matrix.m23(),
                    matrix.m30(), matrix.m31(), matrix.m32(), matrix.m33());
        }
    }

    @Nullable
    public static ResourceLocation getModelFromFile(File filePath) {
        String fullPath = filePath.getPath().replace('\\', '/');

        // find the "assets/" directory in the path
        var assetsIndex = fullPath.indexOf("assets/");
        if (assetsIndex == -1) {
            return null;
        }

        var relativePath = fullPath.substring(assetsIndex + "assets/".length());

        // find mod_id
        var slashIndex = relativePath.indexOf('/');
        if (slashIndex == -1) {
            return null;
        }

        var modId = relativePath.substring(0, slashIndex);
        var subPath = relativePath.substring(slashIndex + 1);

        // find model location
        var modelIndex = subPath.indexOf("models/");
        if (modelIndex == -1) {
            return null;
        }

        var modelPath = subPath.substring(modelIndex + "models/".length());
        if (!modelPath.endsWith(".json")) {
            return null;
        }

        var location = modId + ":" + modelPath.substring(0, modelPath.length() - 5); // remove ".json" suffix

        if (LDLib2.isValidResourceLocation(location)) {
            return ResourceLocation.parse(location);
        }
        return null;
    }
}
