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
     * <p>The OS cursor is still mirrored with {@code glfwSetCursorPos} so tooltips, the carried item
     * and the visible pointer agree with the logical position.
     */
    SYNTHETIC,
    /**
     * Move the real OS cursor and let Minecraft's own {@code MouseHandler}/{@code KeyboardHandler}
     * deliver the events.
     *
     * <p>Highest fidelity — it also covers code that reads GLFW directly, such as
     * {@code ModularUI#onFilesDrop} and {@code UIElement#isShiftDown()}. In exchange it is
     * frame-coupled and needs the window focused, so prefer it only when a scenario depends on real
     * key state (modifier-aware clicks) or raw cursor queries.
     */
    REAL
}
