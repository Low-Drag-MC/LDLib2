package com.lowdragmc.lowdraglib2.test;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * @author KilaBash
 * @date 2022/05/24
 * @implNote TestBlock
 */
public class NoRendererTestBlock extends Block {

    public static final NoRendererTestBlock BLOCK = new NoRendererTestBlock();
    private NoRendererTestBlock() {
        super(Properties.of().noOcclusion().destroyTime(5));
        this.registerDefaultState(this.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(BlockStateProperties.FACING);
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(BlockStateProperties.FACING, context.getNearestLookingDirection());
    }
}
