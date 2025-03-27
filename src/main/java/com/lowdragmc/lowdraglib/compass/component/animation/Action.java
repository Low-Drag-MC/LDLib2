package com.lowdragmc.lowdraglib.compass.component.animation;

import com.lowdragmc.lowdraglib.utils.XmlUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.w3c.dom.Element;

/**
 * @author KilaBash
 * @date 2023/7/28
 * @implNote Action
 */
@Accessors(chain = true, fluent = true)
public abstract class Action {

    @Getter @Setter
    protected int delay;
    @Getter @Setter
    protected boolean startBeforeLast;

    public Action() {
    }

    public Action(Element element) {
        delay = XmlUtils.getAsInt(element, "delay", 0);
        startBeforeLast = XmlUtils.getAsBoolean(element, "start-before-last", false);
    }

    public abstract int getDuration();


    public abstract void performAction(AnimationFrame frame, CompassScene scene, boolean anima);
}
