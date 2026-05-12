package com.lowdragmc.lowdraglib2.client.scene;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Hook for customising entity rendering in a {@link WorldSceneRenderer}. Set via
 * {@link WorldSceneRenderer#setSceneEntityRenderHook}. The hook is queried per entity
 * for both visibility (filter) and per-entity {@link PoseStack} mutation.
 */
public interface ISceneEntityRenderHook {

    /**
     * Return {@code false} to skip rendering this entity entirely.
     * Default: true (render every entity).
     */
    default boolean shouldRender(Level world, Entity entity) {
        return true;
    }

    /**
     * Called once per entity, before {@code EntityRenderDispatcher.submit} is invoked.
     * The {@link PoseStack} is the per-entity stack that the dispatcher will translate
     * to the entity's world position; mutate it here for additional translation, rotation,
     * or scale relative to the entity origin.
     */
    default void applyEntity(Level world, Entity entity, PoseStack poseStack, float partialTicks) {
    }
}
