package com.lowdragmc.lowdraglib2.client.renderer.block;

import com.lowdragmc.lowdraglib2.CommonProxy;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RendererBlockEntity extends BlockEntity {

    @Getter @Setter
    IRenderer renderer = IRenderer.EMPTY;

    public RendererBlockEntity(BlockPos pos, BlockState blockState) {
        super(CommonProxy.TEST_BE_TYPE.get(), pos, blockState);
    }
}
