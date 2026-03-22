package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.DependencyTypes;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ElementUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.UIDependencies;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Model;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class ModelElement extends UIElement {
    @Getter @Setter(AccessLevel.PROTECTED)
    private Model model;
    @Getter
    protected final UIDependencies dependencies;
    // runtime
    @Getter
    protected List<ModelElement> parts = new ArrayList<>();
    @Nullable
    @Getter
    protected GraphView graphView;

    protected ModelElement() {
        dependencies = new UIDependencies(this);
        addEventListener(UIEvents.STYLE_CHANGED, this::onStyleChanged);
        addEventListener(UIEvents.REMOVED, this::onRemoved);
    }

    public void setGraphView(@Nullable GraphView graphView) {
        if (this.graphView == graphView) return;
        dependencies.setGraphView(graphView);
        if (this.graphView != null) {
            parts.forEach(e -> e.setGraphView(graphView));
            this.graphView.unregisterModelElement(this);
        }
        this.graphView = graphView;
        this.parts.clear();
        buildPartList();
        parts.forEach(e -> e.setGraphView(graphView));
        buildUITree();
        if (graphView != null) {
            graphView.registerModelElement(this);
        }
    }

    /**
     * Builds the list of parts for this UI Element.
     */
    protected void buildPartList() {

    }

    protected final void buildUITree() {
        clearAllChildren();
        buildUI();
        postBuildUI();
    }

    protected void buildUI() {

    }

    protected void postBuildUI() {

    }

    protected void onStyleChanged(UIEvent evt) {
        dependencies.onSelfStyleChanged(evt);
    }

    protected void onLayoutChanged() {
        super.onLayoutChanged();
        dependencies.onSelfLayoutChanged();
    }

    protected void onRemoved(UIEvent evt) {
        dependencies.onSelfRemoved(evt);
        clearDependencies();
    }

    public void clearDependencies() {
        dependencies.updateForwardDependencies(DependencyTypes.ANY, ModelUpdateVisitor.UNSPECIFIED);
        dependencies.clearDependencyLists();
    }

    /**
     * Recursively updates this element and its children by the given visitor
     * @param visitor the visitor to use to update the element
     */
    public void updateElement(ElementUpdateVisitor visitor) {
        visitor.update(this);
        dependencies.updateDependencyLists();
        getChildDependencies().forEach(child -> child.updateElement(visitor));
    }

    /**
     * Retrieves the child dependencies associated with this model element.
     * The child dependencies represent parts of the current model element
     * that are considered to be dependent components.
     *
     * @return a collection of child dependencies as {@code Collection<? extends ModelElement>}.
     *         The returned collection provides access to the individual dependent components.
     */
    public Stream<? extends ModelElement> getChildDependencies() {
        return getParts().stream();
    }

    /**
     * Tells whether theUI has some forward dependencies that got changed.
     * <br/>
     * It can be used to know if the ui dependencies should be rebuilt
     * @return true has changed, false otherwise
     */
    public boolean hasForwardsDependenciesChanged() {
        return false;
    }

    /**
     * Tells whether theUI has some backward dependencies that got changed.
     * <br/>
     * It can be used to know if the ui dependencies should be rebuilt
     * @return true has changed, false otherwise
     */
    public boolean hasBackwardsDependenciesChanged() {
        return false;
    }

    /**
     * Tells whether the UI has some dependencies that got changed.
     * <br/>
     * It can be used to know if the ui dependencies should be rebuilt
     * @return true has changed, false otherwise
     */
    public boolean hasModelDependenciesChanged() {
        return false;
    }

    /**
     * Adds graph elements to the model dependencies list.
     * A model dependency is a graph element model that causes this model UI to be updated whenever it is updated.
     */
    public void addModelDependencies() {
    }

    /**
     * Adds graph elements to the forward dependencies list.
     * A forward dependency is a graph element that must be updated whenever this model UI is updated.
     */
    public void addForwardDependencies() {
    }

    /**
     * Adds graph elements to the backward dependencies list.
     * A backward dependency is a graph element that causes this model UI to be updated whenever it is updated.
     */
    public void addBackwardDependencies() {
    }

    /**
     * Update the element to reflect the state of the attached model.
     * @param visitor
     */
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
    }

    /**
     * Fully update the element
     */
    public void doCompleteUpdate() {
        updateElement(ModelUpdateVisitor.UNSPECIFIED);
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
        return true;
    }

    /**
     * Indicates whether this element is currently selected.
     */
    public final boolean isSelected() {
        if (getGraphView() == null) return false;
        return getGraphView().isSelected(getModel());
    }

    /**
     * Called when the selection state of this element changes.
     */
    protected void onSelectionChanged() {
    }

    /**
     * Handles the inspection of the selection state of this element. This method is invoked to
     * allow custom actions or logic to be performed when the associated selection needs inspection.
     *
     * @param inspector the {@link GraphInspector} instance used for inspecting the selection.
     *                  Provides tools for interacting with the graph-related selection.
     */
    protected void onSelectionInspect(GraphInspector inspector) {
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

    /**
     * Determines whether the graph can handle a mouse down action for a given UI event.
     * The method checks specific conditions on the event to allow the mouse down action.
     *
     * @param event the UI event to be evaluated. It contains properties, such as the button
     *              pressed and the collection of bubble listeners, which are used to determine
     *              if the mouse down action should be allowed.
     * @return {@code true} if the graph mouse down action is allowed for the given event,
     *         {@code false} otherwise.
     */
    public boolean allowGraphMouseDown(UIEvent event) {
        return (event.button == 0 || event.button == 1) && event.bubbleListeners.size() == 1;
    }

    public boolean isGraphMouseDownCaptured() {
        return false;
    }
}
