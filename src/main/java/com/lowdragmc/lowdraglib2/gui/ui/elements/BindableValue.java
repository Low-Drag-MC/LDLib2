package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@KJSBindings
@LDLRegister(name = "bindable-value", group = "utils", registry = "ldlib2:ui_element")
public class BindableValue<T> extends BindableUIElement<T> {
    @Getter
    private T value;

    public BindableValue() {
        this(null);
    }

    public BindableValue(@Nullable T value) {
        this.value = value;
    }

    @Override
    public BindableUIElement<T> setValue(@Nullable T value, boolean notify) {
        if (Objects.equals(value, this.value)) return this;
        this.value = value;
        if (notify) notifyListeners();
        return this;
    }
}
