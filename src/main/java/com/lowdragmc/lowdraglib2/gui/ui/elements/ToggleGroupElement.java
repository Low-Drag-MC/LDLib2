package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.appliedenergistics.yoga.YogaGutter;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
@LDLRegister(name = "toggle-group", group = "utils", registry = "ldlib2:ui_element")
public class ToggleGroupElement extends UIElement {
    @Configurable(name = "ToggleGroup", subConfigurable = true)
    public final Toggle.ToggleGroup toggleGroup = new Toggle.ToggleGroup();

    public ToggleGroupElement() {
        getLayout().gapAll(2);
        internalSetup();
    }

    @Override
    public UIElement addChildAt(@Nullable UIElement child, int index) {
        super.addChildAt(child, index);
        if (child instanceof Toggle toggle && toggle.getParent() == this) {
            if (toggle.getToggleGroup() != toggleGroup) {
                toggle.setToggleGroup(toggleGroup);
            }
        }
        return this;
    }

    @Override
    public boolean removeChild(@Nullable UIElement child) {
        if (super.removeChild(child)) {
            if (child instanceof Toggle toggle && toggle.getToggleGroup() == toggleGroup) {
                toggle.setToggleGroup(null);
            }
            return true;
        }
        return false;
    }

    /// Editor + Xml
    @Override
    public boolean canAddEditorChild(AutoRegistry.Holder<LDLRegister, UIElement, Supplier<UIElement>> holder) {
        return Toggle.class.isAssignableFrom(holder.clazz());
    }
}
