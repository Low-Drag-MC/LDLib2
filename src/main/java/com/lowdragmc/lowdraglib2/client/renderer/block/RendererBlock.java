package com.lowdragmc.lowdraglib2.client.renderer.block;

import com.lowdragmc.lowdraglib2.client.renderer.IBlockRendererProvider;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

// used to present a renderer block
public class RendererBlock extends Block implements EntityBlock, IBlockRendererProvider {
    public static RendererBlock BLOCK;
    private static final IRenderer renderer = new RendererBlockRenderer();

    public RendererBlock(BlockBehaviour.Properties properties) {
        super(properties.noOcclusion().destroyTime(2));
        if (BLOCK != null) throw new IllegalStateException("RendererBlock already exists");
        BLOCK = this;
    }

    @Nullable
    @Override
    @ParametersAreNonnullByDefault
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new RendererBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public IRenderer getRenderer(BlockState state) {
        return renderer;
    }
}
