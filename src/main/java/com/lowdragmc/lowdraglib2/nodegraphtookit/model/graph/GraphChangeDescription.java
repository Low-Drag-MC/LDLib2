package com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHintList;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Tracks changes made to a graph during a single operation.
 *
 * <p>This class collects information about models that were added, deleted, or modified,
 * allowing the UI to efficiently update only the affected elements.</p>
 */
public class GraphChangeDescription {
    private @Nullable Set<UUID> newModels;
    private @Nullable Set<UUID> deletedModels;
    private @Nullable Map<UUID, ChangeHintList> changedModels;

    public GraphChangeDescription() {
        newModels = new HashSet<>();
        deletedModels = new HashSet<>();
        changedModels = new HashMap<>();
    }

    public GraphChangeDescription(@Nullable Set<UUID> newModels,
                                  @Nullable Set<UUID> deletedModels,
                                  @Nullable Map<UUID, ChangeHintList> changedModels) {
        this.newModels = newModels != null ? new HashSet<>(newModels) : null;
        this.deletedModels = deletedModels != null ? new HashSet<>(deletedModels) : null;
        this.changedModels = changedModels != null ? new HashMap<>(changedModels) : null;
    }

    public void initialize(@Nullable Set<UUID> newModels,
                           @Nullable Set<UUID> deletedModels,
                           @Nullable Map<UUID, ChangeHintList> changedModels) {
        this.newModels = newModels != null ? new HashSet<>(newModels) : null;
        this.deletedModels = deletedModels != null ? new HashSet<>(deletedModels) : null;
        this.changedModels = changedModels != null ? new HashMap<>(changedModels) : null;
    }

    public void clear() {
        if (newModels != null) newModels.clear();
        if (deletedModels != null) deletedModels.clear();
        if (changedModels != null) changedModels.clear();
    }

    public void union(GraphChangeDescription other) {
        if (other.newModels != null && newModels != null) {
            newModels.addAll(other.newModels);
        }
        if (other.deletedModels != null && deletedModels != null) {
            deletedModels.addAll(other.deletedModels);
        }
        if (other.changedModels != null && changedModels != null) {
            for (var entry : other.changedModels.entrySet()) {
                addChangedModel(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Adds a changed model with the specified change hint.
     *
     * @param uid the changed model uid
     * @param hint the type of change
     */
    protected void addChangedModel(UUID uid, ChangeHint hint) {
        if (changedModels == null) return;
        changedModels.put(uid, ChangeHintList.add(changedModels.get(uid), hint));
    }

    protected void addChangedModel(UUID uid, ChangeHintList hints) {
        if (changedModels == null) return;
        changedModels.put(uid, ChangeHintList.addRange(changedModels.get(uid), hints));
    }

    /**
     * Adds a newly created model.
     *
     * @param model the new model
     */
    public GraphChangeDescription addNewModel(GraphElementModel model) {
        if (model != null) {
            if (!(model instanceof PortModel)) {
                if (model.getGraphModel() != null) {
                    model.getGraphModel().setGraphObjectDirty();
                }
            }
            if (newModels != null) {
                newModels.add(model.getUid());
            }
        }
        return this;
    }

    /**
     * Adds new models to the changes and sets the graph object dirty. This assumes all models are from the same graph.
     */
    public GraphChangeDescription addNewModels(List<GraphElementModel> models) {
        if (models == null || models.isEmpty()) return this;
        var shouldSetDirty = false;
        for (var model : models) {
            if (!(model instanceof PortModel)) {
                shouldSetDirty = true;
            }
            if (newModels != null) {
                newModels.add(model.getUid());
            }
        }
        if (shouldSetDirty) {
            var first = models.getFirst();
            if (first.getGraphModel() != null) {
                first.getGraphModel().setGraphObjectDirty();
            }
        }
        return this;
    }

    /**
     * Adds a deleted model.
     *
     * @param model the deleted model
     */
    public GraphChangeDescription addDeletedModel(GraphElementModel model) {
        if (model != null) {
            if (!(model instanceof PortModel)) {
                if (model.getGraphModel() != null) {
                    model.getGraphModel().setGraphObjectDirty();
                }
            }
            if (deletedModels != null) {
                deletedModels.add(model.getUid());
            }
        }
        return this;
    }

    /**
     * Adds deleted models to the changes and sets the graph object dirty. This assumes all models are from the same graph.
     */
    public GraphChangeDescription addDeletedModels(List<? extends GraphElementModel> models) {
        if (models == null || models.isEmpty()) return this;
        var shouldSetDirty = false;
        for (var model : models) {
            if (!(model instanceof PortModel)) {
                shouldSetDirty = true;
            }
            if (deletedModels != null) {
                deletedModels.add(model.getUid());
            }
        }
        if (shouldSetDirty) {
            var first = models.getFirst();
            if (first.getGraphModel() != null) {
                first.getGraphModel().setGraphObjectDirty();
            }
        }
        return this;
    }

    /**
     * Adds a changed model.
     *
     * @param model the deleted model
     */
    public GraphChangeDescription addChangedModel(GraphElementModel model, ChangeHint change) {
        if (!(model instanceof PortModel)) {
            if (model.getGraphModel() != null) {
                model.getGraphModel().setGraphObjectDirty();
            }
        }
        addChangedModel(model.getUid(), change);
        return this;
    }

    /**
     * Adds changed models to the changes and sets the graph object dirty. This assumes all models are from the same graph.
     */
    public GraphChangeDescription addChangedModels(List<? extends GraphElementModel> models, ChangeHint changeHint) {
        if (models == null || models.isEmpty()) return this;
        var shouldSetDirty = false;
        for (var model : models) {
            if (!(model instanceof PortModel)) {
                shouldSetDirty = true;
            }
            addChangedModel(model.getUid(), changeHint);
        }
        if (shouldSetDirty) {
            var first = models.getFirst();
            if (first.getGraphModel() != null) {
                first.getGraphModel().setGraphObjectDirty();
            }
        }
        return this;
    }

    /**
     * Gets all newly created models.
     *
     * @return unmodifiable set of new models
     */
    public Set<UUID> getNewModels() {
        return newModels == null ? Collections.emptySet() : newModels;
    }

    /**
     * Gets all deleted models.
     *
     * @return unmodifiable set of deleted models
     */
    public Set<UUID> getDeletedModels() {
        return deletedModels == null ? Collections.emptySet() : deletedModels;
    }

    /**
     * Gets all changed models with their change hints.
     *
     * @return unmodifiable map of changed models to their change hints
     */
    public Map<UUID, ChangeHintList> getChangedModels() {
        return changedModels == null ? Collections.emptyMap() : changedModels;
    }

}
