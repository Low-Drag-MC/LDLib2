package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.IPreciseScissor;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.PreciseScissor;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.PreciseScissor.ClipRect;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/// Lets a `ScreenRectangle` carry the unrounded clip it was rounded from, so the gui renderer can
/// quantise once against the physical pixel grid instead of inheriting a decision already made at a
/// third of the resolution. See {@link PreciseScissor}.
///
/// One nullable reference, not a flag plus four floats: `ScreenRectangle` is allocated per element
/// bound, per intersection and per transform, thousands of times a frame. It is 24 bytes today
/// (header, a `ScreenPosition` reference, two ints); one reference takes it to 32 after alignment,
/// where a flag plus four floats would take it to 48. Only the handful that are actually scissor
/// rectangles ever allocate a `ClipRect`.
///
/// No injectors, deliberately. The precise rectangle is computed, intersected and attached by
/// `GUIContext#enableScissor`, so `intersection` is left alone — which is what keeps the shared
/// `ScreenRectangle.empty()` singleton clean, and keeps precise data off the *bounds* rectangles
/// that `FloatBlitRenderState#getBounds` and `LDFonts.Run#submit` derive from a scissor and use for
/// layer assignment. `equals` and `hashCode` are untouched for the same reason: they are a record's
/// value equality, relied on well outside the gui renderer, and `GuiRenderer#scissorChanged` is
/// hooked instead.
@Mixin(ScreenRectangle.class)
public abstract class ScreenRectangleMixin implements IPreciseScissor {

    // No initialiser: that would make Mixin inject into the record's canonical constructor.
    @Unique
    private @Nullable ClipRect ldlib2$clip;

    @Override
    public @Nullable ClipRect ldlib2$preciseClip() {
        return this.ldlib2$clip;
    }

    @Override
    public void ldlib2$setPreciseClip(@Nullable ClipRect clip) {
        this.ldlib2$clip = clip;
    }
}
