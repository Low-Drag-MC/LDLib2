package com.lowdragmc.lowdraglib2.gui.ui.event;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.widget.Widget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.ToString;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nullable;

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
                if (event.keyCode != 0 || event.scanCode != 0 || event.modifiers != 0 || event.codePoint != 0 ) {
                    byteBuf.writeBoolean(true);
                    byteBuf.writeVarInt(event.keyCode);
                    byteBuf.writeVarInt(event.scanCode);
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
                    event.scanCode = byteBuf.readVarInt();
                    event.modifiers = byteBuf.readVarInt();
                    event.codePoint = (char) byteBuf.readVarInt();
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
     * Mouse Event data
     */
    public float x, y, deltaX, deltaY;
    public int button;
    /**
     * Drag Event data
     */
    public float dragStartX, dragStartY;
    public DragHandler dragHandler;
    /**
     * Key Event data
     */
    public int keyCode, scanCode, modifiers;
    /**
     * Hover Tooltips
     */
    public HoverTooltips hoverTooltips;
    /**
     * Command name
     */
    public String command;
    public char codePoint;
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
     * Whether the propagation is canceled.
     */
    public boolean propagationStopped = false;
    /**
     * Whether the immediate propagation is canceled.
     */
    public boolean immediatePropagationStopped = false;
    /**
     * Indicates whether there is a handler associated with the event.
     * This variable helps determine if the event has at least one listener
     * registered that should process it.
     */
    public boolean hasHandler = false;

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
        this.immediatePropagationStopped = true;
    }

    public boolean isShiftDown() {
        return Widget.isShiftDown();
    }

    public boolean isCtrlDown() {
        return Widget.isCtrlDown();
    }

    public boolean isAltDown() {
        return Widget.isAltDown();
    }

    public boolean isKeyDown(int keyCode) {
        return Widget.isKeyDown(keyCode);
    }

}
