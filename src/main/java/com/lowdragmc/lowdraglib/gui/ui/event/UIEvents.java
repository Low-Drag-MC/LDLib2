package com.lowdragmc.lowdraglib.gui.ui.event;

public interface UIEvents {
    /// Mouse Events
    /**
     * The {@code mouseDown} is sent when the user presses a mouse button.
     */
    String MOUSE_DOWN = "mouseDown";
    /**
     * The {@code mouseUp} is sent when the user releases a mouse button.
     */
    String MOUSE_UP = "mouseUp";
    /**
     * The {@code mouseClick} is sent when the user clicks a mouse button.
     */
    String CLICK = "mouseClick";
    /**
     * The {@code mouseDoubleClick} is sent when the user double clicks a mouse button.
     */
    String DOUBLE_CLICK = "doubleClick";
    /**
     * The {@code mouseDrag} is sent when the user drags a mouse button.
     */
    String MOUSE_MOVE = "mouseMove";
    /**
     * The {@code mouseMove} is sent when the mouse enters an element or one of its descendants.
     */
    String MOUSE_ENTER = "mouseEnter";
    /**
     * The {@code mouseLeave} is sent when the mouse leaves an element or one of its descendants.
     */
    String MOUSE_LEAVE = "mouseLeave";
    /**
     * The {@code mouseWheel} is sent when the user activates the mouse wheel.
     */
    String MOUSE_WHEEL = "mouseWheel";


    /// Drag and Drop Events
    /**
     * The {@code dragEnter} is sent when the pointer enters an element during a drag operation.
     * When a drop area element receives a {@code dragEnter}, it needs to provide feedback that lets the user know that it, or one of its children, is a target for a potential drop operation.
     */
    String DRAG_ENTER = "dragEnter";
    /**
     * The {@code dragLeave} is sent when the pointer exits an element as the user moves a draggable object.
     * When a drop area element receives a {@code dragLeave}, it needs to stop providing drop feedback.
     */
    String DRAG_LEAVE = "dragLeave";
    /**
     * The {@code dragUpdate} is sent when the pointer moves over an element as the user moves a draggable object.
     * When a drop area visual element receives a {@code dragUpdate}, it needs to update the drop feedback. For example, you can move the “ghost” of the dragged object so it stays under the mouse pointer.
     */
    String DRAG_UPDATE = "dragUpdate";
    /**
     * The {@code dragPerform} is sent when the user drags any draggable object and releases the mouse pointer over an element.
     */
    String DRAG_PERFORM = "dragPerform";
    /**
     * The {@code dragEnd} is sent to the {@link DragHandler#dragTarget} (if existing) when the user drags any draggable object and releases the mouse pointer over an element.
     * <li> relatedTarget: The element that dropped the object.
     */
    String DRAG_END = "dragEnd";


    /// Focus Events
    /**
     * The {@code focus} is sent after an element gained focus.
     * <li> target: The element that gained focus.
     * <li> relatedTarget: The element that lost focus.
     */
    String FOCUS = "focus";
    /**
     * The {@code blur} is sent after an element lost focus.
     * <li> target: The element that lost focus.
     * <li> relatedTarget: The element that gained focus.
     */
    String BLUR = "blur";
    /**
     * The {@code focusIn} is sent when an element is about to gain focus.
     * <li> target: The element that is about to gain focus.
     * <li> relatedTarget: The element that is about to lose focus.
     */
    String FOCUS_IN = "focusIn";
    /**
     * The {@code focusOut} is sent when an element is about to lose focus.
     * <li> target: The element that is about to lose focus.
     * <li> relatedTarget: The element that is about to gain focus.
     */
    String FOCUS_OUT = "focusOut";

    /// Keyboard Events
    /**
     * The {@code keyDown} is sent when the user presses a key on the keyboard.
     */
    String KEY_DOWN = "keyDown";
    /**
     * The {@code keyUp} is sent when the user releases a key on the keyboard.
     */
    String KEY_UP = "keyUp";

    /// Text Input Events
    /**
     * The {@code charTyped} is sent when data is input to an element.
     */
    String CHAR_TYPED = "charTyped";


    /// Layout Events
    /**
     * The {@code layoutChanged} is sent when the layout of an element changes.
     */
    String LAYOUT_CHANGED = "layoutChanged";
}
