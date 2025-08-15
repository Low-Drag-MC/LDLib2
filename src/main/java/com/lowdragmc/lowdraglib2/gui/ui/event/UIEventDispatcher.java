package com.lowdragmc.lowdraglib2.gui.ui.event;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import lombok.experimental.UtilityClass;

import javax.annotation.Nullable;
import java.util.Stack;

@UtilityClass
public final class UIEventDispatcher {
    public static void dispatchEvent(UIEvent event) {
        // 1. build path from root to target
        var target = event.target;
        var path = target.getStructurePath();

        // 2. capture phase: root -> target.parent
        if (event.hasCapturePhase) {
            event.phase = UIEvent.EventPhase.CAPTURE;
            for (int i = 0; i < path.size() - 1; i++) {
                UIElement elem = path.get(i);
                event.currentElement = elem;
                // call capture listeners
                var captures = elem.getCaptureListeners(event.type);
                for (UIEventListener listener : captures) {
                    listener.handleEvent(event);
                    if (event.immediatePropagationStopped) {
                        break;  // skip to leftover bubble phase
                    }
                }
                if (event.propagationStopped) {
                    return;  // stop propagation, exit loop
                }
            }
        }

        // 3. Target phase: target
        event.phase = UIEvent.EventPhase.AT_TARGET;
        event.currentElement = target;
        // For target element, execute both capture and bubble listeners
        var targetCaptures = target.getCaptureListeners(event.type);
        for (UIEventListener listener : targetCaptures) {
            listener.handleEvent(event);
            if (event.immediatePropagationStopped) break;
        }
        var targetBubbles = target.getBubbleListeners(event.type);
        for (UIEventListener listener : targetBubbles) {
            listener.handleEvent(event);
            if (event.immediatePropagationStopped) break;
        }
        if (event.propagationStopped) {
            return;  // stop propagation, exit loop
        }

        // 4. Bubbling phase: from target's parent back to root
        if (event.hasBubblePhase) {
            event.phase = UIEvent.EventPhase.BUBBLE;
            for (int j = path.size() - 2; j >= 0; j--) {
                UIElement elem = path.get(j);
                event.currentElement = elem;
                var bubbles = elem.getBubbleListeners(event.type);
                for (UIEventListener listener : bubbles) {
                    listener.handleEvent(event);
                    if (event.immediatePropagationStopped) break;
                }
                if (event.propagationStopped) {
                    break;  // 停止传播，退出循环
                }
            }
        }
    }

    public static void dispatchDirectEvent(UIEvent event) {
        if (event.target.getCaptureListeners(event.type).isEmpty() && event.target.getBubbleListeners(event.type).isEmpty()) {
            return;
        }
        UIEventDispatcher.dispatchEvent(event);
    }


    public static void dispatchAllChildren(UIEvent event) {
        event.currentElement = event.target;
        event.phase = UIEvent.EventPhase.AT_TARGET;
        drillDown(event);
    }

    // Avoid using DFS?
    private static boolean drillDown(UIEvent event) {
        var currentElement = event.currentElement;

        for (var listener : currentElement.getCaptureListeners(event.type)) {
            listener.handleEvent(event);
            if (event.immediatePropagationStopped) break;
        }

        if (event.propagationStopped) return true;

        for (var child : currentElement.getSortedChildren()) {
            if (!child.isActive() || !child.isDisplayed()) {
                continue;
            }
            event.currentElement = child;
            boolean handled = drillDown(event);
            if (handled) return true;
        }

        event.currentElement = currentElement;
        for (var listener : currentElement.getBubbleListeners(event.type)) {
            listener.handleEvent(event);
            if (event.immediatePropagationStopped) break;
        }

        return event.propagationStopped;
    }

}
