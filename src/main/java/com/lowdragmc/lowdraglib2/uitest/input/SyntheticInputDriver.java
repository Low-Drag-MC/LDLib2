package com.lowdragmc.lowdraglib2.uitest.input;

import com.lowdragmc.lowdraglib2.uitest.InputMode;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

/**
 * {@link InputMode#SYNTHETIC}: refresh the hover for the target position, then call the matching
 * method on the live {@code Screen}.
 *
 * <p>Going through {@code Screen} rather than {@code ModularUI#getWidget()} directly is deliberate —
 * {@code AbstractContainerScreen#mouseClicked} runs vanilla's slot-click path and its
 * {@code ServerboundContainerClickPacket}, so bypassing it would silently skip all {@code ItemSlot}
 * behaviour and make inventory tests pass while testing nothing.
 *
 * <p>Dispatching raw {@code UIEvent}s through {@code UIEventDispatcher} would be worse still: it
 * never populates {@code lastMouseDownButton} (so {@code UIElement#isMouseDown} stays false and
 * drags never start), never records {@code lastMouseClickTime} (so double-click never fires), and
 * never calls {@code requestFocus} (so focus, {@code __focused__} and keyboard routing all break).
 * That path is exposed only as an explicit escape hatch for exotic event types.
 */
public class SyntheticInputDriver extends InputDriver {

    @Override
    public void placeCursor(float x, float y) {
        cursorX = x;
        cursorY = y;
        // No glfwSetCursorPos. The physical pointer belongs to whoever is using the machine, and GLFW
        // ignores the call anyway while the window is unfocused. The CursorState override installed by
        // install() is what makes MouseHandler#xpos/ypos - and so the mouseX/mouseY every
        // Screen#render receives, and so ModularUI's per-frame hover - report this position instead.
        syncHover(x, y);
    }

    @Override
    public void moveTo(float x, float y) {
        placeCursor(x, y);
        var screen = screen();
        if (screen != null) {
            screen.mouseMoved(x, y);
        }
        dispatchedX = x;
        dispatchedY = y;
    }

    @Override
    public void dragTo(float x, float y, int button) {
        placeCursor(x, y);
        var screen = screen();
        if (screen != null) {
            screen.mouseMoved(x, y);
            screen.mouseDragged(mouseEvent(x, y, button, heldModifiers()), x - dispatchedX, y - dispatchedY);
        }
        dispatchedX = x;
        dispatchedY = y;
    }

    @Override
    public void mouseDown(float x, float y, int button) {
        placeCursor(x, y);
        var screen = screen();
        if (screen != null) {
            screen.mouseClicked(mouseEvent(x, y, button, heldModifiers()), false);
        }
        dispatchedX = x;
        dispatchedY = y;
    }

    @Override
    public void mouseUp(float x, float y, int button) {
        placeCursor(x, y);
        var screen = screen();
        if (screen != null) {
            screen.mouseReleased(mouseEvent(x, y, button, heldModifiers()));
        }
    }

    @Override
    public void scroll(float x, float y, double amount) {
        placeCursor(x, y);
        var screen = screen();
        if (screen != null) {
            screen.mouseScrolled(x, y, 0d, amount);
        }
    }

    @Override
    public void keyDown(int keyCode, int modifiers) {
        // Mark before dispatching: a handler may consult isShiftDown() for the very key being pressed.
        markHeld(keyCode, true);
        var screen = screen();
        if (screen != null) {
            screen.keyPressed(new KeyEvent(keyCode, Keys.scanCodeOf(keyCode), modifiers | heldModifiers()));
        }
    }

    @Override
    public void keyUp(int keyCode, int modifiers) {
        var screen = screen();
        if (screen != null) {
            screen.keyReleased(new KeyEvent(keyCode, Keys.scanCodeOf(keyCode), modifiers | heldModifiers()));
        }
        markHeld(keyCode, false);
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        var screen = screen();
        if (screen != null) {
            screen.charTyped(new CharacterEvent(codePoint));
        }
    }

    /**
     * A synthetic mouse event carrying the modifiers this driver currently considers held.
     *
     * <p>26.1 resolves every shortcut from the event's own modifier bits rather than from
     * {@code glfwGetKey}, so a synthesised chord only works if those bits are filled in here.
     */
    private static MouseButtonEvent mouseEvent(float x, float y, int button, int modifiers) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(button, modifiers));
    }
}
