package com.lowdragmc.lowdraglib.compass.component.animation;

import com.lowdragmc.lowdraglib.compass.CompassManager;
import com.lowdragmc.lowdraglib.utils.XmlUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author KilaBash
 * @date 2023/7/28
 * @implNote AnimationFrame
 */
@Accessors(chain = true, fluent = true)
public class AnimationFrame {
    @Setter
    private int duration = -1; // -1: wait for all actions to finish
    @Getter @Setter
    private int delay;
    @Getter
    private final List<Action> actions = new ArrayList<>();
    @Getter
    private final List<Component> tooltips = new ArrayList<>();
    //runtime
    private int[] actionTime = new int[0];

    public AnimationFrame() {
    }

    public AnimationFrame addActions(Action... actions) {
        this.actions.addAll(Arrays.asList(actions));
        this.calculateActionTime();
        return this;
    }

    public AnimationFrame(int frameIndex, Element element) {
        this.duration = XmlUtils.getAsInt(element, "duration", -1);
        this.delay = XmlUtils.getAsInt(element, "delay", 0);
        var nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            var node = nodeList.item(i);
            if (node instanceof Element actionElement) {
                if (actionElement.getNodeName().equals("description")) {
                    tooltips.addAll(XmlUtils.getComponents(actionElement, Style.EMPTY));
                }
                var action = CompassManager.INSTANCE.createAction(actionElement);
                if (action != null) {
                    actions.add(action);
                }
            }
        }
        this.calculateActionTime();
    }

    protected void calculateActionTime() {
        actionTime = new int[actions.size()];
        for (int i = 0; i < actions.size(); i++) {
            var action = actions.get(i);
            var lastAction = i == 0 ? null : actions.get(i - 1);
            actionTime[i] = action.delay + (i == 0 ? 0 : actionTime[i - 1]);
            if (!action.startBeforeLast) {
                actionTime[i] += lastAction == null ? 0 : lastAction.getDuration();
            }
        }
    }

    public int getDuration() {
        return duration > 0 ? duration : actionTime.length == 0 ? 0 : actionTime[actionTime.length - 1] + actions.get(actions.size() - 1).getDuration();
    }

    protected void onFrameStart(CompassScene scene) {
    }

    protected void onFrameEnd(CompassScene scene) {
    }

    public boolean onFrameTick(CompassScene scene, int frameTick) {
        if (frameTick == 0) {
            onFrameStart(scene);
        }
        if (frameTick >= getDuration()) {
            onFrameEnd(scene);
            return true;
        }
        for (int i = 0; i < actionTime.length; i++) {
            if (actionTime[i] == frameTick) {
                actions.get(i).performAction(this, scene, true);
            } else if (actionTime[i] > frameTick) {
                break;
            }
        }
        return false;
    }

    public void performFrameResult(CompassScene compassScene) {
        for (Action action : actions) {
            action.performAction(this, compassScene, false);
        }
    }
}
