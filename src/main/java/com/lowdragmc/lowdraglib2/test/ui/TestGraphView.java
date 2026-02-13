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

@LDLRegisterClient(name="graph_view", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestGraphView implements IScreenTest {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement();
        root.layout(layout -> {
            layout.width(300);
            layout.height(300);
            layout.paddingAll(10);
        }).setId("root").getStyle().backgroundTexture(Sprites.BORDER);
        var graph = new GraphView();
        root.addChildren(graph.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        }));

        graph.addContentChild(new Button().layout(layout -> layout.width(40)));
        return new ModularUI(UI.of(root));
    }
}
