package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow;
import lombok.Getter;

/**
 * A whole {@link EditorWindow} hosted in an operating-system window, with no {@code Screen} behind it
 * — so the game window is left playable while the editor is open beside it.
 *
 * <p>Contrast with {@link com.lowdragmc.lowdraglib2.editor.ui.floating.FloatingViewWindow}, which
 * tears one pane out of an editor hosted elsewhere.
 *
 * <p>⚠️ The gui scale is the game window's and cannot be its own — see
 * {@link com.lowdragmc.lowdraglib2.gui.ui.rendering.UISurface}.
 */
public class EditorOsWindow extends ModularUIWindow {

    /** How often the OS title is re-read from the editor, in frames. */
    private static final int TITLE_POLL_FRAMES = 20;

    @Getter
    private final EditorWindow editorWindow;
    private int frame;

    public EditorOsWindow(EditorWindow editorWindow) {
        super(new ModularUI(UI.of(editorWindow)), titleOf(editorWindow));
        this.editorWindow = editorWindow;
        editorWindow._setOsWindowInternal(this);
        syncDragArea();
    }

    private static String titleOf(EditorWindow editorWindow) {
        var editor = editorWindow.getCurrentEditor();
        return editor == null ? "Editor" : editor.getTitle().getString();
    }

    /**
     * Points the move gesture at the current editor's title bar. Pushed from
     * {@link EditorWindow#showEditor} because switching tabs changes which bar is on screen.
     */
    public void syncDragArea() {
        var editor = editorWindow.getCurrentEditor();
        setDragArea(editor == null ? null : editor.topPlaceholder);
    }

    /**
     * The editor's close path, not the window's: the "save before closing?" dialog is drawn in this
     * window, so it has to outlive the question.
     */
    @Override
    public void onCloseRequested() {
        editorWindow.closeWindow();
    }

    @Override
    public void renderFrame(float partialTick) {
        if (frame++ % TITLE_POLL_FRAMES == 0) {
            var title = titleOf(editorWindow);
            if (!title.equals(getTitle())) {
                setTitle(title);
            }
        }
        super.renderFrame(partialTick);
    }

    @Override
    public void onDestroyed() {
        editorWindow.rememberBounds(restoredBounds());
        editorWindow._setOsWindowInternal(null);
        super.onDestroyed();
        // What a screen's UI gets when the screen goes (ScreenMixin), and a window has nothing else to give it:
        // without it an editor closed in its own window was never torn down. A move to the game window or a
        // minimize keeps the tree alive — EditorWindow#onRemoved stands down for both.
        getModularUI().onRemoved();
    }
}
