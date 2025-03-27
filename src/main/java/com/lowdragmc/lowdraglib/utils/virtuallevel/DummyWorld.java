package com.lowdragmc.lowdraglib.utils.virtuallevel;

import com.google.common.base.Suppliers;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.client.ClientProxy;
import com.lowdragmc.lowdraglib.client.scene.ParticleManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Author: KilaBash
 * Date: 2022/04/26
 * Description:
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DummyWorld extends Level {

    protected DummyChunkSource chunkProvider = new DummyChunkSource(this);
    private final BiomeManager biomeManager;
    public WeakReference<Level> level;
    protected final LevelLightEngine lighter;
    private final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();
    @Getter
    private Supplier<ClientLevel> asClientWorld = Suppliers.memoize(() -> WrappedClientWorld.of(this));


    public DummyWorld(Level level) {
        super((WritableLevelData) level.getLevelData(), level.dimension(), level.registryAccess(), level.dimensionTypeRegistration(), level::getProfiler,
                true, false, 0, 0);
        this.level = new WeakReference<>(level);
        this.lighter = new LevelLightEngine(chunkProvider, true, false);
        this.biomeManager = new BiomeManager(this, 0);
    }

    @NotNull
    public Level getLevel() {
        Level level = this.level.get();
        if (level == null) {
            level = Minecraft.getInstance().level;
            this.level = new WeakReference<>(level);
        }
        assert level != null;
        return level;
    }

    @Override
    public boolean setBlock(BlockPos pPos, BlockState pState, int pFlags, int pRecursionLeft) {
        return false;
    }

    @Override
    public void setBlockEntity(BlockEntity pBlockEntity) {
    }

    @Override
    public BlockState getBlockState(BlockPos pPos) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public void playSound(@Nullable Player pPlayer,
                          double pX, double pY, double pZ, SoundEvent pSound,
                          SoundSource pCategory, float pVolume, float pPitch) {

    }

    @Override
    public void playSound(@Nullable Player pPlayer,
                          Entity pEntity, SoundEvent pEvent,
                          SoundSource pCategory, float pVolume, float pPitch) {

    }

    @Override
    public String gatherChunkSourceStats() {
        return "";
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pPos) {
        return null;
    }

    @Override
    public float getShade(Direction direction, boolean b) {
        return switch (direction) {
            case DOWN, UP -> 0.9F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    @Override
    public LevelLightEngine getLightEngine() {
        if (LDLib.isClient()) {
            return Minecraft.getInstance().level.getLightEngine();
        }
        return null;
    }

    @Override
    public Holder<Biome> getBiome(BlockPos pPos) {
        return super.getBiome(pPos.offset(Vec3i.ZERO));
    }

    @Override
    public int getBrightness(LightLayer pLightType, BlockPos pBlockPos) {
        return 15;
    }

    @Override
    public int getRawBrightness(@Nonnull BlockPos pos, int p_226659_2_) {
        return 15;
    }

    @Override
    public boolean canSeeSky(@Nonnull BlockPos pos) {
        return true;
    }

    @Override
    public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {

    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int pX, int pY, int pZ) {
        return getLevel().getUncachedNoiseBiome(pX, pY, pZ);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int pX, int pY, int pZ) {
        return getLevel().getNoiseBiome(pX, pY, pZ);
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.biomeManager;
    }

    @Override
    public RegistryAccess registryAccess() {
        return getLevel().registryAccess();
    }

    @Override
    public PotionBrewing potionBrewing() {
        return null;
    }

    @Override
    public void setDayTimeFraction(float v) {

    }

    @Override
    public float getDayTimeFraction() {
        return getLevel().getDayTimeFraction();
    }

    @Override
    public float getDayTimePerTick() {
        return getLevel().getDayTimePerTick();
    }

    @Override
    public void setDayTimePerTick(float v) {

    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return getLevel().enabledFeatures();
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return getLevel().getBlockTicks();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return getLevel().getFluidTicks();
    }

    @Override
    public RecipeManager getRecipeManager() {
        return getLevel().getRecipeManager();
    }

    @Override
    public MapId getFreeMapId() {
        return getLevel().getFreeMapId();
    }

    @Override
    public Scoreboard getScoreboard() {
        return getLevel().getScoreboard();
    }

    @Override
    public Entity getEntity(int id) {
        return null;
    }

    @Override
    public TickRateManager tickRateManager() {
        return new TickRateManager();
    }

    @Nullable
    @Override
    public MapItemSavedData getMapData(MapId p_324234_) {
        return null;
    }

    @Override
    public void setMapData(MapId p_324009_, MapItemSavedData p_151534_) {

    }

    @Override
    public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {

    }

    @Override
    protected LevelEntityGetter<Entity> getEntities() {
        return new LevelEntityGetter<>() {
            @Nullable
            @Override
            public Entity get(int id) {
                return null;
            }

            @Nullable
            @Override
            public Entity get(UUID uuid) {
                return null;
            }

            @Override
            public Iterable<Entity> getAll() {
                return Collections.emptyList();
            }

            @Override
            public <U extends Entity> void get(EntityTypeTest<Entity, U> test, AbortableIterationConsumer<U> consumer) {

            }

            @Override
            public void get(AABB boundingBox, Consumer<Entity> consumer) {

            }

            @Override
            public <U extends Entity> void get(EntityTypeTest<Entity, U> test, AABB bounds, AbortableIterationConsumer<U> consumer) {

            }
        };
    }

    @Override
    public boolean isLoaded(BlockPos p_195588_1_) {
        return true;
    }

    @Override
    public ChunkSource getChunkSource() {
        return chunkProvider;
    }

    @Override
    public void levelEvent(@Nullable Player pPlayer, int pType, BlockPos pPos, int pData) {

    }

    @Override
    public void gameEvent(Holder<GameEvent> p_316267_, Vec3 p_220405_, GameEvent.Context p_220406_) {

    }

    @Override
    public List<? extends Player> players() {
        return Collections.emptyList();
    }

    @Override
    public FluidState getFluidState(BlockPos pPos) {
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public void playSeededSound(@Nullable Player player, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {

    }

    @Override
    public void playSeededSound(@Nullable Player player, double x, double y, double z, SoundEvent soundEvent, SoundSource soundSource, float volume, float pitch, long seed) {

    }

    @Override
    public void playSeededSound(@Nullable Player player, Entity entity, Holder<SoundEvent> sound, SoundSource category, float volume, float pitch, long seed) {

    }

    @Override
    public void addParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        if (particleManager != null) {
            var p = createParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);
            if (p != null) {
                particleManager.addParticle(p);
            }
        }
    }

    @Override
    public void addParticle(ParticleOptions particleData, boolean forceAlwaysRender, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        addParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    @Override
    public void addAlwaysVisibleParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        addParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    @Override
    public void addAlwaysVisibleParticle(ParticleOptions particleData, boolean ignoreRange, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        addParticle(particleData, ignoreRange, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    @Getter @Setter
    private ParticleManager particleManager;

    public BlockState getBlockState(int x, int y, int z) {
        return getBlockState(scratch.set(x, y, z));
    }

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public Particle createParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        ParticleProvider particleProvider = ClientProxy.getProvider(particleData.getType());
        if (particleProvider == null) {
            return null;
        }
        return particleProvider.createParticle(particleData, asClientWorld.get(), x, y, z, xSpeed, ySpeed, zSpeed);
    }
}
