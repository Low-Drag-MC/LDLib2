package com.lowdragmc.lowdraglib2.gui.ui.utils;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.appliedenergistics.yoga.style.StyleSizeLength;

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
        var width = Optional.ofNullable(element.getStyleBag().computeCandidate(LayoutProperties.WIDTH))
                .orElseGet(StyleSizeLength::ofAuto)
                .asYogaValue().value;
        var height = Optional.ofNullable(element.getStyleBag().computeCandidate(LayoutProperties.HEIGHT))
                .orElseGet(StyleSizeLength::ofAuto)
                .asYogaValue().value;
        this.modularUI.init((int) width, (int) height);
    }

}