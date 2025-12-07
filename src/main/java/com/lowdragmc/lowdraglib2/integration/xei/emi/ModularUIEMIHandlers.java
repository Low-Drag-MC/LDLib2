package com.lowdragmc.lowdraglib2.integration.xei.emi;

import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.integration.xei.emi.handler.EMIDragDropHandler;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiExclusionArea;
import dev.emi.emi.api.EmiStackProvider;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.widget.Bounds;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Consumer;

@UtilityClass
public final class ModularUIEMIHandlers {
    public final static EmiExclusionArea<Screen> EXCLUSION_AREA = (Screen screen, Consumer<Bounds> consumer) -> {
        for (var child : screen.children()) {
            if (child instanceof IModularUIHolder holder && holder.getModularUI() != null) {
                for (var area : holder.getModularUI().getGuiExtraAreas()) {
                    consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
                }
            }
        }
    };

    public final static EmiStackProvider<Screen> STACK_PROVIDER = (screen, x, y) -> {
        for (var child : screen.children()) {
            if (child instanceof IModularUIHolder holder && holder.getModularUI() != null) {
                var lastHovered = holder.getModularUI().getLastHoveredElement();
                if (lastHovered == null) return EmiStackInteraction.EMPTY;
                var event = UIEvent.create(EMIUIEvents.STACK_PROVIDER);
                event.target = lastHovered;
                event.x = x;
                event.y = y;
                UIEventDispatcher.dispatchEvent(event);
                if (event.customData instanceof EmiStackInteraction interaction) {
                    return interaction;
                }
            }
        }
        return EmiStackInteraction.EMPTY;
    };

    public final static EmiDragDropHandler<Screen> DRAG_DROP_HANDLER = new EmiDragDropHandler<>() {

        @Override
        public boolean dropStack(Screen screen, EmiIngredient stack, int x, int y) {
            for (var child : screen.children()) {
                if (child instanceof IModularUIHolder holder && holder.getModularUI() != null) {
                    var mui = holder.getModularUI();
                    var event = UIEvent.create(EMIUIEvents.DROP_STACK_HANDLER);
                    event.target = mui.ui.rootElement;
                    event.x = x;
                    event.y = y;
                    event.customData = stack;
                    if (UIEventDispatcher.dispatchAllChildren(event)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void render(Screen screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta) {
            var handler = new EMIDragDropHandler(dragged);
            for (var child : screen.children()) {
                if (child instanceof IModularUIHolder holder && holder.getModularUI() != null) {
                    var mui = holder.getModularUI();
                    var event = UIEvent.create(EMIUIEvents.RENDER_DRAG_HANDLER);
                    event.target = mui.ui.rootElement;
                    event.x = mouseX;
                    event.y = mouseY;
                    event.customData = handler;
                    UIEventDispatcher.dispatchAllChildren(event);
                }
            }
            for (var bound : handler.bounds) {
                draw.fill(bound.x(), bound.y(), bound.x() + bound.width(), bound.y() + bound.height(), 0x8822BB33);
            }
        }
    };
}
