package com.lowdragmc.lowdraglib.integration.rei;

import com.lowdragmc.lowdraglib.integration.jei.ModularWrapper;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * @author KilaBash
 * @date 2022/11/27
 * @implNote ModularWrapperWidget
 */
@OnlyIn(Dist.CLIENT)
public class ModularWrapperWidget extends Widget {
    final ModularWrapper<?> modular;

    public ModularWrapperWidget(ModularWrapper<?> modular) {
        this.modular = modular;
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        modular.draw(graphics, pMouseX, pMouseY, pPartialTick);
        var tooltip = getTooltip(TooltipContext.ofMouse(Item.TooltipContext.of(Minecraft.getInstance().level)));
        if (tooltip != null) {
            tooltip.queue();
        }
    }

    @Override
    @Nullable
    public Tooltip getTooltip(TooltipContext context) {
        if (modular.tooltipTexts != null && !modular.tooltipTexts.isEmpty()) {
            var tooltip = Tooltip.create(context.getPoint(), modular.tooltipTexts);
            if (modular.tooltipComponent != null) {
                tooltip.add(modular.tooltipComponent);
            }
            return tooltip;
        }
        return null;
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
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        return modular.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        modular.focused = false;
        if (modular.modularUI.mainGroup.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return false;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
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
