package com.lowdragmc.lowdraglib2.client.scene;

import com.lowdragmc.lowdraglib2.core.mixins.accessor.MeshDataAccessor;
import com.lowdragmc.lowdraglib2.client.utils.glu.Project;
import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.PositionedRect;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.DummyWorld;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BakedQuadOutput;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static net.minecraft.world.level.block.RenderShape.MODEL;


/**
 * @author KilaBash
 * @implNote render a scene, through VBO compilation scene, greatly optimize rendering performance.
 */
@OnlyIn(Dist.CLIENT)
@Accessors(chain = true)
public abstract class WorldSceneRenderer {
    protected static final FloatBuffer MODELVIEW_MATRIX_BUFFER = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    protected static final FloatBuffer PROJECTION_MATRIX_BUFFER = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    protected static final IntBuffer VIEWPORT_BUFFER = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
    protected static final FloatBuffer PIXEL_DEPTH_BUFFER = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    protected static final FloatBuffer OBJECT_POS_BUFFER = ByteBuffer.allocateDirect(3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private static final float SHEETED_DECAL_TEXTURE_SCALE = 0.0078125F;

    enum CacheState {
        UNCREATED,
        NEED,
        COMPILING,
        COMPILED
    }

    public final Level world;
    public final Map<Collection<BlockPos>, ISceneBlockRenderHook> renderedBlocksMap;
    @Nullable
    protected MeshData[] cachedMeshData;
    @Nullable
    protected ByteBufferBuilder[] cachedByteBuilders;
    @Nullable
    protected boolean[] cachedMeshUsed;
    protected Set<BlockPos> tileEntities;
    @Getter
    protected boolean useCache;
    @Getter @Setter
    protected boolean endBatchLast = false;
    protected boolean ortho;
    protected AtomicReference<CacheState> cacheState;
    protected int maxProgress;
    protected int progress;
    protected Thread thread;
    @Getter
    protected ParticleManager particleManager;
    protected final Camera camera = new Camera();
    protected final CameraEntity cameraEntity;
    /** The projection matrix as Matrix4f, kept for project/unProject */
    protected Matrix4f projectionMatrix = new Matrix4f();
    @Nullable
    protected ProjectionMatrixBuffer projectionMatrixBuffer;
    @Setter @Nullable
    private Consumer<WorldSceneRenderer> beforeWorldRender;
    @Setter @Nullable
    private Consumer<WorldSceneRenderer> afterWorldRender;
    @Setter @Nullable
    private BiConsumer<MultiBufferSource, Float> beforeBatchEnd;
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
        renderedBlocksMap = new LinkedHashMap<>();
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

    public WorldSceneRenderer useOrtho(boolean ortho) {
        this.ortho = ortho;
        return this;
    }

    public WorldSceneRenderer deleteCacheBuffer() {
        if (cachedMeshData != null) {
            for (int i = 0; i < cachedMeshData.length; i++) {
                if (cachedMeshData[i] != null) {
                    cachedMeshData[i].close();
                    cachedMeshData[i] = null;
                }
            }
            if (cachedByteBuilders != null) {
                for (int i = 0; i < cachedByteBuilders.length; i++) {
                    if (cachedByteBuilders[i] != null) {
                        cachedByteBuilders[i].close();
                        cachedByteBuilders[i] = null;
                    }
                }
            }
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
            cachedMeshData = null;
            cachedByteBuilders = null;
            cachedMeshUsed = null;
        }
        this.tileEntities = null;
        cacheState.set(CacheState.UNCREATED);
        return this;
    }

    protected void makeSureCacheBufferCreated() {
        if (cachedMeshData == null) {
            ChunkSectionLayer[] layers = ChunkSectionLayer.values();
            cachedMeshData = new MeshData[layers.length];
            cachedByteBuilders = new ByteBufferBuilder[layers.length];
            cachedMeshUsed = new boolean[layers.length];
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
            cacheState.set(CacheState.NEED);
        }
    }

    public WorldSceneRenderer needCompileCache() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        cacheState.set(CacheState.NEED);
        return this;
    }

    public WorldSceneRenderer addRenderedBlocks(Collection<BlockPos> blocks, @Nullable ISceneBlockRenderHook renderHook) {
        if (blocks != null) {
            this.renderedBlocksMap.put(blocks, renderHook);
        }
        return this;
    }

    public WorldSceneRenderer removeRenderedBlocks(Collection<BlockPos> blocks) {
        if (blocks != null) {
            this.renderedBlocksMap.remove(blocks);
        }
        return this;
    }

    public WorldSceneRenderer removeAllRenderedBlocks() {
        this.renderedBlocksMap.clear();
        return this;
    }

    /**
     * Render the scene directly at the given pixel viewport, bypassing GUI coordinate conversion.
     * Used by the PIP renderer when rendering into a texture.
     */
    public void renderDirect(int viewportWidth, int viewportHeight, int mouseX, int mouseY) {
        if (Minecraft.getInstance().getOverlay() instanceof LoadingOverlay) {
            return;
        }
        PositionedRect viewport = PositionedRect.of(Position.of(0, 0), Size.of(viewportWidth, viewportHeight));
        setupCamera(viewport);
        drawWorld();
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
        drawWorld();
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

        GlStateManager._enableDepthTest();
        GlStateManager._enableBlend();

        //setup viewport and clear GL buffers
        GlStateManager._viewport(x, y, width, height);

        GlStateManager._depthMask(true);
        clearView(x, y, width, height);

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

        GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        GlStateManager._enableCull();
    }

    protected void clearView(int x, int y, int width, int height) {
        GL11.glClearColor(0, 0, 0, 0);
        GlStateManager._clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    protected void resetCamera() {
        //reset viewport
        Minecraft minecraft = Minecraft.getInstance();
        GlStateManager._viewport(0, 0, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());

        //reset projection matrix
        RenderSystem.restoreProjectionMatrix();

        //reset modelview matrix
        Matrix4fStack posesStack = RenderSystem.getModelViewStack();
        posesStack.popMatrix();

        GlStateManager._disableDepthTest();
        GlStateManager._enableBlend();
    }

    protected void drawWorld() {
        if (beforeWorldRender != null) {
            beforeWorldRender.accept(this);
        }

        Minecraft mc = Minecraft.getInstance();

        float particleTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var buffers = mc.renderBuffers().bufferSource();
        if (useCache) {
            renderCacheBuffer(mc, buffers, particleTicks);
        } else {
            renderUncachedWorld(mc, buffers, particleTicks);
        }

        // TODO: entity rendering uses new extract+submit API, needs SubmitNodeCollector implementation

        if (beforeBatchEnd != null) {
            beforeBatchEnd.accept(buffers, particleTicks);
        }

        buffers.endBatch();

        if (particleManager != null) {
            @Nonnull PoseStack poseStack = new PoseStack();
            poseStack.setIdentity();
            poseStack.translate(cameraEntity.getX(), cameraEntity.getY(), cameraEntity.getZ());
            particleManager.render(poseStack, camera, particleTicks, type -> true);
        }

        if (afterWorldRender != null) {
            afterWorldRender.accept(this);
        }
    }

    private void renderUncachedWorld(Minecraft mc, MultiBufferSource.BufferSource buffers, float particleTicks) {
        var bsr = mc.getBlockRenderer();
        var randomSource = RandomSource.createNewThreadLocalInstance();
        var parts = new ObjectArrayList<BlockModelPart>();
        renderedBlocksMap.forEach((renderedBlocks, hook) -> {
            for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                PoseStack poseStack = new PoseStack();

                if (layer == ChunkSectionLayer.TRANSLUCENT) { // render tesr and particle before translucent
                    setDefaultRenderLayerState(null);
                    renderTESR(renderedBlocks, poseStack, buffers, hook, particleTicks);

                    if (hook != null || !endBatchLast) {
                        buffers.endBatch();
                    }

                    if (particleManager != null) {
                        poseStack.pushPose();
                        poseStack.setIdentity();
                        poseStack.translate(cameraEntity.getX(), cameraEntity.getY(), cameraEntity.getZ());
                        particleManager.render(poseStack, camera, particleTicks, type -> true);
                        poseStack.popPose();
                    }
                }

                setDefaultRenderLayerState(layer);
                if (hook != null) {
                    hook.apply(layer.pipeline());
                }

                var buffer = buffers.getBuffer(ItemBlockRenderTypes.getRenderType(layer));
                renderBlocks(poseStack, bsr, layer, new VertexConsumerWrapper(buffer), renderedBlocks, randomSource, parts, hook, particleTicks);

                if (!endBatchLast) {
                    buffers.endBatch();
                }
            }
        });
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

    private void renderCacheBuffer(Minecraft mc, MultiBufferSource.BufferSource buffers, float particleTicks) {
        ChunkSectionLayer[] layers = ChunkSectionLayer.values();
        if (cacheState.get() == CacheState.NEED || cacheState.get() == CacheState.UNCREATED) {
            makeSureCacheBufferCreated();
            progress = 0;
            maxProgress = renderedBlocksMap.keySet().stream().map(Collection::size).reduce(0, Integer::sum) * (layers.length + 1);
            thread = new Thread(() -> {
                cacheState.set(CacheState.COMPILING);
                BlockRenderDispatcher blockRenderDispatcher = mc.getBlockRenderer();
                var randomSource = RandomSource.createNewThreadLocalInstance();
                var parts = new ObjectArrayList<BlockModelPart>();
                MeshData[] compiledMeshData = new MeshData[layers.length];
                ByteBufferBuilder[] compiledByteBuilders = new ByteBufferBuilder[layers.length];
                boolean[] compiledMeshUsed = new boolean[layers.length];
                try {
                    ModelBlockRenderer.enableCaching();
                    PoseStack matrixstack = new PoseStack();
                    for (int i = 0; i < layers.length; i++) {
                        if (Thread.interrupted())
                            return;
                        ChunkSectionLayer layer = layers[i];
                        var renderType = ItemBlockRenderTypes.getRenderType(layer);
                        // Keep the ByteBufferBuilder alive so MeshData's ByteBuffer references remain valid
                        ByteBufferBuilder byteBuilder = new ByteBufferBuilder(renderType.bufferSize());
                        BufferBuilder buffer = new BufferBuilder(byteBuilder, renderType.mode(), renderType.format());
                        renderedBlocksMap.forEach((renderedBlocks, hook) -> {
                            renderBlocks(matrixstack, blockRenderDispatcher, layer, new VertexConsumerWrapper(buffer), renderedBlocks, randomSource, parts, hook, 0);
                        });
                        MeshData data = buffer.build();
                        if (layer == ChunkSectionLayer.TRANSLUCENT) {
//                            results.transparencyState = data.sortQuads(builders.buffer(layer), vertexSorting);
                        }
                        // Close old cached data if present
                        if (data == null) {
                            byteBuilder.close();
                            continue;
                        }
                        compiledMeshUsed[i] = true;
                        compiledMeshData[i] = data;
                        compiledByteBuilders[i] = byteBuilder;
                    }
                    ModelBlockRenderer.clearCache();
                } catch (Exception e) {
                    for (MeshData meshData : compiledMeshData) {
                        if (meshData != null) {
                            meshData.close();
                        }
                    }
                    for (ByteBufferBuilder byteBuilder : compiledByteBuilders) {
                        if (byteBuilder != null) {
                            byteBuilder.close();
                        }
                    }
                    // compilation interrupted or failed
                    return;
                }
                Set<BlockPos> poses = new HashSet<>();
                renderedBlocksMap.forEach((renderedBlocks, hook) -> {
                    for (BlockPos pos : renderedBlocks) {
                        progress++;
                        if (Thread.interrupted())
                            return;
                        BlockEntity tile = world.getBlockEntity(pos);
                        if (tile != null) {
                            if (mc.getBlockEntityRenderDispatcher().getRenderer(tile) != null) {
                                poses.add(pos);
                            }
                        }
                    }
                });
                if (Thread.interrupted())
                    return;
                if (thread != null) {
                    MeshData[] oldMeshData = cachedMeshData;
                    ByteBufferBuilder[] oldByteBuilders = cachedByteBuilders;
                    cachedMeshData = compiledMeshData;
                    cachedByteBuilders = compiledByteBuilders;
                    cachedMeshUsed = compiledMeshUsed;
                    tileEntities = poses;
                    cacheState.set(CacheState.COMPILED);
                    thread = null;
                    if (oldMeshData != null) {
                        for (MeshData meshData : oldMeshData) {
                            if (meshData != null) {
                                meshData.close();
                            }
                        }
                    }
                    if (oldByteBuilders != null) {
                        for (ByteBufferBuilder byteBuilder : oldByteBuilders) {
                            if (byteBuilder != null) {
                                byteBuilder.close();
                            }
                        }
                    }
                }
                maxProgress = -1;
            });
            thread.start();
        } else {
            var poseStack = new PoseStack();
            for (int i = 0; i < layers.length; i++) {
                var layer = layers[i];
                if (layer == ChunkSectionLayer.TRANSLUCENT && tileEntities != null) { // render tesr before translucent
                    renderTESR(tileEntities, poseStack, mc.renderBuffers().bufferSource(), null, particleTicks);
                    if (!endBatchLast) {
                        buffers.endBatch();
                    }

                    if (particleManager != null) {
                        poseStack.pushPose();
                        poseStack.setIdentity();
                        poseStack.translate(cameraEntity.getX(), cameraEntity.getY(), cameraEntity.getZ());
                        particleManager.render(poseStack, camera, particleTicks, type -> true);
                        poseStack.popPose();
                    }
                }

                if (!cachedMeshUsed[i] || cachedMeshData[i] == null) continue;

                setDefaultRenderLayerState(layer);

                // Draw the cached mesh data using the layer's RenderType
                var renderType = ItemBlockRenderTypes.getRenderType(layer);
                drawCachedMesh(renderType, cachedMeshData[i]);
            }
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

    private void renderBlocks(PoseStack poseStack, BlockRenderDispatcher brd, ChunkSectionLayer layer,
                              VertexConsumerWrapper wrapperBuffer, Collection<BlockPos> renderedBlocks,
                              RandomSource randomSource, List<BlockModelPart> parts,
                              @Nullable ISceneBlockRenderHook hook, float partialTicks) {
        // Filter quads by layer in the callback - only write quads that belong to the current layer
        BakedQuadOutput quadOutput = (pose, quad, brightness, color, lightmapCoord, overlayCoords) -> {
            if (quad.spriteInfo().layer() == layer) {
                wrapperBuffer.putBulkData(pose, quad, brightness, color, lightmapCoord, overlayCoords);
            }
        };
        BakedQuadOutput opaqueQuadOutput = (pose, quad, brightness, color, lightmapCoord, overlayCoords) -> {
            if (layer == ChunkSectionLayer.SOLID) {
                wrapperBuffer.putBulkData(pose, quad, brightness, color, lightmapCoord, overlayCoords);
            }
        };

        for (BlockPos pos : renderedBlocks) {
            if (blocked != null && blocked.contains(pos)) {
                continue;
            }
            var state = world.getBlockState(pos);
            var fluidState = state.getFluidState();
            var block = state.getBlock();

            if (hook != null) {
                hook.applyVertexConsumerWrapper(world, pos, state, wrapperBuffer, layer.pipeline(), partialTicks);
            }

            if (block == Blocks.AIR) continue;
            if (state.getRenderShape() == MODEL) {
                BlockStateModel model = brd.getBlockModel(state);
                boolean forceOpaque = ItemBlockRenderTypes.forceOpaque(state);
                randomSource.setSeed(state.getSeed(pos));
                parts.clear();
                model.collectParts(world, pos, state, randomSource, parts);
                poseStack.pushPose();
                poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
                brd.renderBatched(state, pos, world, poseStack, forceOpaque ? opaqueQuadOutput : quadOutput, false, parts);
                poseStack.popPose();
                parts.clear();
            }
            if (!fluidState.isEmpty() && ItemBlockRenderTypes.getRenderLayer(fluidState) == layer) {
                wrapperBuffer.addOffset((pos.getX() - (pos.getX() & 15)), (pos.getY() - (pos.getY() & 15)), (pos.getZ() - (pos.getZ() & 15)));
                brd.renderLiquid(pos, world, wrapperBuffer, state, fluidState);
            }
            wrapperBuffer.clearOffset();
            wrapperBuffer.clearColor();
            if (maxProgress > 0) {
                progress++;
            }
        }
    }

    private void renderTESR(Collection<BlockPos> poses, PoseStack poseStack, MultiBufferSource.BufferSource buffers, @Nullable ISceneBlockRenderHook hook, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        var dispatcher = mc.getBlockEntityRenderDispatcher();
        dispatcher.prepare(new Vec3(eyePos.x(), eyePos.y(), eyePos.z()));

        var cameraRenderState = new CameraRenderState();
        cameraRenderState.pos = new Vec3(eyePos.x(), eyePos.y(), eyePos.z());
        cameraRenderState.blockPos = BlockPos.containing(eyePos.x(), eyePos.y(), eyePos.z());

        var submitCollector = new ImmediateSubmitNodeCollector(buffers);

        for (BlockPos pos : poses) {
            if (blocked != null && blocked.contains(pos)) continue;
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile == null) continue;

            var state = dispatcher.tryExtractRenderState(tile, partialTicks, null, null);
            if (state == null) continue;

            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

            if (hook != null) {
                hook.applyBESR(world, pos, tile, poseStack, partialTicks);
            }

            dispatcher.submit(state, poseStack, submitCollector, cameraRenderState);
            poseStack.popPose();
        }
    }

    public static void setDefaultRenderLayerState(@Nullable ChunkSectionLayer layer) {
        if (layer != null && layer.translucent()) { // TRANSLUCENT
            GlStateManager._enableBlend();
            GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager._depthMask(false);
        } else { // SOLID / CUTOUT
            GlStateManager._enableDepthTest();
            GlStateManager._disableBlend();
            GlStateManager._depthMask(true);
        }
    }

    public BlockHitResult rayTrace(Vector3f hitPos) {
        var startPos = new Vec3(this.eyePos.x(), this.eyePos.y(), this.eyePos.z());
        if (ortho) {
            startPos = startPos.add(new Vec3(startPos.x - lookAt.x(), startPos.y - lookAt.y(), startPos.z - lookAt.z()).multiply(500, 500, 500));
        }
        hitPos = hitPos.mul(2, new Vector3f()); // Double view range to ensure pos can be seen.
        var endPos = new Vec3((hitPos.x() - startPos.x), (hitPos.y() - startPos.y), (hitPos.z() - startPos.z));
        try {
            return this.world.clip(new ClipContext(startPos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, cameraEntity));
        } catch (Exception e) {
            return null;
        }
    }

    public Vector3f project(Vector3f pos) {
        //read current rendering parameters
        RenderSystem.getModelViewMatrix().get(MODELVIEW_MATRIX_BUFFER);
        projectionMatrix.get(PROJECTION_MATRIX_BUFFER);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, VIEWPORT_BUFFER);

        //rewind buffers after write by OpenGL glGet calls
        MODELVIEW_MATRIX_BUFFER.rewind();
        PROJECTION_MATRIX_BUFFER.rewind();
        VIEWPORT_BUFFER.rewind();

        //call gluProject with retrieved parameters
        Project.gluProject(pos.x(), pos.y(), pos.z(), MODELVIEW_MATRIX_BUFFER, PROJECTION_MATRIX_BUFFER, VIEWPORT_BUFFER, OBJECT_POS_BUFFER);

        //rewind buffers after read by gluProject
        VIEWPORT_BUFFER.rewind();
        PROJECTION_MATRIX_BUFFER.rewind();
        MODELVIEW_MATRIX_BUFFER.rewind();

        //rewind buffer after write by gluProject
        OBJECT_POS_BUFFER.rewind();

        //obtain position in Screen
        float winX = OBJECT_POS_BUFFER.get();
        float winY = OBJECT_POS_BUFFER.get();
        float winZ = OBJECT_POS_BUFFER.get();

        //rewind buffer after read
        OBJECT_POS_BUFFER.rewind();

        return new Vector3f(winX, winY, winZ);
    }

    public Vector3f unProject(int mouseX, int mouseY) {
        return unProject(mouseX, mouseY, true);
    }

    public Vector3f unProject(int mouseX, int mouseY, boolean checkDepth) {
        var pixelDepth = 0.999f;
        if (checkDepth) {
            //read depth of pixel under mouse
            GL11.glReadPixels(mouseX, mouseY, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, PIXEL_DEPTH_BUFFER);

            //rewind buffer after write by glReadPixels
            PIXEL_DEPTH_BUFFER.rewind();

            //retrieve depth from buffer (0.0-1.0f)
            pixelDepth = PIXEL_DEPTH_BUFFER.get();
        }

        //rewind buffer after read
        PIXEL_DEPTH_BUFFER.rewind();

        //read current rendering parameters
        RenderSystem.getModelViewMatrix().get(MODELVIEW_MATRIX_BUFFER);
        projectionMatrix.get(PROJECTION_MATRIX_BUFFER);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, VIEWPORT_BUFFER);

        //rewind buffers after write by OpenGL glGet calls
        MODELVIEW_MATRIX_BUFFER.rewind();
        PROJECTION_MATRIX_BUFFER.rewind();
        VIEWPORT_BUFFER.rewind();

        //call gluUnProject with retrieved parameters
        Project.gluUnProject(mouseX, mouseY, pixelDepth, MODELVIEW_MATRIX_BUFFER, PROJECTION_MATRIX_BUFFER, VIEWPORT_BUFFER, OBJECT_POS_BUFFER);

        //rewind buffers after read by gluUnProject
        VIEWPORT_BUFFER.rewind();
        PROJECTION_MATRIX_BUFFER.rewind();
        MODELVIEW_MATRIX_BUFFER.rewind();

        //rewind buffer after write by gluUnProject
        OBJECT_POS_BUFFER.rewind();

        //obtain absolute position in world
        float posX = OBJECT_POS_BUFFER.get();
        float posY = OBJECT_POS_BUFFER.get();
        float posZ = OBJECT_POS_BUFFER.get();

        //rewind buffer after read
        OBJECT_POS_BUFFER.rewind();

        return new Vector3f(posX, posY, posZ);
    }

    /***
     * For better performance, You'd better handle the event {@link #setOnLookingAt(Consumer)} or {@link #getLastTraceResult()}
     * @param mouseX xPos in Texture
     * @param mouseY yPos in Texture
     * @return RayTraceResult Hit
     */
    protected BlockHitResult screenPos2BlockPosFace(int mouseX, int mouseY, int x, int y, int width, int height) {
        // render a frame
        GlStateManager._enableDepthTest();
        setupCamera(getPositionedRect(x, y, width, height));

        drawWorld();

        Vector3f hitPos = this.lastHit == null ? unProject(mouseX, mouseY) : this.lastHit;
        BlockHitResult result = rayTrace(hitPos);

        resetCamera();

        return result;
    }

    /***
     * For better performance, You'd better do project in {@link #setAfterWorldRender(Consumer)}
     * @param pos BlockPos
     * @param depth should pass Depth Test
     * @return x, y, z
     */
    protected Vector3f blockPos2ScreenPos(BlockPos pos, boolean depth, int x, int y, int width, int height) {
        // render a frame
        GlStateManager._enableDepthTest();
        setupCamera(getPositionedRect(x, y, width, height));

        drawWorld();
        Vector3f winPos = project(new Vector3f(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f));

        resetCamera();

        return winPos;
    }

    /**
     * Immediate-mode SubmitNodeCollector that renders submitted geometry directly
     * into a MultiBufferSource instead of deferring to a later render pass.
     */
    static class ImmediateSubmitNodeCollector implements SubmitNodeCollector {
        private final MultiBufferSource bufferSource;

        ImmediateSubmitNodeCollector(MultiBufferSource bufferSource) {
            this.bufferSource = bufferSource;
        }

        @Override
        public OrderedSubmitNodeCollector order(int order) {
            return this; // no ordering needed for immediate rendering
        }

        @Override
        public <S> void submitModel(
                net.minecraft.client.model.Model<? super S> model, S state, PoseStack poseStack,
                RenderType renderType, int lightCoords, int overlayCoords, int tintedColor,
                @Nullable net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
                int outlineColor, net.minecraft.client.renderer.feature.ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
            // model.setupAnim is already called by the renderer before submit
            VertexConsumer buffer = createVertexConsumer(bufferSource, renderType, sprite, poseStack.last(), false);
            model.renderToBuffer(poseStack, buffer, lightCoords, overlayCoords, tintedColor);
        }

        @Override
        public void submitModelPart(
                net.minecraft.client.model.geom.ModelPart modelPart, PoseStack poseStack,
                RenderType renderType, int lightCoords, int overlayCoords,
                @Nullable net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
                boolean sheeted, boolean hasFoil, int tintedColor,
                net.minecraft.client.renderer.feature.ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay,
                int outlineColor) {
            VertexConsumer buffer = createVertexConsumer(bufferSource, renderType, sprite, poseStack.last(), sheeted);
            modelPart.render(poseStack, buffer, lightCoords, overlayCoords, tintedColor);
        }

        @Override
        public void submitCustomGeometry(PoseStack poseStack, RenderType renderType,
                                         SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
            VertexConsumer buffer = bufferSource.getBuffer(renderType);
            customGeometryRenderer.render(poseStack.last(), buffer);
        }

        @Override
        public void submitBlock(PoseStack poseStack, net.minecraft.world.level.block.state.BlockState state,
                                int lightCoords, int overlayCoords, int outlineColor) {
            // Rarely used by BESRs; no-op in scene renderer
        }

        @Override
        public void submitBlockModel(PoseStack poseStack, RenderType renderType,
                                     net.minecraft.client.renderer.block.model.BlockStateModel model,
                                     int tintColor, int lightCoords, int overlayCoords, int outlineColor) {
            // Rarely used by BESRs; no-op in scene renderer
        }

        @Override
        public void submitItem(PoseStack poseStack, net.minecraft.world.item.ItemDisplayContext displayContext,
                               int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers,
                               java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads,
                               net.minecraft.client.renderer.item.ItemStackRenderState.FoilType foilType) {
            // Rarely used by BESRs; no-op in scene renderer
        }

        @Override
        public void submitMovingBlock(PoseStack poseStack, net.minecraft.client.renderer.block.MovingBlockRenderState state) {
            // Not applicable in scene renderer
        }

        // Entity-specific submit methods - no-ops in scene renderer
        @Override
        public void submitShadow(PoseStack poseStack, float radius, java.util.List<net.minecraft.client.renderer.entity.state.EntityRenderState.ShadowPiece> pieces) {}

        @Override
        public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset,
                                  net.minecraft.network.chat.Component name, boolean seeThrough, int lightCoords,
                                  double distanceToCameraSq, CameraRenderState camera) {}

        @Override
        public void submitText(PoseStack poseStack, float x, float y, net.minecraft.util.FormattedCharSequence string,
                               boolean dropShadow, net.minecraft.client.gui.Font.DisplayMode displayMode,
                               int lightCoords, int color, int backgroundColor, int outlineColor) {}

        @Override
        public void submitFlame(PoseStack poseStack, net.minecraft.client.renderer.entity.state.EntityRenderState renderState,
                                org.joml.Quaternionf rotation) {}

        @Override
        public void submitLeash(PoseStack poseStack, net.minecraft.client.renderer.entity.state.EntityRenderState.LeashState leashState) {}

        @Override
        public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer particleGroupRenderer) {}
    }

    private static VertexConsumer createVertexConsumer(
            MultiBufferSource bufferSource,
            RenderType renderType,
            @Nullable TextureAtlasSprite sprite,
            PoseStack.Pose pose,
            boolean sheeted
    ) {
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        if (sprite != null) {
            consumer = sprite.wrap(consumer);
        }
        if (sheeted) {
            consumer = new SheetedDecalTextureGenerator(consumer, pose, SHEETED_DECAL_TEXTURE_SCALE);
        }
        return consumer;
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
