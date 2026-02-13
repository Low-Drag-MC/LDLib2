package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import lombok.Getter;

import java.util.Collection;
import java.util.Collections;

public abstract class DependencyElement extends UIElement {
    @Getter
    protected final UIDependencies dependencies;

    protected DependencyElement() {
        dependencies = new UIDependencies(this);
        addEventListener(UIEvents.STYLE_CHANGED, this::onStyleChanged);
        addEventListener(UIEvents.REMOVED, this::onRemoved);
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
        for (var childDependency : getChildDependencies()) {
            childDependency.updateElement(visitor);
        }
    }

    public Collection<? extends DependencyElement> getChildDependencies() {
        return Collections.emptyList();
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

}
