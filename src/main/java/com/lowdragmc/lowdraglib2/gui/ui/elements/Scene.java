package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.client.scene.*;
import com.lowdragmc.lowdraglib2.client.utils.RenderUtils;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.lowdragmc.lowdraglib2.math.interpolate.Interpolator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.data.BlockPosFace;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
    protected boolean useOrtho = false;
    @Getter
    protected boolean autoReleased = true;
    @Getter @Setter
    protected boolean tickWorld = true;
    protected Consumer<Scene> beforeWorldRender;
    protected Consumer<Scene> afterWorldRender;
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
}
