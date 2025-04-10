package com.lowdragmc.lowdraglib.emi;

import com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author KilaBash
 * @date 2023/4/3
 * @implNote ModularWrapperWidget
 */
@Environment(EnvType.CLIENT)
public class ModularWrapperWidget extends Widget implements ContainerEventHandler {
    @Getter @Setter
    @Nullable
    private GuiEventListener focused;
    @Getter @Setter
    private boolean isDragging;
    public final ModularWrapper<?> modular;
    public final List<Widget> slots;

    public ModularWrapperWidget(ModularWrapper<?> modular, List<Widget> slots) {
        this.modular = modular;
        this.slots = slots;
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(0, 0, modular.getWidget().getSize().width, modular.getWidget().getSize().height);
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        modular.draw(graphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        if (modular.tooltipTexts != null && !modular.tooltipTexts.isEmpty() &&
                !(modular.modularUI.mainGroup.getHoverElement(mouseX + modular.getLeft(), mouseY + modular.getTop()) instanceof IRecipeIngredientSlot)) {
            List<ClientTooltipComponent> tooltips = modular.tooltipTexts.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).collect(Collectors.toList());
            if (modular.tooltipComponent != null) {
                tooltips.add(DrawerHelper.getClientTooltipComponent(modular.tooltipComponent));
            }
            return tooltips;
        }
        return super.getTooltip(mouseX, mouseY);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return Collections.emptyList();
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        return modular.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (modular.mouseClicked(mouseX + modular.getLeft(), mouseY + modular.getTop(), button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        return modular.mouseReleased(pMouseX, pMouseY, pButton);
    }

    @Override
    public void mouseMoved(double pMouseX, double pMouseY) {
        modular.mouseMoved(pMouseX, pMouseY);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        return modular.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        return modular.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        return modular.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        return modular.keyReleased(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        return modular.charTyped(pCodePoint, pModifiers);
    }
}
