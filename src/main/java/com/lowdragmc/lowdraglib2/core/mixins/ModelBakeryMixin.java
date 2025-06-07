package com.lowdragmc.lowdraglib2.core.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.renderer.IBlockRendererProvider;
import com.lowdragmc.lowdraglib2.client.renderer.IItemRendererProvider;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;

/**
 * @author KilaBash
 * @date 2022/05/28
 */
@Mixin(ModelBakery.class)
public abstract class ModelBakeryMixin {

    @Shadow abstract UnbakedModel getModel(ResourceLocation modelPath);

    @WrapOperation(method = "getModel",
              at = @At(value = "INVOKE",
                       target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;[Ljava/lang/Object;)V"))
    protected void injectStateToModelLocation(Logger instance, String s, Object[] objects, Operation<Void> original) {
        ResourceLocation id = objects[0] instanceof ResourceLocation rl ? rl : null;
        if (id != null) {
            if (id.getPath().startsWith("block/")) {
                id = id.withPath(id.getPath().substring("block/".length()));
            } else if (id.getPath().startsWith("item/")) {
                id = id.withPath(id.getPath().substring("item/".length()));
            }
            if (BuiltInRegistries.ITEM.get(id) instanceof IItemRendererProvider) {
                return;
            }
        }
        original.call(instance, s, objects);
    }

    @ModifyExpressionValue(method = "registerModelAndLoadDependencies",
                           at = @At(value = "INVOKE",
                                    target = "Lnet/minecraft/client/resources/model/UnbakedModel;getDependencies()Ljava/util/Collection;"))
    protected Collection<ResourceLocation> ldlib2$changeLoadedModel(Collection<ResourceLocation> original,
                                                                   @Local(argsOnly = true) ModelResourceLocation modelResourceLocation,
                                                                   @Local(argsOnly = true) LocalRef<UnbakedModel> model) {
        if (!modelResourceLocation.getVariant().equals(ModelResourceLocation.STANDALONE_VARIANT)) {
            ResourceLocation resourceLocation = modelResourceLocation.id();
            var block = BuiltInRegistries.BLOCK.get(resourceLocation);
            if (block instanceof IBlockRendererProvider) {
                UnbakedModel newModel = getModel(LDLib2.id("block/renderer_model"));
                model.set(newModel);
                return newModel.getDependencies();
            }
        }
        return original;
    }
}
