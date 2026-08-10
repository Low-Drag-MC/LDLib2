package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.math.Rect;
import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * Corrects the scissor box for a UI that is not being drawn into the game window.
 *
 * <p>{@code GuiGraphics#applyScissor} turns a gui-space rectangle into a GL scissor box by scaling
 * it by the gui scale and flipping it against the framebuffer height — and it reads both of those
 * from {@link net.minecraft.client.Minecraft#getWindow()}, unconditionally. Drawing into an
 * off-screen target of a different size therefore clips at the wrong place, which shows up as every
 * {@code overflow: hidden} element being cut off vertically.
 *
 * <p>The fix is deliberately additive: {@link GUIContext} still lets {@code GuiGraphics} manage the
 * scissor stack — so nested vanilla scissors keep intersecting correctly — and then re-issues the GL
 * call with the right numbers. On the game window this is a no-op after one boolean check, so the
 * on-screen path is untouched.
 */
@OnlyIn(Dist.CLIENT)
public final class UIScissor {

    private UIScissor() {
    }

    /**
     * Re-issue the scissor box for {@code rect} against {@code surface}. A {@code null} rect means
     * "no clipping".
     */
    public static void reapply(UISurface surface, @Nullable Rect rect) {
        if (surface.isMainWindow()) return; // GuiGraphics already got this one right
        if (rect == null) {
            RenderSystem.disableScissor();
            return;
        }
        // Mirrors GuiGraphics#applyScissor exactly, including the truncating casts and the clamp.
        var scale = surface.guiScale();
        var framebufferHeight = surface.framebufferHeight();
        var x = rect.left * scale;
        var y = framebufferHeight - rect.down * scale;
        var width = (rect.right - rect.left) * scale;
        var height = (rect.down - rect.up) * scale;
        RenderSystem.enableScissor((int) x, (int) y, Math.max(0, (int) width), Math.max(0, (int) height));
    }
}
