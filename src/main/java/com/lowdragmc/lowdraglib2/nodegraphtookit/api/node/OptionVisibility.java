package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

/**
 * Where a node option's editor is drawn.
 *
 * <p>The two places are the node body — the rows under the node's title, always on screen — and the
 * inspector, which shows the selected node's full configuration. An option is normally in both, so it
 * can be read at a glance and edited from either.
 *
 * <p>This says nothing about whether the option <em>has</em> an editor:
 * {@link IOptionBuilder#withoutConfigurator()} removes it from both places at once, whatever the
 * visibility says.
 */
public enum OptionVisibility {
    /** Drawn in the node body and listed in the inspector. The default. */
    NODE_AND_INSPECTOR,
    /**
     * Only in the inspector, once the node is selected. For options that are set once and would
     * otherwise take up room in the body — long text, rarely touched switches.
     */
    INSPECTOR_ONLY,
    /**
     * Only in the node body. For options whose editor is the point of the node — the body already
     * shows it, and repeating it in the inspector is noise.
     */
    NODE_ONLY;

    /** Whether the option is drawn in the node body. */
    public boolean showInNode() {
        return this != INSPECTOR_ONLY;
    }

    /** Whether the option is listed in the inspector of the selected node. */
    public boolean showInInspector() {
        return this != NODE_ONLY;
    }
}
