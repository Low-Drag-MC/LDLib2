package com.lowdragmc.lowdraglib2.gui.ui.event;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import lombok.Getter;
import lombok.Setter;

import org.jetbrains.annotations.Nullable;

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
    public final ModularUI modularUI;
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
    @Setter
    public float startX, startY;

    public DragHandler(ModularUI modularUI) {
        this.modularUI = modularUI;
    }

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
     * Initiates the dragging process using the specified object, drag texture, and drag source.
     * If a drag process is already active, it will stop the current drag before starting a new one.
     *
     * @param draggingObject the object being dragged; can be of any type to represent the drag payload
     * @param dragTexture the {@link IGuiTexture} used to visually represent the drag operation
     * @param dragSource the {@link UIElement} that acts as the source of the drag operation
     */
    public void startDrag(Object draggingObject, IGuiTexture dragTexture, UIElement dragSource) {
        if (isDragging) {
            stopDrag();
        }
        this.draggingObject = draggingObject;
        this.dragTexture = dragTexture;
        this.dragSource = dragSource;
        this.startX = modularUI.getLastMouseX();
        this.startY = modularUI.getLastMouseY();
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
            event.x = modularUI.getLastMouseX();
            event.y = modularUI.getLastMouseY();
            // TODO fix dragStartX and dragStartY
            event.dragStartX = modularUI.getLastMouseDownX();
            event.dragStartY = modularUI.getLastMouseDownY();

            event.target = dragSource;
            event.relatedTarget = dropElement;
            event.hasCapturePhase = true;
            event.hasBubblePhase = false;
            event.dragHandler = this;
            UIEventDispatcher.dispatchEvent(event, true, true, false);
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
