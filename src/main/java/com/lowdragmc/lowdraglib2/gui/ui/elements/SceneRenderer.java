package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.client.scene.SceneRenderState;
import com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib2.client.utils.RenderUtils;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.data.BlockPosFace;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "scene", registry = "ldlib2:ui_element_renderer")
public final class SceneRenderer extends DelegatingUIElementRenderer<Scene, SceneRenderer> {
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
                    context.graphics.peekScissorStack()
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
        renderer.setOnLookingAt(ray -> {});
        renderer.setBeforeBatchEnd((bufferSource, partialTicks) -> renderBeforeBatchEnd(scene, bufferSource, partialTicks));
        renderer.setAfterWorldRender(worldRenderer -> renderBlockOverLay(scene, worldRenderer));
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

    private static void renderBeforeBatchEnd(Scene scene, MultiBufferSource bufferSource, float partialTicks) {
        if (scene.lastSelectedPosFace != null && scene.renderSelect) {
            var poseStack = new PoseStack();
            RenderUtils.renderBlockOverLay(poseStack, bufferSource, scene.lastSelectedPosFace.pos(), 0.6f, 0, 0, 1.01f);
        }
    }

    private static void renderBlockOverLay(Scene scene, WorldSceneRenderer renderer) {
        if (renderer == null || scene.dummyWorld == null || scene.core == null || scene.core.isEmpty()) {
            return;
        }
        var poseStack = new PoseStack();
        scene.lastHoverPosFace = null;
        scene.lastHoverItem = null;
        if (scene.isHover()) {
            var hit = renderer.getLastTraceResult();
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
                        if (blockState.getBlock() == Blocks.AIR) {
                            continue;
                        }
                        hit = world.clipWithInteractionOverride(eyePos, endPos, pos, blockState.getShape(world, pos), blockState);
                        if (hit != null && hit.getType() != HitResult.Type.MISS) {
                            double dist = eyePos.distanceToSqr(hit.getLocation());
                            if (dist < min) {
                                min = dist;
                                scene.lastHoverPosFace = new BlockPosFace(hit.getBlockPos(), hit.getDirection());
                            }
                        }
                    }
                }
            }
            var mui = scene.getModularUI();
            if (scene.lastHoverPosFace != null && hit != null && mui != null && mui.player != null) {
                var state = scene.dummyWorld.getBlockState(scene.lastHoverPosFace.pos());
                scene.lastHoverItem = state.getCloneItemStack(scene.dummyWorld, scene.lastHoverPosFace.pos(), false);
            }
        }

        var tmp = scene.dragging ? scene.lastClickPosFace : scene.lastHoverPosFace;
        if (scene.lastSelectedPosFace != null || tmp != null) {
            if (scene.lastSelectedPosFace != null && scene.renderFacing) {
                drawFacingBorder(scene.lastSelectedPosFace, poseStack, 0xff00ff00);
            }
            if (tmp != null && !tmp.equals(scene.lastSelectedPosFace) && scene.renderFacing) {
                drawFacingBorder(tmp, poseStack, 0xffffffff);
            }
        }

        if (scene.afterWorldRender != null) {
            scene.afterWorldRender.accept(scene);
        }
    }

    private static void drawFacingBorder(BlockPosFace posFace, PoseStack poseStack, int color) {
        drawFacingBorder(posFace, poseStack, color, 0);
    }

    private static void drawFacingBorder(BlockPosFace posFace, PoseStack poseStack, int color, int inner) {
        poseStack.pushPose();
        GlStateManager._disableDepthTest();
        RenderUtils.moveToFace(poseStack, posFace.pos().getX(), posFace.pos().getY(), posFace.pos().getZ(), posFace.facing());
        RenderUtils.rotateToFace(poseStack, posFace.facing(), null);
        poseStack.scale(1f / 16, 1f / 16, 0);
        poseStack.translate(-8, -8, 0);
        drawBorder(poseStack, 1 + inner * 2, 1 + inner * 2, 14 - 4 * inner, 14 - 4 * inner, color, 1);
        GlStateManager._enableDepthTest();
        poseStack.popPose();
    }

    private static void drawBorder(PoseStack poseStack, int x, int y, int width, int height, int color, int border) {
        drawSolidRect(poseStack, x - border, y - border, width + 2 * border, border, color);
        drawSolidRect(poseStack, x - border, y + height, width + 2 * border, border, color);
        drawSolidRect(poseStack, x - border, y, border, height, color);
        drawSolidRect(poseStack, x + width, y, border, height, color);
    }

    private static void drawSolidRect(PoseStack poseStack, int x, int y, int width, int height, int color) {
        fill(poseStack, x, y, x + width, y + height, color);
    }

    private static void fill(PoseStack matrices, int x1, int y1, int x2, int y2, int color) {
        if (x1 > x2) {
            int tmp = x1;
            x1 = x2;
            x2 = tmp;
        }
        if (y1 > y2) {
            int tmp = y1;
            y1 = y2;
            y2 = tmp;
        }
        var renderType = RenderTypes.debugQuads();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.addVertex(matrices.last().pose(), (float) x1, (float) y1, 0).setColor(color);
        bufferBuilder.addVertex(matrices.last().pose(), (float) x1, (float) y2, 0).setColor(color);
        bufferBuilder.addVertex(matrices.last().pose(), (float) x2, (float) y2, 0).setColor(color);
        bufferBuilder.addVertex(matrices.last().pose(), (float) x2, (float) y1, 0).setColor(color);
        renderType.draw(bufferBuilder.buildOrThrow());
    }
}
