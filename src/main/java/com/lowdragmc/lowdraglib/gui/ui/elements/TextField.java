package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class TextField extends UIElement {
    @Getter @Setter
    private Predicate<String> textValidator = Predicates.alwaysTrue();
    @Getter @Setter
    private String text = "";
    @Getter @Setter
    private String placeholder = "input here...";

    public TextField() {
        getLayout().setHeight(20);
        setFocusable(true);

    }
}
