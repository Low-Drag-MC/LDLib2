package com.lowdragmc.lowdraglib.configurator.ui;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/2
 * @implNote ArrayConfigurator
 */
public class ArrayConfiguratorGroup<T> extends ConfiguratorGroup {
    protected final Supplier<List<T>> source;
    protected final BiFunction<Supplier<T>, Consumer<T>, Configurator> configuratorProvider;
    @Setter
    protected Supplier<T> addDefault;
    @Setter
    protected Consumer<List<T>> onUpdate;
    @Setter
    protected Consumer<T> onAdd, onRemove;
    @Setter
    protected BiConsumer<Integer, T> onReorder;
    @Setter
    protected boolean canAdd = true, canRemove = true, forceUpdate;
    @Getter
    @Nullable
    protected ItemConfigurator selected;

    public ArrayConfiguratorGroup(String name, boolean isCollapse, Supplier<List<T>> source,
                                  BiFunction<Supplier<T>, Consumer<T>, Configurator> configuratorProvider,
                                  boolean forceUpdate) {
        super(name, isCollapse);
        this.configuratorProvider = configuratorProvider;
        this.source = source;
        this.forceUpdate = forceUpdate;
        for (T object : source.get()) {
            addConfigurators(new ItemConfigurator(object, configuratorProvider));
        }
    }

    public void notifyListUpdate() {
        if (onUpdate != null) {
            onUpdate.accept(configurators.stream()
                    .filter(ItemConfigurator.class::isInstance)
                    .map(ItemConfigurator.class::cast)
                    .map(c -> (T) c.object)
                    .toList());
        }
        notifyChanges();
    }

    public void setSelected(@Nullable ItemConfigurator selected) {
        if (this.selected != null) {
            this.selected.setSelected(false);
        }
        this.selected = selected;
        if (selected != null) {
            selected.setSelected(true);
        }
    }

    public class ItemConfigurator extends Configurator {
        T object;
        Configurator inner;
        boolean isSelected;

        public ItemConfigurator(T object, BiFunction<Supplier<T>, Consumer<T>, Configurator> provider) {
            super("=");
            this.object = object;
            inner = provider.apply(this::getter, this::setter);
            inlineContainer.addChild(inner);
        }

        private void setter(T t) {
            object = t;
            notifyListUpdate();
        }

        private T getter() {
            return object;
        }

        private void setSelected(boolean selected) {
            this.isSelected = selected;
        }
    }

}
