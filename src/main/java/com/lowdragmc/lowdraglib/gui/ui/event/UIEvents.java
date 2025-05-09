package com.lowdragmc.lowdraglib.gui.ui.event;

public interface UIEvents {
    /// Mouse Events
    String MOUSE_DOWN = "mouseDown";
    String MOUSE_UP = "mouseUp";
    String CLICK = "mouseClick";
    String DOUBLE_CLICK = "doubleClick";
    String MOUSE_MOVE = "mouseMove";
    String MOUSE_ENTER = "mouseEnter";
    String MOUSE_LEAVE = "mouseLeave";
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
}
