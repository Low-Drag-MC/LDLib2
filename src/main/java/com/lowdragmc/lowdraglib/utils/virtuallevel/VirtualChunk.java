package com.lowdragmc.lowdraglib.utils.virtuallevel;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.TickContainerAccess;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class VirtualChunk extends LevelChunk {

	final DummyWorld world;
	boolean needsLight;
	final int x;
	final int z;

	private final LevelChunkSection[] sections;

	public VirtualChunk(DummyWorld world, int x, int z) {
		super(world, new ChunkPos(x, z));

		this.world = world;
		this.needsLight = true;
		this.x = x;
		this.z = z;

		int sectionCount = world.getSectionsCount();
		this.sections = new LevelChunkSection[sectionCount];

		for (int i = 0; i < sectionCount; i++) {
			sections[i] = new VirtualChunkSection(this, i << 4);
		}

	}

	@Override
	public LevelChunkSection[] getSections() {
		return sections;
	}

	@Override
	public ChunkStatus getPersistedStatus() {
		return ChunkStatus.LIGHT;
	}

	@Nullable
	@Override
	public BlockState setBlockState(BlockPos p_177436_1_, BlockState p_177436_2_, boolean p_177436_3_) {
		return null;
	}

	@Override
	public void setBlockEntity(BlockEntity p_177426_2_) {}

	@SuppressWarnings("deprecation")
	@Override
	public void addEntity(Entity p_76612_1_) {}

	@Override
	public Set<BlockPos> getBlockEntitiesPos() {
		return Set.of();
	}

	@Override
	public Collection<Map.Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
		return Collections.emptyList();
	}

	@Override
	public void setHeightmap(Heightmap.Types p_201607_1_, long[] p_201607_2_) {}

	@Override
	public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types type) {
		return new Heightmap(this, type);
	}

	@Override
	public void setUnsaved(boolean p_177427_1_) {}

	@Override
	public boolean isUnsaved() {
		return false;
	}

	@Override
	public void removeBlockEntity(BlockPos pos) {
		world.removeBlockEntity(pos);
	}

	@Override
	public ShortList[] getPostProcessing() {
		return new ShortList[0];
	}

	@Nullable
	@Override
	public CompoundTag getBlockEntityNbt(BlockPos p_201579_1_) {
		return null;
	}

	@Override
	public UpgradeData getUpgradeData() {
		return UpgradeData.EMPTY;
	}

	@Override
	public void setInhabitedTime(long p_177415_1_) {}

	@Override
	public long getInhabitedTime() {
		return 0;
	}

	@Override
	public boolean isLightCorrect() {
		return needsLight;
	}

	@Override
	public void setLightCorrect(boolean needsLight) {
		this.needsLight = needsLight;
	}

	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos pos) {
		return world.getBlockEntity(pos);
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		return world.getBlockState(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos p_204610_1_) {
		return Fluids.EMPTY.defaultFluidState();
	}

	@Override
	@Nullable
	public StructureStart getStartForStructure(Structure structure) {
		return null;
	}

	@Override
	public void setStartForStructure(Structure structure, StructureStart start) {
	}

	@Override
	public Map<Structure, StructureStart> getAllStarts() {
		return Collections.emptyMap();
	}

	@Override
	public void setAllStarts(Map<Structure, StructureStart> structureStarts) {
	}

	@Override
	public LongSet getReferencesForStructure(Structure structure) {
		return LongSets.emptySet();
	}

	@Override
	public void addReferenceForStructure(Structure structure, long reference) {
	}

	@Override
	public Map<Structure, LongSet> getAllReferences() {
		return Collections.emptyMap();
	}

	@Override
	public void setAllReferences(Map<Structure, LongSet> structureReferences) {
	}

	@Override
	public int getMinBuildHeight() {
		return world.getMinBuildHeight();
	}

	@Override
	public TickContainerAccess<Fluid> getFluidTicks() {
		return BlackholeTickAccess.emptyContainer();
	}

	@Override
	public TicksToSave getTicksForSerialization() {
		return new TicksToSave((pGameTime, pIdGetter) -> new CompoundTag(), (pGameTime, pIdGetter) -> new CompoundTag());
	}

	@Override
	public TickContainerAccess<Block> getBlockTicks() {
		return BlackholeTickAccess.emptyContainer();
	}

}
