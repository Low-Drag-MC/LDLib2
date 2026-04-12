package com.lowdragmc.lowdraglib2.client.model.fabric;

import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.lowdragmc.lowdraglib2.client.renderer.IBlockRendererProvider;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.ModelData;
import com.lowdragmc.lowdraglib2.client.renderer.ModelProperty;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class LDLRendererModel implements UnbakedModel {
    public static final LDLRendererModel INSTANCE = new LDLRendererModel();

    private LDLRendererModel() {}

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter) {
    }

    @Override
    public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state) {
        return new RendererBakedModel();
    }

    public static final class RendererBakedModel implements net.minecraft.client.resources.model.BakedModel, FabricBakedModel {

        @Override
        public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
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
        public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() { return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS; }
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(MissingTextureAtlasSprite.getLocation());
        }

        @Override
        public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() {
            return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY;
        }

        // Fabric specific: we use ThreadLocals to pass ModelData, or block entity data.
        public static final ModelProperty<IRenderer> RENDERER = new ModelProperty<>();
        public static final ModelProperty<BlockAndTintGetter> WORLD = new ModelProperty<>();
        public static final ModelProperty<BlockPos> POS = new ModelProperty<>();
        public static final ModelProperty<ModelData> MODEL_DATA = new ModelProperty<>();

        @Override
        public boolean isVanillaAdapter() {
            return false;
        }

        @Override
        public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
            IRenderer renderer = null;
            if (state != null && state.getBlock() instanceof IBlockRendererProvider rendererProvider) {
                renderer = rendererProvider.getRenderer(state);
            }
            if (renderer != null) {
                ModelData data = ModelData.builder()
                        .with(RENDERER, renderer)
                        .with(WORLD, blockView)
                        .with(POS, pos)
                        .with(MODEL_DATA, ModelData.EMPTY)
                        .build();
                        
                var emitter = context.getEmitter();
                RandomSource rand = randomSupplier.get();
                
                for (Direction side : Direction.values()) {
                    for (var quad : renderer.renderModel(blockView, pos, state, side, rand, data, null)) {
                        emitter.fromVanilla(quad, null, side);
                        emitter.emit();
                    }
                }
                for (var quad : renderer.renderModel(blockView, pos, state, null, rand, data, null)) {
                    emitter.fromVanilla(quad, null, null);
                    emitter.emit();
                }
            }
        }

        @Override
        public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
            // Emitted via IItemRendererProvider
        }
    }

    public static class Loader implements net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier.OnLoad {
        public static final Loader INSTANCE = new Loader();

        @Override
        public net.minecraft.client.resources.model.UnbakedModel modifyModelOnLoad(net.minecraft.client.resources.model.UnbakedModel model, net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier.OnLoad.Context context) {
            return model;
        }
    }
}
