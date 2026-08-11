package com.lowdragmc.lowdraglib2.gui.ui.utils;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a stand-in pointer wherever {@link CursorState} says the cursor is.
 *
 * <p>A synthetic cursor is invisible by construction: the arrow on screen is drawn by the operating
 * system at the <em>physical</em> pointer, which is exactly the thing a background run stops moving.
 * Without this, watching a run means guessing where it thinks it is clicking.
 *
 * <p>Drawn from {@code ScreenEvent.Render.Post}, so it sits above the screen and its tooltips, and
 * only while a screen is open — with no screen there is nothing to point at.
 *
 * <p>Deliberately absent from screenshots: a real cursor is composited by the window manager and
 * never reaches the framebuffer, so a capture that contained one would differ from every capture
 * taken before this existed, and would put an arrow through element crops. The runner hides it for
 * the frame it is about to read; where a step aimed is recorded in the report instead.
 */
public final class CursorOverlay {

    /** Rendered size in GUI pixels, and the tip's offset within it, from the arrow's own artwork. */
    private static final float SIZE = 12f;
    private static final float TIP_X = SIZE * 8 / 32f;
    private static final float TIP_Y = SIZE * 2 / 32f;

    private static boolean hidden;
    /** Built on first use rather than in a static initializer, which would load {@link Icons} eagerly. */
    @Nullable
    private static IGuiTexture shadow;

    private CursorOverlay() {
    }

    /**
     * Suppresses the pointer until it is shown again. Set around a framebuffer read.
     *
     * <p>The frame this affects is the <em>next</em> one: a capture is serviced from the render frame
     * after the one that requested it, so hiding at request time is what keeps it out of the image.
     */
    public static void setHidden(boolean hidden) {
        CursorOverlay.hidden = hidden;
    }

    public static void render(GuiGraphicsExtractor graphics, float partialTick) {
        var source = CursorState.getSource();
        if (source == null || hidden) return;
        var x = source.cursorX();
        var y = source.cursorY();
        // parked off screen between steps, so that nothing is hovered before a scenario asks for it
        if (x < 0 || y < 0) return;

        var left = x - TIP_X;
        var top = y - TIP_Y;
        // A white arrow vanishes on a light panel and a black one on a dark panel, so draw both: the
        // shadow first, offset by a pixel, exactly as the platform draws its own pointer.
        if (shadow == null) {
            shadow = Icons.CURSOR.copy().setColor(ColorPattern.BLACK.color);
        }
        // Through a GUIContext rather than the texture's own draw(GuiGraphics, ...): in 26.1 a
        // texture records render states into the frame's GuiRenderState, and the context is what
        // carries that plus the pose and the element tint the renderers read.
        var context = GUIContext.of(graphics, 0, 0, partialTick);
        context.drawTexture(shadow, left + 1, top + 1, SIZE, SIZE);
        context.drawTexture(Icons.CURSOR, left, top, SIZE, SIZE);
    }
}
