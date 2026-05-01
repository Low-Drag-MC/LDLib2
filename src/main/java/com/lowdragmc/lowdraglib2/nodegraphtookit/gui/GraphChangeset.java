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

    public boolean addNewModels(Collection<UUID> models) {
        if (models == null) return false;
        var somethingChanged = false;

        for (var uid : models) {
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

    public boolean addChangedModels(Map<UUID, ChangeHintList> changes) {
        if (changes == null) return false;
        var somethingChanged = false;
        for (var entry : changes.entrySet()) {
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

    public boolean addDeletedModels(Collection<UUID> models) {
        if (models == null) return false;
        var somethingChanged = false;
        for (var uid : models) {
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
