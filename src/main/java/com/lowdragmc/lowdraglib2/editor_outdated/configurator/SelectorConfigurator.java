package com.lowdragmc.lowdraglib2.editor_outdated.configurator;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.widget.SelectorWidget;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/3
 * @implNote BooleanConfigurator
 */
public class SelectorConfigurator<T> extends ValueConfigurator<T>{

    protected List<T> candidates;
    protected Function<T, String> mapping;
    protected Map<String, T> nameMap;
    @Getter
    protected SelectorWidget selector;

    @Setter
    protected int max = 5;
    @Setter
    protected boolean isUp = true;

    public SelectorConfigurator(String name, Supplier<T> supplier, Consumer<T> onUpdate, @Nonnull T defaultValue, boolean forceUpdate, List<T> candidates, Function<T, String> mapping) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        this.candidates = candidates;
        this.mapping = mapping;
        this.nameMap = new HashMap<>();
        for (T candidate : candidates) {
            nameMap.put(mapping.apply(candidate), candidate);
        }
    }

    @Override
    protected void onValueUpdate(T newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdate(newValue);
        selector.setValue(mapping.apply(newValue));
    }

    @Override
    public void init(int width) {
        super.init(width);
        addWidget(selector = new SelectorWidget(leftWidth, 2, width - leftWidth - 3 - rightWidth, 10, nameMap.keySet().stream().toList(), -1)
                .setOnChanged(s -> {
                    value = nameMap.get(s);
                    updateValue();
                })
                .setMaxCount(max)
                .setIsUp(isUp)
                .setButtonBackground(ColorPattern.T_GRAY.rectTexture().setRadius(5))
                .setBackground(new GuiTextureGroup(ColorPattern.BLACK.rectTexture(), ColorPattern.GRAY.borderTexture(1)))
                .setValue(mapping.apply(value))
        );
    }

}
