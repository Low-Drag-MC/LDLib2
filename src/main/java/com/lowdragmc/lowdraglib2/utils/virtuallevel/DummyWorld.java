package com.lowdragmc.lowdraglib2.utils.virtuallevel;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.scene.ParticleManager;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.ClientClockManager;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.entity.*;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
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
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;

import javax.annotation.Nonnull;

import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Author: KilaBash
 * Date: 2022/04/26
 * Description:
 */
@ParametersAreNonnullByDefault
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
    private Object clientLevel;
    @Getter @Setter
    protected float dayTimeFraction = 0.0f;
    @Getter @Setter
    protected float dayTimePerTick = -1.0f;
    @Getter @Setter @Nullable
    private ParticleManager particleManager;
    @Getter
    private final ClockManager clockManager;
    private final EnvironmentAttributeSystem environmentAttributes;
    private final WorldBorder worldBorder = new WorldBorder();
    private final FuelValues fuelValues;
    /**
     * 26.2 no longer auto-assigns entity ids in the {@code Entity} constructor (the old global
     * counter was removed; ids now come from the server / network packets). Dummy-world entities are
     * client-only, so we hand out our own unique negative ids — negative to avoid ever colliding with
     * a networked (positive) id.
     */
    private final java.util.concurrent.atomic.AtomicInteger nextEntityId = new java.util.concurrent.atomic.AtomicInteger(0);

    public DummyWorld() {
        this(Platform.getClientRegistryAccess());
    }

    public DummyWorld(RegistryAccess registryAccess) {
        super(ClientSupport.createLevelData(), LEVEL_ID, registryAccess,
                registryAccess.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD),
                true, false, 0L, 1000000);
        this.registryAccess = registryAccess;
        this.chunkProvider = new DummyChunkSource(this);
        this.entityStorage = new TransientEntitySectionManager<>(Entity.class, new EntityCallbacks());
        this.lighter = new LevelLightEngine(chunkProvider, true, false);
        this.litSections = new LongOpenHashSet();
        this.filledBlocks = new LongOpenHashSet();
        byte[] nibbles = new byte[2048];
        Arrays.fill(nibbles, (byte)-1);
        this.defaultDataLayer = new DataLayer(nibbles);
        this.biome = registryAccess.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
        this.tickRateManager = new TickRateManager();
        if (LDLib2.isClient()) {
            particleManager = new ParticleManager();
            // ClientLevel proxy is wired lazily in tick(); asClientWorld.get() reads back
            // `this` and would see uninitialised fields if called from this constructor.
        }
        clockManager = new ClientClockManager();
        this.environmentAttributes = EnvironmentAttributeSystem.builder().build();
        this.fuelValues = new FuelValues.Builder(registryAccess, FeatureFlagSet.of()).build();
        this.updateSkyBrightness();
    }

    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }

    @Override
    public ClockManager clockManager() {
        return clockManager;
    }

    @Override
    public EnvironmentAttributeSystem environmentAttributes() {
        return environmentAttributes;
    }

    @Override
    public FuelValues fuelValues() {
        return fuelValues;
    }

    @Override
    public void setRespawnData(LevelData.RespawnData respawnData) {
        this.levelData.setSpawn(respawnData);
    }

    @Override
    public LevelData.RespawnData getRespawnData() {
        return this.levelData.getRespawnData();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return worldBorder;
    }

    @Override
    public String gatherChunkSourceStats() {
        return "";
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
        ChunkPos minChunk = new ChunkPos(
                SectionPos.blockToSectionCoord(pos.getX() - 1),
                SectionPos.blockToSectionCoord(pos.getZ() - 1));
        ChunkPos maxChunk = new ChunkPos(
                SectionPos.blockToSectionCoord(pos.getX() + 1),
                SectionPos.blockToSectionCoord(pos.getZ() + 1));
        ChunkPos.rangeClosed(minChunk, maxChunk).forEach((chunkPos) -> {
            if (this.litSections.add(chunkPos.pack())) {
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
    public int getSeaLevel() {
        return 0;
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
    public Scoreboard getScoreboard() {
        return new Scoreboard();
    }

    @Override
    public RecipeAccess recipeAccess() {
        if (LDLib2.isClient()) {
            return Minecraft.getInstance().level.recipeAccess();
        } else {
            return Platform.getMinecraftServer().getRecipeManager();
        }
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

    @Override
    public Collection<PartEntity<?>> dragonParts() {
        return List.of();
    }

    public void addEntity(Entity entity) {
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.EntityJoinLevelEvent(entity, this)).isCanceled()) return;
        int id;
        try {
            id = entity.getId();
        } catch (IllegalStateException notAssignedYet) {
            // 26.2: a freshly constructed entity has no id; assign one before dedup/storage.
            entity.setId(nextEntityId.decrementAndGet());
            id = entity.getId();
        }
        this.removeEntity(id, Entity.RemovalReason.DISCARDED);
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
            if (particleManager.getLevel() == null) {
                particleManager.setLevel(ClientSupport.asClientWorld(this));
            }
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
        Profiler.get().push(() -> BuiltInRegistries.ENTITY_TYPE.getKey(pEntity.getType()).toString());
        // Neo: Permit cancellation of Entity#tick via EntityTickEvent.Pre
        if (!net.neoforged.neoforge.event.EventHooks.fireEntityTickPre(pEntity).isCanceled()) {
            pEntity.tick();
            net.neoforged.neoforge.event.EventHooks.fireEntityTickPost(pEntity);
        }
        Profiler.get().pop();

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
    public void gameEvent(Holder<GameEvent> p_316267_, Vec3 p_220405_, GameEvent.Context p_220406_) {

    }

    @Override
    public List<? extends Player> players() {
        return Collections.emptyList();
    }

    @Override
    public FluidState getFluidState(BlockPos pPos) {
        return getBlockState(pPos).getFluidState();
    }

    @Override
    public void playSeededSound(@org.jspecify.annotations.Nullable Entity except, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {

    }

    @Override
    public void playSeededSound(@org.jspecify.annotations.Nullable Entity except, Entity sourceEntity, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {

    }

    @Override
    public void addParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        if (particleManager != null) {
            var p = ClientSupport.createParticle(this, particleData, x, y, z, xSpeed, ySpeed, zSpeed);
            if (p != null) {
                particleManager.addParticle(p);
            }
        }
    }

    @Override
    public void addParticle(ParticleOptions particle, boolean overrideLimiter, boolean alwaysShow, double x, double y, double z, double xd, double yd, double zd) {
        addParticle(particle, x, y, z, xd, yd, zd);
    }

    @Override
    public void levelEvent(@org.jspecify.annotations.Nullable Entity source, int type, BlockPos pos, int data) {

    }

    @Override
    public void addAlwaysVisibleParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        addParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    @Override
    public void addAlwaysVisibleParticle(ParticleOptions particleData, boolean ignoreRange, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        addParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    @Override
    public void explode(@Nullable Entity source, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator damageCalculator, double x, double y, double z, float r, boolean fire, ExplosionInteraction interactionType, ParticleOptions smallExplosionParticles, ParticleOptions largeExplosionParticles, WeightedList<ExplosionParticleInfo> blockParticles, Holder<SoundEvent> explosionSound) {

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

    public static class ClientSupport {
        public static ClientLevel asClientWorld(DummyWorld dummyWorld) {
            if (dummyWorld.clientLevel instanceof ClientLevel clientLevel) {
                return clientLevel;
            }
            dummyWorld.clientLevel = WrappedClientWorld.of(dummyWorld);
            return (ClientLevel) dummyWorld.clientLevel;
        }

        private static WritableLevelData createLevelData() {
            var levelData = new ClientLevel.ClientLevelData(Difficulty.PEACEFUL, false, false);
            levelData.setGameTime(6000L);
            return levelData;
        }

        @Nullable
        public static ParticleProvider<?> getProvider(ParticleType<?> type) {
            return Minecraft.getInstance().particleEngine.resourceManager.getProviders().get(BuiltInRegistries.PARTICLE_TYPE.getKey(type));
        }

        @Nullable
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static Particle createParticle(DummyWorld world, ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            var particleProvider = (ParticleProvider)getProvider(particleData.getType());
            if (particleProvider == null) {
                return null;
            }
            return particleProvider.createParticle(particleData, asClientWorld(world), x, y, z, xSpeed, ySpeed, zSpeed, world.random);
        }
    }
}
