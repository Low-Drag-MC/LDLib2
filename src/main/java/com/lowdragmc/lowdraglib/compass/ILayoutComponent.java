package com.lowdragmc.lowdraglib.compass;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.w3c.dom.Element;

/**
 * @author KilaBash
 * @date 2022/9/3
 * @implNote ILayoutWidget, automatic layout + configurable
 */
public interface ILayoutComponent {

    /**
     * load from xml
     * @param element config
     */
    ILayoutComponent fromXml(Element element);

    /**
     * create and add widgets to the page
     * @param currentPage current page
     * @return latest page
     */
    @OnlyIn(Dist.CLIENT)
    default LayoutPageWidget createWidgets(LayoutPageWidget currentPage) {
        return currentPage;
    }
}
