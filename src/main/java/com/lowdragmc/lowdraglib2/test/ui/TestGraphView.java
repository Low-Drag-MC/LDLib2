package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphView;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;
import org.appliedenergistics.yoga.YogaEdge;

@LDLRegisterClient(name="graph_view", registry = "screen_test")
@NoArgsConstructor
public class TestGraphView implements IScreenTest {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement();
        root.layout(layout -> {
            layout.setWidth(300);
            layout.setHeight(300);
            layout.setPadding(YogaEdge.ALL, 10);
        }).setId("root").getStyle().backgroundTexture(Sprites.BORDER);
        var graph = new GraphView();
        root.addChildren(graph.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        }));

        graph.addContentChild(new Button().layout(layout -> layout.setWidth(40)));
        return new ModularUI(UI.of(root));
    }
}
