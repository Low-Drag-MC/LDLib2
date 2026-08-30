package com.lowdragmc.lowdraglib2.gui.ui.debugger;

import com.lowdragmc.lowdraglib2.client.window.OsWindow;
import com.lowdragmc.lowdraglib2.client.window.OsWindowEvent;
import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The UI debugger in an operating-system window of its own.
 *
 * <p>The debugger opens as a {@link com.lowdragmc.lowdraglib2.gui.holder.DebugScreen} — one keypress,
 * nothing else to know — and the window toggle in its title bar moves it here. What that buys is what
 * the panel cannot do: it can be sized past the game window, moved to a second monitor or pinned
 * above everything, and the inspected UI stays visible <em>and live</em> underneath because nothing
 * is layered over it any more. {@link ModularUI#setDebuggerWindowed} moves it either way, and the
 * size, position and pin survive the trip — see {@link UIDebuggerWindowState}.
 *
 * <p>Nothing about the debugger itself moved into this class. The outlines, the box model and the
 * element picker are drawn and handled by {@link UIDebugger} as part of the <em>inspected</em> UI's
 * own frame — see {@link UIDebugger#renderHostOverlay} — which is what makes this work across
 * windows at all: the UI under inspection may be drawn in the game window, or in a floating window
 * of its own, and either way its overlay lands where it belongs rather than in the debugger's.
 *
 * <p>That is also why the target can be switched at runtime. Press F3 in any window and that
 * window's UI gets a debugger; the picker in the title bar then reaches every other UI on screen
 * without leaving this window — including UIs in windows of their own, whose outlines and element
 * picking then happen over there rather than here.
 */
@OnlyIn(Dist.CLIENT)
public class UIDebuggerWindow extends ModularUIWindow {

    private static final String TITLE = "UI Debugger";
    /** So a second debugger does not land exactly on the first. */
    private static final int CASCADE_STEP = 28;

    /** A UI this window can be pointed at, and what to call it in the picker. */
    private record Candidate(String label, ModularUI ui) {
    }

    private final UIElement root;
    /**
     * The window's own controls, parented into whichever {@link UIDebugger}'s title bar is current
     * rather than given a bar of their own — one title bar, not two stacked ones.
     */
    private final UIElement chrome = new UIElement();
    private final UIElement targetPicker = new UIElement();
    /** Pins the window above the others. Hidden where the platform will not honour it. */
    public final Toggle alwaysOnTopToggle;

    @Getter
    private UIDebugger debugger;
    @Getter
    private ModularUI target;

    /**
     * The window showing the inspected UI, when another window is what it lives in — so this one can
     * go away with it. Null when the inspected UI belongs to a screen instead, which
     * {@link ModularUI#isRemoved()} answers for.
     */
    @Nullable
    private ModularUIWindow hostWindow;

    /** What the picker was last built from, so it is only rebuilt when the answer changes. */
    private List<Candidate> pickerCandidates = List.of();
    /**
     * A retarget asked for by the picker, applied at the top of the next frame.
     *
     * <p>Not applied where it is requested: that is inside the picker button's own event dispatch,
     * and retargeting detaches the debugger — the button's own great-grandparent — mid-walk.
     */
    @Nullable
    private ModularUI pendingTarget;

    /**
     * Whether closing this window should also take the inspected UI out of debug mode. False only
     * while the debugger is being handed to the other host — see {@link #handOff()}.
     */
    private boolean releaseOnClose = true;

    // ------------------------------------------------------------------------------------ opening

    /**
     * Shows {@code debugger} in a window, reusing the one already inspecting its target if there is
     * one.
     *
     * @return {@code false} if no second window could be opened, in which case the debugger should
     *         stay in (or go back to) {@link com.lowdragmc.lowdraglib2.gui.holder.DebugScreen}
     */
    public static boolean openFor(UIDebugger debugger) {
        var existing = windowFor(debugger.modularUI);
        if (existing != null) return true;
        if (!OsWindowManager.isAvailable()) return false;
        var state = UIDebuggerWindowState.get();
        var offset = CASCADE_STEP * debuggerWindows().size();
        var window = new UIDebuggerWindow(debugger);
        if (!window.open(state.xOffsetBy(offset), state.yOffsetBy(offset),
                state.width(), state.height(), false)) {
            // Nothing to undo: the debugger is only taken over once the window really exists, so a
            // platform that refuses one leaves it exactly where it was.
            return false;
        }
        window.attach(debugger);
        window.setAlwaysOnTop(state.alwaysOnTop());
        return true;
    }

    /** Every debugger window currently open, in the order they were opened. */
    public static List<UIDebuggerWindow> debuggerWindows() {
        var windows = new ArrayList<UIDebuggerWindow>();
        for (var window : ModularUIWindow.openWindows()) {
            if (window instanceof UIDebuggerWindow debuggerWindow) {
                windows.add(debuggerWindow);
            }
        }
        return windows;
    }

    /** The window inspecting {@code target}, or {@code null} if nothing is. */
    @Nullable
    public static UIDebuggerWindow windowFor(ModularUI target) {
        for (var window : debuggerWindows()) {
            if (window.getTarget() == target) return window;
        }
        return null;
    }

    /** Closes the window inspecting {@code target}, if there is one. Safe to call when there is not. */
    public static void closeFor(ModularUI target) {
        var window = windowFor(target);
        if (window != null) {
            window.close();
        }
    }

    // ------------------------------------------------------------------------------- construction

    public UIDebuggerWindow(UIDebugger debugger) {
        this(debugger, new UIElement().layout(layout -> layout.widthPercent(100).heightPercent(100)));
    }

    private UIDebuggerWindow(UIDebugger debugger, UIElement root) {
        // The debugger is styled by the modern theme and nothing else; it deliberately does not follow
        // the inspected UI's theme, or a mod shipping a dark-on-dark one would make it unreadable.
        super(ModularUI.of(UI.of(root, StylesheetManager.INSTANCE.getStylesheet(StylesheetManager.MODERN))), TITLE);
        this.root = root;

        targetPicker.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(2);
            layout.heightPercent(100);
        });
        var maximizeButton = new Button().setOnClick(e -> toggleMaximized())
                .noText()
                .addPreIcon(DynamicTexture.of(() -> isMaximized() ? Icons.WINDOW_RESTORE : Icons.WINDOW_MAXIMIZE))
                .addClass("__white_icon__")
                .layout(layout -> layout.height(12));
        var closeButton = new Button().setOnClick(e -> onCloseRequested())
                .noText()
                .addPreIcon(Icons.WINDOW_CLOSE)
                .addClass("__white_icon__")
                .layout(layout -> layout.height(12));
        alwaysOnTopToggle = buildAlwaysOnTopToggle();
        chrome.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(2);
            layout.height(14);
        }).addChildren(targetPicker, alwaysOnTopToggle, maximizeButton, closeButton);

        // F3 here means "close me", handled below — not "open a debugger on the debugger", which is
        // what ModularUIWidget would otherwise do with it.
        getModularUI().setAllowDebugMode(false);

        // The target is known from the start so windowFor() can find this window the moment it is
        // registered; the debugger element itself is adopted by attach(), once the window is open.
        this.target = debugger.modularUI;
    }

    /**
     * The pin, styled like the debugger's own two toggles so the title bar reads as one row of
     * controls.
     *
     * <p>Hidden outright where the platform will not honour it rather than left there doing nothing —
     * see {@link OsWindow#supportsAlwaysOnTop()}.
     */
    private Toggle buildAlwaysOnTopToggle() {
        var toggle = (Toggle) new Toggle()
                .setText("")
                .setOn(UIDebuggerWindowState.get().alwaysOnTop(), false)
                .toggleButton(button -> button.layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                }))
                .setOnToggleChanged(this::setAlwaysOnTop)
                .toggleStyle(style -> {
                    style.setPipelineState(StyleOrigin.DEFAULT);
                    style.baseTexture(Sprites.BORDER1_RT1_DARK);
                    style.hoverTexture(Sprites.BORDER1_RT1);
                    style.setPipelineState(StyleOrigin.INLINE);
                    style.unmarkTexture(Icons.MAGNET.copy().setColor(ColorPattern.GRAY.color).scale(0.8f));
                    style.markTexture(Icons.MAGNET.copy().scale(0.8f));
                })
                // Bound to the remembered intent, not to the live window attribute: the toggle has to
                // read correctly before the window exists and while it is closing.
                .bindDataSource(SupplierDataSource.of(() -> UIDebuggerWindowState.get().alwaysOnTop()))
                .layout(layout -> {
                    layout.paddingAll(0);
                    layout.heightPercent(100);
                    layout.setAspectRatio(1f);
                })
                .style(style -> style.tooltips("debugger.always_on_top.0"));
        toggle.setDisplay(OsWindow.supportsAlwaysOnTop());
        return toggle;
    }

    /**
     * Pins the window above the others, and remembers the choice for the next time it is opened.
     */
    @Override
    public void setAlwaysOnTop(boolean onTop) {
        var state = UIDebuggerWindowState.get();
        UIDebuggerWindowState.put(new UIDebuggerWindowState(
                state.x(), state.y(), state.width(), state.height(), onTop));
        super.setAlwaysOnTop(onTop);
    }

    /**
     * The debugger's shortcuts, taken before the UI in this window sees them.
     *
     * <p>They cannot be ordinary key listeners: {@code ModularUIWidget#keyPressed} only dispatches
     * KEY_DOWN to the focused element, so a chord that is meant to work whatever has focus has to be
     * caught at the window's edge.
     */
    @Override
    protected void handleEvent(OsWindowEvent event) {
        // PRESS only, not REPEAT: a held F1 would otherwise toggle focus mode dozens of times a second.
        if (event instanceof OsWindowEvent.Key key && key.action() == GLFW.GLFW_PRESS
                && handleShortcut(key.key())) {
            return;
        }
        super.handleEvent(event);
    }

    private boolean handleShortcut(int keyCode) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_F1 -> debugger.setFocusMode(!debugger.isFocusMode());
            case GLFW.GLFW_KEY_F4 -> debugger.setRenderUIShaping(!debugger.isRenderUIShaping());
            // F3 and not escape: escape belongs to whatever has focus in here — the LSS editor, a
            // text field — and closing the window out from under it would be its own bug report.
            case GLFW.GLFW_KEY_F3 -> onCloseRequested();
            default -> {
                return false;
            }
        }
        return true;
    }

    /**
     * Points this window at {@code debugger}, taking over its layout and its title bar.
     */
    private void attach(UIDebugger next) {
        if (debugger != null && debugger != next) {
            debugger.removeSelf();
        }
        chrome.removeSelf();

        debugger = next;
        target = next.modularUI;
        next.setFloating(false);
        root.addChild(next);
        next.titleBar.addChild(chrome);
        // The whole title bar moves the window; the buttons and toggles in it are blockers.
        setDragArea(next.titleBar);

        hostWindow = ModularUIWindow.windowOf(target);
        // Force a rebuild: which entry is marked as selected has changed even if the list has not.
        pickerCandidates = List.of();
        setTitle(hostWindow == null ? TITLE : TITLE + " - " + hostWindow.getTitle());
    }

    /**
     * Inspects a different UI.
     *
     * <p>The previous target stops reporting itself as debugged, but through the flag rather than
     * {@link ModularUI#enableDebugger}: nothing closed, this window simply looks elsewhere, and
     * going the long way round would have it close itself on the way past.
     */
    public void setTarget(ModularUI next) {
        if (next == target) return;
        var previous = target;
        attach(next.acquireDebugger());
        previous.setDebugMode(false);
    }

    // ------------------------------------------------------------------------------------ picking

    /**
     * Every UI this window could be pointed at: the game window's, and each floating window's.
     */
    private List<Candidate> candidates() {
        var candidates = new ArrayList<Candidate>();
        var screenUI = ModularUI.of(Minecraft.getInstance().screen);
        if (screenUI != null) {
            candidates.add(new Candidate("Game Window", screenUI));
        }
        for (var window : ModularUIWindow.openWindows()) {
            // Not other debuggers. Inspecting one from another is a hall of mirrors, and inspecting
            // this one from itself would have the tree redraw itself as you walked it.
            if (window instanceof UIDebuggerWindow) continue;
            candidates.add(new Candidate(window.getTitle(), window.getModularUI()));
        }
        // The current target may be hosted by neither, or its host may have just gone; it still has to
        // be offered or the picker would show no way back to what is actually on screen.
        if (candidates.stream().noneMatch(candidate -> candidate.ui() == target)) {
            candidates.addFirst(new Candidate("Target", target));
        }
        return candidates;
    }

    private void refreshTargetPicker() {
        var candidates = candidates();
        if (candidates.equals(pickerCandidates)) return;
        pickerCandidates = candidates;
        targetPicker.clearAllChildren();
        // One candidate is not a choice; do not spend title bar width on it.
        targetPicker.setDisplay(candidates.size() > 1);
        if (candidates.size() <= 1) return;
        for (var candidate : candidates) {
            var selected = candidate.ui() == target;
            targetPicker.addChild(new Button()
                    .setText(selected ? "[" + candidate.label() + "]" : candidate.label())
                    .setOnClick(e -> pendingTarget = candidate.ui())
                    .layout(layout -> layout.height(12)));
        }
    }

    // ------------------------------------------------------------------------------------ lifetime

    /**
     * Whether the inspected UI is still alive.
     *
     * <p>Checked per frame rather than hooked into every teardown path, for the same reason
     * {@code FloatingViewWindow} checks its own emptiness that way: a UI goes away through a screen
     * change, a window close and a game shutdown, and one check covers all of them. Without it, a
     * window is left on the user's desktop showing a tree nothing is drawing any more.
     *
     * <p>{@link ModularUI#isRemoved()} and not "is it {@code Minecraft#screen}": pushing a gui layer
     * — a dialog, or the debugger's own screen host on the way back — leaves the screen underneath
     * perfectly alive but no longer current, and closing on that would be maddening.
     */
    private boolean isTargetShown() {
        if (hostWindow != null) return hostWindow.isOpen();
        return !target.isRemoved();
    }

    @Override
    public void renderFrame(float partialTick) {
        var pending = pendingTarget;
        if (pending != null) {
            pendingTarget = null;
            setTarget(pending);
        }
        // Between frames, which is the only safe place: switching hosts reparents this window's whole
        // contents and then closes it.
        target.applyPendingDebuggerHost();
        if (!isOpen()) return;
        if (!isTargetShown()) {
            onCloseRequested();
            return;
        }
        refreshTargetPicker();
        super.renderFrame(partialTick);
    }

    @Override
    public void onCloseRequested() {
        // Cleared before closing, so ModularUI#enableDebugger's own teardown finds nothing left to do
        // rather than coming straight back here.
        target.setDebugMode(false);
        close();
    }

    /**
     * Closes the window without ending the debugging session, because the debugger is moving to the
     * other host rather than going away.
     */
    public void handOff() {
        releaseOnClose = false;
        close();
    }

    @Override
    public void onDestroyed() {
        rememberGeometry();
        super.onDestroyed();
        // Only if it is still ours. A hand-off to the screen host has already reparented it, and
        // taking it back off there would leave the debugger with no host at all.
        if (debugger != null && debugger.getParent() == root) {
            debugger.removeSelf();
        }
        chrome.removeSelf();
        if (releaseOnClose) {
            target.setDebugMode(false);
        }
    }

    /** Records where the window was, so the next one opens there. */
    private void rememberGeometry() {
        var bounds = restoredBounds();
        if (bounds == null) return;
        UIDebuggerWindowState.put(new UIDebuggerWindowState(bounds.x(), bounds.y(),
                bounds.width(), bounds.height(), UIDebuggerWindowState.get().alwaysOnTop()));
    }

    @Override
    protected boolean isDragBlocker(UIElement element) {
        // Toggle is not a Button, and the debugger's focus-mode and shaping toggles sit inside the
        // title bar - without this, pressing one would start dragging the window.
        return super.isDragBlocker(element) || element instanceof Toggle;
    }
}
