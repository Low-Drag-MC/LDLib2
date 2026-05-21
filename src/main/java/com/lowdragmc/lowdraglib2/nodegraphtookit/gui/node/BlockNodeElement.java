package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.BlockCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.BlockNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ContextNodeModel;
import dev.vfyjxf.taffy.style.TaffyPosition;

/**
 * UI for a single {@link BlockNodeModel}. Unlike a regular top-level node, a block is laid out
 * <em>inside</em> its parent context's {@link BlockListContainerElement} — so it flows in the
 * parent's column rather than being absolutely positioned on the canvas.
 *
 * <p>Supports drag-reorder within the parent context: holding the mouse down on a block and
 * dragging onto a sibling fires a {@link BlockCommands.MoveBlockCommand}. The pattern mirrors
 * {@code Blackboard.onItemNodeCreated} — top/bottom-half hover decides the insertion side, the
 * overlay shown comes from {@link TreeList#createDraggingOverlay}.</p>
 */
public class BlockNodeElement extends CollapsibleInOutNodeElement {
    /** Drag payload — identifies the block being reordered. */
    public record DraggingBlock(BlockNodeModel block) {}

    /** Tracks left-button press timestamp; non-zero means the drag-start gate is open. */
    private long lastMouseDownTime = 0;

    public BlockNodeElement(BlockNodeModel blockNodeModel) {
        super(blockNodeModel);
        addClass("__block-node__");
    }

    @Override
    public BlockNodeModel getModel() {
        return (BlockNodeModel) super.getModel();
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        if (nodeTittle != null) {
            Style.defaultPipeline(nodeTittle.getStyle(), s -> s.background(Sprites.RECT_RD_T));
        }
        // Override the parent's IMPORTANT ABSOLUTE positioning — blocks flow inside their parent's
        // vertical container. Must also be IMPORTANT to outrank the inherited absolute pin.
        Style.importantPipeline(getLayout(), l -> l.positionType(TaffyPosition.RELATIVE).left(0).top(0));
        // Subtle visual differentiation so blocks read as nested inside the context.
        wireDragReorder();
    }

    private void wireDragReorder() {
        // Track left-click; MOUSE_LEAVE while still held promotes to a drag (Blackboard pattern).
        addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 0) lastMouseDownTime = System.currentTimeMillis();
        });
        addEventListener(UIEvents.MOUSE_UP, e -> lastMouseDownTime = 0);
        addEventListener(UIEvents.MOUSE_LEAVE, e -> {
            if (lastMouseDownTime != 0 && isMouseDown(0) && getModel().getContextNodeModel() != null) {
                startDrag(new DraggingBlock(getModel()),
                        new TextTexture(getModel().getTitle().getString()));
            }
            lastMouseDownTime = 0;
        }, true);

        // Highlight insertion point — top half of target = insert before, bottom half = insert after.
        // Drag-overlay feedback must outrank stylesheet to remain visible during drag, hence IMPORTANT.
        addEventListener(UIEvents.DRAG_ENTER, e -> {
            if (e.dragHandler.getDraggingObject() instanceof DraggingBlock(var dragged)
                    && isSameContextSibling(dragged)) {
                Style.importantPipeline(e.currentElement.getStyle(), s -> s.overlayTexture(TreeList.createDraggingOverlay(insertMode(e))));
            }
        }, true);
        addEventListener(UIEvents.DRAG_UPDATE, e -> {
            if (e.dragHandler.getDraggingObject() instanceof DraggingBlock(var dragged)
                    && isSameContextSibling(dragged)) {
                Style.importantPipeline(e.currentElement.getStyle(), s -> s.overlayTexture(TreeList.createDraggingOverlay(insertMode(e))));
            } else {
                Style.importantPipeline(e.currentElement.getStyle(), s -> s.overlayTexture(IGuiTexture.EMPTY));
            }
        });
        addEventListener(UIEvents.DRAG_LEAVE, e ->
                Style.importantPipeline(e.currentElement.getStyle(), s -> s.overlayTexture(IGuiTexture.EMPTY)), true);
        addEventListener(UIEvents.DRAG_END, e ->
                Style.importantPipeline(e.currentElement.getStyle(), s -> s.overlayTexture(IGuiTexture.EMPTY)));

        addEventListener(UIEvents.DRAG_PERFORM, e -> {
            Style.importantPipeline(e.currentElement.getStyle(), s -> s.overlayTexture(IGuiTexture.EMPTY));
            if (!(e.dragHandler.getDraggingObject() instanceof DraggingBlock(var dragged))) return;
            var target = getModel();
            var parent = target.getContextNodeModel();
            if (parent == null || dragged == target || dragged.getContextNodeModel() != parent) return;

            int from = parent.indexOf(dragged);
            int targetIdx = parent.indexOf(target);
            if (from < 0 || targetIdx < 0) return;

            // Top-half of the target = drop above it; bottom-half = drop below.
            int insertAt = isAboveHalf(e) ? targetIdx : targetIdx + 1;
            // moveBlock(from, to) does remove(from) then add(to, ...). When moving forward, the
            // removal shifts later items down by one — compensate so the user-visible position
            // matches where they dropped.
            if (from < insertAt) insertAt -= 1;
            // Clamp into legal moveBlock range (0..size-1; size-1 after the implicit removal).
            int maxIdx = parent.getBlockCount() - 1;
            if (insertAt < 0) insertAt = 0;
            if (insertAt > maxIdx) insertAt = maxIdx;
            if (insertAt == from) return;

            if (getGraphView() != null) {
                getGraphView().dispatchCommand(new BlockCommands.MoveBlockCommand(parent, from, insertAt));
            }
        });
    }

    private boolean isSameContextSibling(BlockNodeModel dragged) {
        ContextNodeModel parent = getModel().getContextNodeModel();
        return parent != null && dragged != getModel() && dragged.getContextNodeModel() == parent;
    }

    /** {@link TreeList#createDraggingOverlay} mode: 0 = above line, 2 = below line. */
    private int insertMode(UIEvent e) {
        return isAboveHalf(e) ? 0 : 2;
    }

    private boolean isAboveHalf(UIEvent e) {
        var ui = e.currentElement;
        return ui.isMouseOver(ui.getPositionX(), ui.getPositionY(),
                ui.getSizeWidth(), ui.getSizeHeight() / 2f, e.x, e.y);
    }
}
