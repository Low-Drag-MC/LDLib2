package com.lowdragmc.lowdraglib2.editor.ui;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable snapshot of an {@link Editor}'s window layout: the {@link SplittableWindow}
 * split tree plus, for each leaf {@link ViewContainer}, which views inhabit it (in tab order)
 * and which is selected.
 *
 * <p>{@code path} is a string of {@code 'f'} (first) / {@code 's'} (second) characters describing
 * the descent from {@code rootWindow} to the leaf window containing the slot. Empty string means
 * {@code rootWindow} itself.
 */
public record EditorLayout(SplittableWindow.LayoutConfig layoutConfig, List<SlotEntry> slots) {

    public record SlotEntry(String path, List<String> viewNames, @Nullable String selectedViewName) {
        public CompoundTag serialize() {
            var tag = new CompoundTag();
            tag.putString("path", path);
            var list = new ListTag();
            for (var name : viewNames) {
                list.add(StringTag.valueOf(name));
            }
            tag.put("viewNames", list);
            if (selectedViewName != null) {
                tag.putString("selected", selectedViewName);
            }
            return tag;
        }

        public static SlotEntry deserialize(CompoundTag tag) {
            var path = tag.getString("path");
            var names = new ArrayList<String>();
            var list = tag.getList("viewNames", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                names.add(list.getString(i));
            }
            var selected = tag.contains("selected") ? tag.getString("selected") : null;
            return new SlotEntry(path, names, selected);
        }
    }

    public CompoundTag serialize() {
        var tag = new CompoundTag();
        tag.put("layout", layoutConfig.serialize());
        var list = new ListTag();
        for (var slot : slots) {
            list.add(slot.serialize());
        }
        tag.put("slots", list);
        return tag;
    }

    public static EditorLayout deserialize(CompoundTag tag) {
        var layout = SplittableWindow.LayoutConfig.deserialize(tag.getCompound("layout"));
        var list = tag.getList("slots", Tag.TAG_COMPOUND);
        var slots = new ArrayList<SlotEntry>();
        for (int i = 0; i < list.size(); i++) {
            slots.add(SlotEntry.deserialize(list.getCompound(i)));
        }
        return new EditorLayout(layout, slots);
    }

    /**
     * Returns the saved {@link SlotEntry} containing a view with the given name, or null if absent.
     */
    @Nullable
    public SlotEntry findSlotForView(String viewName) {
        for (var slot : slots) {
            if (slot.viewNames().contains(viewName)) {
                return slot;
            }
        }
        return null;
    }
}
