package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * A copy of the game's frame, the world and its HUD, for a UI showing the game behind it (the UI editor's HUD
 * preview). Copied only while drawn, see {@link #draw}.
 *
 * <p>Copied before the open screen is drawn, so a screen showing it is not in its own picture; with no screen, at the
 * end of the frame. A mod drawing HUDs of its own can keep them out of the picture by calling {@link #capture()} before
 * drawing them: the first copy of a frame is the one kept.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = LDLib2.MOD_ID, value = Dist.CLIENT)
public final class GameFrameCapture {
    /** Frames nobody drew it for before the texture is let go, a window sized texture is tens of megabytes. */
    private static final int IDLE_FRAMES = 200;

    @Nullable
    private static TextureTarget target;
    private static int frame;
    private static int requestedAt = Integer.MIN_VALUE / 2;
    private static int copiedAt = Integer.MIN_VALUE / 2;

    private GameFrameCapture() {}

    /** Keeps the copy coming: call it every frame the picture is drawn. */
    public static void request() {
        requestedAt = frame;
    }

    /** Whether a copy is wanted this frame and not taken yet. */
    public static boolean isWanted() {
        return copiedAt != frame && frame - requestedAt <= 1 && Minecraft.getInstance().level != null;
    }

    /** The texture, or -1 when there is none. */
    public static int texture() {
        return target == null ? -1 : target.getColorTextureId();
    }

    public static int width() {
        return target == null ? 0 : target.width;
    }

    public static int height() {
        return target == null ? 0 : target.height;
    }

    /**
     * Copies the game's frame now, if a copy is wanted and none was taken this frame. What a {@link GuiGraphics} still
     * holds in its batch is not in the frame yet, flush it first.
     */
    public static void capture() {
        if (!isWanted()) return;
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        if (main.width <= 0 || main.height <= 0) return;
        copiedAt = frame;
        // making or resizing a target binds the window's framebuffer on its way out
        try (var ignored = RenderTargetScope.capture()) {
            if (target == null || target.width != main.width || target.height != main.height) {
                if (target == null) {
                    target = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
                } else {
                    target.resize(main.width, main.height, Minecraft.ON_OSX);
                }
                // drawn smaller than it was taken, nearest would shimmer
                target.setFilterMode(GL11.GL_LINEAR);
            }
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, target.width, target.height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            // the world leaves the alpha as it likes, a picture drawn with blending would let it through
            target.bindWrite(false);
            RenderSystem.colorMask(false, false, false, true);
            RenderSystem.clearColor(0, 0, 0, 1);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.colorMask(true, true, true, true);
        }
    }

    /** Draws the last copy over the given rectangle, nothing until there is one, and asks for the next. */
    public static void draw(GuiGraphics graphics, float x, float y, float width, float height) {
        request();
        var texture = texture();
        if (texture < 0) return;
        // drawn at once, what is batched before it would otherwise be drawn over it
        graphics.flush();
        var pose = graphics.pose().last().pose();
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        buffer.addVertex(pose, x + width, y + height, 0).setUv(1, 0);
        buffer.addVertex(pose, x + width, y, 0).setUv(1, 1);
        buffer.addVertex(pose, x, y, 0).setUv(0, 1);
        buffer.addVertex(pose, x, y + height, 0).setUv(0, 0);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        if (isWanted()) {
            // the HUD under the screen is drawn in batches
            event.getGuiGraphics().flush();
            capture();
        }
    }

    @SubscribeEvent
    public static void onFrameRendered(RenderFrameEvent.Post event) {
        if (Minecraft.getInstance().screen == null) {
            capture();
        }
        frame++;
        if (target != null && (frame - requestedAt > IDLE_FRAMES || Minecraft.getInstance().level == null)) {
            try (var ignored = RenderTargetScope.capture()) {
                target.destroyBuffers();
            }
            target = null;
        }
    }
}
