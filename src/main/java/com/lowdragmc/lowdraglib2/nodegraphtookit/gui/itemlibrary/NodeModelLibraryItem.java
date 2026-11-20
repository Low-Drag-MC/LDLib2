package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

public class NodeModelLibraryItem extends ItemLibraryItem {
    private final Function<GraphNodeCreationData, GraphElementModel> creator;
    /** The group the node is filed under, as {@code a/b}; empty for one at the top level, or a constant. */
    @Getter @Setter @Accessors(chain = true)
    private String group = "";

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
