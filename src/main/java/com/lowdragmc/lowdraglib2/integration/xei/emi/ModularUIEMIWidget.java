package com.lowdragmc.lowdraglib2.integration.xei.emi;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class ModularUIEMIWidget extends Widget {
    public final ModularUI modularUI;

    public ModularUIEMIWidget(ModularUI modularUI) {
        this.modularUI = modularUI;
    }

    @Override
    public Bounds getBounds() {
        var rectangle = modularUI.getWidget().getRectangle();
        return new Bounds(rectangle.left(), rectangle.top(), rectangle.width(), rectangle.height());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.flush();
        var realMouse = guiGraphics.pose().last().pose().invert(new Matrix4f()).transformPosition(new Vector3f(0, 0, 0));
        modularUI.getWidget().render(guiGraphics, (int) (mouseX - realMouse.x), (int) (mouseY - realMouse.y), partialTick);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        var tooltips = new ArrayList<ClientTooltipComponent>();
        if (!modularUI.getDragHandler().isDragging() && modularUI.getTooltipTexts() != null && !modularUI.getTooltipTexts().isEmpty()) {
            modularUI.getTooltipTexts().stream()
                    .map(Component::getVisualOrderText)
                    .map(ClientTooltipComponent::create)
                    .forEach(tooltips::add);
            if (modularUI.getTooltipComponent() != null) {
                tooltips.add(ClientTooltipComponent.create(modularUI.getTooltipComponent()));
            }
        }
        return tooltips;
    }
}
