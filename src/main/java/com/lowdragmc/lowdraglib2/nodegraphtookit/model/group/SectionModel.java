package com.lowdragmc.lowdraglib2.nodegraphtookit.model.group;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Capabilities;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IGraphElementContainer;

public class SectionModel extends GroupModel {
    public SectionModel() {
        setCapability(Capabilities.DELETABLE, false);
        setCapability(Capabilities.DROPPABLE, false);
        setCapability(Capabilities.SELECTABLE, false);
        setCapability(Capabilities.RENAMABLE, false);
        setCapability(Capabilities.COPIABLE, false);
    }

    @Override
    public IGraphElementContainer getContainer() {
        return getGraphModel();
    }

    @Override
    public SectionModel getSection() {
        return this;
    }

    /**
     * Returns whether the given item can be dragged in this section.
     */
    public boolean acceptsDraggedModel(IGroupItemModel itemModel) {
        return itemModel.getSection() == this;
    }
}
