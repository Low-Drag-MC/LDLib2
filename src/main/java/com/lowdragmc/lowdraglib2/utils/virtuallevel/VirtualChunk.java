package com.lowdragmc.lowdraglib2.utils.virtuallevel;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class VirtualChunk extends LevelChunk {

	public VirtualChunk(DummyWorld level, int x, int z) {
		super(level, new ChunkPos(x, z));
	}

	public DummyWorld getDummyWorld() {
		return (DummyWorld) this.getLevel();
	}

	@Override
	public @Nullable BlockState setBlockState(BlockPos pos, BlockState state, @Block.UpdateFlags int flags) {
		var dummyWorld = this.getDummyWorld();
		dummyWorld.prepareLighting(pos);
		BlockState result = super.setBlockState(pos, state, flags);
		if (state.isAir()) {
			dummyWorld.removeFilledBlock(pos);
		} else {
			dummyWorld.addFilledBlock(pos);
		}

		return result;
	}

	public FullChunkStatus getFullStatus() {
		return FullChunkStatus.FULL;
	}

}
