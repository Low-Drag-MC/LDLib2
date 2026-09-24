package com.lowdragmc.lowdraglib2.gui.ui.event;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.Pair;
import lombok.ToString;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@ToString
public class UIEvent {
    // region CODEC
    public final static Codec<UIEvent> CODEC = Codec.STRING
            .comapFlatMap(type -> DataResult.success(UIEvent.create(type)), event -> event.type)
            .stable();

    public final static StreamCodec<FriendlyByteBuf, UIEvent> STREAM_CODEC = StreamCodec.of(
            (byteBuf, event) -> {
                byteBuf.writeUtf(event.type);
                byteBuf.writeVarInt(event.button);
                if (event.x != 0 || event.y != 0 || event.deltaX != 0 || event.deltaY != 0) {
                    byteBuf.writeBoolean(true);
                    byteBuf.writeFloat(event.x);
                    byteBuf.writeFloat(event.y);
                    byteBuf.writeFloat(event.deltaX);
                    byteBuf.writeFloat(event.deltaY);
                } else {
                    byteBuf.writeBoolean(false);
                }
                if (event.keyCode != 0 || event.shortcutKey != 0 || event.modifiers != 0 || event.codePoint != 0 ) {
                    byteBuf.writeBoolean(true);
                    byteBuf.writeVarInt(event.keyCode);
                    byteBuf.writeVarInt(event.shortcutKey);
                    byteBuf.writeVarInt(event.modifiers);
                    byteBuf.writeVarInt(event.codePoint);
                } else {
                    byteBuf.writeBoolean(false);
                }
                if (event.command != null) {
                    byteBuf.writeBoolean(true);
                    byteBuf.writeUtf(event.command);
                } else {
                    byteBuf.writeBoolean(false);
                }
            },
            byteBuf -> {
                var event = UIEvent.create(byteBuf.readUtf());
                event.button = byteBuf.readVarInt();
                if (byteBuf.readBoolean()) {
                    event.x = byteBuf.readFloat();
                    event.y = byteBuf.readFloat();
                    event.deltaX = byteBuf.readFloat();
                    event.deltaY = byteBuf.readFloat();
                }
                if (byteBuf.readBoolean()) {
                    event.keyCode = byteBuf.readVarInt();
                    event.shortcutKey = byteBuf.readVarInt();
                    event.modifiers = byteBuf.readVarInt();
                    event.codePoint = byteBuf.readVarInt();
                }
                if (byteBuf.readBoolean()) {
                    event.command = byteBuf.readUtf();
                }
                return event;
            }
    );
    // endregion

    /**
     * EventPhase represents the phase of the event in the event flow.
     */
    public enum EventPhase {
        CAPTURE,
        AT_TARGET,
        BUBBLE
    }

    /**
     * Event type, e.g., "click", "moseEnter", "mouseLeave" etc.
     */
    public final String type;
    /**
     * Event time stamp, the time when the event was created.
     */
    public final long timeStamp = System.currentTimeMillis();
    /**
     * Mouse buttons, in this library's own numbering rather than the windowing backend's. SDL numbers
     * them left 1, middle 2, right 3 — and GLFW, before it, left 0, right 1, middle 2. {@link #button}
     * keeps the latter no matter which backend the input came from, so a check written as
     * {@code event.button == BUTTON_LEFT} means the same thing on every version; the conversion happens
     * once, where input enters a UI, see {@link #buttonFromInput(int)}.
     */
    public static final int BUTTON_LEFT = 0;
    public static final int BUTTON_RIGHT = 1;
    public static final int BUTTON_MIDDLE = 2;
    /** The side buttons, "back" and "forward" in a browser. */
    public static final int BUTTON_4 = 3;
    public static final int BUTTON_5 = 4;

    /**
     * Mouse Event data
     */
    public float x, y, deltaX, deltaY;
    /**
     * The mouse button, one of the {@code BUTTON_*} constants above.
     */
    public int button;
    /**
     * Drag Event data
     */
    public float dragStartX, dragStartY;
    public DragHandler dragHandler;
    /**
     * Key Event data.
     * <ul>
     *   <li>{@code keyCode} is the physical key: an SDL scancode, the {@code InputConstants.KEY_*} values.
     *   It names a key by where it sits on a US keyboard, whatever the layout. Use it for keys whose
     *   position is what matters — arrows, Enter, Delete, WASD.</li>
     *   <li>{@code shortcutKey} is what the key means under the current layout: an SDL keycode, the
     *   {@code InputConstants.KEYCODE_*} / {@code SDLKeycode.SDLK_*} values — for a letter, its lower-case
     *   character. Use it for shortcuts named after a letter, the way vanilla's {@code KeyEvent#isCopy}
     *   does, so Ctrl+Z is the key labelled Z on an AZERTY keyboard too.</li>
     *   <li>{@code modifiers} is the {@code SDL_KMOD_*} mask, see {@code InputConstants.MOD_*}.</li>
     * </ul>
     */
    public int keyCode, shortcutKey, modifiers;
    /**
     * Hover Tooltips
     */
    public HoverTooltips hoverTooltips;
    /**
     * File Drop Event data: the files dropped onto the window from outside the game.
     */
    public List<File> droppedFiles = List.of();
    /**
     * Command name
     */
    public String command;
    /**
     * The typed character, as a Unicode code point — one that may lie outside the basic multilingual
     * plane, such as an emoji, so it does not always fit in a {@code char}.
     */
    public int codePoint;
    @Nullable
    public Object customData;
    /**
     * Event target, the element that triggered the event.
     */
    public EventPhase phase;
    /**
     * Whether the event has a capture phase and a bubble phase.
     */
    public boolean hasCapturePhase = true, hasBubblePhase = true;
    /**
     * The target element that the event is dispatched to.
     * <br>
     * The related target element may be used in some events. e.g. {@code focus}, {@code blur}, {@code focusIn}, {@code focusOut}.
     */
    public UIElement target, relatedTarget;
    /**
     * The element that is currently being processed.
     */
    public UIElement currentElement;
    /**
     * The listener that is currently being processed.
     */
    public UIEventListener currentListener;
    /**
     * Whether a keymap has already decided what this key press means.
     *
     * <p>Set on a {@code keyDown} that reached a host with a keymap of its own — an editor. The built-in
     * chord table in {@code ModularUI} is the fallback for UIs that have no keymap, and it must not fire
     * as well: it is hardcoded, so a shortcut the user rebound would otherwise keep working on its old
     * key too. Being <em>seen</em> is what counts, not being matched — an action that was unbound is
     * exactly the case where the old chord must stop working.
     */
    public boolean keymapResolved = false;
    /**
     * Whether the propagation is canceled.
     */
    public boolean propagationStopped = false;
    /**
     * Whether the immediate propagation is canceled.
     */
    public boolean laterPropagationStopped = false;
    /**
     * Indicates whether there is a handler associated with the event.
     * This variable helps determine if the event has at least one listener
     * registered that should process it.
     */
    public boolean hasHandler = false;
    public List<Pair<UIElement, UIEventListener>> captureListeners = new ArrayList<>();
    public List<Pair<UIElement, UIEventListener>> bubbleListeners = new ArrayList<>();

    private UIEvent(String type) {
        this.type = type;
    }

    //TODO Shall we use an Event Pool here to avoid the cost of creating instances?
    public static UIEvent create(String type) {
        return new UIEvent(type);
    }

    /**
     * Stops the event from propagating to all later phases.
     * <br>
     * <b>Capture</b> and <b>bubbling</b> both cease: Regardless of whether the event is currently in the capture stage or the bubbling stage, the propagation is immediately interrupted.
     * Applicable scenario: When a certain processor clearly knows that the event should be completely intercepted, for example:
     * <li> A dialog box captures click events and does not want the events to bubble up to the parent level (such as the main interface).
     * <li> A full-screen pop-up window captures all inputs to prevent underlying elements from responding.
     */
    public void stopPropagation() {
        this.propagationStopped = true;
    }

    /**
     * Stops the event from propagating to other listeners and prevents any further event listeners of the current phase.
     * <br>
     * No impact on capture or bubbling: The event propagation of other nodes is not affected.
     * Applicable scenario: When a certain listener knows that it is the only listener that should handle this event, for example:
     * <li> A button has multiple listeners, and one of them is the logic of "highest priority".
     * <li> A certain listener has already handled the event and does not want other listeners on the same node to handle it repeatedly.
     */
    public void stopImmediatePropagation() {
        this.propagationStopped = true;
        this.laterPropagationStopped = true;
    }

    /**
     * Prevents the event from propagating to subsequent phases or handlers in the current event flow.
     *
     * By marking the {@code laterPropagationStopped} flag as {@code true}, this method ensures that
     * after the current phase/process, no further handlers or operations related to this event are executed.
     *
     * This method is typically used to indicate that later stages (such as bubbling or additional listeners)
     * should no longer process the event after the current context has handled it.
     *
     * Similar to {@link #stopPropagation()}, but specific to scenarios where subsequent processing based
     * on dynamic event flow must be halted without immediately affecting current propagation behavior.
     */
    public void stopLaterPropagation() {
        this.laterPropagationStopped = true;
    }

    public boolean isShiftDown() {
        return UIElement.isShiftDown();
    }

    public boolean isCtrlDown() {
        return UIElement.isCtrlOrCmdDown();
    }

    public boolean isAltDown() {
        return UIElement.isAltDown();
    }

    public boolean isKeyDown(int keyCode) {
        return UIElement.isKeyDown(keyCode);
    }

    public boolean isLeftButton() {
        return button == BUTTON_LEFT;
    }

    public boolean isRightButton() {
        return button == BUTTON_RIGHT;
    }

    public boolean isMiddleButton() {
        return button == BUTTON_MIDDLE;
    }

    /**
     * Converts a button as vanilla and SDL number it ({@code InputConstants.MOUSE_BUTTON_*}: left 1,
     * middle 2, right 3, then the side buttons) into this library's {@code BUTTON_*} numbering.
     */
    public static int buttonFromInput(int inputButton) {
        return switch (inputButton) {
            case 1 -> BUTTON_LEFT;
            case 2 -> BUTTON_MIDDLE;
            case 3 -> BUTTON_RIGHT;
            default -> inputButton - 1;
        };
    }

    /**
     * The inverse of {@link #buttonFromInput(int)}, for handing a button back to vanilla — building a
     * {@code MouseButtonInfo}, say.
     */
    public static int buttonToInput(int button) {
        return switch (button) {
            case BUTTON_LEFT -> 1;
            case BUTTON_MIDDLE -> 2;
            case BUTTON_RIGHT -> 3;
            default -> button + 1;
        };
    }

}
