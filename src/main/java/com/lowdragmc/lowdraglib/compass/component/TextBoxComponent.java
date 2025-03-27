package com.lowdragmc.lowdraglib.compass.component;

import com.lowdragmc.lowdraglib.compass.CompassManager;
import com.lowdragmc.lowdraglib.compass.ILayoutComponent;
import com.lowdragmc.lowdraglib.compass.LayoutPageWidget;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.math.Size;
import com.lowdragmc.lowdraglib.utils.XmlUtils;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * @author KilaBash
 * @date 2022/9/3
 * @implNote TextBoxComponent
 */
@NoArgsConstructor
public class TextBoxComponent extends AbstractComponent {
    protected List<Component> components = new ArrayList<>();
    protected int space = 2;
    protected boolean isCenter;

    @Override
    public ILayoutComponent fromXml(Element element) {
        super.fromXml(element);
        components.addAll(XmlUtils.getComponents(element, Style.EMPTY));
        space = XmlUtils.getAsInt(element, "space", space);
        if (element.hasAttribute("isCenter")) {
            isCenter = XmlUtils.getAsBoolean(element, "isCenter", true);
        }
        return this;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected LayoutPageWidget addWidgets(LayoutPageWidget currentPage) {
        var panel = new ComponentPanelWidget(0, 0, components) {
            @Override
            public void updateComponentTextSize() {
                var fontRenderer = Minecraft.getInstance().font;
                int totalHeight = cacheLines.size() * (fontRenderer.lineHeight + space);
                if (totalHeight > 0) {
                    totalHeight -= space;
                    totalHeight += 2;
                }
                setSize(Size.of(maxWidthLimit, totalHeight));
            }
        }.setSpace(space).setCenter(isCenter).setMaxWidthLimit(width(currentPage)).clickHandler(CompassManager::onComponentClick);
        return currentPage.addStreamWidget(wrapper(panel));
    }

}
