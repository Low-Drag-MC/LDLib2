package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.BlockNodeModel;
import dev.vfyjxf.taffy.style.TaffyPosition;

/**
 * UI for a single {@link BlockNodeModel}. Unlike a regular top-level node, a block is laid out
 * <em>inside</em> its parent context's {@link BlockListContainerElement} — so it flows in
 * the parent's column rather than being absolutely positioned on the canvas.
 */
public class BlockNodeElement extends NodeElement {

    public BlockNodeElement(BlockNodeModel blockNodeModel) {
        super(blockNodeModel);
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        // Override the ABSOLUTE positioning that NodeElement sets — blocks flow inside their
        // parent's vertical container, not at canvas-absolute coordinates.
        getLayout().positionType(TaffyPosition.RELATIVE).left(0).top(0);
        // Subtle visual differentiation so blocks read as nested inside the context.
        getStyle().background(Sprites.BORDER_DARK);
    }
}
