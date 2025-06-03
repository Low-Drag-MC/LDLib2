package com.lowdragmc.lowdraglib.utils.virtuallevel;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class VirtualChunk extends LevelChunk {
	final DummyWorld dummyWorld;

	public VirtualChunk(DummyWorld level, int x, int z) {
		super(level, new ChunkPos(x, z));
		this.dummyWorld = level;
	}

	public @Nullable BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
		this.dummyWorld.prepareLighting(pos);
		BlockState result = super.setBlockState(pos, state, isMoving);
		if (state.isAir()) {
			this.dummyWorld.removeFilledBlock(pos);
		} else {
			this.dummyWorld.addFilledBlock(pos);
		}

		return result;
	}

	public FullChunkStatus getFullStatus() {
		return FullChunkStatus.FULL;
	}

}
