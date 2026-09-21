package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout;

/**
 * The axis a laid-out graph flows along.
 *
 * <p>Matches {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortOrientation}: nodes whose
 * ports sit on their left/right sides read left to right, nodes whose ports sit on top/bottom read
 * top to bottom. Everything in this package is written against the abstract <em>flow</em> and
 * <em>cross</em> axes so an algorithm never has to branch on which one it got.</p>
 */
public enum LayoutDirection {
    /** Layers run left to right: a node sits to the right of everything feeding it. */
    LEFT_RIGHT,
    /** Layers run top to bottom: a node sits below everything feeding it. */
    TOP_BOTTOM;

    /** {@code true} when the flow axis is x and the cross axis is y. */
    public boolean isHorizontal() {
        return this == LEFT_RIGHT;
    }
}
