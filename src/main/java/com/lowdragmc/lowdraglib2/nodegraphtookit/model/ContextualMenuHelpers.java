package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for contextual menu operations.
 *
 * <p>TODO: Implement these menu items for your UI framework.</p>
 */
public final class ContextualMenuHelpers {

    private ContextualMenuHelpers() {
        // Utility class
    }

    // Placeholder menu items - implement as needed for your UI
    public static final ContextualMenuItem CREATE_PLACEMAT_ITEM = new ContextualMenuItem("Create Placemat", 100);
    public static final ContextualMenuItem CREATE_LOCAL_SUBGRAPH_FROM_SELECTION_ITEM = new ContextualMenuItem("Create Subgraph from Selection", 101);
    public static final ContextualMenuItem CUT_ITEM = new ContextualMenuItem("Cut", 200);
    public static final ContextualMenuItem COPY_ITEM = new ContextualMenuItem("Copy", 201);
    public static final ContextualMenuItem PASTE_ITEM = new ContextualMenuItem("Paste", 202);
    public static final ContextualMenuItem PASTE_AS_NEW_MENU_ITEM = new ContextualMenuItem("Paste as New", 203);
    public static final ContextualMenuItem RENAME_ITEM = new ContextualMenuItem("Rename", 300);
    public static final ContextualMenuItem DUPLICATE_ITEM = new ContextualMenuItem("Duplicate", 301);
    public static final ContextualMenuItem DELETE_ITEM = new ContextualMenuItem("Delete", 400);
    public static final ContextualMenuItem FRAME_SELECTION_ITEM = new ContextualMenuItem("Frame Selection", 500);
    public static final ContextualMenuItem COLOR_ITEM = new ContextualMenuItem("Color...", 600);
    public static final ContextualMenuItem ALIGN_AND_DISTRIBUTE_ELEMENTS_ITEM = new ContextualMenuItem("Align and Distribute", 700);

    // Node-specific menu items
    public static final ContextualMenuItem deleteAndReconnectItem = new ContextualMenuItem("Delete and Reconnect", 401);
    public static final ContextualMenuItem editSubtitleItem = new ContextualMenuItem("Edit Subtitle", 302);
    public static final ContextualMenuItem bypassNodeItem = new ContextualMenuItem("Bypass Node", 350);
    public static final ContextualMenuItem disableNodeItem = new ContextualMenuItem("Disable Node", 351);
    public static final ContextualMenuItem disconnectAllWiresItem = new ContextualMenuItem("Disconnect All Wires", 360);
    public static final ContextualMenuItem toggleCollapseItem = new ContextualMenuItem("Toggle Collapse", 370);
}
