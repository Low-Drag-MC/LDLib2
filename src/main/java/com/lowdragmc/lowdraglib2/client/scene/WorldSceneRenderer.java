package com.lowdragmc.lowdraglib2.client.scene;

import com.lowdragmc.lowdraglib2.core.mixins.accessor.MeshDataAccessor;
import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.PositionedRect;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.DummyWorld;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.WrappedBlockAndTintGetter;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
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
     * Map a chunk section layer to a RenderType usable outside chunk rendering.
     * The "moving block" RenderTypes wrap {@code SOLID_BLOCK / CUTOUT_BLOCK / TRANSLUCENT_BLOCK} pipelines
     * (standard MVP transforms via {@code core/block} shader, block atlas + lightmap bound).
     * They are chosen over the chunk pipelines (SOLID_TERRAIN, ...) because the latter require the
     * {@code ChunkSection} UBO that only exists inside chunk rendering. Vertex format is the same
     * (DefaultVertexFormat.BLOCK), so the meshes built via {@link ChunkSectionLayer#vertexFormat()}
     * are compatible.
     */
    private static RenderType getRenderTypeForLayer(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
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

    @Nullable
    protected Map<ChunkSectionLayer, List<MeshData>> cachedMeshes;
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
    /** Lazily-created 4-byte readback buffer for async depth pixel sampling. */
    @Nullable
    private GpuBuffer depthReadbackBuffer;
    /** True between issuing a copyTextureToBuffer and the fence callback firing. */
    private boolean depthReadInFlight;
    /** Most recent completed depth sample (NDC 0..1). Lags by 1+ frames; initial value
     *  corresponds to the far plane so first-frame unProject still yields a valid ray. */
    private float lastDepthSample = 0.999f;
    /** Scene-private vanilla submission pipeline, lazily built in
     *  {@link #ensureFeatureRenderDispatcher(RenderBuffers)} and identity-cached on the
     *  {@link RenderBuffers} passed by the caller. Closed + rebuilt only when buffers change. */
    @Nullable private SubmitNodeStorage submitNodeStorage;
    @Nullable private FeatureRenderDispatcher featureRenderDispatcher;
    @Nullable private OutlineBufferSource outlineBufferSource;
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
        outlineBufferSource = null;
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
        outlineBufferSource = new OutlineBufferSource();
        featureRenderDispatcher = new FeatureRenderDispatcher(
                submitNodeStorage,
                mc.getModelManager(),
                buffers.bufferSource(),
                mc.getAtlasManager(),
                outlineBufferSource,
                buffers.crumblingBufferSource(),
                mc.font,
                mc.gameRenderer.getGameRenderState()
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
        final SectionBufferBuilderPack builders;
        final List<RenderedBlocksEntry> entries;
        final BlockCompileContext compileContext;
        final Results results = new Results();
        int entryIndex;
        @Nullable
        Iterator<BlockPos> blockIter;

        SyncCompileState(WorldSceneRenderer renderer, BlockAndTintGetter region,
                         SectionBufferBuilderPack builders, List<RenderedBlocksEntry> entries) {
            this.builders = builders;
            this.entries = entries;
            this.compileContext = new BlockCompileContext(renderer, region, builders);
        }

        boolean step() {
            while (entryIndex < entries.size()) {
                RenderedBlocksEntry entry = entries.get(entryIndex);
                if (blockIter == null) {
                    blockIter = entry.snapshot().iterator();
                }
                if (blockIter.hasNext()) {
                    compileContext.renderBlock(blockIter.next(), entry.hook(), results);
                    return true;
                }
                blockIter = null;
                entryIndex++;
            }
            return false;
        }

        void finish() {
            compileContext.build(results);
        }

        void discard() {
            results.release();
            compileContext.discard();
        }
    }

    public WorldSceneRenderer deleteCacheBuffer() {
        cancelCompile();
        if (cachedMeshes != null) {
            for (List<MeshData> list : cachedMeshes.values()) {
                for (MeshData mesh : list) {
                    mesh.close();
                }
            }
            cachedMeshes = null;
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
        if (cachedMeshes == null) {
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
        renderDirect(viewportWidth, viewportHeight, mouseX, mouseY, Minecraft.getInstance().renderBuffers());
    }

    public void renderDirect(int viewportWidth, int viewportHeight, int mouseX, int mouseY,
                             RenderBuffers buffers) {
        if (Minecraft.getInstance().getOverlay() instanceof LoadingOverlay) {
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
        render(poseStack, x, y, width, height, mouseX, mouseY, Minecraft.getInstance().renderBuffers());
    }

    public void render(@Nonnull PoseStack poseStack, float x, float y, float width, float height, int mouseX, int mouseY,
                       RenderBuffers buffers) {
        // do not render if the minecraft is reloading
        if (Minecraft.getInstance().getOverlay() instanceof LoadingOverlay) {
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
        if (ortho) {
            projectionMatrix.setOrtho(minX, maxX, minY / aspectRatio, maxY / aspectRatio, minZ, maxZ);
        } else {
            projectionMatrix.setPerspective(fov * 0.01745329238474369F, aspectRatio, 0.1f, 10000.0f);
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
    }

    protected void resetCamera() {
        //reset projection matrix
        RenderSystem.restoreProjectionMatrix();

        //reset modelview matrix
        Matrix4fStack posesStack = RenderSystem.getModelViewStack();
        posesStack.popMatrix();
    }

    /**
     * Vanilla-aligned scene render. Mirrors {@code LevelRenderer.java:695-755}: one submit
     * phase into {@link SubmitNodeStorage}, then three dispatch passes (solid / translucent /
     * translucent particles), each followed by {@code BufferSource.endBatch()}.
     *
     * @param buffers RenderBuffers used for both the mesh path and the dispatcher's buffer source.
     *                Identity-cached: same instance across frames keeps the dispatcher warm.
     */
    protected void drawWorld(RenderBuffers buffers) {
        if (beforeWorldRender != null) {
            beforeWorldRender.accept(this);
        }

        var mc = Minecraft.getInstance();
        var bs = buffers.bufferSource();
        var crumb = buffers.crumblingBufferSource();
        var dispatcher = ensureFeatureRenderDispatcher(buffers);
        var storage = submitNodeStorage;
        var cameraRenderState = buildCameraRenderState();
        var poseStack = new PoseStack();
        var partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        camera.setSceneRotation(cameraEntity.getYRot(), cameraEntity.getXRot());

        var ctx = new SceneRenderContext(this, poseStack, storage, bs, cameraRenderState, partialTicks);

        // (1) Mesh pass — chunk-section style mesh draws bypass SubmitNodeStorage and write
        //     directly to the shared BufferSource. Their queued RenderType buckets are flushed
        //     later by the dispatch-phase endBatches.
        if (useCache) {
            renderCacheBuffer(mc);
        } else {
            renderUncachedWorld(bs, partialTicks);
        }

        if (beforeAllSubmit != null) beforeAllSubmit.apply(ctx);

        // (2) Submit phase — builtin scene geometry into the single SubmitNodeStorage.
        submitBlockEntities(poseStack, storage, cameraRenderState, partialTicks);
        submitEntities(poseStack, storage, cameraRenderState, partialTicks);
        submitParticles(storage, cameraRenderState, partialTicks);

        if (afterBuiltinSubmit != null) afterBuiltinSubmit.apply(ctx);

        // (3) Dispatch phase — three endBatches mirroring LevelRenderer 695-755.
        //     Depth copy between mainTarget/translucent/particle FBOs is skipped (PIP = single FBO).
        try {
            dispatcher.renderSolidFeatures();
            bs.endBatch();
            dispatcher.renderTranslucentFeatures();
            bs.endBatch();
            crumb.endBatch();

            if (afterTranslucentDispatch != null) afterTranslucentDispatch.apply(ctx);

            dispatcher.renderTranslucentParticles();
            bs.endBatch();

            if (afterAllDispatch != null) afterAllDispatch.apply(ctx);
        } finally {
            dispatcher.clearSubmitNodes();
            if (particleManager != null) particleManager.afterRender();
        }
    }

    /**
     * Uncached mesh pass: per-frame compile each {@code renderedBlocks} group into per-layer
     * meshes and draw each via the corresponding {@link RenderType}. BESRs are <em>not</em>
     * drawn here — they're submitted to {@link SubmitNodeStorage} in
     * {@link #submitBlockEntities} and drained by the dispatch phase. Final {@code endBatch}
     * is folded into the dispatch phase's endBatches (same shared BufferSource).
     */
    private void renderUncachedWorld(MultiBufferSource.BufferSource buffers, float particleTicks) {
        var mc = Minecraft.getInstance();
        var fixedPack = mc.renderBuffers().fixedBufferPack();
        renderedBlocksMap.forEach((key, entry) -> {
            var renderedBlocks = entry.snapshot();
            var hook = entry.hook();
            var region = world instanceof BlockAndTintGetter g ? g : new WrappedBlockAndTintGetter(world);
            var results = renderBlocks(region, renderedBlocks, hook, fixedPack);
            try {
                for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                    MeshData mesh = results.renderedLayers.remove(layer);
                    if (mesh != null) {
                        // RenderType.draw(MeshData) closes the mesh internally; remove() above
                        // ensures release() in the finally block won't double-close it.
                        getRenderTypeForLayer(layer).draw(mesh);
                    }
                }
            } finally {
                results.release();
            }
        });
    }

    private BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> startedLayers, SectionBufferBuilderPack buffers, ChunkSectionLayer layer) {
        var builder = startedLayers.get(layer);
        if (builder == null) {
            var buffer = buffers.buffer(layer);
            builder = new BufferBuilder(buffer, VertexFormat.Mode.QUADS, layer.vertexFormat());
            startedLayers.put(layer, builder);
        }
        return builder;
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
     * every {@code renderedBlocks} group into one {@link MeshData} per touched layer, storing them
     * into {@link #cachedMeshes}. Subsequent calls in the COMPILED state walk the cache map and
     * draw each mesh via {@link #drawCachedMesh} (which copies before drawing so the cached data
     * stays valid).
     * <p>
     * The compile thread reuses a long-lived {@link SectionBufferBuilderPack} ({@link #cacheBuilders});
     * {@link MeshData} instances reference into its underlying {@link ByteBufferBuilder}s, so the
     * pack must outlive the meshes (managed by {@link #deleteCacheBuffer()}).
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
        if (cachedMeshes != null) {
            for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                List<MeshData> meshes = cachedMeshes.get(layer);
                if (meshes == null || meshes.isEmpty()) continue;
                RenderType rt = getRenderTypeForLayer(layer);
                for (MeshData mesh : meshes) {
                    drawCachedMesh(rt, mesh);
                }
            }
            // BESRs are submitted by submitBlockEntities() during drawWorld's submit phase
            // (using {@link #blockEntities} as the seed set); no inline submit/dispatch here.
        }
    }

    private void startAsyncCompile(Minecraft mc, SectionBufferBuilderPack compileBuilders) {
        var entriesSnapshot = List.copyOf(renderedBlocksMap.values());
        thread = new Thread(() -> {
            cacheState.set(CacheState.COMPILING);
            EnumMap<ChunkSectionLayer, List<MeshData>> compiled = new EnumMap<>(ChunkSectionLayer.class);
            try {
                var region = world instanceof BlockAndTintGetter g ? g : world instanceof DummyWorld dummyWorld ? DummyWorld.ClientSupport.asClientWorld(dummyWorld) : new WrappedBlockAndTintGetter(world);
                for (var entry : entriesSnapshot) {
                    if (Thread.interrupted()) return;
                    Results r = renderBlocks(region, entry.snapshot(), entry.hook(), compileBuilders);
                    r.renderedLayers.forEach((layer, mesh) ->
                            compiled.computeIfAbsent(layer, k -> new ArrayList<>()).add(mesh));
                    // blockEntities collected separately below to keep compile-thread minimal.
                }
            } catch (Exception e) {
                compiled.values().forEach(list -> list.forEach(MeshData::close));
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
                compiled.values().forEach(list -> list.forEach(MeshData::close));
                return;
            }
            if (thread != null) {
                Map<ChunkSectionLayer, List<MeshData>> oldMeshes = cachedMeshes;
                cachedMeshes = compiled;
                blockEntities = poses;
                cacheState.set(CacheState.COMPILED);
                thread = null;
                closeMeshes(oldMeshes);
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
        EnumMap<ChunkSectionLayer, List<MeshData>> compiled = new EnumMap<>(ChunkSectionLayer.class);
        state.results.renderedLayers.forEach((layer, mesh) ->
                compiled.computeIfAbsent(layer, k -> new ArrayList<>()).add(mesh));
        state.results.renderedLayers.clear();
        Map<ChunkSectionLayer, List<MeshData>> oldMeshes = cachedMeshes;
        cachedMeshes = compiled;
        blockEntities = state.results.blockEntities.stream()
                .map(BlockEntity::getBlockPos)
                .collect(java.util.stream.Collectors.toSet());
        syncCompileState = null;
        cacheState.set(CacheState.COMPILED);
        closeMeshes(oldMeshes);
        maxProgress = -1;
    }

    private static void closeMeshes(@Nullable Map<ChunkSectionLayer, List<MeshData>> meshes) {
        if (meshes != null) {
            meshes.values().forEach(list -> list.forEach(MeshData::close));
        }
    }

    /**
     * Draw cached MeshData using a RenderType. This creates a copy of the mesh data for drawing
     * since RenderType.draw() consumes/closes the MeshData.
     */
    private void drawCachedMesh(RenderType renderType, MeshData cachedMesh) {
        ByteBuffer vertexSource = cachedMesh.vertexBuffer();
        ByteBuffer indexSource = cachedMesh.indexBuffer();
        if (vertexSource == null) {
            return;
        }

        try (ByteBufferBuilder vertexBuilder = new ByteBufferBuilder(vertexSource.remaining());
             ByteBufferBuilder indexBuilder = indexSource == null ? null : new ByteBufferBuilder(indexSource.remaining())) {
            ByteBufferBuilder.Result vertexResult = copyBuffer(vertexSource, vertexBuilder);
            if (vertexResult == null) {
                return;
            }

            MeshData drawMesh = new MeshData(vertexResult, cachedMesh.drawState());
            if (indexSource != null && indexBuilder != null) {
                ByteBufferBuilder.Result indexResult = copyBuffer(indexSource, indexBuilder);
                if (indexResult != null) {
                    ((MeshDataAccessor) (Object) drawMesh).setIndexBuffer(indexResult);
                }
            }

            renderType.draw(drawMesh);
        }
    }

    @Nullable
    private static ByteBufferBuilder.Result copyBuffer(ByteBuffer source, ByteBufferBuilder builder) {
        ByteBuffer copySource = source.duplicate();
        int size = copySource.remaining();
        if (size <= 0) {
            return null;
        }
        long ptr = builder.reserve(size);
        org.lwjgl.system.MemoryUtil.memCopy(org.lwjgl.system.MemoryUtil.memAddress(copySource), ptr, size);
        return builder.build();
    }

    public static final class Results {
        public final List<BlockEntity> blockEntities = new ArrayList<>();
        public final Map<ChunkSectionLayer, MeshData> renderedLayers = new EnumMap<>(ChunkSectionLayer.class);
        public VisibilitySet visibilitySet = new VisibilitySet();
        public MeshData.@Nullable SortState transparencyState;

        public void release() {
            this.renderedLayers.values().forEach(MeshData::close);
        }
    }

    private static final class BlockCompileContext {
        private final WorldSceneRenderer renderer;
        private final BlockAndTintGetter region;
        private final SectionBufferBuilderPack builders;
        private final ModelBlockRenderer blockRenderer;
        private final FluidRenderer fluidRenderer;
        private final net.minecraft.client.renderer.block.BlockStateModelSet blockStateModelSet;
        private final net.minecraft.client.renderer.block.FluidStateModelSet fluidModelSet;
        private final net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher blockEntityRenderer;
        private final boolean cutoutLeaves;
        private final Map<ChunkSectionLayer, BufferBuilder> startedLayers = new EnumMap<>(ChunkSectionLayer.class);
        private final float[] curOffset = new float[3];
        private final int[] curColor = {-1};
        private final boolean[] curHasTransform = {false};
        private final float[] fluidOffset = new float[3];
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
                    fluidOffset[0] = pos.getX() - (pos.getX() & 15);
                    fluidOffset[1] = pos.getY() - (pos.getY() & 15);
                    fluidOffset[2] = pos.getZ() - (pos.getZ() & 15);
                    var customRenderer = fluidModelSet.get(fluidState).customRenderer();
                    if (customRenderer == null
                            || !customRenderer.renderFluid(fluidRenderer, fluidState, region, pos, fluidOutput, blockState)) {
                        fluidRenderer.tesselate(region, pos, fluidOutput, blockState, fluidState);
                    }
                }

                if (blockState.getRenderShape() == MODEL) {
                    var model = blockStateModelSet.get(blockState);
                    var forceOpaque = ModelBlockRenderer.forceOpaque(cutoutLeaves, blockState);
                    blockRenderer.tesselateBlock(
                            forceOpaque ? opaqueQuadOutput : quadOutput,
                            pos.getX(), pos.getY(), pos.getZ(),
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

        private void build(Results results) {
            var vertexSorting = VertexSorting.byDistance(renderer.eyePos.x, renderer.eyePos.y, renderer.eyePos.z);
            for (Map.Entry<ChunkSectionLayer, BufferBuilder> entry : startedLayers.entrySet()) {
                ChunkSectionLayer layer = entry.getKey();
                MeshData mesh = entry.getValue().build();
                if (mesh != null) {
                    if (layer == ChunkSectionLayer.TRANSLUCENT) {
                        results.transparencyState = mesh.sortQuads(builders.buffer(layer), vertexSorting);
                    }
                    results.renderedLayers.put(layer, mesh);
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
     * Tesselate a group of blocks into per-layer {@link MeshData}, mirroring
     * {@code SectionCompiler.compile} but rendering at world coordinates instead of section-local.
     * <p>
     * The returned {@link Results#renderedLayers} owns the built meshes; callers must either draw
     * them via {@link RenderType#draw(MeshData)} (which closes the mesh) or call
     * {@link Results#release()}.
     * <p>
     * Fluid offset note: {@link FluidRenderer#tesselate} writes vertex positions in section-local
     * coordinates ({@code pos.getX() & 15}). In vanilla chunk rendering the {@code ChunkSection}
     * UBO adds the section origin back. We render with the BLOCK pipeline at real world
     * coordinates and have no such UBO, so we wrap the fluid {@link VertexConsumer} in a
     * {@link VertexConsumerWrapper} that adds the section origin offset for each fluid block.
     * Block geometry is unaffected because {@link ModelBlockRenderer#tesselateBlock} already
     * accepts the world-space {@code (x, y, z)} as offset and routes quads via
     * {@link BufferBuilder#putBlockBakedQuad}, bypassing the wrapper.
     */
    private Results renderBlocks(BlockAndTintGetter region,
                                 Collection<BlockPos> renderedBlocks,
                                 @Nullable ISceneBlockRenderHook hook,
                                 SectionBufferBuilderPack builders) {
        var results = new Results();
        var compileContext = new BlockCompileContext(this, region, builders);
        BlockModelLighter.enableCaching();
        try {
            for (BlockPos pos : renderedBlocks) {
                compileContext.renderBlock(pos, hook, results);
                // for async progress
                if (maxProgress > 0) {
                    progress++;
                }
            }
            compileContext.build(results);
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
                var state = beDispatcher.tryExtractRenderState(be, partialTicks, null, null);
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
        return new Matrix4f(projectionMatrix).mul(RenderSystem.getModelViewMatrix());
    }

    private int[] currentViewport() {
        return new int[] { viewportX, viewportY, viewportWidth, viewportHeight };
    }

    public Vector3f project(Vector3f pos) {
        Vector3f winCoords = new Vector3f();
        computeCombinedMatrix().project(pos.x(), pos.y(), pos.z(), currentViewport(), winCoords);
        return winCoords;
    }

    public Vector3f unProject(int mouseX, int mouseY) {
        return unProject(mouseX, mouseY, false);
    }

    public Vector3f unProject(int mouseX, int mouseY, boolean checkDepth) {
        float pixelDepth = 0.999f;
        if (checkDepth) {
            pixelDepth = readDepthPixelAsync(mouseX, mouseY);
        }
        Vector3f objCoords = new Vector3f();
        computeCombinedMatrix().unproject(mouseX, mouseY, pixelDepth, currentViewport(), objCoords);
        return objCoords;
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
                            try (var view = RenderSystem.getDevice().createCommandEncoder()
                                    .mapBuffer(issuedBuf.slice(0, 4), true, false)) {
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

        drawWorld(Minecraft.getInstance().renderBuffers());

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

        drawWorld(Minecraft.getInstance().renderBuffers());
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
