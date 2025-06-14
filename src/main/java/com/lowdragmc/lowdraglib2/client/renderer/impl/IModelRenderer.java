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
import com.lowdragmc.lowdraglib2.gui.ui.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
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
import org.appliedenergistics.yoga.YogaAlign;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    protected BakedModel itemModel;

    @OnlyIn(Dist.CLIENT)
    protected Map<ModelState, BakedModel> modelCaches;

    protected IModelRenderer() {
        modelLocation = ResourceLocation.withDefaultNamespace("block/furnace");
    }

    public IModelRenderer(ResourceLocation modelLocation) {
        this.modelLocation = modelLocation;
        initRenderer();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @Nonnull
    public TextureAtlasSprite getParticleTexture(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, ModelData modelData) {
        BakedModel model = getItemBakedModel();
        if (model == null) {
            return IRenderer.super.getParticleTexture(level, pos, modelData);
        }
        return model.getParticleIcon();
    }

    @OnlyIn(Dist.CLIENT)
    protected UnbakedModel getModel() {
        return ModelFactory.getUnBakedModel(modelLocation);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderItem(ItemStack stack,
                           ItemDisplayContext transformType,
                           boolean leftHand, PoseStack poseStack,
                           MultiBufferSource buffer, int combinedLight,
                           int combinedOverlay, BakedModel model) {
        IItemRendererProvider.disabled.set(true);
        model = getItemBakedModel(stack);
        if (model != null) {
            Minecraft.getInstance().getItemRenderer().render(stack, transformType, leftHand, poseStack, buffer, combinedLight, combinedOverlay, model);
        }
        IItemRendererProvider.disabled.set(false);
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
        if (itemModel == null) {
            var model = getModel();
            if (model instanceof BlockModel blockModel && blockModel.getRootModel() == ModelBakery.GENERATION_MARKER) {
                // fabric doesn't help us to fix vanilla bakery, so we have to do it ourselves
                model = ModelFactory.ITEM_MODEL_GENERATOR.generateBlockModel(this::materialMapping, blockModel);
            }
            itemModel = model.bake(
                    ModelFactory.getModelBaker(),
                    this::materialMapping,
                    BlockModelRotation.X0_Y0);
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
                return modelCaches.computeIfAbsent(modelState, ms -> getModel().bake(
                        ModelFactory.getModelBaker(),
                        this::materialMapping,
                        ms));
            }
        }
        return modelCaches.computeIfAbsent(BlockModelRotation.X0_Y0, ms -> getModel().bake(
                ModelFactory.getModelBaker(),
                this::materialMapping,
                ms));
    }


    @OnlyIn(Dist.CLIENT)
    protected TextureAtlasSprite materialMapping(Material material) {
        return material.sprite();
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void onPrepareTextureAtlas(ResourceLocation atlasName, Consumer<ResourceLocation> register) {
        if (atlasName.equals(TextureAtlas.LOCATION_BLOCKS)) {
            itemModel = null;
            modelCaches.clear();
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onAdditionalModel(Consumer<ModelResourceLocation> registry) {
        registry.accept(ModelResourceLocation.standalone(modelLocation));
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

    // ISerializableRenderer
    public void initRenderer() {
        if (this.modelLocation != null) {
            if (LDLib2.isClient()) {
                modelCaches = new ConcurrentHashMap<>();
                registerEvent();
            }
        }
    }

    @ConfigSetter(field = "modelLocation")
    @SuppressWarnings("unused")
    public void updateModelWithoutReloadingResource(ResourceLocation modelLocation) {
        this.modelLocation = modelLocation;
        if (LDLib2.isClient()) {
            itemModel = null;
            if (modelCaches != null) modelCaches.clear();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void updateModelWithReloadingResource(ResourceLocation modelLocation) {
        updateModelWithoutReloadingResource(modelLocation);
        var unBakedModel = getModel();
        if (unBakedModel == ModelFactory.getUnBakedModel(ModelBakery.MISSING_MODEL_LOCATION)) {
            Minecraft.getInstance().reloadResourcePacks();
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void buildConfigurator(ConfiguratorGroup father) {
        IRenderer.super.buildConfigurator(father);
        var buttonConfigurator = new Configurator();
        father.addConfigurators(buttonConfigurator.addInlineChild(new Button().setText("ldlib.gui.editor.tips.select_model").setOnClick(e -> {
            var mui = e.currentElement.getModularUI();
            if (mui == null) return;
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
                    updateModelWithReloadingResource(newModel);
                    buttonConfigurator.notifyChanges();
                }
            }).show(mui.ui.rootElement);
        }).layout(layout -> layout.setAlignSelf(YogaAlign.CENTER))));
    }

    @Nullable
    public ResourceLocation getModelFromFile(File filePath) {
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
