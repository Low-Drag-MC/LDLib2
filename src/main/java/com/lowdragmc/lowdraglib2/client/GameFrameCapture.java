package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderPipelines;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGuiRenderStateExt;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A copy of the game's frame, the world and its HUD, for a UI showing the game behind it (the UI editor's HUD
 * preview). Copied only while drawn, see {@link #draw}.
 *
 * <p>The GUI is drawn in one pass after everything was extracted, the HUD and the open screen alike, so there is
 * no moment the frame holds the HUD without the screen. Where the screen begins is therefore marked while it is
 * extracted ({@link #capture()}, called for every screen just before it extracts), and the gui renderer takes the
 * copy at that point of its pass, between the draws of the HUD and those of the screen. With no screen open it is
 * taken at the end of the pass. Either way it is this frame's picture.
 *
 * <p>A mod extracting HUDs of its own can keep them out of the picture by calling {@link #capture()} before it
 * does: the first mark of a frame is the one kept.
 */
@EventBusSubscriber(modid = LDLib2.MOD_ID, value = Dist.CLIENT)
public final class GameFrameCapture {
    /** Frames nobody drew it for before the texture is let go, a window sized texture is tens of megabytes. */
    private static final int IDLE_FRAMES = 200;
    /** What the copy starts from: {@link LDLibRenderPipelines#OPAQUE_SCREEN_COPY} writes colour only, so alpha stays this. */
    private static final Vector4fc OPAQUE_BLACK = new Vector4f(0, 0, 0, 1);

    @Nullable
    private static TextureTarget target;
    /**
     * Replaced targets, closed at the end of the frame: one may still be in this frame's draws, extracted before the
     * copy found the window resized.
     */
    private static final List<RenderTarget> retired = new ArrayList<>();
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

    /** The last copy, null until there is one. Read it on the render thread, it is replaced when the window resizes. */
    @Nullable
    public static RenderTarget target() {
        return target;
    }

    public static int width() {
        return target == null ? 0 : target.width;
    }

    public static int height() {
        return target == null ? 0 : target.height;
    }

    /**
     * Leaves what is extracted from now on out of this frame's copy, if a copy is wanted. Call it while the frame is
     * extracted: the copy itself is taken when the frame is drawn, at this point of it.
     */
    public static void capture() {
        if (!isWanted()) return;
        // kept by the frame's own render state, which starts every extraction empty: a mark can only ever be this
        // frame's, and only its first one counts
        gameGuiState().ldlib2$markFrameCaptureBoundary();
    }

    private static IGuiRenderStateExt gameGuiState() {
        return (IGuiRenderStateExt) Minecraft.getInstance().gameRenderer.gameRenderState().guiRenderState;
    }

    /**
     * Copies the frame drawn so far into {@code source}, if a copy is wanted. Called by the game's gui renderer at the
     * point {@link #capture()} marked, or at the end of its pass; never inside a render pass.
     */
    public static void copyFrom(RenderTarget source) {
        if (!isWanted()) return;
        var sourceView = source.getColorTextureView();
        if (sourceView == null || source.width <= 0 || source.height <= 0) return;
        copiedAt = frame;
        if (target == null || target.width != source.width || target.height != source.height) {
            if (target != null) {
                retired.add(target);
            }
            target = new TextureTarget("ldlib2 game frame", source.width, source.height, false, GpuFormat.RGBA8_UNORM);
        }
        var output = target.getColorTextureView();
        if (output == null) return;
        // cleared opaque and written colour only: the world leaves the alpha as it likes, and a picture drawn with
        // blending would let it through. A pass rather than copyTextureToTexture, which would carry that alpha over.
        try (var pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "ldlib2 game frame copy", output, Optional.of(OPAQUE_BLACK))) {
            pass.setPipeline(LDLibRenderPipelines.OPAQUE_SCREEN_COPY);
            pass.bindTexture("InSampler", sourceView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.draw(3, 1, 0, 0);
        }
    }

    /**
     * {@link #copyFrom} at the end of the gui pass, which is where the game ends when no screen is open. With one
     * open and its start not marked, which is the first frame the picture is asked for, the end of the pass has the
     * screen in it: that frame goes without.
     */
    public static void copyAtEnd(RenderTarget source) {
        if (gameGuiState().ldlib2$getFrameCaptureStratum() == Integer.MAX_VALUE
                && Minecraft.getInstance().gui.screen() != null) {
            return;
        }
        copyFrom(source);
    }

    /** Draws the last copy over the given rectangle, nothing until there is one, and asks for the next. */
    public static void draw(GUIContext context, float x, float y, float width, float height) {
        request();
        if (target == null) return;
        var view = target.getColorTextureView();
        if (view == null) return;
        // drawn smaller than it was taken, nearest would shimmer; and bottom up, as render targets are
        context.blit(RenderPipelines.GUI_TEXTURED, view, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR),
                x, y, x + width, y + height, 0, 1, 1, 0, -1);
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        // everything extracted before the screen is the game: the HUD and whatever mods drew over it
        capture();
    }

    @SubscribeEvent
    public static void onFrameRendered(RenderFrameEvent.Post event) {
        frame++;
        retired.forEach(RenderTarget::destroyBuffers);
        retired.clear();
        var minecraft = Minecraft.getInstance();
        // not running: the game is quitting, and this is its last frame before the device goes; let go here rather
        // than on GameShuttingDownEvent, which can come before this frame draws what it extracted
        if (target != null && (frame - requestedAt > IDLE_FRAMES || minecraft.level == null || !minecraft.isRunning())) {
            // drawn by this frame at the latest, and this frame is done
            target.destroyBuffers();
            target = null;
        }
    }
}
