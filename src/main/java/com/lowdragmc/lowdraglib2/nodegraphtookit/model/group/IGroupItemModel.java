package com.lowdragmc.lowdraglib2.nodegraphtookit.model.group;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;

import java.util.Map;
import java.util.stream.Stream;

public interface IGroupItemModel {
    /**
     * Get the parent group of this item.
     */
    GroupModelBase getParentGroup();

    /**
     * Set the parent group of this item.
     */
    void setParentGroup(GroupModelBase parentGroup);

    /**
     * This model and the models that this model contains.
     */
    Stream<GraphElementModel> getContainedModels();

    /**
     * Gets a IGroupItemModel representing this IGroupItemModel for the given targetModel.
     * @param targetModel The model the clone belongs to.
     * @param variableTranslation The map between the source variables and the target variables.
     * @return The cloned model.
     */
    IGroupItemModel getGroupItemInTargetGraph(GraphModel targetModel, Map<VariableDeclarationModelBase, VariableDeclarationModelBase> variableTranslation);

    String getName();

    /**
     * Returns the section for a given item.
     */
    default SectionModel getSection() {
        if (this instanceof SectionModel sectionModel) return sectionModel;
        var current = this;
        while (current.getParentGroup() != null) {
            current = current.getParentGroup();
        }
        return (SectionModel) current;
    }

    default boolean isInGroup(GroupModelBase group) {
        var currentGroup = getParentGroup();
        while (currentGroup != null) {
            if (currentGroup == group) return true;
            currentGroup = currentGroup.getParentGroup();
        }
        return false;
    }
}
