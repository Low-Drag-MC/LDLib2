package com.lowdragmc.lowdraglib.configurator.ui;

import com.lowdragmc.lowdraglib.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib.gui.ui.elements.Selector;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/3
 * @implNote BooleanConfigurator
 */
public class SelectorConfigurator<T> extends ValueConfigurator<T> {
    public final Selector<T> selector;
    public final List<T> candidates;

    public SelectorConfigurator(String name, Supplier<T> supplier, Consumer<T> onUpdate, @Nonnull T defaultValue, boolean forceUpdate, List<T> candidates, Function<T, String> mapping) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        this.candidates = candidates;
        if (value == null) value = defaultValue;
        inlineContainer.addChild(selector = new Selector<>());
        selector.setCandidates(candidates);
        selector.setCandidateUIProvider(candidate -> new Label()
                .textStyle(style -> style
                        .textAlignHorizontal(Horizontal.LEFT)
                        .textAlignVertical(Vertical.CENTER))
                .setText(candidate == null ? "---" : mapping.apply(candidate)));
        selector.setSelected(value, false);
        selector.setOnValueChanged(this::updateValueActively);
    }

    @Override
    protected void onValueUpdatePassively(T newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        selector.setSelected(newValue, false);
    }

}
