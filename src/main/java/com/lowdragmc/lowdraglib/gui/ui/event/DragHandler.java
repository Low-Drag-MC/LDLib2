package com.lowdragmc.lowdraglib.gui.ui.event;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

/**
 * DragHandler is used to handle drag events.
 * All drag events will only be triggered after the drag is started {@link #startDrag(Object, IGuiTexture, UIElement)}.
 * The drag and drop lifecycle is as follows:
 * <li> To trigger dragging, for example, in mouse events, you can call {@link #startDrag(Object, IGuiTexture, UIElement)}.
 * <li> do something with drag events, {@link UIEvents#DRAG_ENTER}, {@link UIEvents#DRAG_LEAVE}, {@link UIEvents#DRAG_UPDATE}.
 * <li> When the drag is finished
 * <br>
 * {@link UIEvents#DRAG_PERFORM} will be triggered when the user releases the mouse button over an element.
 * {@link UIEvents#DRAG_END} will be triggered when the drag target is existing.
 */
public class DragHandler {
    @Getter
    private boolean isDragging;
    @Nullable
    public UIElement dragSource;
    @Nullable
    public Object draggingObject;
    @Nullable
    public IGuiTexture dragTexture;
    @Setter
    public float offsetX = -20, offsetY = -20, width = 40, height = 40;

    public <T> T getDraggingObject() {
        return (T) draggingObject;
    }

    public void startDrag() {
        startDrag(null);
    }

    public void startDrag(Object draggingObject) {
        startDrag(draggingObject, null);
    }

    public void startDrag(Object draggingObject, IGuiTexture dragTexture) {
        startDrag(draggingObject, dragTexture, null);
    }

    /**
     * Start dragging an object.
     */
    public void startDrag(Object draggingObject, IGuiTexture dragTexture, UIElement dragSource) {
        if (isDragging) {
            stopDrag();
        }
        this.draggingObject = draggingObject;
        this.dragTexture = dragTexture;
        this.dragSource = dragSource;
        isDragging = true;
    }

    public void startDrag(Object draggingObject, IGuiTexture dragTexture, UIElement dragTarget, UIElement releasedElement) {
        startDrag(draggingObject, dragTexture, dragTarget);
        if (draggingObject != null) {
            var event = UIEvent.create(UIEvents.DRAG_ENTER);
            event.relatedTarget = releasedElement;
            UIEventDispatcher.dispatchEvent(event);
        }
    }

    public void setDragTexture(float x, float y, float width, float height) {
        this.offsetX = x;
        this.offsetY = y;
        this.width = width;
        this.height = height;
    }

    public void stopDrag() {
        stopDrag(null);
    }

    /**
     * Stop dragging an object.
     * This will trigger {@link UIEvents#DRAG_END} event if the drag target is existing.
     */
    public void stopDrag(@Nullable UIElement dropElement) {
        if (dragSource != null) {
            var event = UIEvent.create(UIEvents.DRAG_END);
            event.target = dragSource;
            event.relatedTarget = dropElement;
            UIEventDispatcher.dispatchEvent(event);
        }
        draggingObject = null;
        dragTexture = null;
        dragSource = null;
        isDragging = false;
        offsetX = - 20;
        offsetY = - 20;
        width = 40;
        height = 40;
    }

}
