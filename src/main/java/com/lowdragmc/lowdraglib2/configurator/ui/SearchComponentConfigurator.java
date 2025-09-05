package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.TextWrap;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.appliedenergistics.yoga.YogaOverflow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
public class SearchComponentConfigurator<T> extends ValueConfigurator<T> implements SearchComponent.ISearchUI<T> {
    public final SearchComponent<T> searchComponent;
    public final BiConsumer<String, Consumer<T>> searchAction;
    public final Function<T, String> searchResultText;

    public SearchComponentConfigurator(String name, Supplier<T> supplier, Consumer<T> onUpdate, @Nonnull T defaultValue, boolean forceUpdate,
                                       BiConsumer<String, Consumer<T>> searchAction,
                                       Function<T, String> searchResultText,
                                       Function<T, String> mapping) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        this.searchAction = searchAction;
        this.searchResultText = searchResultText;
        if (value == null) value = defaultValue;
        inlineContainer.addChild(searchComponent = new SearchComponent<>(this));
        searchComponent.setCandidateUIProvider(candidate -> new Label()
                .textStyle(style -> style
                        .textWrap(TextWrap.HOVER_ROLL)
                        .textAlignHorizontal(Horizontal.LEFT)
                        .textAlignVertical(Vertical.CENTER))
                .setText(candidate == null ? "---" : mapping.apply(candidate))
                .setOverflow(YogaOverflow.HIDDEN));
        searchComponent.setSelected(value, false);
        searchComponent.setOnValueChanged(this::updateValueActively);
    }

    @Override
    protected void onValueUpdatePassively(T newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        searchComponent.setSelected(newValue, false);
    }

    @Override
    public String resultText(@NotNull T value) {
        return searchResultText.apply(value);
    }

    @Override
    public void onResultSelected(@Nullable T value) {}

    @Override
    public void search(String word, IResultHandler<T> searchHandler) {
        searchAction.accept(word, searchHandler);
    }
}
