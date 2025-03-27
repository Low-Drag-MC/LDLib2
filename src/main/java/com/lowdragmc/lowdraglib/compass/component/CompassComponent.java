package com.lowdragmc.lowdraglib.compass.component;

import com.lowdragmc.lowdraglib.compass.ILayoutComponent;
import com.lowdragmc.lowdraglib.compass.LayoutPageWidget;
import com.lowdragmc.lowdraglib.compass.component.animation.AnimationFrame;
import com.lowdragmc.lowdraglib.compass.component.animation.CompassScene;
import com.lowdragmc.lowdraglib.utils.XmlUtils;
import lombok.Getter;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * @author KilaBash
 * @date 2023/7/28
 * @implNote AnimationComponent
 */
public class CompassComponent extends AbstractComponent {
    @Getter
    List<AnimationFrame> frames = new ArrayList<>();
    @Getter
    boolean useScene = true;
    @Getter
    boolean tickScene = false;
    @Getter
    float zoom = -1;
    @Getter
    int range = 5;
    @Getter
    int height = 250;
    @Getter
    boolean draggable = false;
    @Getter
    boolean scalable = false;
    @Getter
    boolean ortho = false;
    @Getter
    float yaw = 25;

    @Override
    public ILayoutComponent fromXml(Element element) {
        var frameNodes = element.getElementsByTagName("frame");
        for (int i = 0; i < frameNodes.getLength(); i++) {
            var frameElement = (Element) frameNodes.item(i);
            frames.add(new AnimationFrame(i, frameElement));
        }
        useScene = XmlUtils.getAsBoolean(element, "scene", useScene);
        zoom = XmlUtils.getAsFloat(element, "zoom", zoom);
        height = XmlUtils.getAsInt(element, "height", height);
        tickScene = XmlUtils.getAsBoolean(element, "tick-scene", tickScene);
        draggable = XmlUtils.getAsBoolean(element, "draggable", draggable);
        scalable = XmlUtils.getAsBoolean(element, "scalable", scalable);
        ortho = !XmlUtils.getAsString(element, "camera", "ortho").equals("perspective");
        zoom = XmlUtils.getAsFloat(element, "zoom", zoom);
        range = Math.abs(XmlUtils.getAsInt(element, "range", range));
        yaw = XmlUtils.getAsFloat(element, "yaw", yaw);
        bottomMargin = 10;
        topMargin = 10;
        return super.fromXml(element);
    }

    @Override
    protected LayoutPageWidget addWidgets(LayoutPageWidget currentPage) {
        return currentPage.addStreamWidget(wrapper(new CompassScene(width(currentPage), this)));
    }

}
