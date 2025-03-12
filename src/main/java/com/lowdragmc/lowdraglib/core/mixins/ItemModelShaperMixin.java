package com.lowdragmc.lowdraglib.core.mixins;

import com.lowdragmc.lowdraglib.client.renderer.IItemRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(ItemModelShaper.class)
public class ItemModelShaperMixin {
    @Unique
    private final static Map<IRenderer, BakedModel> SHAPES_CACHE = new HashMap<>();

    @Inject(method = "getItemModel(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/resources/model/BakedModel;", at = @At("HEAD"), cancellable = true)
    public void ldlib$injectGetModel(ItemStack stack, CallbackInfoReturnable<BakedModel> cir) {
        if (stack.getItem() instanceof IItemRendererProvider provider) {
            IRenderer renderer = provider.getRenderer(stack);
            if(renderer != null) {
                cir.setReturnValue(SHAPES_CACHE.computeIfAbsent(renderer, r -> new BakedModel() {
                    @Override
                    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
                        return r.renderModel(null, null, state, direction, random, ModelData.EMPTY, null);
                    }

                    @Override
                    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
                        return r.renderModel(null, null, state, side, rand, data, renderType);
                    }

                    @Override
                    public boolean useAmbientOcclusion() {
                        return r.useAO() == TriState.DEFAULT || r.useAO() == TriState.TRUE;
                    }

                    @Override
                    public boolean isGui3d() {
                        return renderer.isGui3d();
                    }

                    @Override
                    public boolean usesBlockLight() {
                        return r.useBlockLight(stack);
                    }

                    @Override
                    public boolean isCustomRenderer() {
                        return false;
                    }

                    @Override
                    public TextureAtlasSprite getParticleIcon() {
                        return r.getParticleTexture(null, null, ModelData.EMPTY);
                    }

                    @Override
                    public ItemTransforms getTransforms() {
                        return ItemTransforms.NO_TRANSFORMS;
                    }

                    @Override
                    public ItemOverrides getOverrides() {
                        return ItemOverrides.EMPTY;
                    }
                }));
            }
        }
    }
}
