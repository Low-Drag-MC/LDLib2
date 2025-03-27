package com.lowdragmc.lowdraglib.graphprocessor.widget;

import com.lowdragmc.lowdraglib.graphprocessor.data.NodePort;
import lombok.Getter;

import java.util.ArrayList;

@Getter
public class CollapsePortWidget extends NodePortWidget {

    public CollapsePortWidget(NodeWidget nodeWidget, NodePort port, boolean isInput) {
        super(nodeWidget, isInput, port);
    }

    @Override
    public void initPortInformation() {
        setSize(15, 15);
        // setup hover tips
        var tooltips = new ArrayList<String>();
        tooltips.add("Type: %s".formatted(getPortTypeName()));
        if (port.portData.tooltip != null && !port.portData.tooltip.isEmpty()) {
            tooltips.addAll(port.portData.tooltip);
        }
        setHoverTooltips(tooltips.toArray(new String[0]));
    }

    @Override
    public String getDisplayName() {
        return "";
    }
}
