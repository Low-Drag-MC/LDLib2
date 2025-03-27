package com.lowdragmc.lowdraglib.compass.component;

import com.lowdragmc.lowdraglib.compass.ILayoutComponent;
import com.lowdragmc.lowdraglib.compass.LayoutPageWidget;
import com.lowdragmc.lowdraglib.utils.XmlUtils;
import lombok.NoArgsConstructor;
import org.w3c.dom.Element;

/**
 * @author KilaBash
 * @date 2022/9/4
 * @implNote BlankComponent
 */
@NoArgsConstructor
public class BlankComponent implements ILayoutComponent {
    protected int height = 10;

    @Override
    public ILayoutComponent fromXml(Element element) {
        this.height = XmlUtils.getAsInt(element, "height", height);
        return this;
    }

    @Override
    public LayoutPageWidget createWidgets(LayoutPageWidget currentPage) {
        currentPage.addOffsetSpace(height);
        return currentPage;
    }
}
