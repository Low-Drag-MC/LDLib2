package com.lowdragmc.lowdraglib2.uitest.input;

/**
 * Observes the primitive input a driver delivers, after it has been dispatched. Coordinates are
 * GUI-scaled screen space, as for {@link InputDriver}.
 *
 * <p>Only {@link SyntheticInputDriver} reports to it. Called on the render thread.
 *
 * @see InputDriver#setListener(InputListener)
 */
public interface InputListener {

    default void mouseDown(float x, float y, int button) {
    }

    default void mouseUp(float x, float y, int button) {
    }

    default void scroll(float x, float y, double amount) {
    }

    default void keyDown(int keyCode, int modifiers) {
    }

    default void keyUp(int keyCode, int modifiers) {
    }

    default void charTyped(char codePoint) {
    }
}
