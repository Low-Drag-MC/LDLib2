package com.lowdragmc.lowdraglib.compass.component.animation;

import com.lowdragmc.lowdraglib.client.scene.ISceneBlockRenderHook;
import com.lowdragmc.lowdraglib.client.scene.ISceneEntityRenderHook;
import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.client.utils.RenderUtils;
import com.lowdragmc.lowdraglib.gui.animation.Transform;
import com.lowdragmc.lowdraglib.compass.CompassManager;
import com.lowdragmc.lowdraglib.compass.component.CompassComponent;
import com.lowdragmc.lowdraglib.gui.ColorPattern;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.math.Position;
import com.lowdragmc.lowdraglib.utils.*;
import com.lowdragmc.lowdraglib.math.interpolate.Eases;
import com.lowdragmc.lowdraglib.utils.data.BlockInfo;
import com.lowdragmc.lowdraglib.utils.data.BlockPosFace;
import com.lowdragmc.lowdraglib.utils.data.EntityInfo;
import com.lowdragmc.lowdraglib.utils.virtuallevel.TrackedDummyWorld;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.val;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author KilaBash
 * @date 2023/7/28
 * @implNote AnimationScene
 */
@OnlyIn(Dist.CLIENT)
public class CompassScene extends WidgetGroup implements ISceneBlockRenderHook, ISceneEntityRenderHook {
    @Getter
    protected final SceneWidget sceneWidget;
    @Getter
    protected final WidgetGroup headerGroup;
    @Getter
    protected final int minX, minY = 0, minZ, maxX, maxY = 0, maxZ;
    @Getter
    protected final List<AnimationFrame> frames;
    protected final boolean useScene, tickScene;
    protected TrackedDummyWorld world;
    //runtime
    private final Map<BlockPos, Tuple<BlockAnima, Integer>> addedBlocks = new HashMap<>();
    private final Map<BlockPos, Tuple<BlockAnima, Integer>> removedBlocks = new HashMap<>();
    private final Map<BlockPosFace, Integer> highlightBlocks = new HashMap<>();
    private final Map<Vector3f, MutableTriple<Tuple<XmlUtils.SizedIngredient, List<Component>>, Vec2, Integer>> tooltipBlocks = new HashMap<>();
    private final Map<Vector3f, Vec2> tooltipPos = new HashMap<>();
    private int currentFrame = -1;
    private int frameTick = 0;
    private boolean isPause;

    public CompassScene(int width, CompassComponent component) {
        super(0, 0, width, component.getHeight());
        this.frames = component.getFrames();
        this.useScene = component.isUseScene();
        this.tickScene = component.isTickScene();
        this.minX = -component.getRange();
        this.minZ = -component.getRange();
        this.maxX = component.getRange();
        this.maxZ = component.getRange();
        var height = component.getHeight();
        var headerHeight = 80;
        var sceneHeight = height - headerHeight;
        if (useScene) {
            int sw = (sceneHeight * 4);
            sceneWidget = new SceneWidget((width - sw) / 2, headerHeight, sw, sceneHeight, world = new TrackedDummyWorld());
            sceneWidget.setHoverTips(true)
                    .useOrtho(component.isOrtho())
                    .setOrthoRange(0.5f)
                    .setScalable(component.isScalable())
                    .setDraggable(false)
                    .setRenderFacing(false)
                    .setRenderSelect(false);

            sceneWidget.getRenderer().setFov(30);
            sceneWidget.setRenderedCore(List.of(BlockPos.ZERO), this);
            sceneWidget.getRenderer().setSceneEntityRenderHook(this);
            if (component.getZoom() > 0) {
                sceneWidget.setZoom(component.getZoom());
            } else {
                sceneWidget.setZoom((9 * Mth.sqrt(maxX - minX)));
            }

            sceneWidget.setBeforeWorldRender(this::renderBeforeWorld);
            sceneWidget.setAfterWorldRender(this::renderAfterWorld);
            sceneWidget.setCameraYawAndPitch(component.getYaw(), sceneWidget.getRotationPitch());
            addWidget(sceneWidget);
        } else {
            sceneWidget = null;
        }
        addWidget(headerGroup = new WidgetGroup(0, 0, width, useScene ? headerHeight : (height - 25)));
        addWidget(new ButtonWidget((width - 12) / 2 + 20, height - 20, 12, 12, Icons.REPLAY, this::replay).setHoverTexture(Icons.REPLAY.copy().setColor(ColorPattern.GREEN.color)));
        addWidget(new ButtonWidget((width - 12) / 2, height - 20, 12, 12, Icons.ROTATION, this::rotation).setHoverTexture(Icons.ROTATION.copy().setColor(ColorPattern.GREEN.color)));
        addWidget(new ButtonWidget((width - 12) / 2 + 40, height - 20, 12, 12, Icons.borderText(0, "+", -1), cd -> zoom(-1)).setHoverTexture(Icons.borderText(0, "+", ColorPattern.GREEN.color)));
        addWidget(new ButtonWidget((width - 12) / 2 - 40, height - 20, 12, 12, Icons.borderText(0, "-", -1), cd -> zoom(1)).setHoverTexture(Icons.borderText(0, "-", ColorPattern.GREEN.color)));
        addWidget(new SwitchWidget((width - 12) / 2 - 20, height - 20, 12, 12, this::playPause)
                .setSupplier(() -> isPause)
                .setTexture(Icons.PLAY_PAUSE, Icons.PLAY_PAUSE.copy().setColor(ColorPattern.GREEN.color))
                .setClientSideWidget());
        if (!frames.isEmpty()) {
            int progressWidth = (width - 2) / frames.size();
            for (int i = 0; i < frames.size(); i++) {
                val frame = frames.get(i);
                val frameIndex = i;
                addWidget(new ProgressWidget(() -> {
                    if (currentFrame < 0) return 0;
                    if (frameIndex < currentFrame) return 1;
                    if (frameTick > 0 && frameIndex == currentFrame) {
                        var duration = frame.getDuration();
                        return (frameTick + (isPause ? 0 : Minecraft.getInstance().getTimer().getGameTimeDeltaTicks())) / Math.max(duration, 1);
                    }
                    return 0;
                }, progressWidth * i + 1 + 1, height - 6 + 1, progressWidth - 4, 4, new ProgressTexture(IGuiTexture.EMPTY, ColorPattern.WHITE.rectTexture())));
                addWidget(new ButtonWidget(progressWidth * i + 1, height - 6, progressWidth - 2, 6, ColorPattern.WHITE.borderTexture(1), cd -> jumpFrame(frame))
                        .setHoverTooltips(frame.tooltips())
                        .setHoverTexture(ColorPattern.GREEN.borderTexture(1)));
            }
        }
    }

    private void zoom(int value) {
        if (!useScene) return;
        var zoom = (float) Mth.clamp(sceneWidget.getZoom() + value, 0.1, 999);
        sceneWidget.setZoom(zoom);
    }

    private void renderBeforeWorld(SceneWidget sceneWidget) {
        var graphics = new GuiGraphics(Minecraft.getInstance(), MultiBufferSource.immediate(new ByteBufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE)));
        graphics.pose().pushPose();
        RenderUtils.moveToFace(graphics.pose(), (minX + maxX) / 2f, minY, (minZ + maxZ) / 2f, Direction.DOWN);
        RenderUtils.rotateToFace(graphics.pose(), Direction.UP, null);
        int w = (maxX - minX) + 3;
        int h = (maxZ - minZ) + 3;
        new ResourceTexture("ldlib:textures/gui/darkened_slot.png").draw(graphics, 0, 0, w / -2f, h / -2f, w, h);
        graphics.pose().popPose();
    }

    private void renderAfterWorld(SceneWidget sceneWidget) {
        PoseStack matrixStack = new PoseStack();
        var tick = Math.abs((Minecraft.getInstance().getTimer().getGameTimeDeltaTicks() + gui.getTickCount() % 40) - 20) / 20;
        for (Map.Entry<BlockPosFace, Integer> entry : highlightBlocks.entrySet()) {
            if (entry.getValue() <= 0) continue;
            if (entry.getKey().facing() == null) {
                RenderUtils.renderBlockOverLay(matrixStack, entry.getKey().pos(), 0.6f * tick, 0, 0, 1.01f);
            } else {
                sceneWidget.drawFacingBorder(matrixStack, entry.getKey(), ColorUtils.color(1 * tick, 0, 0.6f, 0));
            }
        }
        var window = Minecraft.getInstance().getWindow();
        for (var pos : tooltipBlocks.keySet()) {
            var result = sceneWidget.getRenderer().project(new Vector3f((float) pos.x, (float) pos.y, (float) pos.z));
            //translate gui coordinates to window's ones (y is inverted)
            var x = result.x() * window.getGuiScaledWidth() / window.getWidth();
            var y = (window.getHeight() - result.y()) * window.getGuiScaledHeight() / window.getHeight();
            tooltipPos.put(pos, new Vec2(x, y));
        }
    }

    @Override
    public void drawInForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
        var position = getPosition();
        var size = getSize();
        for (var entry : tooltipBlocks.entrySet()) {
            var screenPos = tooltipPos.get(entry.getKey());
            if (screenPos == null) continue;
            var tuple = entry.getValue().getLeft();
            var ingredient = tuple.getA();
            var items = Arrays.stream(ingredient.ingredient().getItems()).map(i -> {
                var copied = i.copy();
                copied.setCount(ingredient.count());
                return copied;
            }).toArray(ItemStack[]::new);
            var tooltips = tuple.getB();
            mouseX = (int)(position.x + size.width * entry.getValue().getMiddle().x);
            mouseY = (int)(position.y + size.height * entry.getValue().getMiddle().y);
            DrawerHelper.drawLines(graphics, List.of(screenPos, new Vec2(mouseX, mouseY)), -1, -1, 0.75f);
            var componentPanelWidget = new ComponentPanelWidget(0, 0, tooltips).clickHandler(CompassManager::onComponentClick);
            componentPanelWidget.setBackground(TooltipBGTexture.INSTANCE);
            var maxWidth = 200;
            while (maxWidth < size.width && (componentPanelWidget.getSize().height == 0 || componentPanelWidget.getSize().height + mouseY < size.height * (1 - entry.getValue().getMiddle().y))) {
                componentPanelWidget.setMaxWidthLimit(maxWidth);
                maxWidth += 50;
            }
            componentPanelWidget.addSelfPosition(mouseX, mouseY);
            // check width
            maxWidth = componentPanelWidget.getSize().width;
            var maxHeight = componentPanelWidget.getSize().height;
            int rightSpace = getGui().getScreenWidth() - mouseX;
            int bottomSpace = getGui().getScreenHeight() - mouseY;
            if (rightSpace < maxWidth) { // move to Left
                componentPanelWidget.addSelfPosition(rightSpace - maxWidth, 0);
            }
            // check height
            if (bottomSpace < maxHeight) {
                componentPanelWidget.addSelfPosition(0, bottomSpace - maxHeight);
            }
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            componentPanelWidget.drawInBackground(graphics, mouseX, mouseY, partialTicks);
            if (items.length > 0) {
                new ItemStackTexture(items).draw(graphics, mouseX, mouseY,
                        componentPanelWidget.getPosition().x + 2,
                        componentPanelWidget.getPosition().y - 20,
                        16, 16);
            }
            graphics.pose().popPose();
        }
    }

    private void playPause(ClickData clickData, boolean isPressed) {
        isPause = isPressed;
    }

    private void rotation(ClickData clickData) {
        if (!useScene) return;
        float current = sceneWidget.getRotationPitch();
        sceneWidget.setCameraYawAndPitchAnima(sceneWidget.getRotationYaw(), current + 90, 20);
    }

    private void resetScene() {
        world.clear();
        sceneWidget.setCameraYawAndPitch(sceneWidget.getRotationYaw(), -135);
        sceneWidget.getCore().clear();
        sceneWidget.getCore().add(BlockPos.ZERO);
        headerGroup.clearAllWidgets();
        addedBlocks.clear();
        removedBlocks.clear();
        highlightBlocks.clear();
        tooltipBlocks.clear();
        currentFrame = -1;
    }

    private void jumpFrame(AnimationFrame frame) {
        resetScene();
        currentFrame = 0;
        for (AnimationFrame animationFrame : frames) {
            if (animationFrame != frame) {
                animationFrame.performFrameResult(this);
                currentFrame++;
            } else {
                frameTick = 0;
                break;
            }
        }
    }

    private void replay(ClickData clickData) {
        resetScene();
        isPause = false;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (frames.isEmpty()) return;
        // update frames
        if (!isPause) {
            addedBlocks.forEach((p, v) -> v.setB(v.getB() - 1));
            removedBlocks.forEach((p, v) -> v.setB(v.getB() - 1));
            highlightBlocks.replaceAll((p, v) -> v - 1);
            tooltipBlocks.forEach((p, v) -> v.setRight(v.getRight() - 1));

            addedBlocks.entrySet().removeIf(e -> e.getValue().getB() <= 0);
            highlightBlocks.entrySet().removeIf(e -> e.getValue() <= 0);
            tooltipBlocks.entrySet().removeIf(e -> e.getValue().getRight() <= 0);
            final var iterator = removedBlocks.entrySet().iterator();
            while (iterator.hasNext()) {
                final var entry = iterator.next();
                if (entry.getValue().getB() <= 0) {
                    world.removeBlock(entry.getKey());
                    sceneWidget.getCore().remove(entry.getKey());
                    iterator.remove();
                }
            }
            if (currentFrame < 0) {
                nextFrame();
            }
            if (currentFrame >= 0 && currentFrame < frames.size()) {
                if (frames.get(currentFrame).onFrameTick(this, frameTick++)) {
                    nextFrame();
                }
            }

            if (tickScene && useScene) {
                world.tickWorld();
            }
        }
    }

    protected void nextFrame() {
        if (currentFrame < 0) {
            currentFrame = 0;
        } else {
            currentFrame++;
            if (currentFrame >= frames.size()) {
                currentFrame = -1;
                frameTick = 0;
                isPause = true;
                return;
            }
        }
        frameTick = -frames.get(currentFrame).delay();
    }

    @Override
    public void applyEntity(Level world, Entity entity, PoseStack poseStack, float partialTicks) {
        ISceneEntityRenderHook.super.applyEntity(world, entity, poseStack, partialTicks);
    }

    @Override
    public void applyBESR(Level world, BlockPos pos, BlockEntity blockEntity, PoseStack poseStack, float partialTicks) {
        if (isPause) partialTicks = 0;
        if (removedBlocks.containsKey(pos)) {
            var tuple = removedBlocks.get(pos);
            var anima = tuple.getA();
            var tick = 1 - ((tuple.getB() - partialTicks) / anima.duration());
            if (tick > 0) {
                poseStack.translate(tick * anima.offset().x, tick * anima.offset().y, tick * anima.offset().z);
            }
        } else if (addedBlocks.containsKey(pos)) {
            var tuple = addedBlocks.get(pos);
            var anima = tuple.getA();
            var tick = (tuple.getB() - partialTicks) / anima.duration();
            if (tick > 0) {
                tick = Eases.EaseQuadIn.getInterpolation(tick);
                poseStack.translate(tick * anima.offset().x, tick * anima.offset().y, tick * anima.offset().z);
            }
        }
    }

    @Override
    public void applyVertexConsumerWrapper(Level world, BlockPos pos, BlockState state, WorldSceneRenderer.VertexConsumerWrapper wrapperBuffer, RenderType layer, float partialTicks) {
        if (isPause) partialTicks = 0;
        if (removedBlocks.containsKey(pos)) {
            var tuple = removedBlocks.get(pos);
            var anima = tuple.getA();
            float tick = 1 - ((tuple.getB() - partialTicks) / anima.duration());
            if (tick > 0) {
                wrapperBuffer.addOffset(tick * anima.offset().x, tick * anima.offset().y, tick * anima.offset().z);
            }
        } else if (addedBlocks.containsKey(pos)) {
            var tuple = addedBlocks.get(pos);
            var anima = tuple.getA();
            var tick = (tuple.getB() - partialTicks) / anima.duration();
            if (tick > 0) {
                tick = Eases.EaseQuadIn.getInterpolation(tick);
                wrapperBuffer.addOffset(tick * anima.offset().x, tick * anima.offset().y, tick * anima.offset().z);
            }
        }
    }

    public void addBlock(BlockPos pos, BlockInfo blockInfo, @Nullable BlockAnima anima) {
        if (!useScene) return;
        if (blockInfo.getBlockState().getBlock() == Blocks.AIR) {
            removeBlock(pos, null);
            return;
        }
        world.addBlock(pos, blockInfo);
        sceneWidget.getCore().add(pos);
        if (anima != null) {
            addedBlocks.put(pos, new Tuple<>(anima, anima.duration()));
        }
    }

    public void removeBlock(BlockPos pos, @Nullable BlockAnima anima) {
        if (!useScene) return;
        if (anima != null) {
            removedBlocks.put(pos, new Tuple<>(anima, anima.duration()));
        } else {
            world.removeBlock(pos);
            sceneWidget.getCore().remove(pos);
        }
    }

    public void highlightBlock(BlockPosFace key, int duration) {
        if (!useScene) return;
        highlightBlocks.put(key, duration);
    }

    public void addEntity(EntityInfo entityInfo, @Nullable Vector3f pos, boolean update) {
        Entity entity = null;
        if (update) {
            entity = world.entities.get(entityInfo.getId());
        }
        entity = (entity == null && entityInfo.getEntityType() != null) ? entityInfo.getEntityType().create(world) : entity;
        if (entity == null && entityInfo.getEntityType() == EntityType.PLAYER) {
            entity = new RemotePlayer(world.getAsClientWorld().get(), Minecraft.getInstance().player.getGameProfile());
        }
        if (entity != null) {
            entity.setId(entityInfo.getId());
            if (pos != null) {
                entity.setPos(pos.x, pos.y, pos.z);
            }
            if (entityInfo.getTag() != null) {
                entity.load(entity.saveWithoutId(new CompoundTag()).copy().merge(entityInfo.getTag()));
            }
            world.addFreshEntity(entity);
        }
    }

    public void removeEntity(EntityInfo entityInfo, boolean force) {
        if (force) {
            world.entities.remove(entityInfo.getId());
        } else {
            Optional.ofNullable(world.entities.get(entityInfo.getId())).ifPresent(Entity::discard);
        }
    }


    public void addTooltip(Vector3f pos, Tuple<XmlUtils.SizedIngredient, List<Component>> tuple, Vec2 middle, Integer time) {
        tooltipBlocks.put(pos, MutableTriple.of(tuple, middle, time));
    }

    public void rotate(float rotation, boolean anima) {
        if (!useScene) return;
        var current = sceneWidget.getRotationPitch();
        current += (rotation - 135 - current) % 360;
        if (anima) {
            sceneWidget.setCameraYawAndPitchAnima(sceneWidget.getRotationYaw(), current, 20);
        } else {
            sceneWidget.setCameraYawAndPitch(sceneWidget.getRotationYaw(), current);
        }
    }

    public void addInformation(Widget widget, boolean anima) {
        if (anima) {
            for (Widget child : headerGroup.widgets) {
                headerGroup.removeWidgetAnima(child, new Transform().duration(500).offset(-child.getSelfPosition().x - child.getSize().width, 0));
            }
        } else {
            headerGroup.clearAllWidgets();
        }

        var size = headerGroup.getSize();
        widget.setSelfPosition(Position.of((size.width - widget.getSize().width) / 2, (size.height - widget.getSize().height) / 2));
        if (anima) {
            headerGroup.addWidgetAnima(widget, new Transform().ease(Eases.EaseQuadOut).duration(500).offset(size.width - widget.getSelfPosition().x, 0));
        } else {
            headerGroup.addWidget(widget);
        }
    }
}
