package com.lowdragmc.lowdraglib2.gui.ui.utils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * Whether input the operating system delivers to the game window reaches the game at all.
 *
 * <p>Vanilla's {@code MouseHandler#onMove/onPress/onScroll} and
 * {@code KeyboardHandler#keyPress/charTyped} have no {@code isWindowActive()} guard, so a click on
 * an <em>unfocused</em> game window is both focused and delivered by the platform. On a machine
 * someone is using while a scripted run drives that window, one stray click lands in the middle of
 * the run.
 *
 * <p>Kept separate from {@link CursorState} on purpose: a scripted playback wants a synthetic
 * pointer while still letting whoever is watching interrupt it. Only an unattended run wants both.
 *
 * <p>The window's close button is unaffected — it arrives through {@code glfwWindowShouldClose}
 * rather than these callbacks — so a blocked game can still be quit.
 */
@OnlyIn(Dist.CLIENT)
public final class RawInputGate {

    @Nullable
    private static Object owner;

    private RawInputGate() {
    }

    /**
     * Starts dropping OS-delivered input. Must be paired with {@link #unblock(Object)}; a leaked
     * block makes the real mouse and keyboard stop working until the game is restarted.
     */
    public static void block(Object owner) {
        RawInputGate.owner = owner;
    }

    /** Stops dropping OS-delivered input, if {@code owner} is the one that blocked it. */
    public static void unblock(Object owner) {
        if (RawInputGate.owner == owner) {
            RawInputGate.owner = null;
        }
    }

    public static boolean isBlocked() {
        return owner != null;
    }
}
