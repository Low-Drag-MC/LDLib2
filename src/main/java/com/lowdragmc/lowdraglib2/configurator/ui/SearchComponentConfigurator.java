package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.lowdragmc.lowdraglib2.utils.search.ISearch;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
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

    public SearchComponentConfigurator(String name, Supplier<T> supplier, Consumer<T> onUpdate, ISearchConfigurator<T> searchConfigurator, boolean forceUpdate) {
        this(name, supplier, onUpdate, searchConfigurator.defaultValue(), forceUpdate,
                (w, u) -> searchConfigurator.search(w, u::accept),
                searchConfigurator::resultText,
                searchConfigurator.candidateUIProvider());
    }

    public SearchComponentConfigurator(String name, Supplier<T> supplier, Consumer<T> onUpdate, @Nonnull T defaultValue, boolean forceUpdate,
                                       BiConsumer<String, Consumer<T>> searchAction,
                                       Function<T, String> searchResultText,
                                       UIElementProvider<T> candidateProvider) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        this.searchAction = searchAction;
        this.searchResultText = searchResultText;
        if (value == null) value = defaultValue;
        inlineContainer.addChild(searchComponent = new SearchComponent<>(this));
        searchComponent.setCandidateUIProvider(candidateProvider);
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

    public interface ISearchConfigurator<T> extends ISearch<T> {
        /**
         * Returns the default value for the generic type {@code T}.
         *
         * @return the default value of type {@code T}
         */
        T defaultValue();

        /**
         * Generates a string representation of the specified value.
         *
         * @param value the non-null value of type {@code T} to be converted into a string representation
         * @return the string representation of the specified value
         */
        String resultText(@NotNull T value);

        /**
         * Generates a specific string mapping for the provided non-null value of type {@code T}.
         *
         * @param value the non-null value of type {@code T} to be mapped to a string
         * @return a string representation resulting from the mapping of the provided value
         */
        default Component mapping(@NotNull T value) {
            return Component.translatable(value.toString());
        }

        @Nullable
        default UIElementProvider<T> candidateUIProvider() {
            return candidate -> new Label()
                    .textStyle(style -> style
                            .textWrap(TextWrap.HOVER_ROLL)
                            .textAlignHorizontal(Horizontal.LEFT)
                            .textAlignVertical(Vertical.CENTER))
                    .setText(candidate == null ? Component.literal("---") : mapping(candidate))
                    .setOverflowVisible(false);
        }
    }
}
