package com.lowdragmc.lowdraglib2.integration.emi;

import com.lowdragmc.lowdraglib2.gui.ui.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiExclusionArea;
import dev.emi.emi.api.EmiStackProvider;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.widget.Bounds;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@UtilityClass
public final class ModularUIEMIHandlers {
    public final static EmiExclusionArea<Screen> EXCLUSION_AREA = (Screen screen, Consumer<Bounds> consumer) -> {
        for (var child : screen.children()) {
            if (child instanceof IModularUIHolder holder) {
                for (var area : holder.getModularUI().getGuiExtraAreas()) {
                    consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
                }
            }
        }
    };

    public final static EmiStackProvider<Screen> STACK_PROVIDER = (screen, x, y) -> {
        for (var child : screen.children()) {
            if (child instanceof IModularUIHolder holder) {
                var lastHovered = holder.getModularUI().getLastHoveredElement();
                if (lastHovered == null) continue;
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
                if (child instanceof IModularUIHolder holder) {
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
            List<Bounds> bounds = new ArrayList<>();
            for (var child : screen.children()) {
                if (child instanceof IModularUIHolder holder) {
                    var mui = holder.getModularUI();
                    var event = UIEvent.create(EMIUIEvents.RENDER_DRAG_HANDLER);
                    event.target = mui.ui.rootElement;
                    event.x = mouseX;
                    event.y = mouseY;
                    event.customData = new EMIDragDropHandlers(dragged, bounds);
                    UIEventDispatcher.dispatchAllChildren(event);
                }
            }
            for (Bounds bound : bounds) {
                draw.fill(bound.x(), bound.y(), bound.x() + bound.width(), bound.y() + bound.height(), 0x8822BB33);
            }
        }
    };
}
