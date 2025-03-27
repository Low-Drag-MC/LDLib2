package com.lowdragmc.lowdraglib.client.model.custommodel;

import com.lowdragmc.lowdraglib.client.renderer.IBlockRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2023/3/24
 * @implNote ICTMPredicate
 */
public interface ICTMPredicate {
    ICTMPredicate DEFAULT = (level, state, pos, sourceState, sourcePos, side) -> {
        var stateAppearance = state.getAppearance(level, pos, side, sourceState, sourcePos);
        var sourceStateAppearance = sourceState.getAppearance(level, sourcePos, side, state, pos);
        return stateAppearance == sourceStateAppearance;
    };

    /**
     * Can texture connected to model.
     * @param level world
     * @param state current blockstate
     * @param pos block position
     * @param sourceState adjacent blockstate
     * @param sourcePos adjacent block position
     * @param side adjacent side
     * @return is connected
     */
    boolean isConnected(BlockAndTintGetter level, BlockState state, BlockPos pos, BlockState sourceState, BlockPos sourcePos, Direction side);

    @Nonnull
    static ICTMPredicate getPredicate(BlockState state) {
        if (state.getBlock() instanceof ICTMPredicate predicate) {
            return predicate;
        } else if (state.getBlock() instanceof IBlockRendererProvider rendererProvider && rendererProvider.getRenderer(state) instanceof ICTMPredicate predicate) {
            return predicate;
        }
        return DEFAULT;
    }
}
