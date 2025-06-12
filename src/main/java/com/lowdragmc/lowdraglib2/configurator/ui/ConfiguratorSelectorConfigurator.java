package com.lowdragmc.lowdraglib2.configurator.ui;

import org.appliedenergistics.yoga.YogaDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConfiguratorSelectorConfigurator<T> extends SelectorConfigurator<T> {
    public final BiConsumer<T, ConfiguratorGroup> configuratorBuilder;
    public final ConfiguratorGroup container = new ConfiguratorGroup();

    public ConfiguratorSelectorConfigurator(String name,
                                            Supplier<T> supplier,
                                            Consumer<T> onUpdate,
                                            @NotNull T defaultValue,
                                            boolean forceUpdate,
                                            List<T> candidates,
                                            Function<T, String> mapping,
                                            BiConsumer<T, ConfiguratorGroup> configuratorBuilder) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate, candidates, mapping);
        this.configuratorBuilder = configuratorBuilder;
        container.setCollapse(false);
        container.lineContainer.setDisplay(YogaDisplay.NONE);
        reloadContainer();
        addChild(container);
    }

    private void reloadContainer() {
        container.removeAllConfigurators();
        configuratorBuilder.accept(value, container);
        if (container.configurators.isEmpty()) {
            container.setDisplay(YogaDisplay.NONE);
        } else {
            container.setDisplay(YogaDisplay.FLEX);
        }
    }

    @Override
    protected void onValueUpdatePassively(T newValue) {
        var lastValue = value;
        super.onValueUpdatePassively(newValue);
        if (lastValue != value) {
            reloadContainer();
        }
    }

    @Override
    protected void updateValueActively(T newValue) {
        var lastValue = value;
        super.updateValueActively(newValue);
        if (lastValue != value) {
            reloadContainer();
        }
    }
}
