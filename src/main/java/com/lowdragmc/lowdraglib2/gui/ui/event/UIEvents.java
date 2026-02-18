package com.lowdragmc.lowdraglib2.gui.ui.event;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;

@KJSBindings
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


    /// Drag and Drop Events, Drag events won't be sent to the server
    /**
     * The {@code dragEnter} is sent when the pointer enters an element during a drag operation.
     * When a drop area element receives a {@code dragEnter}, it needs to provide feedback that lets the user know that it, or one of its children, is a target for a potential drop operation.
     */
    String DRAG_ENTER = "dragEnter";
    /**
     * The {@code dragLeave} is sent when the pointer exits an element as the user moves a draggable object.
     * When a drop area element receives a {@code dragLeave}, it needs to stop providing drop feedback.
     * If the relatedTarget is not null, it means the new element entered.
     */
    String DRAG_LEAVE = "dragLeave";
    /**
     * The {@code dragUpdate} is sent when the pointer moves over an element as the user moves a draggable object.
     * When a drop area visual element receives a {@code dragUpdate}, it needs to update the drop feedback. For example, you can move the “ghost” of the dragged object so it stays under the mouse pointer.
     */
    String DRAG_UPDATE = "dragUpdate";
    /**
     * The {@code dragSourceUpdate} is sent to the {@link DragHandler#dragSource} (if existing) when the user drags any draggable object.
     */
    String DRAG_SOURCE_UPDATE = "dragSourceUpdate";
    /**
     * The {@code dragPerform} is sent when the user drags any draggable object and releases the mouse pointer over an element.
     */
    String DRAG_PERFORM = "dragPerform";
    /**
     * The {@code dragEnd} is sent to the {@link DragHandler#dragSource} (if existing) when the user drags any draggable object and releases the mouse pointer over an element.
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
     * The {@code focusIn} is sent when an element is about to gain focus. won't be sent to the server
     * <li> target: The element that is about to gain focus.
     * <li> relatedTarget: The element that is about to lose focus.
     */
    String FOCUS_IN = "focusIn";
    /**
     * The {@code focusOut} is sent when an element is about to lose focus. won't be sent to the server
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

    /// Hover Tooltips Events, which won't be sent to the server
    String HOVER_TOOLTIPS = "hoverTooltips";

    ///  Command Events
    /**
     * The {@code validateCommand} is sent when determining whether an element in the panel handles the command. It won't be sent to the server.
     * Supported commands can be found in {@link CommandEvents}.
     * To execute this command, call {@link UIEvent#stopPropagation()}.
     */
    String VALIDATE_COMMAND = "validateCommand";
    /**
     * The {@code executeCommand} is sent when an element in the panel executes a command.
     * Supported commands can be found in {@link CommandEvents}
     */
    String EXECUTE_COMMAND = "executeCommand";

    /// Layout Events, which won't be sent to the server.
    /**
     * The {@code layoutChanged} is sent when the layout of an element changes.
     */
    String LAYOUT_CHANGED = "layoutChanged";

    /// Style Events, which won't be sent to the server.
    // TODO fine shell do style changed for all changes or only custom one??????
    String STYLE_CHANGED = "styleChanged";

    /// Life-Cycle Events, which won't be sent to the server.'
    /**
     * The {@code removed} is sent when the element is removed from the UI tree.
     */
    String REMOVED = "removed";
    /**
     * The {@code added} is sent when the element is added to the UI tree.
     */
    String ADDED = "added";
    /**
     * The {@code muiChanged} is sent when the ModularUI of an element changes.
     */
    String MUI_CHANGED = "muiChanged";


    /// Lifecycle Events
    /**
     * The {@code tick} is sent per tick when the element is {@link UIElement#isActive()} and {@link UIElement#isDisplayed()}.
     * It won't be sent to the server. But you can still listen it on the server side.
     */
    String TICK = "tick";
}
