package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Inspector;
import net.minecraft.network.chat.Component;

public class GraphInspector extends Inspector implements IGraphTool {
    public final GraphView graphView;

    public GraphInspector(GraphView graphView) {
        this.graphView = graphView;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("graph.inspector");
    }
}
