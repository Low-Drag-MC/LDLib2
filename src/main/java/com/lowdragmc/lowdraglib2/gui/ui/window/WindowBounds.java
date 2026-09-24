package com.lowdragmc.lowdraglib2.gui.ui.window;

//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.api.distmarker.OnlyIn;

/**
 * A window rectangle in window coordinates — the space {@code SDL_SetWindowPosition} and
 * {@code SDL_SetWindowSize} speak, which on a HiDPI display is not framebuffer pixels, and
 * <em>not</em> GUI units either.
 *
 * @param x position of the content area's upper-left corner, as {@code SDL_GetWindowPosition} reports it
 */
//@OnlyIn(Dist.CLIENT)
public record WindowBounds(int x, int y, int width, int height) {

    /** Clamped so a remembered or derived rectangle can never be smaller than a window may be. */
    public WindowBounds atLeastMinimum() {
        return new WindowBounds(x, y,
                Math.max(ModularUIWindow.MIN_WIDTH, width),
                Math.max(ModularUIWindow.MIN_HEIGHT, height));
    }

    public WindowBounds offsetBy(int dx, int dy) {
        return new WindowBounds(x + dx, y + dy, width, height);
    }
}
