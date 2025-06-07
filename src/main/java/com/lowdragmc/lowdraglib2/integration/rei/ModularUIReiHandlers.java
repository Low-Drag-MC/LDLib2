package com.lowdragmc.lowdraglib2.integration.rei;

import com.lowdragmc.lowdraglib2.gui.ingredient.Target;
import com.lowdragmc.lowdraglib2.gui.modular.ModularUIGuiContainer;
import dev.architectury.event.CompoundEventResult;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZonesProvider;
import me.shedaniel.rei.api.client.registry.screen.FocusedStackProvider;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author KilaBash
 * @date 2022/11/27
 * @implNote ModularUIREIHandlers
 */
public final class ModularUIReiHandlers  {

    public static final ExclusionZonesProvider<ModularUIGuiContainer> EXCLUSION_ZONES_PROVIDER = screen -> screen.getGuiExtraAreas().stream().map(rect2 -> new Rectangle(rect2.getX(), rect2.getY(), rect2.getWidth(), rect2.getHeight())).toList();
    public static final FocusedStackProvider FOCUSED_STACK_PROVIDER = (screen, mouse) -> {
        if (screen instanceof ModularUIGuiContainer containerScreen) {
            var target = containerScreen.modularUI.mainGroup.getXEIIngredientOverMouse(mouse.getX(), mouse.getY());
            if (target instanceof EntryStack<?> entryStack) {
                return CompoundEventResult.interruptTrue(entryStack);
            } else if (target instanceof EntryIngredient entryStacks && entryStacks.size() > 0) {
                return CompoundEventResult.interruptTrue(entryStacks.get(0));
            }
        }
        return CompoundEventResult.pass();
    };

    public static final DraggableStackVisitor<ModularUIGuiContainer> DRAGGABLE_STACK_VISITOR = new DraggableStackVisitor<>() {
        @Override
        public <R extends Screen> boolean isHandingScreen(R screen) {
            return screen instanceof ModularUIGuiContainer;
        }

        @Override
        public DraggedAcceptorResult acceptDraggedStack(DraggingContext<ModularUIGuiContainer> context, DraggableStack stack) {
            List<Target> targets = context.getScreen().modularUI.mainGroup.getPhantomTargets(stack.get().getValue());
            if (targets.isEmpty()) return DraggedAcceptorResult.PASS;
            for (Target target : targets) {
                var area = target.getArea();
                var rect = new Rectangle(
                        area.getX(),
                        area.getY(),
                        area.getWidth(),
                        area.getHeight()
                );
                if (rect.contains(context.getCurrentPosition())) {
                    target.accept(stack.get().getValue());
                    return DraggedAcceptorResult.ACCEPTED;
                }
            }
            return DraggedAcceptorResult.PASS;
        }

        @Override
        public Stream<BoundsProvider> getDraggableAcceptingBounds(DraggingContext<ModularUIGuiContainer> context, DraggableStack stack) {
            List<Target> targets = context.getScreen().modularUI.mainGroup.getPhantomTargets(stack.get().getValue());
            if (targets.isEmpty()) return Stream.empty();
            return targets.stream().map(Target::getArea).map(area -> BoundsProvider.ofRectangle(new Rectangle(
                    area.getX(),
                    area.getY(),
                    area.getWidth(),
                    area.getHeight()
            )));
        }
    };

}
