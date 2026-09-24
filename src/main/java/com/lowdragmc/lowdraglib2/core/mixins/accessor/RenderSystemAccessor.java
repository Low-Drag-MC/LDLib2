package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.GpuBackend;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The graphics backend the game settled on. Minecraft keeps it only in a local while starting up and
 * hands it to {@code RenderSystem} for shutdown; a second window has to be created through the same
 * backend, so it carries the same OpenGL pixel format or Vulkan capability as the game's own.
 */
@Mixin(RenderSystem.class)
public interface RenderSystemAccessor {
    @Accessor("BACKEND")
    @Nullable
    static GpuBackend ldlib2$getBackend() {
        throw new AssertionError();
    }
}
