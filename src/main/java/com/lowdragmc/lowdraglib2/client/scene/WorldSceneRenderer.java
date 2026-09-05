package com.lowdragmc.lowdraglib2.client.scene;

import com.lowdragmc.lowdraglib2.core.mixins.accessor.MeshDataAccessor;
import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.PositionedRect;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.DummyWorld;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.WrappedBlockAndTintGetter;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.SectionPos;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.*;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.textures.GpuTexture;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static net.minecraft.world.level.block.RenderShape.MODEL;


/**
 * @author KilaBash
 * @implNote render a scene, through VBO compilation scene, greatly optimize rendering performance.
 */
@Accessors(chain = true)
public abstract class WorldSceneRenderer {

    enum CacheState {
        UNCREATED,
        NEED,
        COMPILING,
        COMPILED
    }

    /**
     * A compiled, CPU-side section mesh (one {@link ChunkSectionLayer}). Vertices are in
     * <em>section-local</em> coordinates ({@code pos & 15}); the section's world origin is carried
     * here and applied at draw time via the ChunkSection offset UBO. Backed by a
     * {@link SectionBufferBuilderPack} that must stay open while referenced.
     */
    protected record SectionCpuMesh(MeshData mesh, int originX, int originY, int originZ) {
    }

    /**
     * A GPU-resident section mesh: vertex (and, for sorted translucent, index) data uploaded to
     * {@link GpuBuffer}s, ready to be drawn through the terrain pipeline. {@code indexBuffer} is
     * {@code null} for solid/cutout (they use the shared sequential quad index).
     */
    protected record SectionGpuMesh(GpuBuffer vertexBuffer, @Nullable GpuBuffer indexBuffer,
                                    @Nullable com.mojang.blaze3d.IndexType indexType, int indexCount,
                                    int originX, int originY, int originZ) implements AutoCloseable {
        @Override
        public void close() {
            vertexBuffer.close();
            if (indexBuffer != null) indexBuffer.close();
        }
    }

    /** Pack a section origin (block coords, multiples of 16) into a dedup key for the UBO list. */
    private static long packSectionOrigin(int x, int y, int z) {
        return net.minecraft.core.SectionPos.asLong(x >> 4, y >> 4, z >> 4);
    }

    public final Level world;

    /**
     * Compiled cache: per-layer list of meshes. Each {@code renderedBlocks} group contributes one
     * mesh per layer it touches. Drawing iterates the lists in {@link ChunkSectionLayer} order.
     * Underlying {@link ByteBufferBuilder}s live in {@link #cacheBuilders} and must remain open
     * while these meshes are referenced.
     */
    /**
     * Identity-keyed map: the caller's {@link Collection} reference is the handle used by
     * {@link #removeRenderedBlocks(Collection)}, while {@link RenderedBlocksEntry#snapshot} holds an
     * immutable copy that the (possibly background) compile and render code iterates safely.
     */
    public final Map<Collection<BlockPos>, RenderedBlocksEntry> renderedBlocksMap;

    public record RenderedBlocksEntry(Set<BlockPos> snapshot, @Nullable ISceneBlockRenderHook hook) {
    }

    /** CPU-side compiled cache (section-local meshes per layer), produced by the (possibly background)
     *  compile. Uploaded to {@link #cachedGpuMeshes} lazily on the render thread, then released. */
    @Nullable
    protected Map<ChunkSectionLayer, List<SectionCpuMesh>> cachedMeshes;
    /** GPU-resident cache, drawn each frame via {@link #renderTerrain}. Closed in {@link #deleteCacheBuffer()}. */
    @Nullable
    protected Map<ChunkSectionLayer, List<SectionGpuMesh>> cachedGpuMeshes;
    /** Long-lived buffer pack backing {@link #cachedMeshes}. Closed in {@link #deleteCacheBuffer()}. */
    @Nullable
    protected SectionBufferBuilderPack cacheBuilders;
    protected Set<BlockPos> blockEntities;
    @Getter
    protected boolean useCache;
    /**
     * When {@link #useCache} is on, prefer running the cache compile on the main/render thread,
     * spread across multiple frames, instead of on a background thread. Useful when the backing
     * {@link Level} doesn't support off-thread access.
     */
    @Getter
    protected boolean syncCompile;
    /**
     * Per-frame time budget for {@link #syncCompile} mode, in nanoseconds. Default 2ms.
     * The compile loop always processes at least one block per frame to make forward progress.
     */
    @Getter
    @Setter
    protected long syncCompileTimeBudgetNanos = 2_000_000L;
    /**
     * Hard cap on blocks processed per frame in {@link #syncCompile} mode. Acts as a safety net
     * when individual blocks render extremely fast.
     */
    @Getter
    @Setter
    protected int syncCompileMaxBlocksPerFrame = 200;
    @Nullable
    protected SyncCompileState syncCompileState;
    @Getter
    @Setter
    protected boolean endBatchLast = false;// if true, endBatch will be called after all rendering
    protected boolean ortho;
    protected AtomicReference<CacheState> cacheState;
    protected int maxProgress;
    protected int progress;
    protected Thread thread;
    @Getter
    protected ParticleManager particleManager;
    /** Position is forced to {@link Vec3#ZERO} so particle / entity extraction yields
     *  world-space coords (our view matrix already includes the {@code -eyePos} translation
     *  via {@code lookAt}). Rotation / yaw / pitch are sync'd each frame from
     *  {@link #cameraEntity} via {@link SceneCamera#setSceneRotation}. */
    protected final SceneCamera camera = new SceneCamera();
    protected final CameraEntity cameraEntity;
    /** The projection matrix as Matrix4f, kept for project/unProject */
    protected Matrix4f projectionMatrix = new Matrix4f();
    /** Viewport rect set in {@link #setupCamera}; used by {@link #project} / {@link #unProject}
     *  so we never need to read GL state via {@code glGetIntegerv(GL_VIEWPORT)}. */
    protected int viewportX, viewportY, viewportWidth, viewportHeight;
    @Setter
    protected ClipContext.Block clipBlock = ClipContext.Block.OUTLINE;
    @Setter
    protected ClipContext.Fluid clipFluid = ClipContext.Fluid.NONE;
    @Setter
    @Nullable
    protected ProjectionMatrixBuffer projectionMatrixBuffer;
    /** Scene-private Globals UBO (camera = eyePos) used while drawing the core/terrain pass. */
    @Nullable
    private GlobalSettingsUniform sceneGlobals;
    /** Lazily-created 4-byte readback buffer for async depth pixel sampling. */
    @Nullable
    private GpuBuffer depthReadbackBuffer;
    /** True between issuing a copyTextureToBuffer and the fence callback firing. */
    private boolean depthReadInFlight;
    /** Most recent completed depth sample (NDC 0..1). Lags by 1+ frames; initial value
     *  corresponds to the far plane so first-frame unProject still yields a valid ray. */
    private float lastDepthSample = 0.0001f;
    /** Scene-private vanilla submission pipeline, lazily built in
     *  {@link #ensureFeatureRenderDispatcher(RenderBuffers)} and identity-cached on the
     *  {@link RenderBuffers} passed by the caller. Closed + rebuilt only when buffers change. */
    @Nullable private SubmitNodeStorage submitNodeStorage;
    @Nullable private FeatureRenderDispatcher featureRenderDispatcher;
    @Nullable private RenderBuffers cachedBuffersIdentity;
    @Setter @Nullable
    private Consumer<WorldSceneRenderer> beforeWorldRender;
    @Setter @Nullable
    private SceneRenderHook beforeAllSubmit;
    @Setter @Nullable
    private SceneRenderHook afterBuiltinSubmit;
    @Setter @Nullable
    private SceneRenderHook afterTranslucentDispatch;
    @Setter @Nullable
    private SceneRenderHook afterAllDispatch;
    @Setter @Nullable
    private Consumer<BlockHitResult> onLookingAt;
    @Setter @Nullable
    private ISceneEntityRenderHook sceneEntityRenderHook;
    @Getter
    private Vector3f lastHit;
    @Getter
    private BlockHitResult lastTraceResult;
    @Setter
    private Set<BlockPos> blocked;
    @Getter
    private Vector3f eyePos = new Vector3f(0, 0, 10f);
    @Getter
    private Vector3f lookAt = new Vector3f(0, 0, 0);
    @Getter
    private Vector3f worldUp = new Vector3f(0, 1, 0);
    @Getter @Setter
    private float fov = 60f;
    private float minX, maxX, minY, maxY, minZ, maxZ;
    /**
     * The viewport aspect ratio {@link #setupCamera} last built the projection with. Kept because only
     * the projection knows it — it comes from the viewport rather than from any camera setting — and
     * {@link #getViewHalfHeight} cannot answer for an orthographic camera without it.
     */
    private float lastAspectRatio = 1f;

    public WorldSceneRenderer(Level world) {
        this.world = world;
        renderedBlocksMap = new IdentityHashMap<>();
        cacheState = new AtomicReference<>(CacheState.UNCREATED);
        cameraEntity = new CameraEntity(world);
    }

    /**
     * Release all resources used by this renderer. this should be called in the render thread.
     */
    public void releaseResource() {
        deleteCacheBuffer();
        if (projectionMatrixBuffer != null) {
            projectionMatrixBuffer.close();
            projectionMatrixBuffer = null;
        }
        if (sceneGlobals != null) {
            sceneGlobals.close();
            sceneGlobals = null;
        }
        if (depthReadbackBuffer != null) {
            depthReadbackBuffer.close();
            depthReadbackBuffer = null;
        }
        // Reset in-flight flag so a future renderer reusing this slot doesn't get stuck
        // skipping reads forever if the callback never fired (e.g. context loss).
        depthReadInFlight = false;
        if (featureRenderDispatcher != null) {
            featureRenderDispatcher.close();
            featureRenderDispatcher = null;
        }
        submitNodeStorage = null;
        cachedBuffersIdentity = null;
    }

    /**
     * Identity-cached scene-private {@link FeatureRenderDispatcher}. The dispatcher captures
     * {@code bufferSource} / {@code crumblingBufferSource} in its constructor and can't be
     * rebound; if {@code buffers} differs from {@link #cachedBuffersIdentity} we tear down
     * and rebuild. Same-instance buffers (the common case) hit the cache.
     */
    private FeatureRenderDispatcher ensureFeatureRenderDispatcher(RenderBuffers buffers) {
        if (featureRenderDispatcher != null && cachedBuffersIdentity == buffers) {
            return featureRenderDispatcher;
        }
        if (featureRenderDispatcher != null) {
            featureRenderDispatcher.close();
        }
        var mc = Minecraft.getInstance();
        submitNodeStorage = new SubmitNodeStorage();
        // 26.2: FeatureRenderDispatcher takes (RenderBuffers, ModelManager, AtlasManager, Font,
        // GameRenderState). The SubmitNodeStorage is now driven per-frame via prepareFrame(storage),
        // and OutlineBufferSource / crumblingBufferSource are no longer constructor args
        // (outlines fold into SubmitNodeCollection.outline).
        featureRenderDispatcher = new FeatureRenderDispatcher(
                buffers,
                mc.getModelManager(),
                mc.getAtlasManager(),
                mc.font,
                mc.gameRenderer.gameRenderState()
        );
        cachedBuffersIdentity = buffers;
        return featureRenderDispatcher;
    }

    /** Build a {@link CameraRenderState} positioned at {@link #eyePos}, used by every
     *  scene submission path (BESR / entity / particle). */
    private CameraRenderState buildCameraRenderState() {
        var s = new CameraRenderState();
        s.pos = new Vec3(eyePos.x(), eyePos.y(), eyePos.z());
        s.blockPos = BlockPos.containing(eyePos.x(), eyePos.y(), eyePos.z());
        return s;
    }

    /** Decode ARGB int into the 0..1 float multipliers used by {@link VertexConsumerWrapper#setColorMultiplier}. */
    private static void applyArgbColorMultiplier(VertexConsumerWrapper w, int argb) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >>  8) & 0xFF) / 255f;
        float b = ( argb        & 0xFF) / 255f;
        w.setColorMultiplier(r, g, b, a);
    }

    public WorldSceneRenderer setParticleManager(ParticleManager particleManager) {
        this.particleManager = particleManager;
        if (this.world instanceof DummyWorld dummyWorld) {
            dummyWorld.setParticleManager(particleManager);
        }
        setCameraLookAt(eyePos, lookAt, worldUp);
        return this;
    }

    public WorldSceneRenderer useCacheBuffer(boolean useCache) {
        if (this.useCache || !Minecraft.getInstance().isSameThread()) return this;
        this.useCache = useCache;
        if (!useCache) {
            deleteCacheBuffer();
        }
        return this;
    }

    /**
     * Toggle the incremental main-thread cache compile path. Triggers a recompile so the new
     * mode takes effect on the next render.
     */
    public WorldSceneRenderer syncCompile(boolean syncCompile) {
        if (this.syncCompile == syncCompile) return this;
        this.syncCompile = syncCompile;
        needCompileCache();
        return this;
    }

    public WorldSceneRenderer useOrtho(boolean ortho) {
        this.ortho = ortho;
        return this;
    }

    /**
     * Per-frame, incrementally advanced state for sync-compile mode. The builders are backed by
     * {@link #cacheBuilders}; completed meshes are only published once every block is processed.
     */
    protected static class SyncCompileState {
        /** One 16³ section's worth of blocks (with its group's hook) to compile section-local. */
        private record SectionJob(int ox, int oy, int oz, @Nullable ISceneBlockRenderHook hook, List<BlockPos> blocks) {
        }

        final BlockCompileContext compileContext;
        final Results results = new Results();
        final List<SectionJob> jobs = new ArrayList<>();
        int jobIndex;
        /** -1 = current section not yet begun; otherwise index of the next block within the section. */
        int blockIndex = -1;

        SyncCompileState(WorldSceneRenderer renderer, BlockAndTintGetter region,
                         SectionBufferBuilderPack builders, List<RenderedBlocksEntry> entries) {
            this.compileContext = new BlockCompileContext(renderer, region, builders);
            for (var entry : entries) {
                var bySection = new java.util.LinkedHashMap<Long, List<BlockPos>>();
                for (BlockPos pos : entry.snapshot()) {
                    bySection.computeIfAbsent(packSectionOrigin(pos.getX(), pos.getY(), pos.getZ()),
                            k -> new ArrayList<>()).add(pos);
                }
                bySection.forEach((key, blocks) -> jobs.add(new SectionJob(
                        SectionPos.x(key) << 4, SectionPos.y(key) << 4, SectionPos.z(key) << 4,
                        entry.hook(), blocks)));
            }
        }

        boolean step() {
            while (jobIndex < jobs.size()) {
                SectionJob job = jobs.get(jobIndex);
                if (blockIndex < 0) {
                    compileContext.beginSection(job.ox(), job.oy(), job.oz());
                    blockIndex = 0;
                }
                if (blockIndex < job.blocks().size()) {
                    compileContext.renderBlock(job.blocks().get(blockIndex++), job.hook(), results);
                    return true;
                }
                compileContext.flushSection(results);
                blockIndex = -1;
                jobIndex++;
            }
            return false;
        }

        void finish() {
            // Defensive: flush a section that was begun but not yet flushed (step() normally flushes).
            if (blockIndex >= 0) {
                compileContext.flushSection(results);
                blockIndex = -1;
            }
        }

        void discard() {
            results.release();
            compileContext.discard();
        }
    }

    public WorldSceneRenderer deleteCacheBuffer() {
        cancelCompile();
        if (cachedMeshes != null) {
            for (List<SectionCpuMesh> list : cachedMeshes.values()) {
                for (SectionCpuMesh m : list) {
                    m.mesh().close();
                }
            }
            cachedMeshes = null;
        }
        if (cachedGpuMeshes != null) {
            for (List<SectionGpuMesh> list : cachedGpuMeshes.values()) {
                for (SectionGpuMesh m : list) {
                    m.close();
                }
            }
            cachedGpuMeshes = null;
        }
        if (cacheBuilders != null) {
            cacheBuilders.close();
            cacheBuilders = null;
        }
        this.blockEntities = null;
        cacheState.set(CacheState.UNCREATED);
        return this;
    }

    protected void makeSureCacheBufferCreated() {
        if (cachedMeshes == null && cachedGpuMeshes == null) {
            cachedMeshes = new EnumMap<>(ChunkSectionLayer.class);
            cacheBuilders = new SectionBufferBuilderPack();
            cancelCompile();
            cacheState.set(CacheState.NEED);
        }
    }

    public WorldSceneRenderer needCompileCache() {
        cancelCompile();
        cacheState.set(CacheState.NEED);
        return this;
    }

    /**
     * Cancels any in-flight compile, whether async (background thread) or sync (cursor on main thread).
     * For async, interrupts and joins the thread briefly so subsequent map mutations cannot race
     * with an in-flight iteration of {@link #renderedBlocksMap}.
     */
    private void cancelCompile() {
        var t = thread;
        if (t != null) {
            thread = null;
            t.interrupt();
            try {
                t.join(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (syncCompileState != null) {
            syncCompileState.discard();
            syncCompileState = null;
        }
    }

    public WorldSceneRenderer addRenderedBlocks(Collection<BlockPos> blocks, @Nullable ISceneBlockRenderHook renderHook) {
        if (blocks != null) {
            cancelCompile();
            // Snapshot so later mutations to the caller's collection don't race with the compile thread.
            this.renderedBlocksMap.put(blocks, new RenderedBlocksEntry(Set.copyOf(blocks), renderHook));
        }
        return this;
    }

    public WorldSceneRenderer removeRenderedBlocks(Collection<BlockPos> blocks) {
        if (blocks != null) {
            cancelCompile();
            this.renderedBlocksMap.remove(blocks);
        }
        return this;
    }

    public WorldSceneRenderer removeAllRenderedBlocks() {
        cancelCompile();
        this.renderedBlocksMap.clear();
        return this;
    }

    /**
     * Render the scene directly at the given pixel viewport, bypassing GUI coordinate conversion.
     * Used by the PIP renderer when rendering into a texture.
     */
    public void renderDirect(int viewportWidth, int viewportHeight, int mouseX, int mouseY) {
        renderDirect(viewportWidth, viewportHeight, mouseX, mouseY, Minecraft.getInstance().gameRenderer.renderBuffers());
    }

    public void renderDirect(int viewportWidth, int viewportHeight, int mouseX, int mouseY,
                             RenderBuffers buffers) {
        if (Minecraft.getInstance().gui.overlay() instanceof LoadingOverlay) {
            return;
        }
        PositionedRect viewport = PositionedRect.of(Position.of(0, 0), Size.of(viewportWidth, viewportHeight));
        setupCamera(viewport);
        drawWorld(buffers);
        this.lastTraceResult = null;
        this.lastHit = unProject(mouseX, mouseY);
        if (onLookingAt != null && mouseX > 0 && mouseX < viewportWidth
                && mouseY > 0 && mouseY < viewportHeight) {
            BlockHitResult result = rayTrace(lastHit);
            if (result != null) {
                this.lastTraceResult = result;
                onLookingAt.accept(result);
            }
        }
        resetCamera();
    }

    public void render(@Nonnull PoseStack poseStack, float x, float y, float width, float height, int mouseX, int mouseY) {
        render(poseStack, x, y, width, height, mouseX, mouseY, Minecraft.getInstance().gameRenderer.renderBuffers());
    }

    public void render(@Nonnull PoseStack poseStack, float x, float y, float width, float height, int mouseX, int mouseY,
                       RenderBuffers buffers) {
        // do not render if the minecraft is reloading
        if (Minecraft.getInstance().gui.overlay() instanceof LoadingOverlay) {
            return;
        }
        // setupCamera
        var pose = poseStack.last().pose();
        Vector4f pos = new Vector4f(x, y, 0, 1.0F);
        pos = pose.transform(pos);
        Vector4f size = new Vector4f(x + width, y + height, 0, 1.0F);
        size = pose.transform(size);
        x = pos.x();
        y = pos.y();
        width = size.x() - x;
        height = size.y() - y;
        PositionedRect viewport = getPositionedRect((int) x, (int) y, (int) width, (int) height);
        var topLeft = poseStack.last().pose().transformPosition(new Vector3f(0.0f, 0.0f, 0.0f));
        PositionedRect mouse = getPositionedRect((int) (mouseX + topLeft.x), (int) (mouseY + topLeft.y), 0, 0);
        mouseX = mouse.position.x;
        mouseY = mouse.position.y;
        setupCamera(viewport);
        // render TrackedDummyWorld
        drawWorld(buffers);
        // check lookingAt
        this.lastTraceResult = null;
        this.lastHit = unProject(mouseX, mouseY);
        if (onLookingAt != null && mouseX > viewport.position.x && mouseX < viewport.position.x + viewport.size.width
                && mouseY > viewport.position.y && mouseY < viewport.position.y + viewport.size.height) {
            BlockHitResult result = rayTrace(lastHit);
            if (result != null) {
                this.lastTraceResult = result;
                onLookingAt.accept(result);
            }
        }
        // resetCamera
        resetCamera();
    }

    public void setCameraLookAt(Vector3f eyePos, Vector3f lookAt, Vector3f worldUp) {
        this.eyePos = eyePos;
        this.lookAt = lookAt;
        this.worldUp = worldUp;
        Vector3f xzProduct = new Vector3f(lookAt.x() - eyePos.x(), 0, lookAt.z() - eyePos.z());
        double angleYaw = Math.toDegrees(xzProduct.angle(new Vector3f(0, 0, 1)));
        if (xzProduct.angle(new Vector3f(1, 0, 0)) < Math.PI / 2) {
            angleYaw = -angleYaw;
        }
        double anglePitch = Math.toDegrees(new Vector3f(lookAt).sub(new Vector3f(eyePos)).angle(new Vector3f(0, 1, 0))) - 90;
        cameraEntity.setPos(eyePos.x(), eyePos.y(), eyePos.z());
        cameraEntity.xo = cameraEntity.getX();
        cameraEntity.yo = cameraEntity.getY();
        cameraEntity.zo = cameraEntity.getZ();
        cameraEntity.setYRot((float) angleYaw);
        cameraEntity.setXRot((float) anglePitch);
        cameraEntity.yRotO = cameraEntity.getYRot();
        cameraEntity.xRotO = cameraEntity.getXRot();
    }

    public void setCameraLookAt(Vector3f lookAt, double radius, double yaw, double pitch) {
        Vector3f vecX = new Vector3f((float) Math.cos(yaw), (float) 0, (float) Math.sin(yaw));
        Vector3f vecY = new Vector3f(0, (float) (Math.tan(pitch) * vecX.length()), 0);
        Vector3f pos = new Vector3f(vecX).add(vecY).normalize().mul((float) radius);
        setCameraLookAt(pos.add(lookAt.x(), lookAt.y(), lookAt.z()), lookAt, worldUp);
    }

    public void setCameraOrtho(float x, float y, float z) {
        this.minX = -x;
        this.maxX = x;
        this.minY = -y;
        this.maxY = y;
        this.minZ = -z;
        this.maxZ = z;
    }

    public void setCameraOrtho(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    /** Whether the scene is drawn through an orthographic projection rather than a perspective one. */
    public boolean isOrtho() {
        return ortho;
    }

    /**
     * Half the world-space height the viewport spans {@code distance} in front of the eye — the length
     * that fills half the view vertically, and so the unit for anything that wants to keep a constant
     * size on screen.
     *
     * <p>⚠️ The distance is <b>ignored</b> under an orthographic camera, where it genuinely changes
     * nothing: that projection has no foreshortening, so the answer is the ortho box's own height however
     * far away the thing being measured is. Working out {@code distance * tan(fov / 2)} at the call site
     * instead is right in perspective and badly wrong here — {@link #getEyePos()} in ortho is usually
     * parked a fraction of a block from what it looks at, because nothing about the picture depends on
     * where along the view direction it sits, and a caller scaling by that gets something invisible.
     */
    public float getViewHalfHeight(float distance) {
        if (ortho) {
            // matching setupCamera, which divides the vertical ortho bounds by the aspect ratio
            return (maxY - minY) * 0.5f / lastAspectRatio;
        }
        return distance * (float) Math.tan(fov * 0.5f * Math.PI / 180);
    }

    public PositionedRect getPositionedRect(int x, int y, int width, int height) {
        return PositionedRect.of(Position.of(x, y), Size.of(width, height));
    }

    public PositionedRect getPositionRectRevert(int windowX, int windowY, int windowWidth, int windowHeight) {
        return PositionedRect.of(Position.of(windowX, windowY), Size.of(windowWidth, windowHeight));
    }

    private ProjectionMatrixBuffer getOrCreateProjectionMatrixBuffer() {
        if (projectionMatrixBuffer == null) {
            projectionMatrixBuffer = new ProjectionMatrixBuffer("scene_renderer");
        }
        return projectionMatrixBuffer;
    }

    protected void setupCamera(PositionedRect viewport) {
        int x = viewport.getPosition().x;
        int y = viewport.getPosition().y;
        int width = viewport.getSize().width;
        int height = viewport.getSize().height;

        // Record viewport for {@link #project} / {@link #unProject}. We no longer touch
        // GL_VIEWPORT directly: every RenderPass created by {@code GpuDevice.createCommandEncoder()}
        // sets its own viewport to the bound color texture size automatically.
        this.viewportX = x;
        this.viewportY = y;
        this.viewportWidth = width;
        this.viewportHeight = height;

        // Depth test / blend / cull / depth mask are all pipeline-owned in 26.1
        // (see RenderPipeline.{depthTestFunction, blendFunction, cullMode, ...}); legacy
        // GlStateManager toggles are dead weight in modern render-pass dispatch. Same for
        // raw glClear -- PIP / FBO callers already clear their target via
        // GpuDevice.clearColorAndDepthTextures before invoking us.

        //setup projection matrix
        RenderSystem.backupProjectionMatrix();

        float aspectRatio = width / (height * 1.0f);
        if (Float.isFinite(aspectRatio) && aspectRatio > 0) {
            this.lastAspectRatio = aspectRatio;
        }
        // 26.2 uses reversed-Z: the world depth buffer is cleared to 0.0 and all pipelines test with
        // GREATER_THAN_OR_EQUAL (near plane -> depth 1, far plane -> depth 0). Vanilla's Projection
        // builds this by swapping near/far into setPerspective/setOrtho and passing the device's
        // zZeroToOne. We must match it or every depth test is inverted (everything renders "through").
        boolean zZeroToOne = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
        if (ortho) {
            projectionMatrix.setOrtho(minX, maxX, minY / aspectRatio, maxY / aspectRatio, maxZ, minZ, zZeroToOne);
        } else {
            float near = 0.1f, far = 10000.0f;
            projectionMatrix.setPerspective(fov * 0.01745329238474369F, aspectRatio, far, near, zZeroToOne);
        }
        RenderSystem.setProjectionMatrix(
                getOrCreateProjectionMatrixBuffer().getBuffer(projectionMatrix),
                ortho ? ProjectionType.ORTHOGRAPHIC : ProjectionType.PERSPECTIVE
        );

        //setup model view matrix
        Matrix4fStack posesStack = RenderSystem.getModelViewStack();
        posesStack.pushMatrix();
        posesStack.identity();
        posesStack.lookAt(eyePos.x(), eyePos.y(), eyePos.z(), lookAt.x(), lookAt.y(), lookAt.z(), worldUp.x(), worldUp.y(), worldUp.z());
        // expose the true eye for view-dependent extraction math (position() stays ZERO by design)
        camera.setSceneEye(new net.minecraft.world.phys.Vec3(eyePos.x(), eyePos.y(), eyePos.z()));
    }

    protected void resetCamera() {
        //reset projection matrix
        RenderSystem.restoreProjectionMatrix();

        //reset modelview matrix
        Matrix4fStack posesStack = RenderSystem.getModelViewStack();
        posesStack.popMatrix();
    }

    /**
     * Vanilla-aligned scene render: a terrain mesh pass, one submit phase into {@link SubmitNodeStorage},
     * then the dispatch phase. The numbered comments in the body are the description.
     *
     * @param buffers RenderBuffers the dispatcher is built from. Identity-cached: passing the same instance
     *                across frames keeps the dispatcher warm. The mesh path does not use it - it takes the
     *                game's own fixed buffer pack.
     */
    protected void drawWorld(RenderBuffers buffers) {
        if (beforeWorldRender != null) {
            beforeWorldRender.accept(this);
        }

        var mc = Minecraft.getInstance();
        var dispatcher = ensureFeatureRenderDispatcher(buffers);
        var storage = submitNodeStorage;
        var cameraRenderState = buildCameraRenderState();
        var poseStack = new PoseStack();
        var partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        camera.setSceneRotation(cameraEntity.getYRot(), cameraEntity.getXRot());

        var ctx = new SceneRenderContext(this, poseStack, storage, cameraRenderState, partialTicks);

        // (1) Terrain mesh pass — section-local meshes drawn via the vanilla core/terrain pipeline
        //     with a per-section ChunkSection offset UBO (mirrors LevelRenderer.prepareChunkRenders +
        //     ChunkSectionsToRender.renderGroup). Honors RenderSystem.outputColor/DepthTextureOverride
        //     so it lands in the scene FBO.
        if (useCache) {
            renderCacheBuffer(mc);
        } else {
            renderUncachedWorld();
        }

        // Publish this scene's camera (rotation from the SceneCamera set above, projection from our own matrix)
        // so custom-uniform consumers see the scene camera, not the game's. Must span the WHOLE submit+dispatch
        // cycle and outlive afterRender(): submitters build their per-view uniforms during the submit phase, and
        // renderers that defer their draw to afterRender() (Photon's particle pipeline does) would otherwise
        // fall back to the main world camera and reconstruct position from depth wrongly.
        SceneCameraContext.set(camera.getViewRotationMatrix(new Matrix4f()), projectionMatrix);
        try {
            if (beforeAllSubmit != null) beforeAllSubmit.apply(ctx);

            // (2) Submit phase -- builtin scene geometry into the single SubmitNodeStorage.
            submitBlockEntities(poseStack, storage, cameraRenderState, partialTicks);
            submitEntities(poseStack, storage, cameraRenderState, partialTicks);
            submitParticles(storage, cameraRenderState, partialTicks);

            if (afterBuiltinSubmit != null) afterBuiltinSubmit.apply(ctx);

            // (3) Dispatch phase -- 26.2 replaces the per-phase BufferSource.endBatch() flushes with
            //     FeatureRenderDispatcher.prepareFrame(storage) (which uploads the shared staged vertex
            //     buffer) followed by the four execute* phases; frame.close() clears the submit nodes.
            try (var frame = dispatcher.prepareFrame(storage)) {
                frame.executeSolid();
                frame.executeTranslucent();

                if (afterTranslucentDispatch != null) afterTranslucentDispatch.apply(ctx);

                frame.executeTranslucentAfterTerrain();
                frame.executeAlwaysOnTop();

                if (afterAllDispatch != null) afterAllDispatch.apply(ctx);
            }
        } finally {
            // try-with-resources has already closed the frame, so this still runs after the dispatcher
            // drains and still with the camera context live.
            if (particleManager != null) particleManager.afterRender();
            SceneCameraContext.clear();
        }
    }

    /**
     * Uncached mesh pass: per-frame compile each {@code renderedBlocks} group into per-layer
     * meshes and draw each via the corresponding {@link RenderType}. BESRs are <em>not</em>
     * drawn here — they're submitted to {@link SubmitNodeStorage} in
     * {@link #submitBlockEntities} and drained by the dispatch phase.
     */
    private void renderUncachedWorld() {
        var mc = Minecraft.getInstance();
        var fixedPack = mc.gameRenderer.renderBuffers().fixedBufferPack();
        var device = RenderSystem.getDevice();
        EnumMap<ChunkSectionLayer, List<SectionGpuMesh>> gpu = new EnumMap<>(ChunkSectionLayer.class);
        renderedBlocksMap.forEach((key, entry) -> {
            var region = world instanceof BlockAndTintGetter g ? g : new WrappedBlockAndTintGetter(world);
            var results = renderBlocks(region, entry.snapshot(), entry.hook(), fixedPack);
            try {
                results.renderedLayers.forEach((layer, list) -> {
                    var dst = gpu.computeIfAbsent(layer, l -> new ArrayList<SectionGpuMesh>());
                    for (var cm : list) {
                        var gm = uploadSectionMesh(device, cm);
                        if (gm != null) dst.add(gm);
                    }
                });
            } finally {
                // CPU meshes already copied into GpuBuffers; release the (transient) pack-backed data.
                results.release();
            }
        });
        try {
            renderTerrain(gpu);
        } finally {
            gpu.values().forEach(list -> list.forEach(SectionGpuMesh::close));
        }
    }

    private BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> startedLayers, SectionBufferBuilderPack buffers, ChunkSectionLayer layer) {
        var builder = startedLayers.get(layer);
        if (builder == null) {
            var buffer = buffers.buffer(layer);
            builder = new BufferBuilder(buffer, PrimitiveTopology.QUADS, layer.vertexFormat());
            startedLayers.put(layer, builder);
        }
        return builder;
    }

    /** Upload one section-local {@link SectionCpuMesh} to {@link GpuBuffer}s. Render-thread only. */
    @Nullable
    private static SectionGpuMesh uploadSectionMesh(GpuDevice device, SectionCpuMesh cm) {
        var mesh = cm.mesh();
        ByteBuffer vb = mesh.vertexBuffer();
        if (vb == null) return null;
        var drawState = mesh.drawState();
        GpuBuffer vbo = device.createBuffer(() -> "scene section vbo", GpuBuffer.USAGE_VERTEX, vb);
        GpuBuffer ibo = null;
        IndexType indexType = null;
        ByteBuffer ib = mesh.indexBuffer();
        if (ib != null) {
            // Sorted translucent carries its own index buffer; solid/cutout fall back to the shared
            // sequential quad index supplied as the default in renderTerrain.
            ibo = device.createBuffer(() -> "scene section ibo", GpuBuffer.USAGE_INDEX, ib);
            indexType = drawState.indexType();
        }
        return new SectionGpuMesh(vbo, ibo, indexType, drawState.indexCount(),
                cm.originX(), cm.originY(), cm.originZ());
    }

    private static Map<ChunkSectionLayer, List<SectionGpuMesh>> uploadSectionMeshes(
            Map<ChunkSectionLayer, List<SectionCpuMesh>> cpu) {
        var device = RenderSystem.getDevice();
        EnumMap<ChunkSectionLayer, List<SectionGpuMesh>> out = new EnumMap<>(ChunkSectionLayer.class);
        for (var e : cpu.entrySet()) {
            var list = new ArrayList<SectionGpuMesh>();
            for (var cm : e.getValue()) {
                var gm = uploadSectionMesh(device, cm);
                if (gm != null) list.add(gm);
            }
            if (!list.isEmpty()) out.put(e.getKey(), list);
        }
        return out;
    }

    /**
     * Draw section-local meshes through the vanilla core/terrain pipeline, mirroring
     * {@code ChunkSectionsToRender.renderGroup} + {@code LevelRenderer.prepareChunkRenders}:
     * each section contributes one {@link DynamicUniforms.ChunkSectionInfo} (its world origin +
     * the scene model-view), drawn via {@code drawMultipleIndexed} with the "ChunkSection" UBO.
     * Color/depth targets honor {@code RenderSystem.outputColor/DepthTextureOverride} so the scene
     * lands in the FBO (the PreparedRenderType pattern).
     */
    private void renderTerrain(Map<ChunkSectionLayer, List<SectionGpuMesh>> meshesByLayer) {
        if (meshesByLayer.isEmpty()) return;
        var mc = Minecraft.getInstance();
        var device = RenderSystem.getDevice();
        var blockAtlas = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        int atlasW = blockAtlas.getWidth(0);
        int atlasH = blockAtlas.getHeight(0);
        // The core/terrain shader computes pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset,
        // then ProjMat * ModelViewMat * pos. CameraBlockPos/CameraOffset come from the Globals UBO (set to
        // eyePos below) so the camera translation is handled there; ModelViewMat must therefore be the
        // view ROTATION only. We take the scene model-view (lookAt = rot * translate(-eye)) and zero its
        // translation column to recover the pure rotation — this makes terrain align exactly with the
        // entities/BESRs (which use the full lookAt via DynamicTransforms).
        var viewRotation = new Matrix4f(RenderSystem.getModelViewMatrixCopy());
        viewRotation.m30(0f).m31(0f).m32(0f);

        List<DynamicUniforms.ChunkSectionInfo> infos = new ArrayList<>();
        var originToUbo = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
        originToUbo.defaultReturnValue(-1);
        EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> drawsByLayer =
                new EnumMap<>(ChunkSectionLayer.class);
        int largestIndexCount = 0;

        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            var meshes = meshesByLayer.get(layer);
            if (meshes == null || meshes.isEmpty()) continue;
            var draws = new ArrayList<RenderPass.Draw<GpuBufferSlice[]>>(meshes.size());
            for (var m : meshes) {
                long key = packSectionOrigin(m.originX(), m.originY(), m.originZ());
                int uboIndex = originToUbo.get(key);
                if (uboIndex == -1) {
                    uboIndex = infos.size();
                    infos.add(new DynamicUniforms.ChunkSectionInfo(new Matrix4f(viewRotation),
                            m.originX(), m.originY(), m.originZ(), 1.0f, atlasW, atlasH));
                    originToUbo.put(key, uboIndex);
                }
                final int fUbo = uboIndex;
                if (m.indexBuffer() == null) {
                    largestIndexCount = Math.max(largestIndexCount, m.indexCount());
                }
                draws.add(new RenderPass.Draw<>(0, m.vertexBuffer(), m.indexBuffer(), m.indexType(),
                        0, m.indexCount(), 0,
                        (ubos, up) -> up.upload("ChunkSection", ubos[fUbo])));
            }
            drawsByLayer.put(layer, draws);
        }
        if (infos.isEmpty()) return;

        GpuBufferSlice[] infoSlices = RenderSystem.getDynamicUniforms()
                .writeChunkSections(infos.toArray(new DynamicUniforms.ChunkSectionInfo[0]));
        var seq = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer defaultIndexBuffer = largestIndexCount == 0 ? null : seq.getBuffer(largestIndexCount);
        IndexType defaultIndexType = largestIndexCount == 0 ? null : seq.type();

        GpuSampler sampler0 = RenderSystem.getSamplerCache().getSampler(
                AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, true);
        GpuSampler sampler2 = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        var lightmap = mc.gameRenderer.lightmap();

        var mainTarget = mc.gameRenderer.mainRenderTarget();
        GpuTextureView color = RenderSystem.outputColorTextureOverride != null
                ? RenderSystem.outputColorTextureOverride : mainTarget.getColorTextureView();
        GpuTextureView depth = RenderSystem.outputDepthTextureOverride != null
                ? RenderSystem.outputDepthTextureOverride
                : (mainTarget.useDepth ? mainTarget.getDepthTextureView() : null);

        // Point the Globals UBO at the scene camera (CameraBlockPos/CameraOffset = eyePos) for the
        // duration of the terrain pass, then restore the game's. bindDefaultUniforms binds whatever
        // RenderSystem.getGlobalSettingsUniform() currently points at; swapping the buffer reference
        // (not its contents) means later passes in this frame keep the game's Globals.
        var savedGlobals = RenderSystem.getGlobalSettingsUniform();
        updateSceneGlobals(mc);
        try (RenderPass pass = device.createCommandEncoder().createRenderPass(
                () -> "scene terrain", color, java.util.Optional.empty(), depth, java.util.OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.bindTexture("Sampler0", blockAtlas, sampler0);
            pass.bindTexture("Sampler2", lightmap, sampler2);
            for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                var draws = drawsByLayer.get(layer);
                if (draws == null || draws.isEmpty()) continue;
                if (layer == ChunkSectionLayer.TRANSLUCENT) {
                    draws = draws.reversed();
                }
                pass.setPipeline(layer.pipeline());
                pass.drawMultipleIndexed(draws, defaultIndexBuffer, defaultIndexType,
                        List.of("ChunkSection"), infoSlices);
            }
        } finally {
            if (savedGlobals != null) RenderSystem.setGlobalSettingsUniform(savedGlobals);
        }
    }

    /** Write {@link #eyePos} into a scene-private Globals UBO and make it the active one. */
    private void updateSceneGlobals(Minecraft mc) {
        if (sceneGlobals == null) {
            sceneGlobals = new GlobalSettingsUniform();
        }
        long gameTime = world != null ? world.getGameTime() : 0L;
        sceneGlobals.update(viewportWidth, viewportHeight, 1.0,
                gameTime, mc.getDeltaTracker(), 0,
                new Vec3(eyePos.x(), eyePos.y(), eyePos.z()), false);
    }

    public boolean isCompiling() {
        return cacheState.get() == CacheState.COMPILING;
    }

    public double getCompileProgress() {
        if (maxProgress > 1000) {
            return progress * 1. / maxProgress;
        }
        return 0;
    }

    /**
     * Cached path. On first call (or after invalidation) spawns a background thread that compiles
     * every {@code renderedBlocks} group into per-section {@link SectionCpuMesh}es per touched layer,
     * storing them into {@link #cachedMeshes}. Once COMPILED, the CPU meshes are uploaded once into
     * {@link #cachedGpuMeshes} (render thread) and drawn each frame via {@link #renderTerrain}.
     * <p>
     * The compile thread reuses a long-lived {@link SectionBufferBuilderPack} ({@link #cacheBuilders});
     * {@link MeshData} instances reference into its underlying {@link ByteBufferBuilder}s, so the
     * pack must outlive the CPU meshes (released right after the GPU upload).
     */
    private void renderCacheBuffer(Minecraft mc) {
        if (cacheState.get() == CacheState.NEED || cacheState.get() == CacheState.UNCREATED) {
            makeSureCacheBufferCreated();
            progress = 0;
            int totalBlocks = renderedBlocksMap.values().stream().map(e -> e.snapshot().size()).reduce(0, Integer::sum);
            maxProgress = totalBlocks * (syncCompile ? 1 : 2);
            // Snapshot the pack we'll write into; deleteCacheBuffer() may swap cacheBuilders concurrently.
            final SectionBufferBuilderPack compileBuilders = cacheBuilders;
            if (compileBuilders == null) {
                return;
            }
            if (syncCompile) {
                startSyncCompile(compileBuilders);
            } else {
                startAsyncCompile(mc, compileBuilders);
                return;
            }
        }
        if (syncCompile && cacheState.get() == CacheState.COMPILING && syncCompileState != null) {
            tickSyncCompile();
        }
        // Once compiled, upload the CPU section meshes to GPU buffers once (render thread only),
        // then release the CPU data and the backing pack. Subsequent frames draw straight from GPU.
        if (cacheState.get() == CacheState.COMPILED && cachedMeshes != null) {
            cachedGpuMeshes = uploadSectionMeshes(cachedMeshes);
            closeCpuMeshes(cachedMeshes);
            cachedMeshes = null;
            if (cacheBuilders != null) {
                cacheBuilders.close();
                cacheBuilders = null;
            }
        }
        if (cachedGpuMeshes != null) {
            renderTerrain(cachedGpuMeshes);
            // BESRs are submitted by submitBlockEntities() during drawWorld's submit phase
            // (using {@link #blockEntities} as the seed set); no inline submit/dispatch here.
        }
    }

    private void startAsyncCompile(Minecraft mc, SectionBufferBuilderPack compileBuilders) {
        var entriesSnapshot = List.copyOf(renderedBlocksMap.values());
        thread = new Thread(() -> {
            cacheState.set(CacheState.COMPILING);
            EnumMap<ChunkSectionLayer, List<SectionCpuMesh>> compiled = new EnumMap<>(ChunkSectionLayer.class);
            try {
                var region = world instanceof BlockAndTintGetter g ? g : world instanceof DummyWorld dummyWorld ? DummyWorld.ClientSupport.asClientWorld(dummyWorld) : new WrappedBlockAndTintGetter(world);
                for (var entry : entriesSnapshot) {
                    if (Thread.interrupted()) return;
                    Results r = renderBlocks(region, entry.snapshot(), entry.hook(), compileBuilders);
                    r.renderedLayers.forEach((layer, list) ->
                            compiled.computeIfAbsent(layer, k -> new ArrayList<>()).addAll(list));
                    // blockEntities collected separately below to keep compile-thread minimal.
                }
            } catch (Exception e) {
                closeCpuMeshes(compiled);
                return;
            }
            Set<BlockPos> poses = new HashSet<>();
            for (var entry : entriesSnapshot) {
                for (BlockPos pos : entry.snapshot()) {
                    progress++;
                    if (Thread.interrupted()) return;
                    BlockEntity tile = world.getBlockEntity(pos);
                    if (tile != null && mc.getBlockEntityRenderDispatcher().getRenderer(tile) != null) {
                        poses.add(pos);
                    }
                }
            }
            if (Thread.interrupted()) {
                closeCpuMeshes(compiled);
                return;
            }
            if (thread != null) {
                Map<ChunkSectionLayer, List<SectionCpuMesh>> oldMeshes = cachedMeshes;
                cachedMeshes = compiled;
                blockEntities = poses;
                cacheState.set(CacheState.COMPILED);
                thread = null;
                closeCpuMeshes(oldMeshes);
            }
            maxProgress = -1;
        });
        thread.start();
    }

    private void startSyncCompile(SectionBufferBuilderPack compileBuilders) {
        var entriesSnapshot = List.copyOf(renderedBlocksMap.values());
        var region = world instanceof BlockAndTintGetter g ? g : world instanceof DummyWorld dummyWorld ? DummyWorld.ClientSupport.asClientWorld(dummyWorld) : new WrappedBlockAndTintGetter(world);
        syncCompileState = new SyncCompileState(this, region, compileBuilders, entriesSnapshot);
        cacheState.set(CacheState.COMPILING);
    }

    private void tickSyncCompile() {
        var state = syncCompileState;
        if (state == null) return;
        int maxBlocks = Math.max(1, syncCompileMaxBlocksPerFrame);
        long deadline = System.nanoTime() + Math.max(0L, syncCompileTimeBudgetNanos);
        int processed = 0;
        BlockModelLighter.enableCaching();
        try {
            while (processed < maxBlocks) {
                if (!state.step()) {
                    state.finish();
                    publishSyncCompile(state);
                    return;
                }
                if (maxProgress > 0) {
                    progress++;
                }
                processed++;
                if (processed > 0 && System.nanoTime() >= deadline) {
                    break;
                }
            }
        } finally {
            BlockModelLighter.clearCache();
        }
    }

    private void publishSyncCompile(SyncCompileState state) {
        EnumMap<ChunkSectionLayer, List<SectionCpuMesh>> compiled = new EnumMap<>(ChunkSectionLayer.class);
        state.results.renderedLayers.forEach((layer, list) ->
                compiled.computeIfAbsent(layer, k -> new ArrayList<>()).addAll(list));
        state.results.renderedLayers.clear();
        Map<ChunkSectionLayer, List<SectionCpuMesh>> oldMeshes = cachedMeshes;
        cachedMeshes = compiled;
        blockEntities = state.results.blockEntities.stream()
                .map(BlockEntity::getBlockPos)
                .collect(java.util.stream.Collectors.toSet());
        syncCompileState = null;
        cacheState.set(CacheState.COMPILED);
        closeCpuMeshes(oldMeshes);
        maxProgress = -1;
    }

    private static void closeCpuMeshes(@Nullable Map<ChunkSectionLayer, List<SectionCpuMesh>> meshes) {
        if (meshes != null) {
            meshes.values().forEach(list -> list.forEach(m -> m.mesh().close()));
        }
    }

    public static final class Results {
        public final List<BlockEntity> blockEntities = new ArrayList<>();
        /** Per-layer section-local meshes (one entry per touched 16³ section). */
        public final Map<ChunkSectionLayer, List<SectionCpuMesh>> renderedLayers = new EnumMap<>(ChunkSectionLayer.class);

        public void release() {
            this.renderedLayers.values().forEach(list -> list.forEach(m -> m.mesh().close()));
        }
    }

    private static final class BlockCompileContext {
        private final WorldSceneRenderer renderer;
        private final BlockAndTintGetter region;
        private final SectionBufferBuilderPack builders;
        private final ModelBlockRenderer blockRenderer;
        private final FluidRenderer fluidRenderer;
        private final BlockStateModelSet blockStateModelSet;
        private final FluidStateModelSet fluidModelSet;
        private final BlockEntityRenderDispatcher blockEntityRenderer;
        private final boolean cutoutLeaves;
        private final Map<ChunkSectionLayer, BufferBuilder> startedLayers = new EnumMap<>(ChunkSectionLayer.class);
        private final float[] curOffset = new float[3];
        private final int[] curColor = {-1};
        private final boolean[] curHasTransform = {false};
        private final float[] fluidOffset = new float[3];
        /** World origin of the section currently being compiled (block coords, multiples of 16). */
        private int curOriginX, curOriginY, curOriginZ;
        private final EnumMap<ChunkSectionLayer, VertexConsumerWrapper> layerWrappers =
                new EnumMap<>(ChunkSectionLayer.class);
        private final BlockQuadOutput quadOutput;
        private final BlockQuadOutput opaqueQuadOutput;
        private final FluidRenderer.Output fluidOutput;

        private BlockCompileContext(WorldSceneRenderer renderer, BlockAndTintGetter region,
                                    SectionBufferBuilderPack builders) {
            this.renderer = renderer;
            this.region = region;
            this.builders = builders;
            var mc = Minecraft.getInstance();
            var modelManager = mc.getModelManager();
            this.blockStateModelSet = modelManager.getBlockStateModelSet();
            this.fluidModelSet = modelManager.getFluidStateModelSet();
            this.blockRenderer = new ModelBlockRenderer(mc.options.ambientOcclusion().get(), true, mc.getBlockColors());
            this.fluidRenderer = new FluidRenderer(fluidModelSet);
            this.blockEntityRenderer = mc.getBlockEntityRenderDispatcher();
            this.cutoutLeaves = mc.options.cutoutLeaves().get();
            this.quadOutput = (x, y, z, quad, instance) -> {
                var layer = quad.materialInfo().layer();
                var builder = renderer.getOrBeginLayer(startedLayers, builders, layer);
                if (!curHasTransform[0]) {
                    builder.putBlockBakedQuad(x, y, z, quad, instance);
                    return;
                }
                var wrapper = layerWrappers.computeIfAbsent(layer, l -> new VertexConsumerWrapper(builder));
                wrapper.clearOffset();
                wrapper.addOffset(curOffset[0], curOffset[1], curOffset[2]);
                wrapper.clearColor();
                if (curColor[0] != -1) applyArgbColorMultiplier(wrapper, curColor[0]);
                wrapper.putBlockBakedQuad(x, y, z, quad, instance);
            };
            this.opaqueQuadOutput = (x, y, z, quad, instance) -> {
                var builder = renderer.getOrBeginLayer(startedLayers, builders, ChunkSectionLayer.SOLID);
                if (!curHasTransform[0]) {
                    builder.putBlockBakedQuad(x, y, z, quad, instance);
                    return;
                }
                var wrapper = layerWrappers.computeIfAbsent(ChunkSectionLayer.SOLID, l -> new VertexConsumerWrapper(builder));
                wrapper.clearOffset();
                wrapper.addOffset(curOffset[0], curOffset[1], curOffset[2]);
                wrapper.clearColor();
                if (curColor[0] != -1) applyArgbColorMultiplier(wrapper, curColor[0]);
                wrapper.putBlockBakedQuad(x, y, z, quad, instance);
            };
            this.fluidOutput = layer -> {
                BufferBuilder builder = renderer.getOrBeginLayer(startedLayers, builders, layer);
                VertexConsumerWrapper wrapper = new VertexConsumerWrapper(builder);
                wrapper.addOffset(fluidOffset[0] + curOffset[0],
                                  fluidOffset[1] + curOffset[1],
                                  fluidOffset[2] + curOffset[2]);
                if (curColor[0] != -1) applyArgbColorMultiplier(wrapper, curColor[0]);
                return wrapper;
            };
        }

        private void renderBlock(BlockPos pos, @Nullable ISceneBlockRenderHook hook, Results results) {
            if (renderer.blocked != null && renderer.blocked.contains(pos)) {
                return;
            }
            var blockState = region.getBlockState(pos);
            refreshHookState(pos, hook, blockState);

            if (!blockState.isAir()) {
                if (blockState.hasBlockEntity()) {
                    BlockEntity blockEntity = region.getBlockEntity(pos);
                    if (blockEntity != null) {
                        var blockEntityRenderer = this.blockEntityRenderer.getRenderer(blockEntity);
                        if (blockEntityRenderer != null && !blockEntityRenderer.shouldRenderOffScreen()) {
                            results.blockEntities.add(blockEntity);
                        }
                    }
                }

                var fluidState = blockState.getFluidState();
                if (!fluidState.isEmpty()) {
                    // FluidRenderer.tesselate already emits section-local positions (pos & 15), which is
                    // exactly what the core/terrain pipeline + ChunkSection UBO expect — no origin add-back.
                    fluidOffset[0] = 0f;
                    fluidOffset[1] = 0f;
                    fluidOffset[2] = 0f;
                    var customRenderer = fluidModelSet.get(fluidState).customRenderer();
                    if (customRenderer == null
                            || !customRenderer.renderFluid(fluidRenderer, fluidState, region, pos, fluidOutput, blockState)) {
                        fluidRenderer.tesselate(region, pos, fluidOutput, blockState, fluidState);
                    }
                }

                if (blockState.getRenderShape() == MODEL) {
                    var model = blockStateModelSet.get(blockState);
                    var forceOpaque = ModelBlockRenderer.forceOpaque(cutoutLeaves, blockState);
                    // Section-local position offset (pos - sectionOrigin); the ChunkSection UBO adds the origin.
                    blockRenderer.tesselateBlock(
                            forceOpaque ? opaqueQuadOutput : quadOutput,
                            pos.getX() - curOriginX, pos.getY() - curOriginY, pos.getZ() - curOriginZ,
                            region,
                            pos,
                            blockState,
                            model,
                            blockState.getSeed(pos)
                    );
                }
            }
        }

        private void refreshHookState(BlockPos pos, @Nullable ISceneBlockRenderHook hook,
                                      net.minecraft.world.level.block.state.BlockState blockState) {
            if (hook != null) {
                Vector3f off = hook.getOffset(renderer.world, pos, blockState);
                if (off != null) {
                    curOffset[0] = off.x;
                    curOffset[1] = off.y;
                    curOffset[2] = off.z;
                } else {
                    curOffset[0] = curOffset[1] = curOffset[2] = 0f;
                }
                curColor[0] = hook.getColorMultiplier(renderer.world, pos, blockState);
            } else {
                curOffset[0] = curOffset[1] = curOffset[2] = 0f;
                curColor[0] = -1;
            }
            curHasTransform[0] = curOffset[0] != 0f || curOffset[1] != 0f || curOffset[2] != 0f || curColor[0] != -1;
        }

        /** Start compiling a new 16³ section at the given world origin. */
        private void beginSection(int originX, int originY, int originZ) {
            this.curOriginX = originX;
            this.curOriginY = originY;
            this.curOriginZ = originZ;
        }

        /** Build the current section's started layers into per-layer {@link SectionCpuMesh} lists. */
        private void flushSection(Results results) {
            // Translucent sorting happens in section-local space, so the camera is offset by the origin.
            var vertexSorting = VertexSorting.byDistance(
                    renderer.eyePos.x - curOriginX, renderer.eyePos.y - curOriginY, renderer.eyePos.z - curOriginZ);
            for (Map.Entry<ChunkSectionLayer, BufferBuilder> entry : startedLayers.entrySet()) {
                ChunkSectionLayer layer = entry.getKey();
                MeshData mesh = entry.getValue().build();
                if (mesh != null) {
                    if (layer == ChunkSectionLayer.TRANSLUCENT) {
                        mesh.sortQuads(builders.buffer(layer), vertexSorting);
                    }
                    results.renderedLayers.computeIfAbsent(layer, k -> new ArrayList<>())
                            .add(new SectionCpuMesh(mesh, curOriginX, curOriginY, curOriginZ));
                }
            }
            startedLayers.clear();
            layerWrappers.clear();
        }

        private void discard() {
            for (BufferBuilder builder : startedLayers.values()) {
                MeshData mesh = builder.build();
                if (mesh != null) {
                    mesh.close();
                }
            }
            startedLayers.clear();
            layerWrappers.clear();
        }
    }

    /**
     * Tesselate a group of blocks into per-section, per-layer {@link SectionCpuMesh}, mirroring
     * vanilla {@code SectionCompiler.compile}: blocks are bucketed into 16³ sections and compiled in
     * <em>section-local</em> coordinates ({@code pos - sectionOrigin}). The section origin is applied
     * back at draw time via the {@code ChunkSection} offset UBO (see {@link #renderTerrain}). Only one
     * {@link BufferBuilder} per layer is alive at a time because the section is built (and its builders
     * reset) before the next section starts.
     * <p>
     * The returned {@link Results#renderedLayers} owns the built CPU meshes; callers must either upload
     * them (then close) or call {@link Results#release()}.
     */
    private Results renderBlocks(BlockAndTintGetter region,
                                 Collection<BlockPos> renderedBlocks,
                                 @Nullable ISceneBlockRenderHook hook,
                                 SectionBufferBuilderPack builders) {
        var results = new Results();
        var compileContext = new BlockCompileContext(this, region, builders);
        // Bucket blocks by section so each section compiles into its own section-local mesh.
        var bySection = new java.util.LinkedHashMap<Long, List<BlockPos>>();
        for (BlockPos pos : renderedBlocks) {
            bySection.computeIfAbsent(packSectionOrigin(pos.getX(), pos.getY(), pos.getZ()),
                    k -> new ArrayList<>()).add(pos);
        }
        BlockModelLighter.enableCaching();
        try {
            for (var sectionEntry : bySection.entrySet()) {
                long key = sectionEntry.getKey();
                compileContext.beginSection(SectionPos.x(key) << 4, SectionPos.y(key) << 4, SectionPos.z(key) << 4);
                for (BlockPos pos : sectionEntry.getValue()) {
                    compileContext.renderBlock(pos, hook, results);
                    // for async progress
                    if (maxProgress > 0) {
                        progress++;
                    }
                }
                compileContext.flushSection(results);
            }
        } finally {
            BlockModelLighter.clearCache();
        }
        return results;
    }

    /**
     * Submit phase for BlockEntity special renderers. Walks every {@code renderedBlocks} group,
     * locates per-pos BEs (cached set in {@link #blockEntities} for the cached path, fresh lookup
     * for the uncached path), and submits them through {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher#submit}
     * into the scene's single {@link SubmitNodeStorage}. The dispatch happens later in
     * {@link #drawWorld(RenderBuffers)}.
     */
    private void submitBlockEntities(PoseStack poseStack, SubmitNodeStorage storage,
                                     CameraRenderState cameraRenderState, float partialTicks) {
        var mc = Minecraft.getInstance();
        var beDispatcher = mc.getBlockEntityRenderDispatcher();
        beDispatcher.prepare(new Vec3(eyePos.x(), eyePos.y(), eyePos.z()));

        // Iterate per-group so per-hook BESR transforms (offsets, colors) apply to the right BEs.
        renderedBlocksMap.forEach((key, entry) -> {
            var snapshot = entry.snapshot();
            var hook = entry.hook();
            for (BlockPos pos : snapshot) {
                if (blocked != null && blocked.contains(pos)) continue;
                BlockEntity be = world.getBlockEntity(pos);
                if (be == null) continue;
                var state = beDispatcher.tryExtractRenderState(be, partialTicks, null, false);
                if (state == null) continue;

                poseStack.pushPose();
                poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
                if (hook != null) hook.applyBESR(world, pos, be, poseStack, partialTicks);
                beDispatcher.submit(state, poseStack, storage, cameraRenderState);
                poseStack.popPose();
            }
        });
    }

    /**
     * Submit phase for entities living in a {@link TrackedDummyWorld}. Coords are passed
     * straight through (not vanilla's {@code state.x - camX}) because our view matrix is
     * {@code lookAt(eyePos)} which already encodes the camera translation.
     */
    private void submitEntities(PoseStack poseStack, SubmitNodeStorage storage,
                                CameraRenderState cameraRenderState, float partialTicks) {
        if (!(world instanceof TrackedDummyWorld tw)) return;
        var entitiesIter = tw.getAllRenderedEntities();
        if (!entitiesIter.iterator().hasNext()) return;

        var entityDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        entityDispatcher.prepare(camera, cameraEntity);

        var hook = sceneEntityRenderHook;
        for (var entity : entitiesIter) {
            if (hook != null && !hook.shouldRender(world, entity)) continue;
            net.minecraft.client.renderer.entity.state.EntityRenderState state;
            try {
                state = entityDispatcher.extractEntity(entity, partialTicks);
            } catch (Throwable ignored) {
                continue; // a broken renderer for one entity shouldn't kill the frame
            }
            if (state == null) continue;

            poseStack.pushPose();
            if (hook != null) hook.applyEntity(world, entity, poseStack, partialTicks);
            entityDispatcher.submit(state, cameraRenderState, state.x, state.y, state.z, poseStack, storage);
            poseStack.popPose();
        }
    }

    /**
     * Submit phase for live particles: extract via {@link ParticleManager} and submit into
     * {@code storage}. {@link SceneCamera#position()} returns {@link Vec3#ZERO} so extracted
     * vertices stay in world space. {@code ParticleManager.afterRender()} runs from
     * {@code drawWorld}'s finally block (must run <em>after</em> the dispatcher drains).
     */
    private void submitParticles(SubmitNodeStorage storage, CameraRenderState cameraRenderState,
                                 float partialTicks) {
        if (particleManager == null) return;
        particleManager.render(storage, cameraRenderState, camera, NO_CULL_FRUSTUM, partialTicks);
    }

    /** Scene previews are tiny — skip frustum work entirely. */
    private static final net.minecraft.client.renderer.culling.Frustum NO_CULL_FRUSTUM =
            new net.minecraft.client.renderer.culling.Frustum(new Matrix4f(), new Matrix4f()) {
                @Override
                public boolean pointInFrustum(double x, double y, double z) { return true; }
            };

    public BlockHitResult rayTrace(Vector3f hitPos) {
        var startPos = new Vec3(this.eyePos.x(), this.eyePos.y(), this.eyePos.z());
        if (ortho) {
            startPos = startPos.add(new Vec3(startPos.x - lookAt.x(), startPos.y - lookAt.y(), startPos.z - lookAt.z()).multiply(500, 500, 500));
        }
        hitPos = hitPos.mul(2, new Vector3f()); // Double view range to ensure pos can be seen.
        var endPos = new Vec3((hitPos.x() - startPos.x), (hitPos.y() - startPos.y), (hitPos.z() - startPos.z));
        try {
            return this.world.clip(new ClipContext(startPos, endPos, clipBlock, clipFluid, cameraEntity));
        } catch (Exception e) {
            return null;
        }
    }

    /** Combined projection * modelView, for {@link Matrix4f#project} / {@link Matrix4f#unproject}. */
    private Matrix4f computeCombinedMatrix() {
        return new Matrix4f(projectionMatrix).mul(RenderSystem.getModelViewMatrixCopy());
    }

    private int[] currentViewport() {
        return new int[] { viewportX, viewportY, viewportWidth, viewportHeight };
    }

    /**
     * We can't use JOML's {@link Matrix4f#project}/{@link Matrix4f#unproject}: those hard-assume an
     * NDC depth range of [-1,1], but MC's GL backend sets {@code glClipControl(.., ZERO_TO_ONE)} so
     * the device reports {@code isZZeroToOne()} and the depth NDC is [0,1]. Mixing the two corrupts
     * the depth term — which barely shows in ortho (parallel rays) but makes the perspective pick ray
     * point at garbage (can't select). So we map window<->NDC depth ourselves with the device flag.
     */
    public Vector3f project(Vector3f pos) {
        boolean zZeroToOne = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
        int[] vp = currentViewport();
        Vector4f clip = new Vector4f(pos.x(), pos.y(), pos.z(), 1f);
        computeCombinedMatrix().transform(clip);
        if (clip.w() != 0f) clip.div(clip.w());
        float winX = vp[0] + (clip.x() * 0.5f + 0.5f) * vp[2];
        float winY = vp[1] + (clip.y() * 0.5f + 0.5f) * vp[3];
        float winZ = zZeroToOne ? clip.z() : (clip.z() * 0.5f + 0.5f);
        return new Vector3f(winX, winY, winZ);
    }

    public Vector3f unProject(int mouseX, int mouseY) {
        return unProject(mouseX, mouseY, false);
    }

    public Vector3f unProject(int mouseX, int mouseY, boolean checkDepth) {
        boolean zZeroToOne = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
        // Reversed-Z: the far plane is window depth 0. Without a depth sample we unproject at the far
        // plane to build the pick ray; checkDepth reads the real rendered depth for an exact point.
        float winZ = checkDepth ? readDepthPixelAsync(mouseX, mouseY) : 0f;
        int[] vp = currentViewport();
        float ndcX = (mouseX - vp[0]) / (float) vp[2] * 2f - 1f;
        float ndcY = (mouseY - vp[1]) / (float) vp[3] * 2f - 1f;
        float ndcZ = zZeroToOne ? winZ : (winZ * 2f - 1f);
        Vector4f obj = new Vector4f(ndcX, ndcY, ndcZ, 1f);
        computeCombinedMatrix().invert(new Matrix4f()).transform(obj);
        if (obj.w() != 0f) obj.div(obj.w());
        return new Vector3f(obj.x(), obj.y(), obj.z());
    }

    /**
     * Asynchronously schedule a 1-pixel depth read from the currently-bound depth texture
     * (set by the PIP framework / FBOWorldSceneRenderer via {@code outputDepthTextureOverride}).
     * Returns the most recently completed sample; first call returns the far-plane fallback.
     * <p>
     * The copy is encoded via {@link com.mojang.blaze3d.systems.CommandEncoder#copyTextureToBuffer
     * copyTextureToBuffer}, which internally registers the readback callback through
     * {@code RenderSystem.queueFencedTask}; the callback fires when the fence signals (typically
     * the next frame) without stalling the render thread. Repeated calls within a single frame
     * coalesce into a single in-flight task.
     */
    private float readDepthPixelAsync(int mouseX, int mouseY) {
        var depthView = RenderSystem.outputDepthTextureOverride;
        if (depthView == null) return lastDepthSample;
        var depthTex = depthView.texture();
        // Vanilla PictureInPictureRenderer creates its depth texture with usage flag 9
        // (USAGE_RENDER_ATTACHMENT | USAGE_COPY_DST) -- NO USAGE_COPY_SRC. copyTextureToBuffer
        // would throw IllegalArgumentException("Texture needs USAGE_COPY_SRC..."). When the
        // bound depth texture isn't readable we fall back to the last completed sample
        // (initial value: far plane). Subclasses that own their depth texture (e.g.
        // FBOWorldSceneRenderer) should create it with USAGE_COPY_SRC included to opt in.
        if ((depthTex.usage() & GpuTexture.USAGE_COPY_SRC) == 0) return lastDepthSample;
        // Caller may pass a mouse outside the scene viewport (e.g. cursor outside the
        // scene element); copyTextureToBuffer would throw "source texture not large enough"
        // for any (x,y) that falls outside the texture. Treat out-of-bounds as "no fresh
        // sample" and keep returning the last one.
        int texW = depthTex.getWidth(0);
        int texH = depthTex.getHeight(0);
        if (mouseX < 0 || mouseY < 0 || mouseX >= texW || mouseY >= texH) return lastDepthSample;

        if (!depthReadInFlight) {
            var device = RenderSystem.getDevice();
            if (depthReadbackBuffer == null) {
                depthReadbackBuffer = device.createBuffer(
                        () -> "scene depth readback",
                        GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST,
                        4L
                );
            }
            // Capture the buffer locally so the callback can detect releaseResource() racing
            // ahead of the fence signal: when the buffer instance has changed (or the
            // renderer was disposed and depthReadbackBuffer set to null), bail out instead
            // of dereferencing a closed buffer.
            final GpuBuffer issuedBuf = depthReadbackBuffer;
            depthReadInFlight = true;
            device.createCommandEncoder().copyTextureToBuffer(
                    depthTex, issuedBuf, 0L,
                    () -> {
                        try {
                            if (issuedBuf != depthReadbackBuffer) return; // released or recreated
                            // 26.2: CommandEncoder.mapBuffer is gone; map directly on the GpuBuffer.
                            try (var view = issuedBuf.map(0L, 4L, true, false)) {
                                lastDepthSample = view.data().getFloat(0);
                            }
                        } finally {
                            depthReadInFlight = false;
                        }
                    },
                    0, mouseX, mouseY, 1, 1
            );
        }
        return lastDepthSample;
    }

    /***
     * For better performance, You'd better handle the event {@link #setOnLookingAt(Consumer)} or {@link #getLastTraceResult()}
     * @param mouseX xPos in Texture
     * @param mouseY yPos in Texture
     * @return RayTraceResult Hit
     */
    protected BlockHitResult screenPos2BlockPosFace(int mouseX, int mouseY, int x, int y, int width, int height) {
        // render a frame
        setupCamera(getPositionedRect(x, y, width, height));

        drawWorld(Minecraft.getInstance().gameRenderer.renderBuffers());

        Vector3f hitPos = this.lastHit == null ? unProject(mouseX, mouseY) : this.lastHit;
        BlockHitResult result = rayTrace(hitPos);

        resetCamera();

        return result;
    }

    /***
     * For better performance, You'd better do project in {@link #setAfterAllDispatch(SceneRenderHook)}
     * @param pos BlockPos
     * @return x, y, z
     */
    protected Vector3f blockPos2ScreenPos(BlockPos pos, int x, int y, int width, int height) {
        // render a frame
        setupCamera(getPositionedRect(x, y, width, height));

        drawWorld(Minecraft.getInstance().gameRenderer.renderBuffers());
        Vector3f winPos = project(new Vector3f(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f));

        resetCamera();

        return winPos;
    }


    public static class VertexConsumerWrapper implements VertexConsumer {

        final VertexConsumer builder;
        @Setter
        float offsetX, offsetY, offsetZ;
        float r = 1, g = 1, b = 1, a = 1;

        public VertexConsumerWrapper(VertexConsumer builder) {
            this.builder = builder;
        }

        public void addOffset(float offsetX, float offsetY, float offsetZ) {
            this.offsetX += offsetX;
            this.offsetY += offsetY;
            this.offsetZ += offsetZ;
        }

        public void setColorMultiplier(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        public void clearOffset() {
            this.offsetX = 0;
            this.offsetY = 0;
            this.offsetZ = 0;
        }

        public void clearColor() {
            this.r = 1;
            this.g = 1;
            this.b = 1;
            this.a = 1;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return builder.addVertex(x + offsetX, y + offsetY, z + offsetZ);
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return builder.setColor((int) (red * r), (int) (green * g), (int) (blue * b), (int) (alpha * a));
        }

        @Override
        public VertexConsumer setColor(int color) {
            int a0 = (color >> 24) & 0xFF;
            int r0 = (color >> 16) & 0xFF;
            int g0 = (color >> 8) & 0xFF;
            int b0 = color & 0xFF;
            return builder.setColor((int) (r0 * r), (int) (g0 * g), (int) (b0 * b), (int) (a0 * a));
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return builder.setUv(u, v);
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return builder.setUv1(u, v);
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return builder.setUv2(u, v);
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return builder.setNormal(x, y, z);
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return builder.setLineWidth(width);
        }
    }
}
