package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.client.renderer.IItemRendererProvider;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.ModelData;
import com.lowdragmc.lowdraglib2.client.renderer.TriState;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
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
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Mixin(ItemModelShaper.class)
public abstract class ItemModelShaperMixin {
    @Unique
    private final static Map<IRenderer, BakedModel> SHAPES_CACHE = new HashMap<>();

    @Inject(method = "getItemModel(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/resources/model/BakedModel;", at = @At("HEAD"), cancellable = true)
    public void ldlib2$injectGetModel(ItemStack stack, CallbackInfoReturnable<BakedModel> cir) {
        if (stack.getItem() instanceof IItemRendererProvider provider) {
            IRenderer renderer = provider.getRenderer(stack);
            if(renderer != null) {
                cir.setReturnValue(SHAPES_CACHE.computeIfAbsent(renderer, r -> new LDFabricBakedModel(r, stack)));
            }
        }
    }
}

class LDFabricBakedModel implements BakedModel, FabricBakedModel {
    private final IRenderer renderer;
    private final ItemStack stack;

    public LDFabricBakedModel(IRenderer renderer, ItemStack stack) {
        this.renderer = renderer;
        this.stack = stack;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(net.minecraft.world.level.BlockAndTintGetter blockView, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
        var quads = renderer.renderModel(null, null, null, null, randomSupplier.get(), ModelData.EMPTY, null);
        var emitter = context.getEmitter();
        for (var quad : quads) {
            emitter.fromVanilla(quad, null, null);
            emitter.emit();
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
        return renderer.renderModel(null, null, state, direction, random, ModelData.EMPTY, null);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return renderer.useAO() == TriState.DEFAULT || renderer.useAO() == TriState.TRUE;
    }

    @Override
    public boolean isGui3d() {
        return renderer.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return renderer.useBlockLight(stack);
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return renderer.getParticleTexture(null, null, ModelData.EMPTY);
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
