package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.math.Rect;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.function.Consumer;

import org.lwjgl.opengl.GL30;

public class GUIContext {
    @OnlyIn(Dist.CLIENT)
    public ModularUI modularUI;
    @OnlyIn(Dist.CLIENT)
    public GuiGraphics graphics;
    @OnlyIn(Dist.CLIENT)
    public int mouseX, mouseY;
    @OnlyIn(Dist.CLIENT)
    public float partialTick;
    @OnlyIn(Dist.CLIENT)
    public EnhancedPoseStack pose;
    @OnlyIn(Dist.CLIENT)
    public Minecraft mc;

    // runtime
    @OnlyIn(Dist.CLIENT)
    public boolean refreshLocalMouse = true;
    /**
     * Current element tint color (ARGB), set by UIElement before drawing its background/overlay textures.
     * -1 (0xFFFFFFFF) means no tint. Textures read this to multiply (per-channel) with their own color.
     */
    @OnlyIn(Dist.CLIENT)
    public int elementColor = -1;
    @OnlyIn(Dist.CLIENT)
    public float localMouseX, localMouseY;
    @OnlyIn(Dist.CLIENT)
    public ObjectArrayList<UIVisualLayer> visualLayers = new ObjectArrayList<>();
    @OnlyIn(Dist.CLIENT)
    public final ObjectArrayList<Rect> scissorStack = new ObjectArrayList<>();
    @OnlyIn(Dist.CLIENT)
    private final ObjectArrayList<PostCall> postRenderingCalls = new ObjectArrayList<>();
    private record PostCall(Consumer<GUIContext> call, PoseStack.Pose pose) {}
    private int lastFBO = -1;
    
    @OnlyIn(Dist.CLIENT)
    public static GUIContext of(ModularUI modularUI, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var context = new GUIContext();
        context.modularUI = modularUI;
        context.graphics = graphics;
        context.mouseX = mouseX;
        context.mouseY = mouseY;
        context.partialTick = partialTick;
        context.pose = new EnhancedPoseStack(graphics.pose()).setOnTransform(context::refreshLocalMouse);
        context.mc = Minecraft.getInstance();
        context.refreshLocalMouse();
        return context;
    }

    @OnlyIn(Dist.CLIENT)
    public void drawTexture(IGuiTexture texture, float x, float y, float width, float height) {
        texture.draw(this, x, y, width, height);
    }

    @OnlyIn(Dist.CLIENT)
    public void enableScissor(float x, float y, float width, float height) {
        enableScissor(x, y, width, height, graphics.pose().last().pose());
    }

    @OnlyIn(Dist.CLIENT)
    public void enableScissor(float x, float y, float width, float height, Matrix4f trans) {
        var realPos = trans.transform(new Vector4f(x, y, 0, 1));
        var realPos2 = trans.transform(new Vector4f(x + width, y + height, 0, 1));
        var rect = Rect.of(Mth.floor(realPos.x), Mth.floor(realPos.y), Mth.ceil(realPos2.x), Mth.ceil(realPos2.y));
        var peek = scissorStack.isEmpty() ? null : scissorStack.top();
        scissorStack.push(peek == null ? rect : peek.intersects(rect));
        graphics.enableScissor(rect.left, rect.up, rect.right, rect.down);
    }

    @OnlyIn(Dist.CLIENT)
    public void disableScissor() {
        graphics.disableScissor();
        scissorStack.pop();
    }

    @OnlyIn(Dist.CLIENT)
    public void refreshLocalMouse() {
        var realMouse = pose.last().pose().invert(new Matrix4f()).transformPosition(new Vector3f(mouseX, mouseY, 0));
        localMouseX = realMouse.x;
        localMouseY = realMouse.y;
    }

    @OnlyIn(Dist.CLIENT)
    public void pushVisualLayer(UIVisualLayer layer) {
        graphics.flush();
        if (visualLayers.isEmpty()) {
            int[] fbo = new int[1];
            GL30.glGetIntegerv(GL30.GL_FRAMEBUFFER_BINDING, fbo);
            lastFBO = fbo[0];
        }
        visualLayers.push(layer);
        layer.bind(this);
        layer.clear();
    }

    @OnlyIn(Dist.CLIENT)
    public void popVisualLayer() {
        var popped = visualLayers.pop();
        if (popped != null) {
            graphics.flush();
            popped.unbind();
            var mainTarget = Minecraft.getInstance().getMainRenderTarget();
            if (visualLayers.isEmpty()) {
                if (lastFBO == -1) {
                    mainTarget.bindWrite(false);
                } else {
                    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, lastFBO);
                }
            } else {
                visualLayers.top().bind(this);
            }
            popped.draw(this);
            popped.release();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void setElementColor(int elementColor) {
        if (this.elementColor == elementColor) return;
        this.elementColor = elementColor;
        RenderSystem.setShaderColor(ColorUtils.red(elementColor), ColorUtils.green(elementColor),
                ColorUtils.blue(elementColor), ColorUtils.alpha(elementColor));
    }

    @OnlyIn(Dist.CLIENT)
    public void resetElementColor() {
        if (this.elementColor == -1) return;
        this.elementColor = -1;
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public void postRendering(Consumer<GUIContext> call) {
        postRenderingCalls.add(new PostCall(call, pose.last().copy()));
    }

    public void callPostRendering() {
        final Object[] postCallsElements = postRenderingCalls.elements();

        for (int i = 0; i < postRenderingCalls.size(); i++) {
            final PostCall postRenderingCall = (PostCall) postCallsElements[i];
            pose.pushPose();
            pose.setIdentity();
            pose.mulPose(postRenderingCall.pose.pose());
            postRenderingCall.call.accept(this);
            pose.popPose();
        }
    }
}
