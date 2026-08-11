package com.lowdragmc.lowdraglib2.client.window;


import java.io.File;
import java.util.List;

/**
 * A raw GLFW callback, recorded rather than acted on.
 *
 * <p>Callbacks for a second window fire from inside {@code glfwPollEvents}, which Minecraft calls
 * from two places that are both bad times to run UI logic: the middle of
 * {@code RenderSystem.flipFrame}, between the buffer swap and the next frame; and inside
 * {@code RenderSystem.limitDisplayFPS}, where the game is deliberately idling in
 * {@code glfwWaitEventsTimeout}. On Windows there is a third — while the user drags or resizes a
 * decorated window, the platform runs a nested modal message loop inside that same poll.
 *
 * <p>So the callbacks only enqueue, and the host drains the queue at one defined point per frame,
 * immediately before it renders. That ordering also happens to be the one the UI requires: nothing
 * on {@code ModularUIWidget} hit-tests, so the hovered element has to be resolved before any pointer
 * event is dispatched.
 */
public sealed interface OsWindowEvent {

    /**
     * Cursor moved, in window screen coordinates. Coalesced on enqueue — the device reports far
     * faster than the UI needs and only the latest position matters.
     */
    record CursorPos(double x, double y) implements OsWindowEvent {
    }

    record MouseButton(int button, int action, int mods) implements OsWindowEvent {
    }

    record Scroll(double deltaX, double deltaY) implements OsWindowEvent {
    }

    record Key(int key, int scancode, int action, int mods) implements OsWindowEvent {
    }

    record Char(int codepoint, int mods) implements OsWindowEvent {
    }

    record CursorEnter(boolean entered) implements OsWindowEvent {
    }

    record FramebufferSize(int width, int height) implements OsWindowEvent {
    }

    record WindowPos(int x, int y) implements OsWindowEvent {
    }

    record Focus(boolean focused) implements OsWindowEvent {
    }

    record FileDrop(List<File> files) implements OsWindowEvent {
    }

    /**
     * The user asked to close the window. Purely advisory — GLFW has already been told not to act on
     * it, so the host decides.
     */
    record CloseRequest() implements OsWindowEvent {
    }

}
