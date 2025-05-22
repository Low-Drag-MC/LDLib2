package com.lowdragmc.lowdraglib.compass;

import com.lowdragmc.lowdraglib.editor_outdated.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CompassSelectorWidget extends WidgetGroup {
    private final List<CompassNode> nodes;

    public CompassSelectorWidget(List<CompassNode> nodes) {
        super(0, 0, 210, 100);
        setClientSideWidget();
        this.nodes = nodes;
    }

    @Override
    public void initWidget() {
        super.initWidget();
        setBackground(new GuiTextureGroup(ColorPattern.BLACK.rectTexture(), ColorPattern.T_GRAY.borderTexture(1)));
        var listGroup = new DraggableScrollableWidgetGroup(4, 4, 202, 92)
                .setYScrollBarWidth(2)
                .setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(1))
                .setBackground(ColorPattern.T_GRAY.rectTexture().setRadius(1));
        this.addWidget(listGroup);
        Map<ResourceLocation, List<CompassNode>> map = nodes.stream().collect(Collectors.groupingBy(n -> n.getSection().getSectionName()));
        int y = 3;
        for (var entry : map.entrySet()) {
            listGroup.addWidget(new LabelWidget(2, y, entry.getKey().toLanguageKey("compass.section")));
            y += 12;
            int x = 0;
            for (var node : entry.getValue()) {
                var background = node.getBackground();
                var hoverBackground = node.getHoverBackground();
                var buttonTexture = node.getButtonTexture();
                var config = CompassManager.INSTANCE.getUIConfig(node.getSection().getSectionName().getNamespace());
                if (background == null) {
                    background = config.getNodeBackground();
                }
                if (hoverBackground == null) {
                    hoverBackground = config.getNodeHoverBackground();
                }
                listGroup.addWidget(new ButtonWidget(x + 2, y + 2, 20, 20, new GuiTextureGroup(background, new GuiTextureGroup(buttonTexture).scale(0.8f)), cd -> {
                    CompassManager.INSTANCE.openCompass(node);
                }).setHoverTexture(new GuiTextureGroup(hoverBackground, new GuiTextureGroup(buttonTexture).scale(0.8f))).setHoverTooltips(node.getNodeName().toLanguageKey("compass.node")));
                x += 25;
                if (x > 175) {
                    x = 0;
                    y += 25;
                }
            }
            if (x != 0) {
                y += 25;
            }
        }
    }
}
