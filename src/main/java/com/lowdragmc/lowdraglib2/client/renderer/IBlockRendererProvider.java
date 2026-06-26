package com.lowdragmc.lowdraglib2.client.renderer;

import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

public interface IBlockRendererProvider {

    /**
     * Get the renderer for the block state.
     * @return return null if the block state does not have a renderer.
     */
    @Nullable
    IRenderer getRenderer(BlockState state);

}
