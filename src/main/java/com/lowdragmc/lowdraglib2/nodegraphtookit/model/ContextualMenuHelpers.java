package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import java.util.ArrayList;
import java.util.List;
public final class ContextualMenuHelpers {

    private ContextualMenuHelpers() {
        // Utility class
    }

    // Placeholder menu items - implement as needed for your UI
    public static final ContextualMenuItem CREATE_PLACEMAT_ITEM = new ContextualMenuItem("Create Placemat", 100);
    public static final ContextualMenuItem CREATE_LOCAL_SUBGRAPH_FROM_SELECTION_ITEM = new ContextualMenuItem("Create Subgraph from Selection", 101);
    public static final ContextualMenuItem CREATE_STICKY_NOTE_ITEM = new ContextualMenuItem("Create Sticky Note", 102);
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

    // ViewSelection menu items:
//    internal static ContextualMenuItem cutItem = new(ContextualMenuCategory.CutCopyPaste, "Cut");
//    internal static ContextualMenuItem copyItem = new(ContextualMenuCategory.CutCopyPaste, "Copy");
//    internal static ContextualMenuItem pasteItem = new(ContextualMenuCategory.CutCopyPaste, "Paste");
//    internal static ContextualMenuItem renameItem = new(ContextualMenuCategory.RenameDuplicateDelete, "Rename");
//    internal static ContextualMenuItem duplicateItem = new(ContextualMenuCategory.RenameDuplicateDelete, "Duplicate");
//    internal static ContextualMenuItem deleteItem = new(ContextualMenuCategory.RenameDuplicateDelete, "Delete");
//    internal static ContextualMenuItem selectUnusedItem = new(ContextualMenuCategory.Organization, "Select Unused");
//    internal static ContextualMenuItem pasteAsNewMenuItem = new(ContextualMenuCategory.CutCopyPaste, "Paste as New");
//
//    // Common graph element menu items:
//    internal static ContextualMenuItem createPlacematItem = new(ContextualMenuCategory.OrganizationalElements, "Create Placemat");
//    internal static ContextualMenuItem createLocalSubgraphFromSelectionItem = new(ContextualMenuCategory.Conversions, "Create Local Subgraph from Selection");
//    internal static ContextualMenuItem frameSelectionItem = new(ContextualMenuCategory.Modifications, "Frame Selection");
//    internal static ContextualMenuItem colorItem = new(ContextualMenuCategory.Modifications, "Color");
//    internal static ContextualMenuItem alignAndDistributeElementsItem = new(ContextualMenuCategory.Organization, "Align and Distribute Elements");
//
//    // GraphView menu items:
//    internal static ContextualMenuItem addNodeItem = new(ContextualMenuCategory.FunctionalElements, "Add Node");
//    internal static ContextualMenuItem createStickyNoteItem = new(ContextualMenuCategory.OrganizationalElements, "Create Sticky Note");
//    internal static ContextualMenuItem createEmptyLocalSubgraphItem = new(ContextualMenuCategory.OrganizationalElements, "Create Empty Local Subgraph");
//    internal static ContextualMenuItem selectAllItem = new(ContextualMenuCategory.Organization, "Select All");
//    internal static ContextualMenuItem showOverlayMenuItem = new(ContextualMenuCategory.External, "Show Overlay Menu");
//
//    // Node menu items:
//    internal static ContextualMenuItem editSubtitleItem = new(ContextualMenuCategory.Modifications, "Edit Subtitle");
//    internal static ContextualMenuItem bypassNodeItem = new(ContextualMenuCategory.Modifications, "Bypass Node");
//    internal static ContextualMenuItem disableNodeItem = new(ContextualMenuCategory.Modifications, "Disable Node");
//    internal static ContextualMenuItem disconnectAllWiresItem = new(ContextualMenuCategory.Modifications, "Disconnect All Wires");
//    internal static ContextualMenuItem toggleCollapseItem = new(ContextualMenuCategory.Modifications, "Toggle Collapse");
//    internal static ContextualMenuItem deleteAndReconnectItem = new(ContextualMenuCategory.RenameDuplicateDelete, "Delete and reconnect");
//
//    // State menu items:
//    internal static ContextualMenuItem createTransitionMenuItem = new(ContextualMenuCategory.FunctionalElements, "Create Transition");
//    internal static ContextualMenuItem createLocalTransitionMenuItem = new(ContextualMenuCategory.FunctionalElements, "Create Local Transition");
//    internal static ContextualMenuItem createOnEnterTransitionMenuItem = new(ContextualMenuCategory.FunctionalElements, "Create OnEnter Transition");
//    internal static ContextualMenuItem createSelfTransitionMenuItem = new(ContextualMenuCategory.FunctionalElements, "Create Self Transition");
//    internal static ContextualMenuItem setAsDefaultStateMenuItem = new(ContextualMenuCategory.Modifications, "Set Default State");
//
//    // Subgraph menu items:
//    internal static ContextualMenuItem extractContentsToPlacematItem = new(ContextualMenuCategory.Conversions, "Extract Contents to Placemat");
//    internal static ContextualMenuItem openLocalSubgraphItem = new(ContextualMenuCategory.AssetManagement, "Open Local Subgraph");
//    internal static ContextualMenuItem openAssetSubgraphItem = new(ContextualMenuCategory.AssetManagement, "Open Asset Subgraph");
//    internal static ContextualMenuItem unpackToLocalSubgraphItem = new(ContextualMenuCategory.AssetManagement, "Unpack to Local Subgraph");
//    internal static ContextualMenuItem findAssetInProjectItem = new(ContextualMenuCategory.AssetManagement, "Find Asset in Project");
//    internal static ContextualMenuItem convertToAssetSubgraphItem = new(ContextualMenuCategory.AssetManagement, "Convert to Asset Subgraph");
//
//    // Variable and constant menu items:
//    internal static ContextualMenuItem itemizeItem = new(ContextualMenuCategory.Modifications, "Itemize");
//    internal static ContextualMenuItem convertToConstantItem = new(ContextualMenuCategory.Conversions, "Convert to Constant");
//    internal static ContextualMenuItem convertToVariableItem = new(ContextualMenuCategory.Conversions, "Convert to Variable");
//
//    // Blackboard menu items:
//    internal static ContextualMenuItem createVariableItem = new(ContextualMenuCategory.FunctionalElements, "Create Variable");
//    internal static ContextualMenuItem createGroupItem = new(ContextualMenuCategory.FunctionalElements, "Create Group");
//
//    // Ports menu items:
//    internal static ContextualMenuItem addNodeFromPortItem = new(ContextualMenuCategory.FunctionalElements, "Add Node from port");
//    internal static ContextualMenuItem createVariableFromPortItem = new(ContextualMenuCategory.FunctionalElements, "Create Variable from port");
//    internal static ContextualMenuItem copyValueItem = new(ContextualMenuCategory.CutCopyPaste, "Copy Value");
//    internal static ContextualMenuItem pasteValueItem = new(ContextualMenuCategory.CutCopyPaste, "Paste Value");
//    internal static ContextualMenuItem expandPortItem = new(ContextualMenuCategory.Modifications, "Expand Port");
//    internal static ContextualMenuItem collapsePortItem = new(ContextualMenuCategory.Modifications, "Collapse Port");
//
//    // Wire menu items:
//    internal static ContextualMenuItem insertNodeItem = new(ContextualMenuCategory.FunctionalElements, "Insert Node");
//    internal static ContextualMenuItem insertJunctionPointItem = new(ContextualMenuCategory.FunctionalElements, "Insert Junction Point");
//    internal static ContextualMenuItem convertToPortalsItem = new(ContextualMenuCategory.Conversions, "Convert to Portals");
//    internal static ContextualMenuItem reorderWireItem = new(ContextualMenuCategory.Modifications, "Reorder Wire");
//
//    // Context and block menu items:
//    internal static ContextualMenuItem addBlockItem = new(ContextualMenuCategory.FunctionalElements, "Add Block");
//    internal static ContextualMenuItem insertBlockAboveItem = new(ContextualMenuCategory.FunctionalElements, "Insert Block Above");
//    internal static ContextualMenuItem insertBlockBelowItem = new(ContextualMenuCategory.FunctionalElements, "Insert Block Below");
//    internal static ContextualMenuItem convertToBlockSubgraphItem = new(ContextualMenuCategory.Conversions, "Convert to Block Subgraph");
//
//    // Sticky Note menu items:
//    internal static ContextualMenuItem fitToTextItem = new(ContextualMenuCategory.Modifications, "Fit to Text");
//    internal static ContextualMenuItem fontSizeAndThemeItem = new(ContextualMenuCategory.Modifications, "Font Size");
//
//    // Placemat menu items:
//    internal static ContextualMenuItem deleteAndSelectContentsItem = new(ContextualMenuCategory.RenameDuplicateDelete, "Delete and Select Contents");
//    internal static ContextualMenuItem smartResizeItem = new(ContextualMenuCategory.Modifications, "Smart Resize");
//    internal static ContextualMenuItem reorderPlacematItem = new(ContextualMenuCategory.Modifications, "Reorder Placemat");
//    internal static ContextualMenuItem selectAllPlacematContentsItem = new(ContextualMenuCategory.Organization, "Select All Placemat Contents");
//
//    // Portals menu items:
//    internal static ContextualMenuItem createOppositePortalItem = new(ContextualMenuCategory.Conversions, "Create Opposite Portal");
//    internal static ContextualMenuItem revertToWireItem = new(ContextualMenuCategory.Conversions, "Revert to Wire");
//    internal static ContextualMenuItem revertAllToWiresItem = new(ContextualMenuCategory.Conversions, "Revert All to Wire");

}
