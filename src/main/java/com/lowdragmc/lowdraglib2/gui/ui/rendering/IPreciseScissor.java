package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.PreciseScissor.ClipRect;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/// The unrounded clip rectangle, hung on the integer one vanilla carries.
///
/// Implemented on `ScreenRectangle` by mixin. Attaching to the rectangle rather than keeping a side
/// table is what makes the lifetime a non-question: the gui renderer is deferred, so a clip
/// rectangle is created during extraction and consumed several call layers later inside
/// `GuiRenderer#render` — possibly a nested sub-render for a visual layer or for a floating window —
/// and a map keyed on it would need a clearing point that is correct for all three. The field is
/// simply collected with the object.
public interface IPreciseScissor {

    @Nullable ClipRect ldlib2$preciseClip();

    void ldlib2$setPreciseClip(@Nullable ClipRect clip);

    /// Never `instanceof` against the rectangle directly: `ScreenRectangle` is a record, so it is
    /// final and javac rejects a cast to an interface it does not declare. Going through `Object`
    /// also means this degrades to null when the mixin did not apply, and in unit tests, where it is
    /// a plain record.
    static @Nullable ClipRect of(@Nullable ScreenRectangle rectangle) {
        return ((Object) rectangle) instanceof IPreciseScissor precise ? precise.ldlib2$preciseClip() : null;
    }

    /// What a rectangle clips to, tagged or not: an untagged one — a vanilla widget's, or any pushed
    /// before this mixin existed — clips to its integer edges, which is exactly what they mean.
    ///
    /// Kept next to {@link #of} because it is the same semantic decision, and both places that need it
    /// (nesting in {@link GUIContext}, quantising in the gui renderer) would otherwise spell it out.
    static @Nullable ClipRect clipOf(@Nullable ScreenRectangle rectangle) {
        if (rectangle == null) return null;
        var precise = of(rectangle);
        return precise != null ? precise
                : new ClipRect(rectangle.left(), rectangle.top(), rectangle.right(), rectangle.bottom());
    }

    static void attach(@Nullable ScreenRectangle rectangle, @Nullable ClipRect clip) {
        // ScreenRectangle.empty() is a shared singleton, handed out by ScissorStack whenever an
        // intersection comes out empty. Tagging it would give every future empty clip in the game
        // whatever this one happened to be.
        if (rectangle == null || rectangle == ScreenRectangle.empty()) {
            return;
        }
        if (((Object) rectangle) instanceof IPreciseScissor precise) {
            precise.ldlib2$setPreciseClip(clip);
        }
        // No else: when the mixin did not apply every call site degrades to vanilla's whole-gui-pixel
        // clipping rather than failing, so there is nothing to recover from here.
    }

    /// Orders two scissor rectangles that share an integer box by their precise clip, so the gui
    /// renderer's element sort keeps them apart instead of interleaving them. Untagged sorts first.
    ///
    /// `comparingDouble`, not `comparing`: the keys are floats, and `Float.valueOf` has no cache, so
    /// the boxed form allocates two objects per level per comparison — on a sort that runs over every
    /// gui element every frame. The identity check ahead of it is not just a fast path: elements
    /// pushed under one `enableScissor` share a `ClipRect` instance, which is the common case here.
    Comparator<ClipRect> BY_EDGES = Comparator.comparingDouble(ClipRect::top)
            .thenComparingDouble(ClipRect::bottom)
            .thenComparingDouble(ClipRect::left)
            .thenComparingDouble(ClipRect::right);

    Comparator<ScreenRectangle> COMPARATOR = Comparator.comparing(IPreciseScissor::of,
            Comparator.nullsFirst((a, b) -> a == b ? 0 : BY_EDGES.compare(a, b)));
}
