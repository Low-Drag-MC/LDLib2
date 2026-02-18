package com.lowdragmc.lowdraglib2.editor;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import lombok.Getter;
import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.Nullable;
import java.util.function.Supplier;

@KJSBindings
public final class ClipboardManager {
    public static ClipboardManager INSTANCE = new ClipboardManager();
    private ClipboardManager() {}

    // runtime
    @Getter
    @Nullable
    private Object clipboardContent;
    @Getter
    @Nullable
    private Class<?> clipboardType;
    @Nullable
    private Supplier<?> pasteSupplier;

    public synchronized void clear() {
        clipboardContent = null;
        clipboardType = null;
        pasteSupplier = null;
    }

    public synchronized void copyDirect(Object obj) {
        clipboardContent = obj;
        clipboardType = obj.getClass();
        if (obj instanceof String string) {
            Minecraft.getInstance().keyboardHandler.setClipboard(string);
        }
    }

    public synchronized void copy(Supplier<?> pasteSupplier) {
        this.pasteSupplier = pasteSupplier;
        this.clipboardType = pasteSupplier.get().getClass();
    }

    public synchronized void copy(Supplier<?> pasteSupplier, Class<?> type) {
        this.pasteSupplier = pasteSupplier;
        this.clipboardType = type;
    }

    @Nullable
    public synchronized <T> T paste() {
        if (pasteSupplier != null) {
            return (T) pasteSupplier.get();
        }
        return (T) clipboardContent;
    }
    
}
