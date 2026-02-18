package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency;


import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHintList;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class UIDependencies {
    public record Dependency(ModelElement element, DependencyTypes type) { }

    protected static ModelUpdateVisitor LAYOUT_VISITOR = new ModelUpdateVisitor(ChangeHintList.LAYOUT);
    protected static ModelUpdateVisitor STYLE_VISITOR = new ModelUpdateVisitor(ChangeHintList.STYLE);

    public final ModelElement owner;

    public final UIEventListener onBackWardDependencyStyleChanged;
    public final UIEventListener onBackWardDependencyLayoutChanged;
    public final UIEventListener onBackWardDependencyRemoved;
    // runtime
    @Setter
    private @Nullable GraphView graphView;
    /**
     * Graph elements that we affect when we change.
     */
    private @Nullable Set<Dependency> forwardDependencies;
    /**
     * Graph elements that affect us when they change.
     */
    private @Nullable Set<Dependency> backwardDependencies;
    private @Nullable Set<GraphElementModel> modelDependencies;

    public UIDependencies(ModelElement owner) {
        this.owner = owner;
        onBackWardDependencyStyleChanged = this::onBackWardDependencyStyleChanged;
        onBackWardDependencyLayoutChanged = this::onBackWardDependencyLayoutChanged;
        onBackWardDependencyRemoved = this::onBackWardDependencyRemoved;
    }

    protected void onBackWardDependencyStyleChanged(UIEvent event) {
        owner.updateElement(STYLE_VISITOR);
    }

    protected void onBackWardDependencyLayoutChanged(UIEvent event) {
        owner.updateElement(LAYOUT_VISITOR);
    }

    protected void onBackWardDependencyRemoved(UIEvent event) {
        owner.doCompleteUpdate();
    }

    public void onSelfStyleChanged(UIEvent event) {
        updateForwardDependencies(DependencyTypes.STYLE, STYLE_VISITOR);
    }

    public void onSelfLayoutChanged() {
        updateForwardDependencies(DependencyTypes.LAYOUT, LAYOUT_VISITOR);
    }

    public void onSelfRemoved(UIEvent event) {
        if (forwardDependencies == null) return;
        for (Dependency dependency : forwardDependencies) {
            if (dependency.type.hasFlag(DependencyTypes.REMOVAL)) {
                dependency.element.doCompleteUpdate();
            }
        }
    }

    /**
     * Clear all outdated dependencies
     */
    public void clearDependencyLists() {
        if (forwardDependencies != null && owner.hasForwardsDependenciesChanged()) {
            forwardDependencies.clear();
        }

        if (backwardDependencies != null && owner.hasBackwardsDependenciesChanged()) {
            for (var dependency : backwardDependencies) {
                var target = dependency.element;
                if (dependency.type.hasFlag(DependencyTypes.STYLE)) {
                    target.removeEventListener(UIEvents.STYLE_CHANGED, onBackWardDependencyStyleChanged);
                } else if (dependency.type.hasFlag(DependencyTypes.LAYOUT)) {
                    target.removeEventListener(UIEvents.LAYOUT_CHANGED, onBackWardDependencyLayoutChanged);
                } else if (dependency.type.hasFlag(DependencyTypes.REMOVAL)) {
                    target.removeEventListener(UIEvents.REMOVED, onBackWardDependencyRemoved);
                }
            }
            backwardDependencies.clear();
        }

        if (modelDependencies != null && owner.hasModelDependenciesChanged()) {
            for (GraphElementModel model : modelDependencies) {
                if (graphView != null) {
                    graphView.removeModelDependency(model.getUid(), owner);
                }
            }
            modelDependencies.clear();
        }
    }

    /**
     * Asks the owner UI to update its dependency lists.
     */
    public void updateDependencyLists() {
        // clean up first
        clearDependencyLists();
        
        if (owner.hasForwardsDependenciesChanged())
            owner.addForwardDependencies();

        if (owner.hasBackwardsDependenciesChanged()) {
            owner.addBackwardDependencies();
            if (backwardDependencies != null) {
                for (var dependency : backwardDependencies) {
                    var target = dependency.element;
                    if (dependency.type.hasFlag(DependencyTypes.STYLE)) {
                        target.addEventListener(UIEvents.STYLE_CHANGED, onBackWardDependencyStyleChanged);
                    } else if (dependency.type.hasFlag(DependencyTypes.LAYOUT)) {
                        target.addEventListener(UIEvents.LAYOUT_CHANGED, onBackWardDependencyLayoutChanged);
                    } else if (dependency.type.hasFlag(DependencyTypes.REMOVAL)) {
                        target.addEventListener(UIEvents.REMOVED, onBackWardDependencyRemoved);
                    }
                }
            }
        }

        if (owner.hasModelDependenciesChanged()) {
            owner.addModelDependencies();
            if (modelDependencies != null) {
                for (var model : modelDependencies) {
                    if (graphView != null) {
                        graphView.addModelDependency(model.getUid(), owner);
                    }
                }
            }
        }
    }

    public void updateForwardDependencies(DependencyTypes dependencyType, ModelUpdateVisitor visitor) {
        if (forwardDependencies != null) {
            for (Dependency dependency : forwardDependencies) {
                if (dependency.type.hasFlag(dependencyType)) {
                    dependency.element.updateElement(visitor);
                }
            }
        }
    }

    public void addBackwardDependency(ModelElement dependency, DependencyTypes dependencyType) {
        if (backwardDependencies == null)
            backwardDependencies = new HashSet<>();
        backwardDependencies.add(new Dependency(dependency, dependencyType));
    }

    public void addForwardDependency(ModelElement dependency, DependencyTypes dependencyType) {
        if (forwardDependencies == null)
            forwardDependencies = new HashSet<>();
        forwardDependencies.add(new Dependency(dependency, dependencyType));
    }

    public void addModelDependency(GraphElementModel model) {
        if (modelDependencies == null)
            modelDependencies = new HashSet<>();
        modelDependencies.add(model);
        if (graphView != null) {
            graphView.addModelDependency(model.getUid(), owner);
        }
    }

}
