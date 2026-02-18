package com.lowdragmc.lowdraglib2.nodegraphtookit.model.group;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public abstract class GroupModelBase extends GraphElementModel implements IGroupItemModel, IGraphElementContainer, IHasName {
    @Getter @Setter
    protected GroupModelBase parentGroup;

    public GroupModelBase() {
        capabilities.addAll(List.of(
                Capabilities.DELETABLE,
                Capabilities.DROPPABLE,
                Capabilities.SELECTABLE,
                Capabilities.COLLAPSIBLE,
                Capabilities.COPIABLE,
                Capabilities.RENAMABLE
        ));
    }

    /**
     * The list of items contained in this group.
     */
    public abstract List<IGroupItemModel> getItems();

    /**
     * Returns the section for this Group.
     */
    public SectionModel getSection() {
        if (getParentGroup() == null) return null;
        return getParentGroup().getSection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<GraphElementModel> getContainedModels() {
        return getItems().stream()
                .flatMap(t -> Stream.concat(
                        t instanceof GraphElementModel model
                                ? Stream.of(model)
                                : Stream.empty(),
                        t.getContainedModels()
                ));
    }

    @Override
    public List<GraphElementModel> getGraphElementModels() {
        return getItems().stream().filter(GraphElementModel.class::isInstance).map(GraphElementModel.class::cast).toList();
    }

    @Override
    public void removeContainerElements(Collection<? extends GraphElementModel> elementsToRemove) {}

    @Override
    public boolean repair() {
        return false;
    }

}
