package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;

import java.util.*;
import java.util.stream.Stream;

public abstract class GraphElementModel extends Model implements IGraphElementModelHolder, IHasContextualMenuItems {
    protected GraphModel graphModel;

    /**
     * The graph model to which the element belongs.
     */
    public GraphModel getGraphModel() {
        return graphModel;
    }

    public IGraphElementContainer getContainer() {
        return graphModel;
    }

    @Override
    public GraphElementModel getGraphElementModel() {
        return this;
    }

    public void setGraphModel(GraphModel value) {
        this.graphModel = value;
        getDependentModels().forEach(m -> {
            if (m != null) m.setGraphModel(value);
        });
    }

    /**
     * The dependent models for this model (for example, ports on a node, blocks in context node).
     * Default: empty.
     */
    public Stream<GraphElementModel> getDependentModels() {
        return Stream.empty();
    }

    @Override
    public List<ContextualMenuItem> getContextualMenuItems() {
        return COMMON_GRAPH_ELEMENT_MENU_ITEMS;
    }

    protected static final List<ContextualMenuItem> COMMON_GRAPH_ELEMENT_MENU_ITEMS = List.of(
            ContextualMenuHelpers.CREATE_PLACEMAT_ITEM,
            ContextualMenuHelpers.CREATE_LOCAL_SUBGRAPH_FROM_SELECTION_ITEM,
            ContextualMenuHelpers.CUT_ITEM,
            ContextualMenuHelpers.COPY_ITEM,
            ContextualMenuHelpers.PASTE_ITEM,
            ContextualMenuHelpers.PASTE_AS_NEW_MENU_ITEM,
            ContextualMenuHelpers.RENAME_ITEM,
            ContextualMenuHelpers.DUPLICATE_ITEM,
            ContextualMenuHelpers.DELETE_ITEM,
            ContextualMenuHelpers.FRAME_SELECTION_ITEM,
            ContextualMenuHelpers.COLOR_ITEM,
            ContextualMenuHelpers.ALIGN_AND_DISTRIBUTE_ELEMENTS_ITEM
    );

    // -------------------------
    // Capabilities (from your previous partial)
    // -------------------------
    protected final List<Capabilities> capabilities = new ArrayList<>();
    private final List<Capabilities> capabilitiesView = Collections.unmodifiableList(capabilities);

    public List<Capabilities> getCapabilities() {
        return capabilitiesView;
    }

    public boolean hasCapability(Capabilities capability) {
        if (capability == null) return false;

        String id = capability.getId();
        for (Capabilities c : capabilities) {
            if (c != null && Objects.equals(id, c.getId())) {
                return true;
            }
        }
        return false;
    }

    public void setCapability(Capabilities capability, boolean active) {
        if (capability == null) return;

        if (active) {
            if (!hasCapability(capability)) {
                capabilities.add(capability);
            }
        } else {
            String id = capability.getId();
            capabilities.removeIf(c -> c != null && Objects.equals(id, c.getId()));
        }
    }

    public void clearCapabilities() {
        capabilities.clear();
    }

    public boolean isSelectable()  { return hasCapability(Capabilities.SELECTABLE); }
    public boolean isCollapsible() { return hasCapability(Capabilities.COLLAPSIBLE); }
    public boolean isResizable()   { return hasCapability(Capabilities.RESIZABLE); }
    public boolean isMovable()     { return hasCapability(Capabilities.MOVABLE); }
    public boolean isDeletable()   { return hasCapability(Capabilities.DELETABLE); }
    public boolean isDroppable()   { return hasCapability(Capabilities.DROPPABLE); }
    public boolean isRenamable()   { return hasCapability(Capabilities.RENAMABLE); }
    public boolean isCopiable()    { return hasCapability(Capabilities.COPIABLE); }
    public boolean isColorable()   { return hasCapability(Capabilities.COLORABLE); }
    public boolean isAscendable()  { return hasCapability(Capabilities.ASCENDABLE); }
    public boolean needsContainer(){ return hasCapability(Capabilities.NEEDS_CONTAINER); }
    public boolean isDisableable() { return hasCapability(Capabilities.DISABLEABLE); }

}
