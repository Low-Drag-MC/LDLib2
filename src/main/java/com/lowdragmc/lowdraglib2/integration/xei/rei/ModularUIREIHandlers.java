package com.lowdragmc.lowdraglib2.integration.xei.rei;

import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.integration.xei.rei.handler.REIDraggableStackBoundsHandler;
import dev.architectury.event.CompoundEventResult;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZonesProvider;
import me.shedaniel.rei.api.client.registry.screen.FocusedStackProvider;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public final class ModularUIREIHandlers {
    public static final ExclusionZonesProvider<Screen> EXCLUSION_ZONES_PROVIDER = screen -> {
        var areas = new ArrayList<Rectangle>();
        for (var child : screen.children()) {
            if (child instanceof IModularUIHolder modularUIHolder && modularUIHolder.getModularUI() != null) {
                for (var area : modularUIHolder.getModularUI().getGuiExtraAreas()) {
                    areas.add(new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
                }
            }
        }
        return areas;
    };

    @SuppressWarnings({"unchecked"})
    public static final FocusedStackProvider FOCUSED_STACK_PROVIDER = (screen, mouse) -> {
        for (var child : screen.children()) {
            if (child instanceof IModularUIHolder holder && holder.getModularUI() != null) {
                var lastHovered = holder.getModularUI().getLastHoveredElement();
                if (lastHovered == null) continue;
                var event = UIEvent.create(REIUIEvents.FOCUSED_STACK);
                event.target = lastHovered;
                event.x = mouse.getX();
                event.y = mouse.getY();
                UIEventDispatcher.dispatchEvent(event);
                if (event.customData instanceof CompoundEventResult<?> compoundEventResult && compoundEventResult.object() instanceof EntryStack<?>) {
                    return (CompoundEventResult<EntryStack<?>>) compoundEventResult;
                }
            }
        }
        return CompoundEventResult.pass();
    };

    public static final DraggableStackVisitor<Screen> DRAGGABLE_STACK_VISITOR = new DraggableStackVisitor<>() {
        @Override
        public boolean isHandingScreen(Screen screen) {
            return screen.children().stream().anyMatch(IModularUIHolder.class::isInstance);
        }

        @Override
        public DraggedAcceptorResult acceptDraggedStack(DraggingContext<Screen> context, DraggableStack stack) {
            for (var child : context.getScreen().children()) {
                if (child instanceof IModularUIHolder holder && holder.getModularUI() != null) {
                    var mui = holder.getModularUI();
                    var event = UIEvent.create(REIUIEvents.ACCEPT_DRAGGABLE_STACK);
                    event.target = mui.ui.rootElement;
                    event.customData = new REIDraggableStackBoundsHandler(context, stack, Collections.emptyList());;
                    if (UIEventDispatcher.dispatchAllChildren(event)) {
                        return DraggedAcceptorResult.ACCEPTED;
                    }
                }
            }
            return DraggableStackVisitor.super.acceptDraggedStack(context, stack);
        }

        @Override
        public Stream<BoundsProvider> getDraggableAcceptingBounds(DraggingContext<Screen> context, DraggableStack stack) {
            List<BoundsProvider> boundsProviders = new ArrayList<>();
            for (var child : context.getScreen().children()) {
                if (child instanceof IModularUIHolder holder && holder.getModularUI() != null) {
                    var mui = holder.getModularUI();
                    var event = UIEvent.create(REIUIEvents.DRAGGABLE_STACK_BOUNDS);
                    event.target = mui.ui.rootElement;
                    event.customData = new REIDraggableStackBoundsHandler(context, stack, boundsProviders);
                    UIEventDispatcher.dispatchAllChildren(event);
                }
            }
            return boundsProviders.stream();
        }
    };
}
