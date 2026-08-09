package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.util;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;

/**
 * Resolves the single flat colour that stands in for a node wherever the node is too small to draw
 * in full — the minimap ({@link com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphPreview}) and the
 * zoomed-out LOD path in {@link com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodeElement}.
 */
public final class NodeColors {
    /** Used when a node's colour is fully transparent, i.e. it carries no usable colour at all. */
    public static final int DEFAULT_NODE_COLOR = 0xFF_555555;

    private NodeColors() {
    }

    /**
     * The node's user colour if it has one, otherwise its type's default colour, forced fully
     * opaque. A flat stand-in rect has nothing behind it to blend with, so a partially transparent
     * colour would just read as "wrong" rather than "translucent".
     */
    public static int resolve(AbstractNodeModel model) {
        int color = model.hasUserColor() ? model.getElementColor() : model.getDefaultColor();
        if ((color & 0xFF000000) == 0) {
            return DEFAULT_NODE_COLOR;
        }
        if ((color & 0xFF000000) != 0xFF000000) {
            return (color & 0x00FFFFFF) | 0xFF000000;
        }
        return color;
    }
}
