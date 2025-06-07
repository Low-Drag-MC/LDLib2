package com.lowdragmc.lowdraglib2.utils.virtuallevel;

import com.google.common.base.Suppliers;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.ClientProxy;
import com.lowdragmc.lowdraglib2.client.scene.ParticleManager;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.Difficulty;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.entity.*;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Author: KilaBash
 * Date: 2022/04/26
 * Description:
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DummyWorld extends Level {
    private static final ResourceKey<Level> LEVEL_ID;
    static {
        LEVEL_ID = ResourceKey.create(Registries.DIMENSION, LDLib2.id("dummy_world"));
    }

    protected final RegistryAccess registryAccess;
    protected final DummyChunkSource chunkProvider;
    protected final TransientEntitySectionManager<Entity> entityStorage;
    protected final LevelLightEngine lighter;
    protected final LongSet litSections;
    protected final DataLayer defaultDataLayer;
    @Getter
    protected final LongSet filledBlocks;
    protected final Holder<Biome> biome;
    protected final TickRateManager tickRateManager;
    @Getter
    protected Supplier<ClientLevel> asClientWorld = Suppliers.memoize(() -> WrappedClientWorld.of(this));
    @Getter @Setter
    protected float dayTimeFraction = 0.0f;
    @Getter @Setter
    protected float dayTimePerTick = -1.0f;
    @OnlyIn(Dist.CLIENT)
    @Getter @Setter
    private ParticleManager particleManager;

    public DummyWorld() {
        this(Platform.getFrozenRegistry());
    }

    public DummyWorld(RegistryAccess registryAccess) {
        super(createLevelData(), LEVEL_ID, registryAccess, registryAccess.registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD),
                () -> InactiveProfiler.INSTANCE, true, false, 0L, 1000000);
        this.registryAccess = registryAccess;
        this.chunkProvider = new DummyChunkSource(this);
        this.entityStorage = new TransientEntitySectionManager<>(Entity.class, new EntityCallbacks());
        this.lighter = new LevelLightEngine(chunkProvider, true, false);
        this.litSections = new LongOpenHashSet();
        this.filledBlocks = new LongOpenHashSet();
        byte[] nibbles = new byte[2048];
        Arrays.fill(nibbles, (byte)-1);
        this.defaultDataLayer = new DataLayer(nibbles);
        this.biome = registryAccess.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
        this.tickRateManager = new TickRateManager();
        if (LDLib2.isClient()) {
            particleManager = new ParticleManager();
        }
    }

    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }

    private static ClientLevel.ClientLevelData createLevelData() {
        var levelData = new ClientLevel.ClientLevelData(Difficulty.PEACEFUL, false, false);
        levelData.setDayTime(6000L);
        return levelData;
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

    @Override
    public float getShade(Direction direction, boolean shade) {
        if (!shade) {
            return 1.0F;
        }
        return switch (direction) {
            case DOWN, UP -> 0.9F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    @Override
    public Holder<Biome> getBiome(BlockPos pPos) {
        return super.getBiome(pPos.offset(Vec3i.ZERO));
    }

    ///  light
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

    public void prepareLighting(BlockPos pos) {
        ChunkPos minChunk = new ChunkPos(pos.offset(-1, -1, -1));
        ChunkPos maxChunk = new ChunkPos(pos.offset(1, 1, 1));
        ChunkPos.rangeClosed(minChunk, maxChunk).forEach((chunkPos) -> {
            if (this.litSections.add(chunkPos.toLong())) {
                LevelLightEngine lightEngine = this.getLightEngine();

                for(int i = 0; i < this.getSectionsCount(); ++i) {
                    int y = this.getSectionYFromSectionIndex(i);
                    SectionPos sectionPos = SectionPos.of(chunkPos, y);
                    lightEngine.updateSectionStatus(sectionPos, false);
                    lightEngine.queueSectionData(LightLayer.BLOCK, sectionPos, this.defaultDataLayer);
                    lightEngine.queueSectionData(LightLayer.SKY, sectionPos, this.defaultDataLayer);
                }

                lightEngine.setLightEnabled(chunkPos, true);
                lightEngine.propagateLightSources(chunkPos);
                lightEngine.retainData(chunkPos, false);
            }
        });
    }

    public AABB getBounds() {
        if (this.filledBlocks.isEmpty()) {
            return new AABB(0, 0, 0, 0, 0, 0);
        } else {
            BlockPos.MutableBlockPos min = new BlockPos.MutableBlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
            BlockPos.MutableBlockPos max = new BlockPos.MutableBlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
            BlockPos.MutableBlockPos cur = new BlockPos.MutableBlockPos();
            this.filledBlocks.forEach((packedPos) -> {
                cur.set(packedPos);
                min.setX(Math.min(min.getX(), cur.getX()));
                min.setY(Math.min(min.getY(), cur.getY()));
                min.setZ(Math.min(min.getZ(), cur.getZ()));
                max.setX(Math.max(max.getX(), cur.getX() + 1));
                max.setY(Math.max(max.getY(), cur.getY() + 1));
                max.setZ(Math.max(max.getZ(), cur.getZ() + 1));
            });
            return new AABB(min.getX(), min.getY(), min.getZ(),
                            max.getX(), max.getY(), max.getZ());
        }
    }

    public boolean isFilledBlock(BlockPos blockPos) {
        return this.filledBlocks.contains(blockPos.asLong());
    }

    protected void removeFilledBlock(BlockPos pos) {
        this.filledBlocks.remove(pos.asLong());
    }

    protected void addFilledBlock(BlockPos pos) {
        this.filledBlocks.add(pos.asLong());
    }

    @Override
    public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {

    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int pX, int pY, int pZ) {
        return this.biome;
    }

    @Override
    public PotionBrewing potionBrewing() {
        return null;
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return FeatureFlags.DEFAULT_FLAGS;
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public RecipeManager getRecipeManager() {
        if (LDLib2.isClient()) {
            return Minecraft.getInstance().level.getRecipeManager();
        } else {
            return Platform.getMinecraftServer().getRecipeManager();
        }
    }

    @Override
    public MapId getFreeMapId() {
        return new MapId(1);
    }

    @Override
    public Scoreboard getScoreboard() {
        return new Scoreboard();
    }

    /// entities
    @Override
    protected LevelEntityGetter<Entity> getEntities() {
        return this.entityStorage.getEntityGetter();
    }

    @Override
    @Nullable
    public Entity getEntity(int id) {
        return this.getEntities().get(id);
    }

    public void addEntity(Entity entity) {
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.EntityJoinLevelEvent(entity, this)).isCanceled()) return;
        this.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
        this.entityStorage.addEntity(entity);
        entity.onAddedToLevel();
    }

    public void removeEntity(int entityId, Entity.RemovalReason reason) {
        Entity entity = this.getEntities().get(entityId);
        if (entity != null) {
            entity.setRemoved(reason);
            entity.onClientRemoval();
        }
    }

    /// tick
    public void tickWorld() {
        tickEntities();
        if (LDLib2.isClient() && particleManager != null) {
            particleManager.tick();
        }
    }

    public void tickEntities() {
        for (var entity : getEntities().getAll()) {
            if (!entity.isRemoved() && !entity.isPassenger() && !this.tickRateManager.isEntityFrozen(entity)) {
                tickNonPassenger(entity);
            }
        }
        this.tickBlockEntities();
    }

    private void tickNonPassenger(Entity pEntity) {
        pEntity.setOldPosAndRot();
        pEntity.tickCount++;
        this.getProfiler().push(() -> BuiltInRegistries.ENTITY_TYPE.getKey(pEntity.getType()).toString());
        // Neo: Permit cancellation of Entity#tick via EntityTickEvent.Pre
        if (!net.neoforged.neoforge.event.EventHooks.fireEntityTickPre(pEntity).isCanceled()) {
            pEntity.tick();
            net.neoforged.neoforge.event.EventHooks.fireEntityTickPost(pEntity);
        }
        this.getProfiler().pop();

        for (Entity entity : pEntity.getPassengers()) {
            this.tickPassenger(pEntity, entity);
        }
    }

    private void tickPassenger(Entity mount, Entity rider) {
        if (rider.isRemoved() || rider.getVehicle() != mount) {
            rider.stopRiding();
        } else if (rider instanceof Player) {
            rider.setOldPosAndRot();
            rider.tickCount++;
            rider.rideTick();

            for (var entity : rider.getPassengers()) {
                this.tickPassenger(rider, entity);
            }
        }
    }

    @Override
    public TickRateManager tickRateManager() {
        return tickRateManager;
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

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public Particle createParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        ParticleProvider particleProvider = ClientProxy.getProvider(particleData.getType());
        if (particleProvider == null) {
            return null;
        }
        return particleProvider.createParticle(particleData, asClientWorld.get(), x, y, z, xSpeed, ySpeed, zSpeed);
    }

    private class EntityCallbacks implements LevelCallback<Entity> {
        private EntityCallbacks() {
        }

        public void onCreated(Entity entity) {
        }

        public void onDestroyed(Entity entity) {
        }

        public void onTickingStart(Entity entity) {
        }

        public void onTickingEnd(Entity entity) {
        }

        public void onTrackingStart(Entity entity) {
        }

        public void onTrackingEnd(Entity entity) {
        }

        public void onSectionChange(Entity object) {
        }
    }
}
