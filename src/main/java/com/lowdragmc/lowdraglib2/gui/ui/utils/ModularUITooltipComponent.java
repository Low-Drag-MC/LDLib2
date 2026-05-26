package com.lowdragmc.lowdraglib2.gui.ui.utils;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import dev.vfyjxf.taffy.style.TaffyDimension;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
public class ModularUITooltipComponent implements TooltipComponent {
    public final ModularUI modularUI;

    public ModularUITooltipComponent(ModularUI modularUI) {
        this.modularUI = modularUI;
    }

    public ModularUITooltipComponent(UIElement element) {
        this(new ModularUI(UI.of(element)));
        var widthDim = Optional.ofNullable(element.getStyleBag().computeCandidate(LayoutProperties.WIDTH))
                .orElseGet(TaffyDimension::auto);
        var heightDim = Optional.ofNullable(element.getStyleBag().computeCandidate(LayoutProperties.HEIGHT))
                .orElseGet(TaffyDimension::auto);
        this.modularUI.init((int) widthDim.getValue(), (int) heightDim.getValue());
        // If the root is auto-sized, screenWidth/screenHeight passed above is 0,
        // which makes ModularUI.init compute leftPos = (0 - width) / 2 (i.e. -width/2)
        // due to the relative-root centering logic. Re-init with the now-known
        // content size so leftPos/topPos collapse back to 0.
        if (widthDim.isAuto() || heightDim.isAuto()) {
            this.modularUI.init((int) this.modularUI.getWidth(), (int) this.modularUI.getHeight());
        }
    }

}