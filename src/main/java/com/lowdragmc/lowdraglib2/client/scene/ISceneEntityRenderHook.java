package com.lowdragmc.lowdraglib2.client.scene;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public interface ISceneEntityRenderHook {
    default void applyEntity(Level world, Entity entity, PoseStack poseStack, float partialTicks) {

    }
}
