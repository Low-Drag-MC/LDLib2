package com.lowdragmc.lowdraglib2.client.renderer;

import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * Author: KilaBash
 * Date: 2022/04/21
 * Description: 
 */
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
    default int getLightMap(BlockAndTintGetter world, BlockState state, BlockPos pos) {
        if (state.emissiveRendering(world, pos)) {
            return 15728880;
        } else {
            int i = world.getBrightness(LightLayer.SKY, pos);
            int j = world.getBrightness(LightLayer.BLOCK, pos);
            int k = state.getLightEmission();
            if (j < k) {
                j = k;
            }
            return i << 20 | j << 4;
        }
    }

    /**
     * Provide a way to modify the model state based on the block in the world.
     * you can use this to rotate the model based on the block state.
     */
    @OnlyIn(Dist.CLIENT)
    default ModelState getModelState(BlockAndTintGetter world, BlockPos pos, BlockState state) {
        return BlockModelRotation.X0_Y0;
    }

}
