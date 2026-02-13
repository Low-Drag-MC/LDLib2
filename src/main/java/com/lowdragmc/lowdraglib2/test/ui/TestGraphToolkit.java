package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestAddNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestConstantNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestStringConcatNode;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector2f;

@LDLRegisterClient(name="graph_toolkit", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestGraphToolkit implements IScreenTest {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement();
        root.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.paddingAll(4);
        }).setId("root").getStyle().backgroundTexture(Sprites.BORDER);
        var graphEditor = new GraphView();
        root.addChildren(graphEditor.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        }));
        graphEditor.loadGraph(createTestGraph());
        return new ModularUI(UI.of(root));
    }

    public static Graph createTestGraph() {
        var graph = new TestGraph();
        // nodes
        graph.graphModel.createNodeModel(new TestStringConcatNode(), new Vector2f(200, 200));
        var constant = graph.graphModel.createNodeModel(new TestConstantNode(), new Vector2f(0));
        var add1 = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(50));
        var add2 = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(150));
        // wires
        graph.graphModel.createWire(constant.getOutputsById().get("out"), add2.getInputsById().get("in2"));
        return graph;
    }
}
