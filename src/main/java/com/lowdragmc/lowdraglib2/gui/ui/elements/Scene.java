package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.client.scene.*;
import com.lowdragmc.lowdraglib2.client.utils.RenderUtils;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils.TransformGizmo;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.lowdragmc.lowdraglib2.math.interpolate.Interpolator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.data.BlockPosFace;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Accessors(chain = true)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
@LDLRegister(name = "scene", group = "misc", registry = "ldlib2:ui_element")
public class Scene extends UIElement {
    private static final Object ROTATION_DRAGGING = new Object();
    private static final Object PAN_DRAGGING = new Object();
    @Nullable
    protected Object renderer;
    @Nullable
    @Getter
    protected TrackedDummyWorld dummyWorld;
    @Nullable
    protected Level level;
    @Getter
    protected boolean dragging;
    @Getter @Setter
    protected boolean renderFacing = true;
    @Getter @Setter
    protected boolean renderSelect = true;
    @Getter @Setter
    protected boolean draggable = true;
    @Getter @Setter
    protected boolean scalable = true;
    @Getter @Setter
    protected boolean intractable = true;
    @Getter @Setter
    protected boolean showHoverBlockTips;
    @Getter
    protected Vector3f center = new Vector3f(0.5f);
    @Getter
    protected float rotationPitch = 25;
    @Getter
    protected float rotationYaw = -135;
    @Getter
    protected float zoom = 5;
    @Getter
    protected float range = 1;

    @Getter @Setter
    protected BiConsumer<BlockPos, Direction> onSelected;
    final Set<BlockPos> core = new HashSet<>();
    @Getter
    protected boolean useCache;
    @Getter
    protected boolean syncCompile;
    @Getter
    protected boolean useOrtho = false;
    @Getter
    protected boolean autoReleased = true;
    @Getter @Setter
    protected boolean tickWorld = true;
    protected Consumer<Scene> beforeWorldRender;
    protected Consumer<Scene> afterWorldRender;
    /** Ctx-aware overlay hook fired during the renderer's {@code afterTranslucentDispatch}.
     *  Use this when you need to submit / draw via {@link SceneRenderContext}, e.g. gizmos. */
    @Nullable
    @Setter
    protected SceneRenderHook overlay;
    // editor support
//    @Nullable
//    private Identifier editorStructureName = null;
    // runtime
    @Getter
    protected ItemStack lastHoverItem;
    @Getter
    protected BlockPosFace lastClickPosFace;
    @Getter
    protected BlockPosFace lastHoverPosFace;
    @Getter
    protected BlockPosFace lastSelectedPosFace;

    public Scene() {
        setOverflowVisible(false);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.MOUSE_UP, this::onMouseUp);
        addEventListener(UIEvents.MOUSE_WHEEL, this::onMouseWheel);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSourceUpdate);
        internalSetup();
    }

    public Scene useCacheBuffer() {
        return useCacheBuffer(true);
    }

    public Scene useCacheBuffer(boolean cacheBuffer) {
        useCache = cacheBuffer;
        if (renderer != null) {
            this.<WorldSceneRenderer>getRenderer().useCacheBuffer(true);
        }
        return this;
    }

    public Scene syncCompile() {
        return syncCompile(true);
    }

    /**
     * Compile the cached vertex buffers incrementally on the main/render thread instead of on a
     * background thread. Use this when the backing {@link Level} doesn't tolerate off-thread access.
     * Has no effect unless {@link #useCacheBuffer(boolean)} is also enabled.
     */
    public Scene syncCompile(boolean syncCompile) {
        this.syncCompile = syncCompile;
        if (renderer != null) {
            var renderer = this.<WorldSceneRenderer>getRenderer();
            renderer.syncCompile(syncCompile);
        }
        return this;
    }

    public Scene useOrtho() {
        return useOrtho(true);
    }

    public Scene useOrtho(boolean useOrtho) {
        this.useOrtho = useOrtho;
        if (renderer != null) {
            var renderer = this.<WorldSceneRenderer>getRenderer();
            renderer.useOrtho(useOrtho);
            renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
            renderer.setCameraLookAt(center, camZoom(), Math.toRadians(rotationYaw), Math.toRadians(rotationPitch));
        }
        return this;
    }

    public Scene setBeforeWorldRender(Consumer<Scene> beforeWorldRender) {
        this.beforeWorldRender = beforeWorldRender;
        SceneRenderer.syncBeforeWorldRender(this);
        return this;
    }

    public Scene setAfterWorldRender(Consumer<Scene> afterWorldRender) {
        this.afterWorldRender = afterWorldRender;
        return this;
    }

    public float camZoom() {
        if (useOrtho) {
            return 0.1f;
        } else {
            return zoom;
        }
    }

    @Override
    protected void onRemoved() {
        super.onRemoved();
        if (autoReleased) {
            releaseRendererResource();
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getRenderer() {
        return (T) renderer;
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (tickWorld && dummyWorld != null) {
            dummyWorld.tickWorld();
        }
    }

    /**
     * Releases all resources held by the renderer.
     */
    public void releaseRendererResource() {
        if (renderer != null) {
            var _renderer = this.<WorldSceneRenderer>getRenderer();
            if (RenderSystem.isOnRenderThread()) {
                _renderer.releaseResource();
            } else {
                 //todo schedule to the main thread
//                RenderSystem.recordRenderCall(_renderer::releaseResource);
            }
        }
    }

    public void needCompileCache() {
        if (renderer != null) {
            this.<WorldSceneRenderer>getRenderer().needCompileCache();
        }
    }

    /**
     * Creates a scene with the given world and whether to use FBO scene renderer.
     */
    public final Scene createScene(Level world, boolean useFBOSceneRenderer, @Nullable Size fboSize) {
        releaseRendererResource();
        core.clear();
        level = world;
        dummyWorld = world instanceof TrackedDummyWorld trackedLevel ? trackedLevel : new TrackedDummyWorld(world);
        //compute window size from scaled width & height
        this.renderer = ClientWrapper.createWorldSceneRenderer(dummyWorld, useFBOSceneRenderer, fboSize);
        dummyWorld.setBlockFilter(core::contains);
        center = new Vector3f(0, 0, 0);
        var renderer = this.<WorldSceneRenderer>getRenderer();
        renderer.useOrtho(useOrtho);
        SceneRenderer.configureRenderer(this, renderer);
        renderer.setCameraLookAt(center, camZoom(), Math.toRadians(rotationYaw), Math.toRadians(rotationPitch));
        renderer.useCacheBuffer(useCache);
        renderer.syncCompile(syncCompile);
        if (dummyWorld.getParticleManager() != null) {
            renderer.setParticleManager(dummyWorld.getParticleManager());
        }
        lastClickPosFace = null;
        lastHoverPosFace = null;
        lastHoverItem = null;
        lastSelectedPosFace = null;
        return this;
    }

    private static class ClientWrapper {
        private static WorldSceneRenderer createWorldSceneRenderer(Level world, boolean useFBOSceneRenderer, @Nullable Size fboSize) {
            return useFBOSceneRenderer ?
                    new FBOWorldSceneRenderer(world, fboSize == null ? 1080 : fboSize.width, fboSize == null ? 1080 : fboSize.height) :
                    new ImmediateWorldSceneRenderer(world);
        }
    }

    public final Scene createScene(Level world) {
        return createScene(world, false, null);
    }

    /**
     * Sets the core blocks to be rendered in the scene.
     * @param blocks the collection of block positions to be rendered as the core of the scene.
     * @param renderHook an optional render hook that can be used to customize the rendering of the blocks.
     * @return
     */
    public Scene setRenderedCore(Collection<BlockPos> blocks, @Nullable ISceneBlockRenderHook renderHook, boolean autoCamera) {
        if (renderer == null) return this;
        var renderer = this.<WorldSceneRenderer>getRenderer();
        renderer.removeRenderedBlocks(core);
        core.clear();
        core.addAll(blocks);
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos vPos : blocks) {
            minX = Math.min(minX, vPos.getX());
            minY = Math.min(minY, vPos.getY());
            minZ = Math.min(minZ, vPos.getZ());
            maxX = Math.max(maxX, vPos.getX());
            maxY = Math.max(maxY, vPos.getY());
            maxZ = Math.max(maxZ, vPos.getZ());
        }
        center = new Vector3f((minX + maxX) / 2f + 0.5F, (minY + maxY) / 2f + 0.5F, (minZ + maxZ) / 2f + 0.5F);
        renderer.addRenderedBlocks(core, renderHook);
        if (autoCamera) {
            this.zoom = (float) (3.5 * Math.sqrt(Math.max(Math.max(Math.max(maxX - minX + 1, maxY - minY + 1), maxZ - minZ + 1), 1)));
            renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
            renderer.setCameraLookAt(center, camZoom(), Math.toRadians(rotationYaw), Math.toRadians(rotationPitch));
        }
        needCompileCache();
        return this;
    }

    public Scene setRenderedCore(Collection<BlockPos> blocks, @Nullable ISceneBlockRenderHook renderHook) {
        return setRenderedCore(blocks, renderHook, true);
    }

    public Scene setRenderedCore(Collection<BlockPos> blocks) {
        return setRenderedCore(blocks, null);
    }

    // TODO XEI ingredient support


    /// Event handlers
    protected void onMouseDown(UIEvent event) {
        if (!intractable) return;
        if (event.button == 0 && isHover()) {
            if (draggable) {
                dragging = true;
                startDrag(ROTATION_DRAGGING, null);
            }
            lastClickPosFace = lastHoverPosFace;
        } else if (event.button == 2 && isHover()) {
            if (draggable) {
                dragging = true;
                startDrag(PAN_DRAGGING, null);
            }
        }
    }

    protected void onDragSourceUpdate(UIEvent event) {
        if (!intractable || event.target != this || !dragging) return;

        if (event.dragHandler.getDraggingObject() == ROTATION_DRAGGING) {
            var realDelta = getLocalMouseNormal(event.deltaX, event.deltaY);
            rotationYaw += realDelta.x + 360;
            rotationYaw = rotationYaw % 360;
            rotationPitch = (float) Mth.clamp(rotationPitch + realDelta.y, -89.9, 89.9);
            if (renderer != null) {
                this.<WorldSceneRenderer>getRenderer().setCameraLookAt(center, camZoom(), Math.toRadians(rotationYaw), Math.toRadians(rotationPitch));
            }
        } else if (event.dragHandler.getDraggingObject() == PAN_DRAGGING) {
            // Calculate right vector as cross product of world up and camera direction
            var forward = new Vector3f(
                    (float) (Math.cos(Math.toRadians(rotationPitch)) * Math.cos(Math.toRadians(rotationYaw))),
                    (float) Math.sin(Math.toRadians(rotationPitch)),
                    (float) (Math.cos(Math.toRadians(rotationPitch)) * Math.sin(Math.toRadians(rotationYaw)))
            );
            var worldUp = new Vector3f(0, 1, 0);
            var right = new Vector3f();
            forward.cross(worldUp, right);
            right.normalize();
            // Calculate camera up vector
            var up = new Vector3f();
            right.cross(forward, up);
            up.normalize();
            // Move center based on drag delta
            var moveSpeed = zoom * 0.005f;
            var realDelta = getLocalMouseNormal(event.deltaX, event.deltaY);
            center.add(
                    right.x * realDelta.x * moveSpeed + up.x * realDelta.y * moveSpeed,
                    right.y * realDelta.x * moveSpeed + up.y * realDelta.y * moveSpeed,
                    right.z * realDelta.x * moveSpeed + up.z * realDelta.y * moveSpeed
            );
            if (renderer != null) {
                this.<WorldSceneRenderer>getRenderer().setCameraLookAt(center, camZoom(), Math.toRadians(rotationYaw), Math.toRadians(rotationPitch));
            }
        }

    }

    protected void onMouseUp(UIEvent event) {
        dragging = false;
        if (lastHoverPosFace != null && lastHoverPosFace.equals(lastClickPosFace)) {
            lastSelectedPosFace = lastHoverPosFace;
            if (onSelected != null) {
                onSelected.accept(lastSelectedPosFace.pos(), lastSelectedPosFace.facing());
            }
        }
        lastClickPosFace = null;
    }

    protected void onMouseWheel(UIEvent event) {
        if (!intractable || !scalable || event.target != this) return;
        zoom = (float) Mth.clamp(zoom + (event.deltaY < 0 ? 0.5 : -0.5), 0.1, 999);
        if (renderer != null) {
            var renderer = this.<WorldSceneRenderer>getRenderer();
            renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
            renderer.setCameraLookAt(center, camZoom(), Math.toRadians(rotationYaw), Math.toRadians(rotationPitch));
        }
        event.stopPropagation();
    }

    /// Camera control methods
    public Scene setCenter(Vector3f center) {
        this.center = center;
        if (renderer != null) {
            this.<WorldSceneRenderer>getRenderer().setCameraLookAt(this.center, camZoom(), Math.toRadians(rotationYaw), Math.toRadians(rotationPitch));
        }
        return this;
    }

    public Scene setZoom(float zoom) {
        this.zoom = zoom;
        if (renderer != null) {
            var renderer = this.<WorldSceneRenderer>getRenderer();
            renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
            renderer.setCameraLookAt(this.center, camZoom(), Math.toRadians(rotationYaw), Math.toRadians(rotationPitch));
        }
        return this;
    }

    public Scene setOrthoRange(float range) {
        this.range = range;
        if (renderer != null) {
            this.<WorldSceneRenderer>getRenderer().setCameraOrtho(range * zoom, range * zoom, range * zoom);
        }
        return this;
    }

    public Scene setCameraYawAndPitch(float rotationYaw, float rotationPitch) {
        this.rotationYaw = rotationYaw;
        this.rotationPitch = rotationPitch;
        if (renderer != null) {
            this.<WorldSceneRenderer>getRenderer().setCameraLookAt(this.center, camZoom(), Math.toRadians(rotationYaw), Math.toRadians(rotationPitch));
        }
        return this;
    }

    /// Camera animation methods
    protected Interpolator interpolator;
    protected long startTick;

    public void setCameraYawAndPitchAnima(float rotationYaw, float rotationPitch, int dur) {
        if (interpolator != null || getModularUI() == null) return ;
        final float oRotationYaw = this.rotationPitch;
        final float oRotationPitch = this.rotationYaw;
        startTick = getModularUI().getTickCounter();
        interpolator = new Interpolator(0, 1, dur, Eases.QUAD_OUT, value -> {
            this.rotationPitch = (rotationYaw - oRotationYaw) * value.floatValue() + oRotationYaw;
            this.rotationYaw = (rotationPitch - oRotationPitch) * value.floatValue() + oRotationPitch;
            if (renderer != null) {
                this.<WorldSceneRenderer>getRenderer().setCameraLookAt(this.center, camZoom(), Math.toRadians(this.rotationYaw), Math.toRadians(this.rotationPitch));
            }
        }, () -> interpolator = null);
    }

    /// Editor support
    @Override
    public void afterDeserialize() {
        super.afterDeserialize();
        // TODO structure template support
//        if (LDLib2.isRemote()) {
//            if (editorStructureName != null) {
//                var res = Minecraft.getInstance().getResourceManager().getResource(editorStructureName);
//                if (res.isPresent()) {
//                    try (var inputstream = res.get().open()){
//                        try (var datainputstream = new DataInputStream(inputstream)) {
//                            var structureTag = NbtIo.read(datainputstream);
//                            var template = new StructureTemplate();
//                            template.load(BuiltInRegistries.BLOCK.asLookup(), structureTag);
//                        }
//                    } catch (IOException ignored) {}
//                }
//            }
//        }
    }

    @LDLRegisterClient(name = "scene", registry = "ldlib2:ui_element_renderer")
    public static final class SceneRenderer extends DelegatingUIElementRenderer<Scene, SceneRenderer> {
        @Override
        public Class<Scene> type() {
            return Scene.class;
        }

        @Override
        public void drawBackgroundAdditional(Scene scene, IGUIContext context) {
            if (!(context instanceof GUIContext guiContext)) {
                drawParentBackgroundAdditional(scene, context);
                return;
            }
            drawBackgroundAdditional(scene, guiContext);
        }

        static void drawBackgroundAdditional(Scene scene, GUIContext context) {
            var x = scene.getContentX();
            var y = scene.getContentY();
            var width = scene.getContentWidth();
            var height = scene.getPaddingHeight();
            if (scene.interpolator != null && scene.getModularUI() != null) {
                scene.interpolator.update(scene.getModularUI().getTickCounter() + context.partialTick);
            }
            var renderer = scene.<WorldSceneRenderer>getRenderer();
            if (renderer != null) {
                context.addPicturesInPictureState(new SceneRenderState(
                        renderer,
                        x, y, width, height,
                        (int) context.localMouseX, (int) context.localMouseY,
                        context.pose.copyPose(),
                        Mth.floor(x), Mth.floor(y),
                        Mth.ceil(x + width), Mth.ceil(y + height),
                        1.0f,
                        null
                ));
                if (renderer.isCompiling()) {
                    double progress = renderer.getCompileProgress();
                    if (progress > 0) {
                        context.drawTexture(new TextTexture("Renderer is compiling! " + String.format("%.1f", progress * 100) + "%%")
                                .setWidth((int) width), x, y, width, height);
                    }
                }
            }
            if (scene.isHover() && scene.showHoverBlockTips && scene.lastHoverItem != null && scene.getModularUI() != null) {
                ModularUIClientAccess.setHoverTooltip(scene.getModularUI(), HoverTooltips.create(DrawerHelperClient.getItemToolTip(scene.lastHoverItem).toArray()).stack(scene.lastHoverItem));
            }
        }

        public static void configureRenderer(Scene scene, WorldSceneRenderer renderer) {
            renderer.setOnLookingAt(hit -> updateHoverState(scene, hit));
            renderer.setAfterTranslucentDispatch(ctx -> renderOverlay(scene, ctx));
            syncBeforeWorldRender(scene);
        }

        public static void syncBeforeWorldRender(Scene scene) {
            var renderer = scene.<WorldSceneRenderer>getRenderer();
            if (renderer == null) {
                return;
            }
            if (scene.beforeWorldRender != null) {
                renderer.setBeforeWorldRender(s -> scene.beforeWorldRender.accept(scene));
            } else {
                renderer.setBeforeWorldRender(null);
            }
        }

        private static void updateHoverState(Scene scene, net.minecraft.world.phys.BlockHitResult hit) {
            scene.lastHoverPosFace = null;
            scene.lastHoverItem = null;
            if (!scene.isHover() || scene.dummyWorld == null || scene.core == null || scene.core.isEmpty()) {
                return;
            }
            var renderer = scene.<WorldSceneRenderer>getRenderer();
            if (renderer == null) return;

            if (hit != null) {
                if (scene.core.contains(hit.getBlockPos())) {
                    scene.lastHoverPosFace = new BlockPosFace(hit.getBlockPos(), hit.getDirection());
                } else if (!scene.useOrtho) {
                    Vector3f hitPos = hit.getLocation().toVector3f();
                    Level world = renderer.world;
                    Vec3 eyePos = new Vec3(renderer.getEyePos());
                    hitPos.mul(2);
                    Vec3 endPos = new Vec3((hitPos.x - eyePos.x), (hitPos.y - eyePos.y), (hitPos.z - eyePos.z));
                    double min = Float.MAX_VALUE;
                    for (var pos : scene.core) {
                        BlockState blockState = world.getBlockState(pos);
                        if (blockState.getBlock() == Blocks.AIR) continue;
                        var coreHit = world.clipWithInteractionOverride(eyePos, endPos, pos,
                                blockState.getShape(world, pos), blockState);
                        if (coreHit != null && coreHit.getType() != HitResult.Type.MISS) {
                            double dist = eyePos.distanceToSqr(coreHit.getLocation());
                            if (dist < min) {
                                min = dist;
                                scene.lastHoverPosFace = new BlockPosFace(coreHit.getBlockPos(), coreHit.getDirection());
                            }
                        }
                    }
                }
            }
            var mui = scene.getModularUI();
            if (scene.lastHoverPosFace != null && mui != null && mui.player != null) {
                var state = scene.dummyWorld.getBlockState(scene.lastHoverPosFace.pos());
                scene.lastHoverItem = state.getCloneItemStack(scene.dummyWorld, scene.lastHoverPosFace.pos(), false);
            }
        }

        private static void renderOverlay(Scene scene, SceneRenderContext ctx) {
            var bs = ctx.bufferSource();
            if (scene.lastSelectedPosFace != null && scene.renderSelect) {
                RenderUtils.renderBlockOverLay(new PoseStack(), bs, scene.lastSelectedPosFace.pos(),
                        0.6f, 0, 0, 1.01f);
            }
            var tmp = scene.dragging ? scene.lastClickPosFace : scene.lastHoverPosFace;
            if (scene.renderFacing) {
                if (scene.lastSelectedPosFace != null) {
                    drawFacingBorder(bs, scene.lastSelectedPosFace, 0xff00ff00);
                }
                if (tmp != null && !tmp.equals(scene.lastSelectedPosFace)) {
                    drawFacingBorder(bs, tmp, 0xffffffff);
                }
            }
            if (scene.afterWorldRender != null) {
                scene.afterWorldRender.accept(scene);
            }
            if (scene.overlay != null) {
                scene.overlay.apply(ctx);
            }
        }

        private static void drawFacingBorder(net.minecraft.client.renderer.MultiBufferSource.BufferSource bs,
                                             BlockPosFace posFace, int color) {
            var poseStack = new PoseStack();
            RenderUtils.moveToFace(poseStack, posFace.pos().getX(), posFace.pos().getY(), posFace.pos().getZ(), posFace.facing());
            RenderUtils.rotateToFace(poseStack, posFace.facing(), null);
            poseStack.scale(1f / 16, 1f / 16, 0);
            poseStack.translate(-8, -8, 0);
            var vc = bs.getBuffer(TransformGizmo.POSITION_COLOR_NO_DEPTH);
            drawBorder(poseStack.last().pose(), vc, 1, 1, 14, 14, color, 1);
        }

        private static void drawBorder(org.joml.Matrix4f pose, VertexConsumer vc,
                                       int x, int y, int width, int height, int color, int border) {
            fill(pose, vc, x - border, y - border, x + width + border, y + 0, color);
            fill(pose, vc, x - border, y + height, x + width + border, y + height + border, color);
            fill(pose, vc, x - border, y, x, y + height, color);
            fill(pose, vc, x + width, y, x + width + border, y + height, color);
        }

        private static void fill(org.joml.Matrix4f pose, VertexConsumer vc, int x1, int y1, int x2, int y2, int color) {
            if (x1 > x2) { int t = x1; x1 = x2; x2 = t; }
            if (y1 > y2) { int t = y1; y1 = y2; y2 = t; }
            vc.addVertex(pose, x1, y1, 0).setColor(color);
            vc.addVertex(pose, x1, y2, 0).setColor(color);
            vc.addVertex(pose, x2, y2, 0).setColor(color);
            vc.addVertex(pose, x1, y1, 0).setColor(color);
            vc.addVertex(pose, x2, y2, 0).setColor(color);
            vc.addVertex(pose, x2, y1, 0).setColor(color);
        }
    }
}
