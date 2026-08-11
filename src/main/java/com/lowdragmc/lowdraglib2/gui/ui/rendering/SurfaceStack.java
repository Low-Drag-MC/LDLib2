package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.utils.Scope;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Backing store for {@link UISurface#current()}.
 *
 * <p>Render passes run one at a time on the render thread, so a plain static stack is enough — and a
 * stack rather than a single field because a UI frame can nest (a scene or preview element drawing
 * another UI into its own target).
 */
final class SurfaceStack {
    private static final ObjectArrayList<UISurface> STACK = new ObjectArrayList<>();

    private SurfaceStack() {
    }

    static UISurface current() {
        return STACK.isEmpty() ? MainWindowSurface.INSTANCE : STACK.top();
    }

    static Scope push(UISurface surface) {
        STACK.push(surface);
        return () -> {
            // Defensive: an exception mid-frame must not leave the stack pinned to a dead surface,
            // and a mismatched pop is a bug worth surviving rather than compounding.
            if (!STACK.isEmpty()) {
                STACK.pop();
            }
        };
    }
}
