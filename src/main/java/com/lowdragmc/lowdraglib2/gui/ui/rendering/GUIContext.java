package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.math.Rect;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;

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
    @OnlyIn(Dist.CLIENT)
    public float localMouseX, localMouseY;
    @OnlyIn(Dist.CLIENT)
    public Stack<UIVisualLayer> visualLayers = new Stack<>();
    @OnlyIn(Dist.CLIENT)
    public final Stack<Rect> scissorStack = new Stack<>();
    @OnlyIn(Dist.CLIENT)
    private final List<PostCall> postRenderingCalls = new ArrayList<>();
    private record PostCall(Consumer<GUIContext> call, PoseStack.Pose pose) {}

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
        var peek = scissorStack.isEmpty() ? null : scissorStack.peek();
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
        visualLayers.push(layer);
        layer.bind(this);
        layer.clear();
    }

    @OnlyIn(Dist.CLIENT)
    public void popVisualLayer() {
        var popped = visualLayers.pop();
        if (popped != null) {
            graphics.flush();
            var mainTarget = Minecraft.getInstance().getMainRenderTarget();
            if (visualLayers.isEmpty()) {
                mainTarget.bindWrite(false);
            } else {
                visualLayers.peek().bind(this);
            }
            popped.draw(this);
            popped.release();
        }
    }

    public void postRendering(Consumer<GUIContext> call) {
        postRenderingCalls.add(new PostCall(call, pose.last().copy()));
    }

    public void callPostRendering() {
        for (var postRenderingCall : postRenderingCalls) {
            pose.pushPose();
            pose.setIdentity();
            pose.mulPose(postRenderingCall.pose.pose());
            postRenderingCall.call.accept(this);
            pose.popPose();
        }
    }
}
