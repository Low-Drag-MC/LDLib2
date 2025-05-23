package com.lowdragmc.lowdraglib.graphprocessor.widget;

import com.lowdragmc.lowdraglib.gui.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;

@Getter
public class DebugPanelWidget extends DraggablePanelWidget {
    private final GraphViewWidget graphView;

    public DebugPanelWidget(GraphViewWidget graphView) {
        super("Debug", 0, 0, 120, 75);
        this.graphView = graphView;
    }

    @Override
    protected void loadWidgets() {
        super.loadWidgets();
        content.addWidget(new ImageWidget(5, 2, 110, 10,
                new TextTexture().setSupplier(() -> LocalizationUtils.format("graph_processor.depth", graphView.getGraph().getDepth()))
                        .setWidth(110).setType(TextTexture.TextType.LEFT)));
        var width = Minecraft.getInstance().font.width(LocalizationUtils.format("graph_processor.show_debug_info"));
        content.addWidget(new ImageWidget(5, 15, width, 15, new TextTexture("graph_processor.show_debug_info")));
        content.addWidget(new SwitchWidget(width + 8, 17, 10, 10, (cd, pressed) ->
                graphView.setShowDebugInfo(pressed)).setPressed(graphView.isShowDebugInfo()).setTexture(
                new ColorBorderTexture(-1, -1).setRadius(5),
                new GuiTextureGroup(new ColorBorderTexture(-1, -1).setRadius(5), new ColorRectTexture(-1).setRadius(5).scale(0.5f))));
        content.addWidget(new ButtonWidget(5, 32, 110, 10, (cd) -> graphView.runAll())
                .setButtonTexture(ColorPattern.T_GRAY.rectTexture().setRadius(5), new GuiTextureGroup(
                        new TextTexture("graph_processor.process_mode.run_all"),
                        new DynamicTexture(() -> !graphView.isRunStep() ? ColorPattern.YELLOW.borderTexture(1).setRadius(5) : IGuiTexture.EMPTY)
                )));
        content.addWidget(new ButtonWidget(5, 47, 110, 10, (cd) -> graphView.runStep())
                .setButtonTexture(ColorPattern.T_GRAY.rectTexture().setRadius(5), new GuiTextureGroup(
                        new TextTexture(() -> graphView.isRunStepFinish() ? "graph_processor.process_mode.step_finish" : "graph_processor.process_mode.step"),
                        new DynamicTexture(() -> graphView.isRunStep() ? ColorPattern.rainbowRectTexture(1).setRadius(5) : IGuiTexture.EMPTY)
                )));
    }
}
