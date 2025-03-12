package com.lowdragmc.lowdraglib.client.model.forge;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.lowdragmc.lowdraglib.client.model.custommodel.CustomBakedModel;
import com.lowdragmc.lowdraglib.client.renderer.IBlockRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author KilaBash
 * @date 2022/05/28
 * @implNote LDLModel, use vanilla way to improve model rendering
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LDLRendererModel implements IUnbakedGeometry<LDLRendererModel> {
    public static final LDLRendererModel INSTANCE = new LDLRendererModel();

    private LDLRendererModel() {}

    @Override
    public BakedModel bake(IGeometryBakingContext iGeometryBakingContext, ModelBaker arg, Function<Material, TextureAtlasSprite> function, ModelState arg2, ItemOverrides arg3) {
        return new RendererBakedModel();
    }

    public static final class RendererBakedModel implements BakedModel {

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
            return Collections.emptyList();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public boolean isGui3d() {
            return true;
        }

        @Override
        public boolean usesBlockLight() {
            return false;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(MissingTextureAtlasSprite.getLocation());
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }

        // forge

        public static final ModelProperty<IRenderer> IRENDERER = new ModelProperty<>();
        public static final ModelProperty<BlockAndTintGetter> WORLD = new ModelProperty<>();
        public static final ModelProperty<BlockPos> POS = new ModelProperty<>();
        public static final ModelProperty<ModelData> MODEL_DATA = new ModelProperty<>();


        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
            var renderer = data.get(IRENDERER);
            var world = data.get(WORLD);
            var pos = data.get(POS);
            var modelData = data.get(MODEL_DATA);
            if (renderer != null) {
                var quads = renderer.renderModel(world, pos, state, side, rand, modelData, renderType);
                if (renderer.reBakeCustomQuads() && state != null && world != null && pos != null) {
                    return CustomBakedModel.reBakeCustomQuads(quads, world, pos, state, side, renderer.reBakeCustomQuadsOffset());
                }
                return quads;
            }
            return Collections.emptyList();
        }

        @Override
        public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
            if (state.getBlock() instanceof IBlockRendererProvider rendererProvider) {
                IRenderer renderer = rendererProvider.getRenderer(state);
                if (renderer != null) {
                    return renderer.useAO(state, data, renderType);
                }
            }
            return BakedModel.super.useAmbientOcclusion(state, data, renderType);
        }


        @Override
        public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
            if (state.getBlock() instanceof IBlockRendererProvider rendererProvider) {
                IRenderer renderer = rendererProvider.getRenderer(state);
                if (renderer != null) {
                    modelData = ModelData.builder()
                            .with(IRENDERER, renderer)
                            .with(WORLD, level)
                            .with(POS, pos)
                            .with(MODEL_DATA, modelData)
                            .build();
                }
            }
            return modelData;
        }

        @Override
        public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
            var renderer = data.get(IRENDERER);
            var world = data.get(WORLD);
            var pos = data.get(POS);
            var modelData = data.get(MODEL_DATA);
            if (renderer != null) {
                return renderer.getParticleTexture(world, pos, modelData);
            }
            return BakedModel.super.getParticleIcon(data);
        }
    }

    public static final class Loader implements IGeometryLoader<LDLRendererModel> {

        public static final Loader INSTANCE = new Loader();
        private Loader() {}

        @Override
        public LDLRendererModel read(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            return LDLRendererModel.INSTANCE;
        }
    }
}
