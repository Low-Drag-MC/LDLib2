package com.lowdragmc.lowdraglib.gui.ui;

import com.lowdragmc.lowdraglib.gui.ui.style.StyleSheet;
import lombok.Data;

@Data(staticConstructor = "of")
public final class UI {
    public final UIElement rootElement;
    public final StyleSheet styleSheet;

    public static UI of(UIElement rootElement) {
        return of(rootElement, new StyleSheet());
    }

    public static UI of() {
        return of(new UIElement(), new StyleSheet());
    }

}
