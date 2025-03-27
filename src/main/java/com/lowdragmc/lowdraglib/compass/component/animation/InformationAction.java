package com.lowdragmc.lowdraglib.compass.component.animation;

import com.lowdragmc.lowdraglib.compass.CompassManager;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.XmlUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * @author KilaBash
 * @date 2023/7/29
 * @implNote InformationAction
 */
public class InformationAction extends Action {

    private final IGuiTexture guiTexture;
    private final List<Component> text = new ArrayList<>();

    public InformationAction(IGuiTexture guiTexture, List<Component> text) {
        this.guiTexture = guiTexture;
        this.text.addAll(text);
    }

    public InformationAction(Element element) {
        super(element);
        var u0 = XmlUtils.getAsFloat(element, "u0", 0);
        var v0 = XmlUtils.getAsFloat(element, "v0", 0);
        var u1 = XmlUtils.getAsFloat(element, "u1", 1);
        var v1 = XmlUtils.getAsFloat(element, "v1", 1);
        String type = XmlUtils.getAsString(element, "type", "");
        String url = XmlUtils.getAsString(element, "url", "");

        guiTexture = switch (type) {
            case "resource" -> new ResourceTexture(url).getSubTexture(u0, v0, u1, v1);
            case "item" -> new ItemStackTexture(BuiltInRegistries.ITEM.get(ResourceLocation.parse(url)));
            case "shader" -> ShaderTexture.createShader(ResourceLocation.parse(url));
            default -> IGuiTexture.EMPTY;
        };

        text.addAll(XmlUtils.getComponents(element, Style.EMPTY));
    }

    @Override
    public int getDuration() {
        return 20;
    }

    @Override
    public void performAction(AnimationFrame frame, CompassScene scene, boolean anima) {
        var size = Math.min(scene.getHeaderGroup().getSize().width, scene.getHeaderGroup().getSize().height);
        if (text.isEmpty() && guiTexture == IGuiTexture.EMPTY) return;
        if (text.isEmpty()) {
            scene.addInformation(new ImageWidget(0, 0, size, size, guiTexture), anima);
        } else if (guiTexture == IGuiTexture.EMPTY) {
            var componentPanelWidget = new ComponentPanelWidget(0, 0, text).clickHandler(CompassManager::onComponentClick);
            componentPanelWidget.setBackground(TooltipBGTexture.INSTANCE);
            var maxWidth = 0;
            while (maxWidth < scene.getHeaderGroup().getSize().width && (componentPanelWidget.getSize().height == 0 || componentPanelWidget.getSize().height > size)) {
                maxWidth += 50;
                componentPanelWidget.setMaxWidthLimit(maxWidth);
            }
            scene.addInformation(componentPanelWidget, anima);
        } else {
            var componentPanelWidget = new ComponentPanelWidget(0, 0, text).clickHandler(CompassManager::onComponentClick);
            componentPanelWidget.setBackground(TooltipBGTexture.INSTANCE);
            var maxWidth = 0;
            while (maxWidth < scene.getHeaderGroup().getSize().width && (componentPanelWidget.getSize().height == 0 || componentPanelWidget.getSize().height > size)) {
                maxWidth += 50;
                componentPanelWidget.setMaxWidthLimit(maxWidth);
            }
            var container = new WidgetGroup(0, 0, size + maxWidth, size);
            container.addWidget(new ImageWidget(2, 2, size - 4, size - 4, guiTexture));
            componentPanelWidget.addSelfPosition(size, (size - componentPanelWidget.getSize().height) / 2);
            container.addWidget(componentPanelWidget);
            scene.addInformation(container, anima);
        }
    }
}
