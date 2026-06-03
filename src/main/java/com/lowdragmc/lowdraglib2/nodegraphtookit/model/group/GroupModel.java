package com.lowdragmc.lowdraglib2.nodegraphtookit.model.group;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IGraphElementContainer;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class GroupModel extends GroupModelBase {
    @Getter
    protected List<IGroupItemModel> items = new ArrayList<>();

    @Persisted @Getter
    protected String name = "";

    @Override
    public void setName(String title) {
        if (this.name.equals(title)) return;
        this.name = title;
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    @Override
    public Stream<GraphElementModel> getDependentModels() {
        return Stream.concat(super.getDependentModels(), getGraphElementModels().stream());
    }

    @Override
    public IGraphElementContainer getContainer() {
        return getParentGroup();
    }

    @Override
    public IGroupItemModel getGroupItemInTargetGraph(GraphModel targetModel, Map<VariableDeclarationModelBase, VariableDeclarationModelBase> variableTranslation) {
        var newGroup = targetModel.createGroup(getName(), null);
        newGroup.copyFrom(this, variableTranslation);
        return newGroup;
    }

    /**
     * Copy from a source group to this group.
     * @param source The source group
     * @param variableTranslation The translation between source variable and our variables.
     */
    public void copyFrom(GroupModelBase source, Map<VariableDeclarationModelBase, VariableDeclarationModelBase> variableTranslation) {
        for (var item : source.getItems()) {
            var newItem = item.getGroupItemInTargetGraph(graphModel, variableTranslation);
            if (newItem != null) insertItem(newItem, items.size());
        }
    }

    /**
     * Inserts an item at the given index.
     * @param item The item.
     * @param index The index at which insert the item. For index = 0, The item will be added at the beginning.
     *              For index = Items.Count, items will be added at the end.
     */
    public void insertItem(IGroupItemModel item, int index) {
        GroupModelBase current = this;
        while (current != null) {
            if (current == item) {
                return;
            }
            current = current.getParentGroup();
        }

        if (item.getParentGroup() instanceof GroupModel group) {
            group.removeItem(item);
        }

        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.GROUPING);
        }
        item.setParentGroup(this);
        index = Mth.clamp(index, 0, items.size());
        items.add(index, item);

    }

    /**
     * Removes an item from the group.
     */
    public void removeItem(IGroupItemModel item) {
        if (items.contains(item)) {
            item.setParentGroup(null);
            items.remove(item);
            if (graphModel != null) {
                graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.GROUPING);
            }
        }
    }
}
