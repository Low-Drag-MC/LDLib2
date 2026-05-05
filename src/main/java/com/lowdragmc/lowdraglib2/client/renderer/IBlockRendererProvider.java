package com.lowdragmc.lowdraglib2.client.renderer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

public interface IBlockRendererProvider {

    /**
     * Get the renderer for the block state.
     * @return return null if the block state does not have a renderer.
     */
    @Nullable
    IRenderer getRenderer(BlockState state);

    /**
     * Provide a way to modify the light map based on the block in the world.
     */
    default int getLightMap(BlockAndLightGetter world, BlockState state, BlockPos pos) {
        if (state.emissiveRendering(world, pos)) {
            return 15728880;
        } else {
            int i = world.getBrightness(LightLayer.SKY, pos);
            int j = world.getBrightness(LightLayer.BLOCK, pos);
            int k = state.getLightEmission(world, pos);
            if (j < k) {
                j = k;
            }
            return i << 20 | j << 4;
        }
    }
}
