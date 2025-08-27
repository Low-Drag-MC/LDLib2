package com.lowdragmc.lowdraglib2.gui.ui.event;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import lombok.experimental.UtilityClass;

import javax.annotation.Nullable;
import java.util.Stack;

@UtilityClass
public final class UIEventDispatcher {
    public static void dispatchEvent(UIEvent event) {
        dispatchEvent(event, true, true, true);
    }

    public static void dispatchEvent(UIEvent event, boolean capturePhase, boolean bubblePhase, boolean sendServer) {
        // 1. build path from root to target
        var target = event.target;
        var path = target.getStructurePath();

        // 2. capture phase: root -> target.parent
        if (capturePhase && event.hasCapturePhase) {
            event.phase = UIEvent.EventPhase.CAPTURE;
            for (int i = 0; i < path.size() - 1; i++) {
                UIElement elem = path.get(i);
                event.currentElement = elem;
                // call capture listeners
                var captures = elem.getCaptureListeners(event.type);
                for (UIEventListener listener : captures) {
                    listener.handleEvent(event);
                    event.hasHandler = true;
                    if (event.immediatePropagationStopped) {
                        break;  // skip to leftover bubble phase
                    }
                }
                if (sendServer) {
                    var serverEvent = elem.getCaptureServerEvent(event.type);
                    if (serverEvent != null) {
                        elem.sendEvent(serverEvent, event);
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
            event.hasHandler = true;
            if (event.immediatePropagationStopped) break;
        }
        var targetBubbles = target.getBubbleListeners(event.type);
        for (UIEventListener listener : targetBubbles) {
            listener.handleEvent(event);
            event.hasHandler = true;
            if (event.immediatePropagationStopped) break;
        }
        if (sendServer) {
            var serverEvent = target.getCaptureServerEvent(event.type);
            if (serverEvent != null) {
                target.sendEvent(serverEvent, event);
            }
            serverEvent = target.getBaubleServerEvent(event.type);
            if (serverEvent != null) {
                target.sendEvent(serverEvent, event);
            }
        }
        if (event.propagationStopped) {
            return;  // stop propagation, exit loop
        }

        // 4. Bubbling phase: from target's parent back to root
        if (bubblePhase && event.hasBubblePhase) {
            event.phase = UIEvent.EventPhase.BUBBLE;
            for (int j = path.size() - 2; j >= 0; j--) {
                UIElement elem = path.get(j);
                event.currentElement = elem;
                var bubbles = elem.getBubbleListeners(event.type);
                for (UIEventListener listener : bubbles) {
                    listener.handleEvent(event);
                    event.hasHandler = true;
                    if (event.immediatePropagationStopped) break;
                }
                if (sendServer) {
                    var serverEvent = elem.getBaubleServerEvent(event.type);
                    if (serverEvent != null) {
                        elem.sendEvent(serverEvent, event);
                    }
                }
                if (event.propagationStopped) {
                    break;  // stop propagation, exit loop
                }
            }
        }
    }

    public static void dispatchDirectEvent(UIEvent event) {
        dispatchDirectEvent(event, true);
    }

    public static void dispatchDirectEvent(UIEvent event, boolean sendServer) {
        if (event.target.getCaptureListeners(event.type).isEmpty() && event.target.getBubbleListeners(event.type).isEmpty()) {
            return;
        }
        UIEventDispatcher.dispatchEvent(event, false, false, sendServer);
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
            event.hasHandler = true;
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
            event.hasHandler = true;
            if (event.immediatePropagationStopped) break;
        }

        return event.propagationStopped;
    }

}
