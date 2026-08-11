package com.lowdragmc.lowdraglib2.editor.ui.floating;

import com.lowdragmc.lowdraglib2.editor.ui.EditorLayout;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/**
 * A saved floating window: where it was, how big, and the dock tree inside it.
 *
 * <p>The dock tree is an {@link EditorLayout} rather than a second copy of its fields, so the split
 * tree and slot format have exactly one reader and one writer no matter which window they describe.
 *
 * <p>Kept separate from {@code EditorLayout} rather than added to it, so that record's canonical
 * constructor keeps its shape and existing callers keep compiling. The two are written to the same
 * file under different keys, and a file without the floating key simply restores no windows — which
 * is what every layout saved before this feature existed looks like.
 */
public record FloatingLayout(int x, int y, int width, int height, EditorLayout layout) {

    public CompoundTag serialize() {
        var tag = layout.serialize();
        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("width", width);
        tag.putInt("height", height);
        return tag;
    }

    public static FloatingLayout deserialize(CompoundTag tag) {
        return new FloatingLayout(tag.getIntOr("x", 0), tag.getIntOr("y", 0),
                tag.getIntOr("width", 0), tag.getIntOr("height", 0), EditorLayout.deserialize(tag));
    }

    /**
     * Every view name in this window, in dock order.
     */
    public List<String> viewNames() {
        var names = new ArrayList<String>();
        for (var slot : layout.slots()) {
            names.addAll(slot.viewNames());
        }
        return names;
    }
}
