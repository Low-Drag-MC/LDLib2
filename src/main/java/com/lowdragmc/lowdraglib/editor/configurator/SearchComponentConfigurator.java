package com.lowdragmc.lowdraglib.editor.configurator;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SearchComponentWidget;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SearchComponentConfigurator<T> extends ValueConfigurator<T> implements SearchComponentWidget.IWidgetSearch<T> {

    protected Function<T, String> mapping;
    protected SearchComponentWidget<T> searchComponent;
    protected BiConsumer<String, Consumer<T>> searchAction;
    @Setter
    protected int max = 5;
    @Setter
    protected boolean isUp = true;

    public SearchComponentConfigurator(String name, Supplier<T> supplier, Consumer<T> onUpdate, @Nonnull T defaultValue, boolean forceUpdate, BiConsumer<String, Consumer<T>> searchAction, Function<T, String> mapping) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        this.searchAction = searchAction;
        this.mapping = mapping;
    }

    @Override
    protected void onValueUpdate(T newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdate(newValue);
        searchComponent.textFieldWidget.setCurrentString(mapping.apply(newValue));
    }

    @Override
    public void init(int width) {
        super.init(width);
        var componentWidth = width - leftWidth - rightWidth - 3;
        searchComponent = new SearchComponentWidget<>(leftWidth + 3, 2, componentWidth - 3, 10, this);
        searchComponent.setShowUp(true);
        searchComponent.setCapacity(5);
        searchComponent.setCurrent(supplier.get());
        var textFieldWidget = searchComponent.textFieldWidget;
        textFieldWidget.setClientSideWidget();
        textFieldWidget.setBordered(false);
        addWidget(new ImageWidget(leftWidth, 2, componentWidth, 10, ColorPattern.T_GRAY.rectTexture().setRadius(5)));
        addWidget(searchComponent);
    }

    @Override
    public String resultDisplay(T value) {
        return mapping.apply(value);
    }

    @Override
    public void selectResult(T value) {
        this.value = value;
        updateValue();
    }

    @Override
    public void search(String word, Consumer<T> find) {
        searchAction.accept(word, find);
    }
}
