package com.lowdragmc.lowdraglib2.client.window;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Whatever fills an {@link OsWindow}. {@link OsWindowManager} drives one of these per frame in the
 * order {@link #drainInput()}, {@link #renderFrame(float)}, {@link #present()}.
 */
@OnlyIn(Dist.CLIENT)
public interface OsWindowHost {

    /**
     * The window, valid from {@link #onAttached} until {@link #onDestroyed}.
     */
    OsWindow window();

    /**
     * The window has been created. Last chance to size or position it before it is shown.
     */
    default void onAttached(OsWindow window) {
    }

    /**
     * Dispatch everything {@link OsWindow#drain} hands over. Runs before the render so hover state
     * resolved here is the state the frame is drawn with.
     */
    void drainInput();

    /**
     * Draw a frame into this host's own render target, in Minecraft's context.
     */
    void renderFrame(float partialTick);

    /**
     * Blit the frame into the window and swap.
     */
    void present();

    /**
     * The user asked to close the window — the close button, or the platform's own gesture. The host
     * decides whether to honour it, so it may prompt first, or move its contents somewhere else.
     */
    void onCloseRequested();

    /**
     * The window is gone. Release anything tied to it.
     */
    default void onDestroyed() {
    }
}
