package com.lowdragmc.lowdraglib.gui.widget;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.client.scene.*;
import com.lowdragmc.lowdraglib.client.utils.RenderUtils;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.jei.JEIPlugin;
import com.lowdragmc.lowdraglib.utils.BlockPosFace;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.lowdraglib.utils.interpolate.Eases;
import com.lowdragmc.lowdraglib.utils.interpolate.Interpolator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.emi.emi.api.stack.EmiStack;
import lombok.Getter;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "UnusedReturnValue", "SameParameterValue"})
public class SceneWidget extends WidgetGroup {
    @OnlyIn(Dist.CLIENT)
    protected WorldSceneRenderer renderer;
    @OnlyIn(Dist.CLIENT)
    protected TrackedDummyWorld dummyWorld;
    @Getter
    protected boolean dragging;
    @Getter
    protected boolean renderFacing = true;
    @Getter
    protected boolean renderSelect = true;
    @Getter
    protected boolean draggable = true;
    @Getter
    protected boolean scalable = true;
    @Getter
    protected boolean intractable = true;
    @Getter
    protected boolean hoverTips;
    @Getter
    protected int currentMouseX;
    @Getter
    protected int currentMouseY;
    @Getter
    protected Vector3f center;
    @Getter
    protected float rotationYaw = 25;
    @Getter
    protected float rotationPitch = -135;
    @Getter
    protected float zoom = 5;
    @Getter
    protected float range;
    @Getter
    protected BlockPosFace clickPosFace;
    @Getter
    protected BlockPosFace hoverPosFace;
    @Getter
    protected BlockPosFace selectedPosFace;
    @Getter
    protected ItemStack hoverItem;
    @Getter
    protected BiConsumer<BlockPos, Direction> onSelected;
    @Getter
    protected Set<BlockPos> core;
    @Getter
    protected boolean useCache;
    @Getter
    protected boolean useOrtho = false;
    @Getter
    protected boolean autoReleased;
    protected BiConsumer<SceneWidget, List<Component>> onAddedTooltips;
    protected Consumer<SceneWidget> beforeWorldRender;
    protected Consumer<SceneWidget> afterWorldRender;

    public SceneWidget(int x, int y, int width, int height, Level world, boolean useFBO) {
        super(x, y, width, height);
        if (isRemote()) {
            createScene(world, useFBO);
        }
    }

    public SceneWidget(int x, int y, int width, int height, Level world) {
        super(x, y, width, height);
        if (isRemote()) {
            createScene(world);
        }
    }

    public SceneWidget setOnAddedTooltips(BiConsumer<SceneWidget, List<Component>> onAddedTooltips) {
        this.onAddedTooltips = onAddedTooltips;
        return this;
    }

    public SceneWidget useCacheBuffer() {
        return useCacheBuffer(true);
    }

    public SceneWidget useCacheBuffer(boolean autoReleased) {
        useCache = true;
        this.autoReleased = autoReleased;
        if (isRemote() && renderer != null) {
            renderer.useCacheBuffer(true);
        }
        return this;
    }

    public SceneWidget useOrtho() {
        return useOrtho(true);
    }

    public SceneWidget useOrtho(boolean useOrtho) {
        this.useOrtho = useOrtho;
        if (isRemote() && renderer != null) {
            renderer.useOrtho(useOrtho);
        }
        return this;
    }

    public SceneWidget setBeforeWorldRender(Consumer<SceneWidget> beforeWorldRender) {
        this.beforeWorldRender = beforeWorldRender;
        if (this.beforeWorldRender != null && isRemote() && renderer != null) {
            renderer.setBeforeWorldRender(s -> beforeWorldRender.accept(this));
        }
        return this;
    }

    public SceneWidget setAfterWorldRender(Consumer<SceneWidget> afterWorldRender) {
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

    @OnlyIn(Dist.CLIENT)
    public ParticleManager getParticleManager() {
        if (renderer == null) return null;
        return renderer.getParticleManager();
    }

    @Override
    public void setGui(ModularUI gui) {
        super.setGui(gui);
        if (gui == null) {
            if (isInitialized()) {
                releaseCacheBuffer();
            }
        } else {
            gui.registerCloseListener(this::releaseCacheBuffer);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void updateScreen() {
        super.updateScreen();
        ParticleManager particleManager = getParticleManager();
        if (particleManager != null) {
            particleManager.tick();
        }
    }

    public void releaseCacheBuffer() {
        if (isRemote() && renderer != null && autoReleased) {
            renderer.deleteCacheBuffer();
        }
    }

    public void needCompileCache() {
        if (isRemote() && renderer != null) {
            renderer.needCompileCache();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public final void createScene(Level world) {
        createScene(world, false);
    }

    @OnlyIn(Dist.CLIENT)
    protected ParticleManager createParticleManager() {
        return new ParticleManager();
    }

    @OnlyIn(Dist.CLIENT)
    public final void createScene(Level world, boolean useFBOSceneRenderer) {
        if (world == null) return;
        core = new HashSet<>();
        dummyWorld = new TrackedDummyWorld(world);
        dummyWorld.setRenderFilter(pos -> renderer.renderedBlocksMap.keySet().stream().anyMatch(c -> c.contains(pos)));
        if (renderer != null) {
            renderer.deleteCacheBuffer();
        }
        if (useFBOSceneRenderer) {
            renderer = new FBOWorldSceneRenderer(dummyWorld,1080, 1080);
        } else {
            renderer = new ImmediateWorldSceneRenderer(dummyWorld);
        }
        center = new Vector3f(0, 0, 0);
        renderer.useOrtho(useOrtho);
        renderer.setOnLookingAt(ray -> {});
        renderer.setBeforeBatchEnd(this::renderBeforeBatchEnd);
        renderer.setAfterWorldRender(this::renderBlockOverLay);
        if (this.beforeWorldRender != null) {
            renderer.setBeforeWorldRender(s -> beforeWorldRender.accept(this));
        }
        renderer.setCameraLookAt(center, camZoom(), Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
        renderer.useCacheBuffer(useCache);
        renderer.setParticleManager(createParticleManager());
        clickPosFace = null;
        hoverPosFace = null;
        hoverItem = null;
        selectedPosFace = null;
    }

    @OnlyIn(Dist.CLIENT)
    public WorldSceneRenderer getRenderer() {
        return renderer;
    }

    @OnlyIn(Dist.CLIENT)
    public TrackedDummyWorld getDummyWorld() {
        return dummyWorld;
    }

    public SceneWidget setOnSelected(BiConsumer<BlockPos, Direction> onSelected) {
        this.onSelected = onSelected;
        return this;
    }

    public SceneWidget setClearColor(int color) {
        if (isRemote()) {
            renderer.setClearColor(color);
        }
        return this;
    }


    public SceneWidget setRenderSelect(boolean renderSelect) {
        this.renderSelect = renderSelect;
        return this;
    }

    public SceneWidget setRenderFacing(boolean renderFacing) {
        this.renderFacing = renderFacing;
        return this;
    }

    public SceneWidget setDraggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    public SceneWidget setScalable(boolean scalable) {
        this.scalable = scalable;
        return this;
    }

    public SceneWidget setIntractable(boolean intractable) {
        this.intractable = intractable;
        return this;
    }

    public SceneWidget setHoverTips(boolean hoverTips) {
        this.hoverTips = hoverTips;
        return this;
    }

    public SceneWidget setRenderedCore(Collection<BlockPos> blocks) {
        return setRenderedCore(blocks, null);
    }

    public SceneWidget setRenderedCore(Collection<BlockPos> blocks, ISceneBlockRenderHook renderHook) {
        if (isRemote()) {
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
            this.zoom = (float) (3.5 * Math.sqrt(Math.max(Math.max(Math.max(maxX - minX + 1, maxY - minY + 1), maxZ - minZ + 1), 1)));
            renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
            renderer.setCameraLookAt(center, camZoom(), Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
            needCompileCache();
        }
        return this;
    }

    private List<Component> getToolTips(List<Component> list) {
        if (this.onAddedTooltips != null) {
            this.onAddedTooltips.accept(this, list);
        }
        return list;
    }

    @OnlyIn(Dist.CLIENT)
    protected void renderBeforeBatchEnd(MultiBufferSource bufferSource, float partialTicks) {
    }

    @OnlyIn(Dist.CLIENT)
    public void renderBlockOverLay(WorldSceneRenderer renderer) {
        PoseStack poseStack = new PoseStack();
        hoverPosFace = null;
        hoverItem = null;
        if (isMouseOverElement(currentMouseX, currentMouseY)) {
            BlockHitResult hit = renderer.getLastTraceResult();
            if (hit != null) {
                if (core.contains(hit.getBlockPos())) {
                    hoverPosFace = new BlockPosFace(hit.getBlockPos(), hit.getDirection());
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
                                hoverPosFace = new BlockPosFace(hit.getBlockPos(), hit.getDirection());
                            }
                        }
                    }
                }
            }
        }
        if (hoverPosFace != null) {
            var state = getDummyWorld().getBlockState(hoverPosFace.pos());
            hoverItem = state.getBlock().getCloneItemStack(getDummyWorld(), hoverPosFace.pos(), state);
        }
        BlockPosFace tmp = dragging ? clickPosFace : hoverPosFace;
        if (selectedPosFace != null || tmp != null) {
            if (selectedPosFace != null && renderFacing) {
                drawFacingBorder(poseStack, selectedPosFace, 0xff00ff00);
            }
            if (tmp != null && !tmp.equals(selectedPosFace) && renderFacing) {
                drawFacingBorder(poseStack, tmp, 0xffffffff);
            }
        }
        if (selectedPosFace != null && renderSelect) {
            RenderUtils.renderBlockOverLay(poseStack, selectedPosFace.pos(), 0.6f, 0, 0, 1.01f);
        }

        if (this.afterWorldRender != null) {
            this.afterWorldRender.accept(this);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void drawFacingBorder(PoseStack poseStack, BlockPosFace posFace, int color) {
        drawFacingBorder(poseStack, posFace, color, 0);
    }

    @OnlyIn(Dist.CLIENT)
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

    @OnlyIn(Dist.CLIENT)
    private static void drawBorder(PoseStack poseStack, int x, int y, int width, int height, int color, int border) {
        drawSolidRect(poseStack,x - border, y - border, width + 2 * border, border, color);
        drawSolidRect(poseStack,x - border, y + height, width + 2 * border, border, color);
        drawSolidRect(poseStack,x - border, y, border, height, color);
        drawSolidRect(poseStack,x + width, y, border, height, color);
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawSolidRect(PoseStack poseStack, int x, int y, int width, int height, int color) {
        fill(poseStack, x, y, x + width, y + height, 0, color);
        RenderSystem.enableBlend();
    }

    @OnlyIn(Dist.CLIENT)
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

    @Override
    public Object getXEIIngredientOverMouse(double mouseX, double mouseY) {
        Object result = super.getXEIIngredientOverMouse(mouseX, mouseY);
        if (result == null && hoverItem != null && !hoverItem.isEmpty()) {
            if (LDLib.isJeiLoaded()) {
                return JEIPlugin.getItemIngredient(hoverItem, (int) mouseX, (int) mouseY, 1, 1);
            }
            if (LDLib.isReiLoaded()) {
                return EntryStacks.of(hoverItem);
            }
            if (LDLib.isEmiLoaded()) {
                return EmiStack.of(hoverItem);
            }
            return hoverItem;
        }
        return result;
    }

    @Override
    public void handleClientAction(int id, RegistryFriendlyByteBuf buffer) {
        if (id == -1) {
            selectedPosFace = new BlockPosFace(buffer.readBlockPos(), buffer.readEnum(Direction.class));
            if (onSelected != null) {
                onSelected.accept(selectedPosFace.pos(), selectedPosFace.facing());
            }
        } else {
            super.handleClientAction(id, buffer);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (!intractable) return false;
        if (isMouseOverElement(mouseX, mouseY)) {
            if (draggable) {
                dragging = true;
            }
            clickPosFace = hoverPosFace;
            return true;
        }
        dragging = false;
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseWheelMove(double mouseX, double mouseY, double scrollX, double scrollY) {
        var result = super.mouseWheelMove(mouseX, mouseY, scrollX, scrollY);
        if (!result && isMouseOverElement(mouseX, mouseY) && scalable) {
            zoom = (float) Mth.clamp(zoom + (scrollY < 0 ? 0.5 : -0.5), 0.1, 999);
            if (renderer != null) {
                renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
                renderer.setCameraLookAt(center, camZoom(), Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
            }
            return true;
        }
        return result;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!intractable) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (dragging) {
            rotationPitch += dragX + 360;
            rotationPitch = rotationPitch % 360;
            rotationYaw = (float) Mth.clamp(rotationYaw + dragY, -89.9, 89.9);
            if (renderer != null) {
                renderer.setCameraLookAt(center, camZoom(), Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
            }
            return false;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!intractable) return super.mouseReleased(mouseX, mouseY, button);
        dragging = false;
        if (hoverPosFace != null && hoverPosFace.equals(clickPosFace)) {
            selectedPosFace = hoverPosFace;
            writeClientAction(-1, buffer -> {
                buffer.writeBlockPos(selectedPosFace.pos());
                buffer.writeEnum(selectedPosFace.facing());
            });
            if (onSelected != null) {
                onSelected.accept(selectedPosFace.pos(), selectedPosFace.facing());
            }
            clickPosFace = null;
            return true;
        }
        clickPosFace = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@NotNull @Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (hoverTips && isMouseOverElement(mouseX, mouseY)) {
            if (hoverItem != null && !hoverItem.isEmpty()) {
                gui.getModularUIGui().setHoverTooltip(getToolTips(DrawerHelper.getItemToolTip(hoverItem)), hoverItem, null, hoverItem.getTooltipImage().orElse(null));
            }
        }
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // draw background
        drawBackgroundTexture(graphics, mouseX, mouseY);

        int x = getPosition().x;
        int y = getPosition().y;
        int width = getSize().width;
        int height = getSize().height;
        if (interpolator != null) {
            interpolator.update(gui.getTickCount() + partialTicks);
        }
        if (renderer != null) {
            renderer.render(graphics.pose(), x, y, width, height, mouseX, mouseY);
            if (renderer.isCompiling()) {
                double progress = renderer.getCompileProgress();
                if (progress > 0) {
                    new TextTexture("Renderer is compiling! " + String.format("%.1f", progress * 100) + "%%").setWidth(width).draw(graphics, mouseX, mouseY, x, y, width, height);
                }
            }
        }

        // draw widgets
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        drawWidgetsBackground(graphics, mouseX, mouseY, partialTicks);
        drawOverlay(graphics, mouseX, mouseY, partialTicks);
        currentMouseX = mouseX;
        currentMouseY = mouseY;
    }

    public SceneWidget setCenter(Vector3f center) {
        this.center = center;
        if (renderer != null) {
            renderer.setCameraLookAt(this.center, camZoom(), Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
        }
        return this;
    }

    public SceneWidget setZoom(float zoom) {
        this.zoom = zoom;
        if (renderer != null) {
            renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
            renderer.setCameraLookAt(this.center, camZoom(), Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
        }
        return this;
    }

    public SceneWidget setOrthoRange(float range) {
        this.range = range;
        if (renderer != null) {
            renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
        }
        return this;
    }

    public SceneWidget setCameraYawAndPitch(float rotationYaw, float rotationPitch) {
        this.rotationYaw = rotationYaw;
        this.rotationPitch = rotationPitch;
        if (renderer != null) {
            renderer.setCameraLookAt(this.center, camZoom(), Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
        }
        return this;
    }

    protected Interpolator interpolator;
    protected long startTick;

    public void setCameraYawAndPitchAnima(float rotationYaw, float rotationPitch, int dur) {
        if (interpolator != null) return ;
        final float oRotationYaw = this.rotationYaw;
        final float oRotationPitch = this.rotationPitch;
        startTick = gui.getTickCount();
        interpolator = new Interpolator(0, 1, dur, Eases.EaseQuadOut, value -> {
            this.rotationYaw = (rotationYaw - oRotationYaw) * value.floatValue() + oRotationYaw;
            this.rotationPitch = (rotationPitch - oRotationPitch) * value.floatValue() + oRotationPitch;
            if (renderer != null) {
                renderer.setCameraLookAt(this.center, camZoom(), Math.toRadians(this.rotationPitch), Math.toRadians(this.rotationYaw));
            }
        }, x -> interpolator = null);
    }

}
