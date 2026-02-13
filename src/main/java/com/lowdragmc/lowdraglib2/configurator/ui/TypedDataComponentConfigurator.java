package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TagField;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings({"unchecked", "rawtypes"})
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TypedDataComponentConfigurator extends ValueConfigurator<TypedDataComponent> implements SearchComponent.ISearchUI<DataComponentType> {
    public final SearchComponent searchComponent = new SearchComponent<>(this);
    public final TagField tagField = new TagField();

    public TypedDataComponentConfigurator(String name, Supplier<TypedDataComponent> supplier, Consumer<TypedDataComponent> onUpdate, @Nonnull TypedDataComponent<?> defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setCopiable(value -> value);

        if (value == null) value = defaultValue;
        searchComponent.layout(layout -> layout.widthPercent(50));
        tagField.layout(layout -> layout.widthPercent(50));
        tagField.setTagResponder(tag -> {
            var type = Optional.ofNullable(this.value).map(TypedDataComponent::type).orElse(null);
            TypedDataComponent result;
            if (type == null || type.isTransient() || tag == EndTag.INSTANCE) {
                result = TypedDataComponent.createUnchecked(type, null);
            } else {
                var value = type.codec().parse(Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(null);
                result = TypedDataComponent.createUnchecked(type, value);
            }
            updateValueActively(result);
        });
        inlineContainer.layout(layout -> layout.flexDirection(FlexDirection.ROW));
        inlineContainer.addChildren(searchComponent, tagField);
        refreshUI();
    }

    protected void refreshUI() {
        var type = this.value == null ? null : this.value.type();
        var value = this.value == null ? null : this.value.value();
        searchComponent.setValue(type, false);
        if (type == null || type.isTransient()) {
            tagField.setValue(null, false);
            tagField.setTagValidator(tag -> false);
        } else {
            if (value == null) {
                tagField.setValue(null, false);

            } else {
                tagField.setValue((Tag) Objects.requireNonNull(type.codec())
                        .encodeStart(Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE), value)
                        .result().orElse(null), false);
            }
            tagField.setTagValidator(tag -> {
                var result = type.codec().parse(Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE), tag);
                return result.result().isPresent();
            });
        }
    }


    @Override
    protected void onValueUpdatePassively(@Nullable TypedDataComponent newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        refreshUI();
    }

    @Override
    public String resultText(DataComponentType value) {
        return Optional.ofNullable(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(value))
                .map(Object::toString)
                .orElse("unknown");
    }

    @Override
    public void onResultSelected(@Nullable DataComponentType type) {
        var newTypedDataComponent = TypedDataComponent.createUnchecked(type, null);
        updateValueActively(newTypedDataComponent);
        refreshUI();
    }

    @Override
    public void search(String word, IResultHandler<DataComponentType> searchHandler) {
        var wordLower = word.toLowerCase();
        for (var typeEntry : BuiltInRegistries.DATA_COMPONENT_TYPE.entrySet()) {
            if (Thread.currentThread().isInterrupted()) return;
            var type = typeEntry.getValue();
            var id = typeEntry.getKey().location();
            if (id.toString().contains(wordLower)) {
                searchHandler.accept(type);
            }
        }
    }
}
