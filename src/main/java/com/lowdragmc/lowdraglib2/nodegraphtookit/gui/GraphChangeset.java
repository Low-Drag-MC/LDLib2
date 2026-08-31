package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHintList;
import lombok.Getter;

import java.util.*;

public class GraphChangeset {
    @Getter
    private Set<UUID> newModels;
    @Getter
    private Map<UUID, ChangeHintList> changedModelsAndHints;
    @Getter
    private Set<UUID> deletedModels;

    public GraphChangeset() {
        newModels = new HashSet<>();
        changedModelsAndHints = new HashMap<>();
        deletedModels = new HashSet<>();
    }

    public void clear() {
        newModels.clear();
        changedModelsAndHints.clear();
        deletedModels.clear();
    }

    public boolean isEmpty() {
        return newModels.isEmpty() && changedModelsAndHints.isEmpty() && deletedModels.isEmpty();
    }

    public boolean hasChanges() {
        return !isEmpty();
    }

    /**
     * Merges a batch of new-model ids into this changeset.
     *
     * <h2>Why the input is snapshotted</h2>
     * These three merge methods take a collection they do not own — in practice the sets of a
     * {@link com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphChangeDescription} just
     * flushed off a graph model. Iterating a caller's live collection is only safe if nothing can
     * touch it in between, which is not an invariant this class can enforce.
     *
     * <p>It was broken in the field: the blackboard's type picker searched on a background thread and
     * ended up building models in the graph being viewed, racing the render thread here and crashing
     * the screen. That cause is fixed at its source — see
     * {@link com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl#detectSupportedTypes}
     * — and the copy stays as the cheap second line of defence, because a merge method that iterates
     * someone else's live collection is a latent version of the same crash.</p>
     *
     * <p>{@code new ArrayList<>} rather than {@code List.copyOf}: these loops check for null ids, so
     * the input may legitimately contain them and {@code copyOf} would throw.
     * {@code GraphView.updateGraphModelChanges} already copies defensively two statements later.</p>
     */
    public boolean addNewModels(Collection<UUID> models) {
        if (models == null) return false;
        var somethingChanged = false;

        for (var uid : new ArrayList<>(models)) {
            if (uid != null) {
                if (deletedModels.contains(uid))
                    continue;

                changedModelsAndHints.remove(uid);
                newModels.add(uid);

                somethingChanged = true;
            }
        }
        return somethingChanged;
    }

    /** @see #addNewModels for why the input is snapshotted. */
    public boolean addChangedModels(Map<UUID, ChangeHintList> changes) {
        if (changes == null) return false;
        var somethingChanged = false;
        for (var entry : new HashMap<>(changes).entrySet()) {
            var uid = entry.getKey();
            var changeHints = entry.getValue();
            if (uid == null || changeHints == null || newModels.contains(uid) || deletedModels.contains(uid)) continue;
            addChangedModel(uid, changeHints);
            somethingChanged = true;
        }
        return somethingChanged;
    }

    protected void addChangedModel(UUID uid, ChangeHintList changeHints) {
        changedModelsAndHints.put(uid, ChangeHintList.addRange(changedModelsAndHints.get(uid), changeHints));
    }

    /** @see #addNewModels for why the input is snapshotted. */
    public boolean addDeletedModels(Collection<UUID> models) {
        if (models == null) return false;
        var somethingChanged = false;
        for (var uid : new ArrayList<>(models)) {
            if (uid == null) continue;
            var wasNew = newModels.remove(uid);
            changedModelsAndHints.remove(uid);
            if (wasNew) continue;
            deletedModels.add(uid);
            somethingChanged = true;
        }
        return somethingChanged;
    }
}
