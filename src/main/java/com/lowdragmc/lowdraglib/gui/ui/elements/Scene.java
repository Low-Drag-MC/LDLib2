package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.lowdragmc.lowdraglib.client.scene.*;
import com.lowdragmc.lowdraglib.client.utils.RenderUtils;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.math.Size;
import com.lowdragmc.lowdraglib.math.interpolate.Eases;
import com.lowdragmc.lowdraglib.math.interpolate.Interpolator;
import com.lowdragmc.lowdraglib.utils.data.BlockPosFace;
import com.lowdragmc.lowdraglib.utils.virtuallevel.TrackedDummyWorld;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.appliedenergistics.yoga.YogaOverflow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Accessors(chain = true)
public class Scene extends UIElement {
    private static Object DRAGGING = new Object();
    @Nullable
    @Getter
    protected WorldSceneRenderer renderer;
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
    protected Vector3f center;
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
    @Getter
    protected final Set<BlockPos> core = new HashSet<>();
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
        setOverflow(YogaOverflow.HIDDEN);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.MOUSE_UP, this::onMouseUp);
        addEventListener(UIEvents.MOUSE_WHEEL, this::onMouseWheel);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSourceUpdate);
    }

    public Scene useCacheBuffer() {
        return useCacheBuffer(true);
    }

    public Scene useCacheBuffer(boolean cacheBuffer) {
        useCache = cacheBuffer;
        if (renderer != null) {
            renderer.useCacheBuffer(true);
        }
        return this;
    }

    public Scene useOrtho() {
        return useOrtho(true);
    }

    public Scene useOrtho(boolean useOrtho) {
        this.useOrtho = useOrtho;
        if (renderer != null) {
            renderer.useOrtho(useOrtho);
            renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
            renderer.setCameraLookAt(center, camZoom(), Math.toRadians(rotationYaw), Math.toRadians(rotationPitch));
        }
        return this;
    }

    public Scene setBeforeWorldRender(Consumer<Scene> beforeWorldRender) {
        this.beforeWorldRender = beforeWorldRender;
        if (this.beforeWorldRender != null && renderer != null) {
            renderer.setBeforeWorldRender(s -> beforeWorldRender.accept(this));
        }
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
    public void onRemoved() {
        super.onRemoved();
        if (autoReleased) {
            releaseRendererResource();
        }
    }

    @Nullable
    public ParticleManager getParticleManager() {
        if (renderer == null) return null;
        return renderer.getParticleManager();
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
            var _renderer = renderer;
            if (RenderSystem.isOnRenderThread()) {
                _renderer.releaseResource();
            } else {
                RenderSystem.recordRenderCall(_renderer::releaseResource);
            }
        }
    }

    public void needCompileCache() {
        if (renderer != null) {
            renderer.needCompileCache();
        }
    }


    protected ParticleManager createParticleManager() {
        return new ParticleManager();
    }

    /**
     * Creates a scene with the given world and whether to use FBO scene renderer.
     */
    public final Scene createScene(@Nonnull Level world, boolean useFBOSceneRenderer, @Nullable Size fboSize) {
        releaseRendererResource();
        core.clear();
        level = world;
        dummyWorld = world instanceof TrackedDummyWorld trackedLevel ? trackedLevel : new TrackedDummyWorld(world);
        //compute window size from scaled width & height
        final WorldSceneRenderer renderer = useFBOSceneRenderer ?
                new FBOWorldSceneRenderer(dummyWorld, fboSize == null ? 1080 : fboSize.width, fboSize == null ? 1080 : fboSize.height) :
                new ImmediateWorldSceneRenderer(dummyWorld);
        this.renderer = renderer;

        dummyWorld.setBlockFilter(pos -> renderer.renderedBlocksMap.keySet().stream().anyMatch(c -> c.contains(pos)));
        center = new Vector3f(0, 0, 0);
        renderer.useOrtho(useOrtho);
        renderer.setOnLookingAt(ray -> {});
        renderer.setBeforeBatchEnd(this::renderBeforeBatchEnd);
        renderer.setAfterWorldRender(this::renderBlockOverLay);
        if (this.beforeWorldRender != null) {
            renderer.setBeforeWorldRender(s -> beforeWorldRender.accept(this));
        }
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

    public final Scene createScene(Level world) {
        return createScene(world, false, null);
    }


    /**
     * Sets the core blocks to be rendered in the scene.
     * @param blocks the collection of block positions to be rendered as the core of the scene.
     * @param renderHook an optional render hook that can be used to customize the rendering of the blocks.
     * @return
     */
    public Scene setRenderedCore(Collection<BlockPos> blocks, ISceneBlockRenderHook renderHook, boolean autoCamera) {
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

    public Scene setRenderedCore(Collection<BlockPos> blocks, ISceneBlockRenderHook renderHook) {
        return setRenderedCore(blocks, renderHook, true);
    }

    public Scene setRenderedCore(Collection<BlockPos> blocks) {
        return setRenderedCore(blocks, null);
    }

    protected void renderBeforeBatchEnd(MultiBufferSource bufferSource, float partialTicks) {
    }

    public void renderBlockOverLay(WorldSceneRenderer renderer) {
        if (renderer == null || dummyWorld == null || core == null || core.isEmpty()) {
            return;
        }
        var poseStack = new PoseStack();
        lastHoverPosFace = null;
        lastHoverItem = null;
        if (isHover()) {
            var hit = renderer.getLastTraceResult();
            if (hit != null) {
                if (core.contains(hit.getBlockPos())) {
                    lastHoverPosFace = new BlockPosFace(hit.getBlockPos(), hit.getDirection());
                } else if (!useOrtho) {
                    Vector3f hitPos = hit.getLocation().toVector3f();
                    Level world = renderer.world;
                    Vec3 eyePos = new Vec3(renderer.getEyePos());
                    hitPos.mul(2); // Double view range to ensure pos can be seen.
                    Vec3 endPos = new Vec3((hitPos.x - eyePos.x), (hitPos.y - eyePos.y), (hitPos.z - eyePos.z));
                    double min = Float.MAX_VALUE;
                    for (BlockPos pos : core) {
                        BlockState blockState = world.getBlockState(pos);
                        if (blockState.getBlock() == Blocks.AIR) {
                            continue;
                        }
                        hit = world.clipWithInteractionOverride(eyePos, endPos, pos, blockState.getShape(world, pos), blockState);
                        if (hit != null && hit.getType() != HitResult.Type.MISS) {
                            double dist = eyePos.distanceToSqr(hit.getLocation());
                            if (dist < min) {
                                min = dist;
                                lastHoverPosFace = new BlockPosFace(hit.getBlockPos(), hit.getDirection());
                            }
                        }
                    }
                }
            }
            if (lastHoverPosFace != null && hit != null) {
                var state = dummyWorld.getBlockState(lastHoverPosFace.pos());
                lastHoverItem = state.getBlock().getCloneItemStack(state, hit, dummyWorld, lastHoverPosFace.pos(),
                        Minecraft.getInstance().player);
            }
        }

        var tmp = dragging ? lastClickPosFace : lastHoverPosFace;
        if (lastSelectedPosFace != null || tmp != null) {
            if (lastSelectedPosFace != null && renderFacing) {
                drawFacingBorder(poseStack, lastSelectedPosFace, 0xff00ff00);
            }
            if (tmp != null && !tmp.equals(lastSelectedPosFace) && renderFacing) {
                drawFacingBorder(poseStack, tmp, 0xffffffff);
            }
        }
        if (lastSelectedPosFace != null && renderSelect) {
            RenderUtils.renderBlockOverLay(poseStack, lastSelectedPosFace.pos(), 0.6f, 0, 0, 1.01f);
        }

        if (this.afterWorldRender != null) {
            this.afterWorldRender.accept(this);
        }
    }

    public void drawFacingBorder(PoseStack poseStack, BlockPosFace posFace, int color) {
        drawFacingBorder(poseStack, posFace, color, 0);
    }

    public void drawFacingBorder(PoseStack poseStack, BlockPosFace posFace, int color, int inner) {
        poseStack.pushPose();
        RenderSystem.disableDepthTest();
        RenderUtils.moveToFace(poseStack, posFace.pos().getX(), posFace.pos().getY(), posFace.pos().getZ(), posFace.facing());
        RenderUtils.rotateToFace(poseStack, posFace.facing(), null);
        poseStack.scale(1f / 16, 1f / 16, 0);
        poseStack.translate(-8, -8, 0);
        drawBorder(poseStack, 1 + inner * 2, 1 + inner * 2, 14 - 4 * inner, 14 - 4 * inner, color, 1);
        RenderSystem.enableDepthTest();
        poseStack.popPose();
    }

    private static void drawBorder(PoseStack poseStack, int x, int y, int width, int height, int color, int border) {
        drawSolidRect(poseStack,x - border, y - border, width + 2 * border, border, color);
        drawSolidRect(poseStack,x - border, y + height, width + 2 * border, border, color);
        drawSolidRect(poseStack,x - border, y, border, height, color);
        drawSolidRect(poseStack,x + width, y, border, height, color);
    }

    private static void drawSolidRect(PoseStack poseStack, int x, int y, int width, int height, int color) {
        fill(poseStack, x, y, x + width, y + height, 0, color);
        RenderSystem.enableBlend();
    }

    private static void fill(PoseStack matrices, int x1, int y1, int x2, int y2, int z, int color) {
        Matrix4f matrix4f = matrices.last().pose();
        int i;
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }

        float f = (float) FastColor.ARGB32.alpha(color) / 255.0F;
        float g = (float) FastColor.ARGB32.red(color) / 255.0F;
        float h = (float) FastColor.ARGB32.green(color) / 255.0F;
        float j = (float) FastColor.ARGB32.blue(color) / 255.0F;
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.addVertex(matrix4f, (float)x1, (float)y1, (float)z).setColor(g, h, j, f);
        bufferBuilder.addVertex(matrix4f, (float)x1, (float)y2, (float)z).setColor(g, h, j, f);
        bufferBuilder.addVertex(matrix4f, (float)x2, (float)y2, (float)z).setColor(g, h, j, f);
        bufferBuilder.addVertex(matrix4f, (float)x2, (float)y1, (float)z).setColor(g, h, j, f);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        RenderSystem.disableBlend();
    }

    // TODO XEI ingredient support


    /// Event handlers
    protected void onMouseDown(UIEvent event) {
        if (!intractable) return;
        if (isHover()) {
            if (draggable) {
                dragging = true;
                startDrag(DRAGGING, null);
            }
            lastClickPosFace = lastHoverPosFace;
        }
    }

    protected void onDragSourceUpdate(UIEvent event) {
        if (!intractable || event.target != this || event.dragHandler.getDraggingObject() != DRAGGING || !dragging) return;
        rotationYaw += event.deltaX + 360;
        rotationYaw = rotationYaw % 360;
        rotationPitch = (float) Mth.clamp(rotationPitch + event.deltaY, -89.9, 89.9);
        if (renderer != null) {
            renderer.setCameraLookAt(center, camZoom(), Math.toRadians(rotationYaw), Math.toRadians(rotationPitch));
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
            renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
            renderer.setCameraLookAt(center, camZoom(), Math.toRadians(rotationYaw), Math.toRadians(rotationPitch));
        }
    }

    @Override
    public void drawBackgroundAdditional(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var x = getContentX();
        var y = getContentY();
        var width = getContentWidth();
        var height = getPaddingHeight();
        if (interpolator != null && getModularUI() != null) {
            interpolator.update(getModularUI().getTickCounter() + partialTicks);
        }
        if (renderer != null) {
            renderer.render(graphics.pose(), x, y, width, height, mouseX, mouseY);
            if (renderer.isCompiling()) {
                double progress = renderer.getCompileProgress();
                if (progress > 0) {
                    new TextTexture("Renderer is compiling! " + String.format("%.1f", progress * 100) + "%%")
                            .setWidth((int) width)
                            .draw(graphics, mouseX, mouseY, x, y, width, height, partialTicks);
                }
            }
        }
    }

    @Override
    public void drawInForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
        if (isHover() && showHoverBlockTips && lastHoverItem != null && getModularUI() != null) {
            getModularUI().setHoverTooltip(DrawerHelper.getItemToolTip(lastHoverItem), lastHoverItem, null, lastHoverItem.getTooltipImage().orElse(null));
        }
    }

    /// Camera control methods
    public Scene setCenter(Vector3f center) {
        this.center = center;
        if (renderer != null) {
            renderer.setCameraLookAt(this.center, camZoom(), Math.toRadians(rotationYaw), Math.toRadians(rotationPitch));
        }
        return this;
    }

    public Scene setZoom(float zoom) {
        this.zoom = zoom;
        if (renderer != null) {
            renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
            renderer.setCameraLookAt(this.center, camZoom(), Math.toRadians(rotationYaw), Math.toRadians(rotationPitch));
        }
        return this;
    }

    public Scene setOrthoRange(float range) {
        this.range = range;
        if (renderer != null) {
            renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
        }
        return this;
    }

    public Scene setCameraYawAndPitch(float rotationYaw, float rotationPitch) {
        this.rotationPitch = rotationYaw;
        this.rotationYaw = rotationPitch;
        if (renderer != null) {
            renderer.setCameraLookAt(this.center, camZoom(), Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
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
        interpolator = new Interpolator(0, 1, dur, Eases.EaseQuadOut, value -> {
            this.rotationPitch = (rotationYaw - oRotationYaw) * value.floatValue() + oRotationYaw;
            this.rotationYaw = (rotationPitch - oRotationPitch) * value.floatValue() + oRotationPitch;
            if (renderer != null) {
                renderer.setCameraLookAt(this.center, camZoom(), Math.toRadians(this.rotationYaw), Math.toRadians(this.rotationPitch));
            }
        }, x -> interpolator = null);
    }
}
