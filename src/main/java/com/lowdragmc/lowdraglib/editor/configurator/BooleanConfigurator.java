package com.lowdragmc.lowdraglib.editor.configurator;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/3
 * @implNote BooleanConfigurator
 */
public class BooleanConfigurator extends ValueConfigurator<Boolean>{
    protected SwitchWidget switchWidget;

    public BooleanConfigurator(String name, Supplier<Boolean> supplier, Consumer<Boolean> onUpdate, @Nonnull Boolean defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        if (value == null) value = defaultValue;
    }

    @Override
    protected void onValueUpdate(Boolean newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdate(newValue);
        switchWidget.setPressed(newValue);
    }

    @Override
    public void init(int width) {
        super.init(width);
        addWidget(switchWidget = new SwitchWidget(leftWidth, 2, 10, 10, (cd, pressed) -> {
            value = pressed;
            updateValue();
        }));
        switchWidget.setPressed(value);
        switchWidget.setTexture(new ColorBorderTexture(-1, -1).setRadius(5), new GuiTextureGroup(new ColorBorderTexture(-1, -1).setRadius(5), new ColorRectTexture(-1).setRadius(5).scale(0.5f)));
    }
}
