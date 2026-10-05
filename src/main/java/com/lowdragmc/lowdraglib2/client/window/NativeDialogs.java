package com.lowdragmc.lowdraglib2.client.window;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLProperties;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * The system's own pickers, through SDL3's dialog API.
 *
 * <p>SDL shows them without blocking: depending on the platform the picker runs on a thread of its own
 * or is answered from the event pump, so the callback may arrive on any thread. Results are handed back
 * on the client thread.
 *
 * <p>One upcall stub serves every request, told apart by {@code userdata}, and is never freed: a stub
 * freed from the callback it is running, or from another thread while that callback is still returning
 * through it, is a crash.
 */
public final class NativeDialogs {

    private static final AtomicLong NEXT_ID = new AtomicLong(1);
    private static final Map<Long, BiConsumer<@Nullable String, @Nullable String>> PENDING = new ConcurrentHashMap<>();
    @Nullable
    private static SDL_DialogFileCallback callback;

    private NativeDialogs() {
    }

    /**
     * Opens the system folder picker, modal to {@code parentWindow}.
     *
     * @param parentWindow the SDL window to show it over, or {@code 0} for none
     * @param done         on the client thread: the picked folder's path, or {@code null} if the user
     *                     cancelled; or SDL's error, with the path {@code null}
     */
    public static void selectFolder(long parentWindow, String title, @Nullable File initialDir,
                                    BiConsumer<@Nullable String, @Nullable String> done) {
        RenderSystem.assertOnRenderThread();
        if (callback == null) {
            callback = SDL_DialogFileCallback.create(NativeDialogs::onResult);
        }
        long id = NEXT_ID.getAndIncrement();
        PENDING.put(id, done);
        int props = SDLProperties.SDL_CreateProperties();
        try {
            // SDL copies what it needs from these before returning, so they can go straight away
            if (parentWindow != 0) {
                SDLProperties.SDL_SetPointerProperty(props, SDLDialog.SDL_PROP_FILE_DIALOG_WINDOW_POINTER, parentWindow);
            }
            if (initialDir != null) {
                SDLProperties.SDL_SetStringProperty(props, SDLDialog.SDL_PROP_FILE_DIALOG_LOCATION_STRING, initialDir.getAbsolutePath());
            }
            SDLProperties.SDL_SetStringProperty(props, SDLDialog.SDL_PROP_FILE_DIALOG_TITLE_STRING, title);
            SDLProperties.SDL_SetBooleanProperty(props, SDLDialog.SDL_PROP_FILE_DIALOG_MANY_BOOLEAN, false);
            SDLDialog.SDL_ShowFileDialogWithProperties(SDLDialog.SDL_FILEDIALOG_OPENFOLDER, callback, id, props);
        } finally {
            SDLProperties.SDL_DestroyProperties(props);
        }
    }

    /** SDL's answer: {@code filelist} is null on an error, and empty (a null first entry) on a cancel. */
    private static void onResult(long userdata, long filelist, int filter) {
        var done = PENDING.remove(userdata);
        if (done == null) return;
        String path = null;
        String error = null;
        if (filelist == MemoryUtil.NULL) {
            error = SDLError.SDL_GetError();
            if (error == null || error.isEmpty()) error = "unknown error";
        } else {
            long first = MemoryUtil.memGetAddress(filelist);
            if (first != MemoryUtil.NULL) path = MemoryUtil.memUTF8(first);
        }
        var picked = path;
        var failed = error;
        Minecraft.getInstance().execute(() -> done.accept(picked, failed));
    }
}
