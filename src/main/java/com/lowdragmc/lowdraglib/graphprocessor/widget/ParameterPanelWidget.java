package com.lowdragmc.lowdraglib.graphprocessor.widget;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.editor.Icons;
import com.lowdragmc.lowdraglib.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.lowdraglib.graphprocessor.data.parameter.ParameterNode;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.layout.Layout;
import com.lowdragmc.lowdraglib.utils.TypeAdapter;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ParameterPanelWidget extends DraggablePanelWidget {
    private final GraphViewWidget graphView;
    // runtime
    private DraggableScrollableWidgetGroup nodeListView;
    private final Map<ExposedParameter<?>, SelectableWidgetGroup> mapping = new HashMap<>();
    @Nullable
    private ExposedParameter<?> selectedParameter;
    private boolean firstClick;
    private ExposedParameter<?> firstClickParameter;
    private long firstClickTime;

    public ParameterPanelWidget(GraphViewWidget graphView, int x, int y, int width, int height) {
        super("graph_processor.parameter_panel", x, y, width, height);
        this.graphView = graphView;
    }

    @Override
    protected void loadWidgets() {
        super.loadWidgets();
        mapping.clear();
        if (graphView.getGraph().exposedParameters.isEmpty()) {
            setVisible(false);
        }

        // node list
        nodeListView = new DraggableScrollableWidgetGroup(3, 2, content.getSizeWidth() - 6, content.getSizeHeight() - 4);
        nodeListView.setYScrollBarWidth(4).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(2));
        nodeListView.setLayout(Layout.VERTICAL_LEFT);
        nodeListView.setLayoutPadding(3);
        for (var parameter : graphView.getGraph().exposedParameters.values()) {
            var selectableWidgetGroup = new SelectableWidgetGroup(0, 0, nodeListView.getSizeWidth() - 4, 12);
            selectableWidgetGroup.setDraggingProvider(() -> createParameterNode(parameter), (n, pos) -> new TextTexture(n.getDisplayName()));
            var displayName = parameter.getDisplayName();
            var nameWidth = Minecraft.getInstance().font.width(displayName);

            selectableWidgetGroup.addWidget(new ImageWidget(0, 0, 5 + nameWidth + 15, 12,
                    ColorPattern.GRAY.rectTexture().setRadius(5)));

            selectableWidgetGroup.addWidget(new ImageWidget(0, 0, selectableWidgetGroup.getSizeWidth() - 2, 12,
                    new TextTexture(TypeAdapter.getTypeDisplayName(parameter.type))
                            .setWidth(selectableWidgetGroup.getSizeWidth()).setType(TextTexture.TextType.RIGHT)));

            if (parameter.getAccessor() == ExposedParameter.ParameterAccessor.Get) {
                selectableWidgetGroup.addWidget(new ImageWidget(5, 1, nameWidth, 12,
                        new TextTexture(displayName).setWidth(nameWidth).setType(TextTexture.TextType.LEFT_ROLL)));
                selectableWidgetGroup.addWidget(new ImageWidget(5 + nameWidth + 2,  2, 8, 8,
                        Icons.RADIOBOX_MARKED.copy().setColor(TypeAdapter.getTypeColor(parameter.type))));
            } else {
                selectableWidgetGroup.addWidget(new ImageWidget(15, 1, nameWidth, 12,
                        new TextTexture(displayName).setWidth(nameWidth).setType(TextTexture.TextType.LEFT_ROLL)));
                selectableWidgetGroup.addWidget(new ImageWidget(3,  2, 8, 8,
                        Icons.RADIOBOX_MARKED.copy().setColor(TypeAdapter.getTypeColor(parameter.type))));
            }


            selectableWidgetGroup.setOnSelected(s -> selectedParameter = parameter);
            selectableWidgetGroup.setOnUnSelected(s -> selectedParameter = null);
            selectableWidgetGroup.setSelectedTexture(ColorPattern.T_GRAY.rectTexture());
            nodeListView.addWidget(selectableWidgetGroup);
            mapping.put(parameter, selectableWidgetGroup);
        }
        nodeListView.setLayout(Layout.NONE);
        nodeListView.setLayoutPadding(0);
        nodeListView.computeMax();
        content.addWidget(nodeListView);
    }

    private ParameterNode createParameterNode(ExposedParameter<?> parameter) {
        var node = new ParameterNode();
        node.parameterIdentifier = parameter.identifier;
        node.parameter = parameter;
        return node;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var result = super.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && nodeListView.isMouseOverElement(mouseX, mouseY)) {
            if (selectedParameter != null && mapping.get(selectedParameter).isMouseOverElement(mouseX, mouseY)) {
                if (firstClick && firstClickParameter.equals(selectedParameter) && gui.getTickCount() - firstClickTime < 10) {
                    graphView.addNodeToCenter(createParameterNode(selectedParameter));
                    selectedParameter = null;
                    return true;
                }
                firstClick = true;
                firstClickParameter = selectedParameter;
                firstClickTime = gui.getTickCount();
            }
        }
        return result;
    }

}
