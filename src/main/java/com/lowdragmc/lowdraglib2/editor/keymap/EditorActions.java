package com.lowdragmc.lowdraglib2.editor.keymap;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.resources.ResourceLocation;

/**
 * The ids of the actions every editor registers.
 *
 * <p>Public because an id is how anything else refers to a binding: a subclass replacing a built-in
 * action with its own, a project checking what a key is bound to, or a settings file naming one.
 */
public final class EditorActions {
    // File
    public static final ResourceLocation SAVE = LDLib2.id("editor.save");
    public static final ResourceLocation SAVE_AS = LDLib2.id("editor.save_as");
    public static final ResourceLocation OPEN_PROJECT = LDLib2.id("editor.open_project");
    public static final ResourceLocation SETTINGS = LDLib2.id("editor.settings");
    public static final ResourceLocation CLOSE_EDITOR = LDLib2.id("editor.close");

    // Edit — these run through the UI's command events, so whatever has focus handles them
    public static final ResourceLocation UNDO = LDLib2.id("editor.undo");
    public static final ResourceLocation REDO = LDLib2.id("editor.redo");
    public static final ResourceLocation COPY = LDLib2.id("editor.copy");
    public static final ResourceLocation CUT = LDLib2.id("editor.cut");
    public static final ResourceLocation PASTE = LDLib2.id("editor.paste");
    public static final ResourceLocation DUPLICATE = LDLib2.id("editor.duplicate");
    public static final ResourceLocation SELECT_ALL = LDLib2.id("editor.select_all");
    public static final ResourceLocation FIND = LDLib2.id("editor.find");

    // View
    public static final ResourceLocation NEXT_VIEW = LDLib2.id("editor.next_view");
    public static final ResourceLocation PREVIOUS_VIEW = LDLib2.id("editor.previous_view");
    public static final ResourceLocation MAXIMIZE_PANE = LDLib2.id("editor.maximize_pane");

    // Window
    public static final ResourceLocation MINIMIZE_WINDOW = LDLib2.id("editor.minimize_window");
    public static final ResourceLocation MAXIMIZE_WINDOW = LDLib2.id("editor.maximize_window");

    private EditorActions() {}
}
