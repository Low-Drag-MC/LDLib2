package com.lowdragmc.lowdraglib2.editor.ui.floating;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.SplittableWindow;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * An editor pane torn off into its own operating-system window.
 *
 * <p>Everything about being a window — moving, resizing, the pointer shapes, keeping the parent's
 * theme — comes from {@link ModularUIWindow} and is available to any LDLib UI. What is added here is
 * only the dock behaviour: the window's contents are a real {@link SplittableWindow} tree, so tabs,
 * splitting and dragging a tab between this window's own panes all work with the code that already
 * does that in the editor.
 *
 * <p>What is deliberately <em>not</em> supported is dragging a tab between windows — GLFW gives no
 * cross-window drag, and faking it needs global cursor tracking and hit-testing against every window,
 * so floating and docking back are menu-driven instead.
 */
@OnlyIn(Dist.CLIENT)
public class FloatingViewWindow extends ModularUIWindow {

    @Getter
    private final Editor editor;
    @Getter
    private final FloatingViewRoot root;

    public FloatingViewWindow(Editor editor, Component title) {
        this(editor, new FloatingViewRoot(), title);
    }

    private FloatingViewWindow(Editor editor, FloatingViewRoot root, Component title) {
        super(new ModularUI(UI.of(root)), title.getString());
        this.editor = editor;
        this.root = root;
        root.setHostedEditor(editor);
        root.setTitle(title);
        // Themes scope editor styling under the editor element's own classes — every built-in
        // stylesheet has rules like `.__editor__ code-editor { ... }`. A floated view is no longer a
        // descendant of that element, so without carrying its classes across, every such rule stops
        // matching and the view renders unstyled. Copied rather than hardcoded so an Editor subclass
        // that adds its own scoping class keeps working.
        root.addClasses(editor.getClasses().toArray(new String[0]));
        root.closeButton.setOnClick(e -> onCloseRequested());
        root.maximizeButton.setOnClick(e -> toggleMaximized());
        // The whole title bar moves the window; the buttons in it are blockers by default.
        setDragArea(root.titleBar);
        // The editor's theme lives in its style engine, not in its UI's stylesheet list, and can be
        // switched while this window is open — so follow it rather than copying it once.
        setStyleSource(editor.getModularUI());
    }

    public SplittableWindow rootWindow() {
        return root.rootWindow;
    }

    /**
     * Moves {@code view} into this window, selecting it.
     */
    public void dock(View view) {
        var container = root.rootWindow.getLeftTop();
        container.addView(view);
        container.selectView(view);
    }

    public List<View> views() {
        return root.rootWindow.getAllViews();
    }

    public boolean isEmpty() {
        return !root.rootWindow.hasAnyView();
    }

    /**
     * Closes the window as soon as its last tab is gone, the way a docked pane collapses when it
     * empties. Checked per frame rather than hooked into every removal path — views leave a window
     * through a close button, a menu, a drag between its own panes and a dock-back, and one check
     * covers all of them.
     */
    @Override
    public void renderFrame(float partialTick) {
        if (isEmpty()) {
            onCloseRequested();
            return;
        }
        root.setMaximized(isMaximized());
        super.renderFrame(partialTick);
    }
}
