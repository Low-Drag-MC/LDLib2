package com.lowdragmc.lowdraglib2.gui.ui.event;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.experimental.UtilityClass;

@UtilityClass
@KJSBindings
public final class UIEventDispatcher {
    /**
     * Dispatches the given {@link UIEvent} to its target element and through the event phases,
     * including the capture phase, target phase, and bubble phase with default parameters.
     *
     * This method is a simplified wrapper around the more general
     * {@link #dispatchEvent(UIEvent, boolean, boolean, boolean)} method, using default values.
     *
     * @param event the {@link UIEvent} instance to be dispatched. The event's
     *              {@code target}, {@code phase}, and associated lifecycle fields
     *              (e.g., propagation flags) will be processed during dispatching.
     */
    public static void dispatchEvent(UIEvent event) {
        dispatchEvent(event, true, true, true);
    }

    /**
     * Dispatches the given {@link UIEvent} to its target element and processes the
     * event through the capture phase, target phase, and bubble phase, depending
     * on the specified parameters.
     *
     * This method triggers appropriate event listeners during each phase, updates
     * the event's lifecycle fields (such as propagation flags and phase), and optionally
     * communicates the event to the server.
     *
     * @param event the {@link UIEvent} instance to be dispatched. The event's
     *              {@code target}, {@code phase}, and propagation flags
     *              will be modified as it flows through the lifecycle phases.
     * @param capturePhase a {@code boolean} indicating whether the event should
     *                     propagate through the capture phase, traveling from
     *                     the root to the target.
     * @param bubblePhase a {@code boolean} specifying whether the event should
     *                    propagate through the bubble phase, traveling from the
     *                    target back to the root.
     * @param sendServer a {@code boolean} specifying whether server-side event handling
     *                   should be invoked for the event during its propagation through
     *                   the different phases.
     */
    public static void dispatchEvent(UIEvent event, boolean capturePhase, boolean bubblePhase, boolean sendServer) {
        // 1. build path from root to target
        var target = event.target;
        var path = target.getStructurePath();
        // Input never reaches an inactive subtree: from the first inactive element on the path down,
        // nothing hears a press, a release, a drop or a key. Before this only the inactive element
        // itself refused (each widget checking its own flag), so a read-only inspector — inactive as a
        // whole — still let every field inside it be typed into, toggled and dropped onto.
        int inactiveFrom = isInput(event.type) ? firstInactive(path) : path.size();

        // 2. capture phase: root -> target.parent
        if (capturePhase && event.hasCapturePhase) {
            event.phase = UIEvent.EventPhase.CAPTURE;
            for (int i = 0; i < Math.min(path.size() - 1, inactiveFrom); i++) {
                UIElement elem = path.get(i);
                event.currentElement = elem;
                // call capture listeners
                var captures = elem.getCaptureListeners(event.type);
                for (UIEventListener listener : captures) {
                    handleCaptureEventListener(event, elem, listener);
                    if (event.laterPropagationStopped) {
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
        // the target in an inactive subtree hears nothing; the active part of the path above it still
        // bubbles, as it would past a target that ignored the event
        if (inactiveFrom == path.size()) {
            // For target element, execute both capture and bubble listeners
            var targetCaptures = target.getCaptureListeners(event.type);
            for (UIEventListener listener : targetCaptures) {
                handleCaptureEventListener(event, target, listener);
                if (event.laterPropagationStopped) break;
            }
            var targetBubbles = target.getBubbleListeners(event.type);
            for (UIEventListener listener : targetBubbles) {
                handleBubbleEventListener(event, target, listener);
                if (event.laterPropagationStopped) break;
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
        }

        // 4. Bubbling phase: from target's parent back to root
        if (bubblePhase && event.hasBubblePhase) {
            event.phase = UIEvent.EventPhase.BUBBLE;
            for (int j = Math.min(path.size() - 2, inactiveFrom - 1); j >= 0; j--) {
                UIElement elem = path.get(j);
                event.currentElement = elem;
                var bubbles = elem.getBubbleListeners(event.type);
                for (UIEventListener listener : bubbles) {
                    handleBubbleEventListener(event, elem, listener);
                    if (event.laterPropagationStopped) break;
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

    /**
     * The events that are somebody <b>using</b> an element — pressing, releasing, dropping, typing — which
     * an inactive subtree does not hear. Moving over it, hovering for a tooltip and scrolling still reach
     * it: a disabled panel is still read, and reading a long one means scrolling it.
     */
    private static boolean isInput(String type) {
        return switch (type) {
            case UIEvents.MOUSE_DOWN, UIEvents.MOUSE_UP, UIEvents.CLICK, UIEvents.DOUBLE_CLICK,
                 UIEvents.DRAG_PERFORM, UIEvents.FILE_DROP,
                 UIEvents.KEY_DOWN, UIEvents.KEY_UP, UIEvents.CHAR_TYPED -> true;
            default -> false;
        };
    }

    /** The index of the first inactive element on a root-to-target path, or the path's length. */
    private static int firstInactive(java.util.List<UIElement> path) {
        for (int i = 0; i < path.size(); i++) {
            if (!path.get(i).isActive()) {
                return i;
            }
        }
        return path.size();
    }

    public static void dispatchDirectEvent(UIEvent event) {
        dispatchDirectEvent(event, true);
    }

    /**
     * Dispatches the given {@link UIEvent} directly to its target element, without
     * propagating through other phases like capture or bubble, unless specific listeners
     * are registered for the event type. If no listeners are present, the method returns
     * without any action. Optionally, the event can also be sent to the server.
     *
     * @param event the {@link UIEvent} to be dispatched. It must have a designated target
     *              and event type. The event's propagation behavior depends on the presence
     *              of appropriate listeners.
     * @param sendServer a {@code boolean} flag indicating whether the event should be sent
     *                   to the server for additional processing after dispatching.
     */
    public static void dispatchDirectEvent(UIEvent event, boolean sendServer) {
        if (event.target.getCaptureListeners(event.type).isEmpty() && event.target.getBubbleListeners(event.type).isEmpty()) {
            return;
        }
        UIEventDispatcher.dispatchEvent(event, false, false, sendServer);
    }

    public static boolean dispatchAllChildren(UIEvent event) {
        return dispatchAllChildren(event, true, true);
    }

    /**
     * Dispatches the provided {@link UIEvent} to all child elements of its target, ensuring
     * the event status and phase are updated appropriately during the dispatch operation.
     *
     * @param event the {@link UIEvent} to be dispatched to child elements. The event's
     *              {@code currentElement} is updated to match its {@code target}, and
     *              its {@code phase} is set to {@link UIEvent.EventPhase#AT_TARGET}.
     * @return {@code true} if the event dispatch to children is successful as determined
     *         by the {@code drillDown} operation, otherwise {@code false}.
     */
    public static boolean dispatchAllChildren(UIEvent event, boolean onlyActive, boolean onlyDisplay) {
        event.currentElement = event.target;
        event.phase = UIEvent.EventPhase.AT_TARGET;
        return drillDown(event, onlyActive, onlyDisplay);
    }

    // Avoid using DFS?
    private static boolean drillDown(UIEvent event, boolean onlyActive, boolean onlyDisplay) {
        var currentElement = event.currentElement;

        for (var listener : currentElement.getCaptureListeners(event.type)) {
            handleCaptureEventListener(event, currentElement, listener);
            if (event.laterPropagationStopped) break;
        }

        if (event.propagationStopped) return true;

        for (var child : currentElement.getSafeSortedChildren()) {
            if ((onlyActive && !child.isActive()) || (onlyDisplay && !child.isDisplayed())) {
                continue;
            }
            event.currentElement = child;
            boolean handled = drillDown(event, onlyActive, onlyDisplay);
            if (handled) return true;
        }

        event.currentElement = currentElement;
        for (var listener : currentElement.getBubbleListeners(event.type)) {
            handleBubbleEventListener(event, currentElement, listener);
            if (event.laterPropagationStopped) break;
        }

        return event.propagationStopped;
    }

    private static void handleBubbleEventListener(UIEvent event, UIElement elem, UIEventListener listener) {
        event.hasHandler = true;
        event.currentListener = listener;
        event.bubbleListeners.add(Pair.of(elem, listener));
        listener.handleEvent(event);
    }

    private static void handleCaptureEventListener(UIEvent event, UIElement elem, UIEventListener listener) {
        event.hasHandler = true;
        event.currentListener = listener;
        event.captureListeners.add(Pair.of(elem, listener));
        listener.handleEvent(event);
    }
}
