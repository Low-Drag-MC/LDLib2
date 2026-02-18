package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

public class NodeModelLibraryItem extends ItemLibraryItem {
    private final Function<GraphNodeCreationData, GraphElementModel> creator;

    public NodeModelLibraryItem(String name, Function<GraphNodeCreationData, GraphElementModel> creator) {
        this.icon = Icons.NODE;
        this.displayName = Component.translatable(name);
        this.searchableName = name;
        this.creator = creator;
    }

    public GraphElementModel createNode(GraphNodeCreationData data) {
        return creator.apply(data);
    }
}
