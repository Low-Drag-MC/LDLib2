package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.io.File;

/**
 * The sizes window mode dialogs were last resized to, by the key they {@link Dialog#rememberSize remember} it
 * under. Kept across game sessions in one small file next to the editor layouts.
 */
public final class DialogSizeStore {

    /** Read once per session; every later get and put goes through here. */
    @Nullable
    private static CompoundTag cached;

    private DialogSizeStore() {}

    @Nullable
    public static Vector2f get(String key) {
        var size = read().getCompound(key).orElse(null);
        if (size == null) return null;
        // no smaller than a resize can make it: a damaged entry must not open the dialog at nothing
        return new Vector2f(Math.max(Dialog.MIN_WINDOW_SIZE, size.getFloatOr("width", 0)),
                Math.max(Dialog.MIN_WINDOW_SIZE, size.getFloatOr("height", 0)));
    }

    public static void put(String key, float width, float height) {
        var root = read();
        var size = new CompoundTag();
        size.putFloat("width", width);
        size.putFloat("height", height);
        if (size.equals(root.get(key))) return;
        root.put(key, size);
        try {
            var file = file();
            file.getParentFile().mkdirs();
            NbtIo.write(root, file.toPath());
        } catch (Exception e) {
            LDLib2.LOGGER.warn("Failed to save the size of dialog {}", key, e);
        }
    }

    private static File file() {
        return new File(new File(LDLib2.getAssetsDir().getParentFile(), "editor_layouts"), "dialog_sizes.nbt");
    }

    private static CompoundTag read() {
        if (cached == null) {
            cached = new CompoundTag();
            var file = file();
            if (file.exists()) {
                try {
                    var tag = NbtIo.read(file.toPath());
                    if (tag != null) cached = tag;
                } catch (Exception ignored) {
                    // a corrupt file costs the remembered sizes, not the dialog
                }
            }
        }
        return cached;
    }
}
