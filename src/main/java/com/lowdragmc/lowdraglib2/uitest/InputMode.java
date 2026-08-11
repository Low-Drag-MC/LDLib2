package com.lowdragmc.lowdraglib2.uitest;

/**
 * How a scenario delivers synthetic input.
 */
public enum InputMode {
    /**
     * Move the logical cursor by refreshing {@code ModularUI}'s hover, then call the corresponding
     * method on the live {@link net.minecraft.client.gui.screens.Screen}. The default.
     *
     * <p>This is the whole real path — {@code Screen} → {@code ModularUIWidget} →
     * {@code UIEventDispatcher} → server RPC — including {@code AbstractContainerScreen}'s vanilla
     * slot handling, which any {@code ItemSlot} test depends on. It is deterministic: a step does
     * not have to wait a frame for the operating system to deliver a cursor event.
     *
     * <p>Nothing moves the physical pointer. The position is published through
     * {@link com.lowdragmc.lowdraglib2.gui.ui.utils.CursorState}, which a mixin on
     * {@code MouseHandler#xpos()/ypos()} answers with, so tooltips, the carried item and the hovered
     * slot follow it just as they would a real cursor — and the run needs neither the window's focus
     * nor the mouse of whoever is using the machine. It can therefore run in the background.
     */
    SYNTHETIC,
    /**
     * Move the real OS cursor and let Minecraft's own {@code MouseHandler}/{@code KeyboardHandler}
     * deliver the events.
     *
     * <p>Highest fidelity — it also covers code that reads GLFW directly, such as
     * {@code ModularUI#onFilesDrop} and {@code UIElement#isShiftDown()}. In exchange it is
     * frame-coupled, <b>focuses the game window and moves your physical pointer</b>, so the machine
     * is unusable for the duration of a run. Prefer it only when a scenario depends on real key state
     * (modifier-aware clicks) or raw cursor queries.
     */
    REAL
}
