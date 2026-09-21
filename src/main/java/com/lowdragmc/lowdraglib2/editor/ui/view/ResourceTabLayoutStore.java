package com.lowdragmc.lowdraglib2.editor.ui.view;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;

/**
 * Persists a {@link ResourceTabLayout} per editor, next to the editor layouts.
 *
 * <p>Keyed by editor rather than by project type, because the strip belongs to the editor's own
 * furniture: which resource types are worth a tab, and how much room their tabs deserve, is a
 * property of the tool — a UI editor and a graph editor disagree about it while opening the very same
 * project type. One file holding one compound per key, so a key nobody writes any more costs a few
 * bytes instead of a stray file.
 */
public final class ResourceTabLayoutStore {

    private ResourceTabLayoutStore() {}

    private static File getFile() {
        var dir = new File(LDLib2.getAssetsDir().getParentFile(), "editor_layouts");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "resource_views.nbt");
    }

    private static CompoundTag read() {
        var file = getFile();
        if (!file.exists()) return new CompoundTag();
        try {
            var tag = NbtIo.read(file.toPath());
            return tag == null ? new CompoundTag() : tag;
        } catch (Exception e) {
            return new CompoundTag();
        }
    }

    /** The saved arrangement for {@code editorKey}, or a fresh default one when there is none. */
    public static ResourceTabLayout load(String editorKey) {
        var stored = read().getCompound(editorKey);
        if (stored.isEmpty()) {
            return new ResourceTabLayout();
        }
        try {
            return ResourceTabLayout.deserialize(stored.get());
        } catch (Exception e) {
            return new ResourceTabLayout();
        }
    }

    public static void save(String editorKey, ResourceTabLayout layout) {
        try {
            var root = read();
            root.put(editorKey, layout.serialize());
            NbtIo.write(root, getFile().toPath());
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to save the resource view layout for {}: ", editorKey, e);
        }
    }
}
