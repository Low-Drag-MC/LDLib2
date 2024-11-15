package com.lowdragmc.lowdraglib.fabric.core.mixins;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author KilaBash
 * @date 2023/2/17
 * @implNote ModelBakeryMixin
 */
@Mixin(ModelBakery.class)
public abstract class ModelBakeryMixin {

    @Shadow public abstract UnbakedModel getModel(ResourceLocation modelLocation);

    @Shadow @Final private Map<ResourceLocation, UnbakedModel> unbakedCache;

    @Shadow @Final private Map<ResourceLocation, UnbakedModel> topLevelModels;

    /**
     * register additional models as what forge does
     */
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", ordinal = 3))
    private void injectModelBakery(BlockColors blockColors, ProfilerFiller profilerFiller, Map modelResources, Map blockStateResources, CallbackInfo ci) {
        Set<ResourceLocation> models = new HashSet<>();

        for (var entry : Minecraft.getInstance().getResourceManager().listResources("models",
                id -> id.getNamespace().equals("ldlib") && id.getPath().endsWith(".json")).entrySet()) {
            if (entry.getValue().sourcePackId().equals("ldlib")) {
                models.add(new ResourceLocation(entry.getKey().getNamespace(),
                        entry.getKey().getPath().replace("models/", "").replace(".json", "")));
            }
        }

        for (IRenderer renderer : IRenderer.EVENT_REGISTERS) {
            renderer.onAdditionalModel(models::add);
        }

        for (ResourceLocation rl : models) {
            UnbakedModel unbakedmodel = this.getModel(rl);
            this.unbakedCache.put(rl, unbakedmodel);
            this.topLevelModels.put(rl, unbakedmodel);
        }
    }

}
