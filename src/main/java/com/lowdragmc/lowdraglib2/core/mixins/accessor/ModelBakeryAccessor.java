package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(ModelBakery.class)
public interface ModelBakeryAccessor {

    @Invoker
    UnbakedModel invokeGetModel(ResourceLocation modelLocation);

    @Accessor
    Map<ModelResourceLocation, UnbakedModel> getTopLevelModels();
}
