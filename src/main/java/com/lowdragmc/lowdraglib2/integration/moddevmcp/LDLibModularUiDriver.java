//package com.lowdragmc.lowdraglib2.integration.moddevmcp;
//
//import com.lowdragmc.lowdraglib2.LDLib2;
//import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
//import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
//import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
//import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
//import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
//import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
//import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
//import dev.vfyjxf.mcp.api.model.OperationResult;
//import dev.vfyjxf.mcp.api.runtime.*;
//import dev.vfyjxf.mcp.api.ui.*;
//import net.minecraft.client.gui.screens.Screen;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//public final class LDLibModularUiDriver implements UiDriver {
//    private static final String DRIVER_ID = "ldlib2:modular_ui";
//    public static final DriverDescriptor DRIVER_DESCRIPTOR = new DriverDescriptor(
//            DRIVER_ID,
//            LDLib2.MOD_ID,
//            1000,
//            Set.of("snapshot", "query", "inspect", "inspectAt", "action", "tooltip", "checkActionability")
//    );
//
//
//    @Nullable
//    public ModularUI resolveModularUI(UiContext context) {
//        if (context.screenHandle() instanceof Screen screen) {
//            if (screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> containerScreen &&
//                    containerScreen.getMenu() instanceof IModularUIHolder holder && holder.hasModularUI()) {
//                return holder.getModularUI();
//            }
//            if (screen instanceof IModularUIHolder holder && holder.hasModularUI()) {
//                return holder.getModularUI();
//            }
//            for (var child : screen.children()) {
//                if (child instanceof IModularUIHolder holder && holder.hasModularUI()) {
//                    return holder.getModularUI();
//                }
//            }
//        }
//        return null;
//    }
//
//    @Override
//    public DriverDescriptor descriptor() {
//        return DRIVER_DESCRIPTOR;
//    }
//
//    @Override
//    public boolean matches(UiContext context) {
//        return resolveModularUI(context) != null;
//    }
//
//    @Override
//    public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
//        var modularUI = resolveModularUI(context);
//        if (modularUI == null) {
//            return new UiSnapshot(null, context.screenClass(), DRIVER_ID, List.of(), List.of(), null, null, null, null, Map.of());
//        }
//
//        var targets = new ArrayList<UiTarget>();
//        String focusedId = null;
//        String hoveredId = null;
//
//        var rootElement = modularUI.ui.rootElement;
//        collectTargets(rootElement, context, targets);
//
//        // find focused and hovered element ids
//        var focusedElement = modularUI.getFocusedElement();
//        if (focusedElement != null) {
//            focusedId = getTargetId(focusedElement);
//        }
//
//        // find hovered element
//        for (var target : targets) {
//            if (target.state().hovered()) {
//                hoveredId = target.targetId();
//                break;
//            }
//        }
//
//        return new UiSnapshot(
//                context.screenClass(),
//                context.screenClass(),
//                DRIVER_ID,
//                targets,
//                List.of(),
//                focusedId,
//                null,
//                hoveredId,
//                null,
//                Map.of()
//        );
//    }
//
//    @Override
//    public List<UiTarget> query(UiContext context, TargetSelector selector) {
//        var snap = snapshot(context, SnapshotOptions.DEFAULT);
//        return snap.targets().stream()
//                .filter(target -> matchesSelector(target, selector))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public OperationResult<List<UiTarget>> inspectAt(UiContext context, int x, int y) {
//        var modularUI = resolveModularUI(context);
//        if (modularUI == null) {
//            return OperationResult.success(List.of());
//        }
//
//        var hitResult = modularUI.ui.rootElement.hitTest(x, y);
//        if (hitResult == null) {
//            return OperationResult.success(List.of());
//        }
//
//        var element = hitResult.getA();
//        // collect the hit element and its ancestors
//        var targets = new ArrayList<UiTarget>();
//        var current = element;
//        while (current != null) {
//            targets.add(buildTarget(current, context));
//            current = current.hasParent() ? current.getParent() : null;
//        }
//        Collections.reverse(targets);
//        return OperationResult.success(targets);
//    }
//
//    @Override
//    public UiInteractionState interactionState(UiContext context) {
//        var modularUI = resolveModularUI(context);
//        if (modularUI == null) {
//            return new UiInteractionState(null, null, null, null, 0, 0, false, null, DRIVER_ID);
//        }
//
//        UiTarget focusedTarget = null;
//        UiTarget hoveredTarget = null;
//
//        var focusedElement = modularUI.getFocusedElement();
//        if (focusedElement != null) {
//            focusedTarget = buildTarget(focusedElement, context);
//        }
//
//        // check for hovered element via hit test
//        int mouseX = context.mouseX();
//        int mouseY = context.mouseY();
//        var hoveredElement = modularUI.getLastHoveredElement();
//        if (hoveredElement != null) {
//            hoveredTarget = buildTarget(hoveredElement, context);
//        }
//
//        boolean textInputActive = focusedElement instanceof TextElement ||
//                (focusedElement != null && focusedElement.name().contains("text-field"));
//
//        return new UiInteractionState(
//                focusedTarget,
//                null,
//                hoveredTarget,
//                null,
//                mouseX,
//                mouseY,
//                textInputActive,
//                null,
//                DRIVER_ID
//        );
//    }
//
//    @Override
//    public UiInspectResult inspect(UiContext context, SnapshotOptions options) {
//        var snap = snapshot(context, options);
//        var interaction = interactionState(context);
//
//        var summary = new LinkedHashMap<String, Object>();
//        summary.put("driver", DRIVER_ID);
//        summary.put("screen", context.screenClass());
//        summary.put("targetCount", snap.targets().size());
//
//        var interactionMap = new LinkedHashMap<String, Object>();
//        interactionMap.put("mouseX", interaction.cursorX());
//        interactionMap.put("mouseY", interaction.cursorY());
//        interactionMap.put("textInputActive", interaction.textInputActive());
//        if (interaction.focusedTarget() != null) {
//            interactionMap.put("focusedTargetId", interaction.focusedTarget().targetId());
//        }
//        if (interaction.hoveredTarget() != null) {
//            interactionMap.put("hoveredTargetId", interaction.hoveredTarget().targetId());
//        }
//
//        return new UiInspectResult(
//                context.screenClass(),
//                context.screenClass(),
//                DRIVER_ID,
//                summary,
//                snap.targets(),
//                interactionMap,
//                null
//        );
//    }
//
//    @Override
//    public OperationResult<Map<String, Object>> action(UiContext context, UiActionRequest request) {
//        var modularUI = resolveModularUI(context);
//        if (modularUI == null) {
//            return OperationResult.rejected("No ModularUI found");
//        }
//
//        var targets = query(context, request.target());
//        if (targets.isEmpty()) {
//            return OperationResult.rejected("No target matched selector");
//        }
//
//        var uiTarget = targets.getFirst();
//        var element = findElementByTargetId(modularUI.ui.rootElement, uiTarget.targetId());
//        if (element == null) {
//            return OperationResult.rejected("Element not found: " + uiTarget.targetId());
//        }
//
//        var actionName = request.action();
//        switch (actionName) {
//            case "click" -> {
//                var centerX = element.getPositionX() + element.getSizeWidth() / 2;
//                var centerY = element.getPositionY() + element.getSizeHeight() / 2;
//                // mouse down
//                var mouseDown = UIEvent.create(UIEvents.MOUSE_DOWN);
//                mouseDown.x = centerX;
//                mouseDown.y = centerY;
//                mouseDown.button = 0;
//                mouseDown.target = element;
//                UIEventDispatcher.dispatchEvent(mouseDown);
//                // mouse up
//                var mouseUp = UIEvent.create(UIEvents.MOUSE_UP);
//                mouseUp.x = centerX;
//                mouseUp.y = centerY;
//                mouseUp.button = 0;
//                mouseUp.target = element;
//                UIEventDispatcher.dispatchEvent(mouseUp);
//                // click
//                var click = UIEvent.create(UIEvents.CLICK);
//                click.x = centerX;
//                click.y = centerY;
//                click.button = 0;
//                click.target = element;
//                UIEventDispatcher.dispatchEvent(click);
//            }
//            case "focus" -> modularUI.requestFocus(element);
//            default -> {
//                return OperationResult.rejected("Unsupported action: " + actionName);
//            }
//        }
//
//        return OperationResult.success(Map.of("action", actionName, "targetId", uiTarget.targetId()));
//    }
//
//    @Override
//    public OperationResult<TooltipSnapshot> tooltip(UiContext context, TargetSelector selector) {
//        var modularUI = resolveModularUI(context);
//        if (modularUI == null) {
//            return OperationResult.rejected("No ModularUI found");
//        }
//
//        var targets = query(context, selector);
//        if (targets.isEmpty()) {
//            return OperationResult.rejected("No target matched selector");
//        }
//
//        var uiTarget = targets.getFirst();
//        var element = findElementByTargetId(modularUI.ui.rootElement, uiTarget.targetId());
//        if (element == null) {
//            return OperationResult.rejected("Element not found");
//        }
//
//        var tooltips = element.getStyle().tooltips();
//        if (tooltips == null || tooltips.isEmpty()) {
//            return OperationResult.success(new TooltipSnapshot(uiTarget.targetId(), List.of(), uiTarget.bounds(), Map.of()));
//        }
//
//        var lines = Arrays.stream(tooltips.tooltips())
//                .map(c -> c.getString())
//                .collect(Collectors.toList());
//
//        return OperationResult.success(new TooltipSnapshot(uiTarget.targetId(), lines, uiTarget.bounds(), Map.of()));
//    }
//
//    @Override
//    public UiActionabilityResult checkActionability(UiContext context, UiTarget target, String action) {
//        var modularUI = resolveModularUI(context);
//        if (modularUI == null) {
//            return new UiActionabilityResult(false, false, false, false, "no_modular_ui", Map.of());
//        }
//
//        var element = findElementByTargetId(modularUI.ui.rootElement, target.targetId());
//        if (element == null) {
//            return new UiActionabilityResult(false, false, false, false, "element_not_found", Map.of());
//        }
//
//        boolean visible = element.isVisible();
//        boolean enabled = element.isActive();
//        boolean supported = switch (action) {
//            case "click" -> true;
//            case "focus" -> element.isFocusable();
//            default -> false;
//        };
//        boolean actionable = visible && enabled && supported;
//
//        return new UiActionabilityResult(actionable, visible, enabled, supported, actionable ? null : "not_actionable", Map.of());
//    }
//
//
//    // === Private helpers ===
//
//    private void collectTargets(UIElement element, UiContext context, List<UiTarget> targets) {
//        targets.add(buildTarget(element, context));
//        for (var child : element.getSafeChildren()) {
//            collectTargets(child, context, targets);
//        }
//    }
//
//    private UiTarget buildTarget(UIElement element, UiContext context) {
//        var targetId = getTargetId(element);
//        var role = element.name();
//        var text = extractText(element);
//        var bounds = new Bounds(
//                (int) element.getPositionX(),
//                (int) element.getPositionY(),
//                (int) element.getSizeWidth(),
//                (int) element.getSizeHeight()
//        );
//        var state = new UiTargetState(
//                element.isVisible(),
//                element.isActive(),
//                element.isFocused(),
//                element.isHover(),
//                false,
//                false
//        );
//
//        var actions = new ArrayList<String>();
//        if (element.isActive()) {
//            actions.add("click");
//        }
//
//        var extensions = new LinkedHashMap<String, Object>();
//        if (!element.getId().isEmpty()) {
//            extensions.put("elementId", element.getId());
//        }
//        if (!element.getClasses().isEmpty()) {
//            extensions.put("classes", new ArrayList<>(element.getClasses()));
//        }
//
//        return new UiTarget(
//                targetId,
//                DRIVER_ID,
//                context.screenClass(),
//                LDLib2.MOD_ID,
//                role,
//                text,
//                bounds,
//                state,
//                actions,
//                extensions
//        );
//    }
//
//    @Nullable
//    private static UIElement findElementByTargetId(UIElement root, String targetId) {
//        if (getTargetId(root).equals(targetId)) {
//            return root;
//        }
//        for (var child : root.getSafeChildren()) {
//            var found = findElementByTargetId(child, targetId);
//            if (found != null) {
//                return found;
//            }
//        }
//        return null;
//    }
//
//    private static String getTargetId(UIElement element) {
//        if (!element.getId().isEmpty()) {
//            return element.getId();
//        }
//        // use class name + hashCode as a stable-ish id
//        return element.name() + "@" + Integer.toHexString(System.identityHashCode(element));
//    }
//
//    @Nullable
//    private static String extractText(UIElement element) {
//        if (element instanceof TextElement textElement) {
//            var str = textElement.getText().getString();
//            if (!str.isEmpty()) {
//                return str;
//            }
//        }
//        return null;
//    }
//
//    private static boolean matchesSelector(UiTarget target, TargetSelector selector) {
//        if (selector.id() != null && !selector.id().equals(target.targetId())) {
//            return false;
//        }
//        if (selector.role() != null && !selector.role().equals(target.role())) {
//            return false;
//        }
//        if (selector.text() != null && !selector.text().equals(target.text())) {
//            return false;
//        }
//        if (selector.modId() != null && !selector.modId().equals(target.modId())) {
//            return false;
//        }
//        if (selector.screen() != null && !selector.screen().equals(target.screenClass())) {
//            return false;
//        }
//        if (selector.exclude() != null) {
//            for (var exclude : selector.exclude()) {
//                if (matchesSelector(target, exclude)) {
//                    return false;
//                }
//            }
//        }
//        return true;
//    }
//}
