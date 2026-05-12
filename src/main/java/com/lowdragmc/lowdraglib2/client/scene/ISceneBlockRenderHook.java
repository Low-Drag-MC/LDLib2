package com.lowdragmc.lowdraglib2.client.scene;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Hook for customising how a group of blocks is rendered in a {@link WorldSceneRenderer}.
 * Each {@code renderedBlocks} group is associated with one hook (passed to
 * {@link WorldSceneRenderer#addRenderedBlocks}). The hook is queried per block — return
 * per-block offsets / colors / BESR transforms based on position, blockstate, or external
 * state captured by the implementation.
 */
public interface ISceneBlockRenderHook {

    /**
     * ARGB color multiplier applied to the given block's (and its fluid's) vertices.
     * Default {@code -1} (0xFFFFFFFF) = no tint.
     *
     * <p>Called once per block per frame (uncached) or per block per compile (cached).
     * In cached mode the returned value is baked into the cached mesh; if the value depends
     * on dynamic state, call {@link WorldSceneRenderer#needCompileCache()} after that state
     * changes.
     *
     * @param world the scene's level (may be a virtual {@code DummyWorld})
     * @param pos   the block currently being rendered
     * @param state the block state at {@code pos}
     */
    default int getColorMultiplier(Level world, BlockPos pos, BlockState state) {
        return -1;
    }

    /**
     * World-space offset added to the given block's (and its fluid's) vertices.
     * Return {@code null} for no offset.
     *
     * <p>Called once per block per frame (uncached) or per block per compile (cached).
     * In cached mode the returned value is baked into the cached mesh; if the value depends
     * on dynamic state, call {@link WorldSceneRenderer#needCompileCache()} after that state
     * changes.
     *
     * @param world the scene's level (may be a virtual {@code DummyWorld})
     * @param pos   the block currently being rendered
     * @param state the block state at {@code pos}
     */
    @Nullable
    default Vector3f getOffset(Level world, BlockPos pos, BlockState state) {
        return null;
    }

    /**
     * Called once before each BESR submission in this group. The {@link PoseStack} has
     * already been translated to the block entity's world position; mutate it here for
     * extra transforms (rotation, scale, additional translation).
     */
    default void applyBESR(Level world, BlockPos pos, BlockEntity blockEntity,
                           PoseStack poseStack, float partialTicks) {
    }
}
