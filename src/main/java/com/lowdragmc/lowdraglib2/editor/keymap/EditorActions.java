package com.lowdragmc.lowdraglib2.editor.keymap;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.resources.Identifier;

/**
 * The ids of the actions every editor registers.
 *
 * <p>Public because an id is how anything else refers to a binding: a subclass replacing a built-in
 * action with its own, a project checking what a key is bound to, or a settings file naming one.
 */
public final class EditorActions {
    // File
    public static final Identifier SAVE = LDLib2.id("editor.save");
    public static final Identifier SAVE_AS = LDLib2.id("editor.save_as");
    public static final Identifier OPEN_PROJECT = LDLib2.id("editor.open_project");
    public static final Identifier SETTINGS = LDLib2.id("editor.settings");
    public static final Identifier CLOSE_EDITOR = LDLib2.id("editor.close");

    // Edit — these run through the UI's command events, so whatever has focus handles them
    public static final Identifier UNDO = LDLib2.id("editor.undo");
    public static final Identifier REDO = LDLib2.id("editor.redo");
    public static final Identifier COPY = LDLib2.id("editor.copy");
    public static final Identifier CUT = LDLib2.id("editor.cut");
    public static final Identifier PASTE = LDLib2.id("editor.paste");
    public static final Identifier DUPLICATE = LDLib2.id("editor.duplicate");
    public static final Identifier SELECT_ALL = LDLib2.id("editor.select_all");
    public static final Identifier FIND = LDLib2.id("editor.find");

    // View
    public static final Identifier NEXT_VIEW = LDLib2.id("editor.next_view");
    public static final Identifier PREVIOUS_VIEW = LDLib2.id("editor.previous_view");
    public static final Identifier MAXIMIZE_PANE = LDLib2.id("editor.maximize_pane");

    // Window
    public static final Identifier MINIMIZE_WINDOW = LDLib2.id("editor.minimize_window");
    public static final Identifier MAXIMIZE_WINDOW = LDLib2.id("editor.maximize_window");

    private EditorActions() {}
}
