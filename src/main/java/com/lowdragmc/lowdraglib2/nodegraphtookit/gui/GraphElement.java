package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.DependencyElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

public abstract class GraphElement<T extends GraphElementModel> extends DependencyElement {
    public final T model;

    // runtime
    @Nullable
    @Getter
    protected GraphView graphView;

    public GraphElement(T model) {
        this.model = model;
    }

    public String getLayerName() {
        return "";
    }

    /**
     * Indicates whether this element can be selected.
     *
     * @return {@code true} if the element is selectable, {@code false} otherwise.
     */
    public boolean isSelectable() {
        return model.isSelectable();
    }

    /**
     * Indicates whether this element is currently selected.
     */
    public final boolean isSelected() {
        if (getGraphView() == null) return false;
        return getGraphView().isSelected(model);
    }

    /**
     * Called when the selection state of this element changes.
     */
    protected void onSelectionChanged() {
    }

    /**
     * Checks if this element can be selected within the specified region.
     * Determines whether the element overlaps with the given rectangular region
     * defined by its bounds.
     *
     * @param region the region to test for overlap, represented as a {@code Vector4f} of local transform already,
     *               where {@code x} and {@code y} define the position, and
     *               {@code z} and {@code w} define the size of the region.
     * @return {@code true} if the element overlaps with the specified region,
     *         {@code false} otherwise.
     */
    public boolean canBeRegionSelected(Vector4f region) {
        return isOverlapping(region.x, region.y, region.z, region.w);
    }

    /**
     * Checks if this element is currently under region selection.
     */
    public final boolean isUnderRegionSelection() {
        if (graphView == null) return false;
        if (graphView.getDragRegionSelection() == null) return false;
        return canBeRegionSelected(graphView.getDragRegionSelection());
    }

    protected void setGraphView(@Nullable GraphView graphView) {
        getDependencies().setGraphView(graphView);
        if (this.graphView == graphView) return;
        if (this.graphView != null) this.graphView.unregisterModelElement(this);
        this.graphView = graphView;
        if (graphView != null) graphView.registerModelElement(this);
    }

}
