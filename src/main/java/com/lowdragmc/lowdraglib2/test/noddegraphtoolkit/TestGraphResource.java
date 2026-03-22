package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphResource;

public class TestGraphResource extends GraphResource<TestGraph> {
    public static final TestGraphResource INSTANCE = new TestGraphResource();

    @Override
    public TestGraph createGraph() {
        return new TestGraph();
    }

    @Override
    public IGuiTexture getIcon() {
        return Icons.WIDGET_CUSTOM;
    }

    @Override
    public String getName() {
        return "test_graph";
    }
}
