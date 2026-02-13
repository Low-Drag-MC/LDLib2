package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.configurator.ConfiguratorAccessors;
import com.lowdragmc.lowdraglib2.configurator.accessors.IConfiguratorAccessor;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.mojang.serialization.Codec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
@Accessors(chain = true)
public class Property<VALUE> {

    public static final Codec<Property<?>> CODEC = Codec.STRING.xmap(PropertyRegistry::byName, property -> property.name);
    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);

    public final int id;
    public final String name;
    public final Class<VALUE> type;
    public final Codec<VALUE> codec;
    public final VALUE initialValue;
    public final ValueParser<VALUE> valueParser;
    private final List<StyleChangeListener<VALUE>> styleChangeListeners = new ArrayList<>();
    @Setter @Getter
    private IValueInterpolator<VALUE> interpolator = IValueInterpolator.BINARY;
    // config
    @Setter @Getter
    private boolean allowTransition = false;
    @Getter @Setter
    private String configName;
    @Setter
    private Tooltips configTooltips = Tooltips.empty();

    @Getter(lazy = true, value = AccessLevel.PROTECTED)
    private static final Field VALUE_FIELD = getValueField();
    private static Field getValueField() {
        try {
            return Property.class.getDeclaredField("initialValue");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public Property(String name, Class<VALUE> type, Codec<VALUE> codec, VALUE initialValue, ValueParser<VALUE> valueParser) {
        this.id = ID_COUNTER.getAndIncrement();
        this.name = name;
        this.type = type;
        this.codec = codec;
        this.initialValue = initialValue;
        this.valueParser = valueParser;
        this.configName = "property." + name;
    }

    public Property<VALUE> addListener(StyleChangeListener<VALUE> listener) {
        styleChangeListeners.add(listener);
        return this;
    }

    public void notifyListeners(UIElement element, @Nullable VALUE oldVal, @Nullable VALUE newVal) {
        for (var listener : styleChangeListeners) {
            listener.onComputedChange(element, this, oldVal, newVal);
        }
    }

    public static <T> Property<T> of(String name, Class<T> type, Codec<T> codec, T initialValue, ValueParser<T> valueParser) {
        return new Property<>(name, type, codec, initialValue, valueParser);
    }

    public static <T> Property<T> of(String name, Codec<T> codec, @Nonnull T initialValue, ValueParser<T> valueParser) {
        return new Property<>(name, (Class<T>) initialValue.getClass(), codec, initialValue, valueParser);
    }

    public Optional<VALUE> getValue(Map<String, StyleValue<?>> properties) {
        if (properties.containsKey(name)) {
            try {
                return (Optional<VALUE>) Optional.ofNullable(properties.get(name).compute());
            } catch (Exception ignored) {}
        }
        return Optional.empty();
    }

    public Component[] getConfigTooltips() {
        return Tooltips.of(Component.translatable("property.style_name", name)).merge(configTooltips).tooltips();
    }

    /**
     * Creates a Configurator instance, which provides a user interface component
     * to manage and modify a property inline value.
     *
     * @param getter a supplier that retrieves the current inline value of the property;
     *               it must return the property inline value, or null if not set.
     * @param setter a consumer that accepts a value to update the property;
     *               passing null resets the property to an initial state.
     * @return a Configurator instance with configured tooltips, a reset button,
     *         and event listeners to manage user interactions.
     */
    public final Configurator createConfigurator(Supplier<VALUE> getter, Consumer<VALUE> setter, VALUE defaultValue) {
        var name = getConfigName();
        var inlineMark = new AtomicBoolean(getter.get() != null);
        var configurator = createConfiguratorInternal(name, () -> {
            var value = getter.get();
            if (value == null) return defaultValue;
            return value;
        }, setter);
        var tooltips = getConfigTooltips();
        configurator.setTips(tooltips);
        var clearButton = new Button().noText().setOnClick(e -> setter.accept(null));
        clearButton.layout(layout -> layout.height(14).width(14)).addChild(new UIElement()
                .layout(layout -> layout.height(10).width(10))
                .style(style -> style.backgroundTexture(Icons.REPLAY).tooltips("property.reset"))
        );
        clearButton.setDisplay(inlineMark.get());
        configurator.label.setText(inlineMark.get() ?
                configurator.label.getText().copy().withStyle(style -> style.withColor(ColorPattern.ORANGE.color)) :
                configurator.label.getText().copy().withStyle(style -> style.withColor(-1)));
        configurator.lineContainer.addChildAt(clearButton, configurator.tip.getSiblingIndex());
        configurator.addEventListener(UIEvents.TICK, e -> {
            var hasInline = getter.get() != null;
            if (hasInline == inlineMark.get()) return;
            inlineMark.set(hasInline);
            clearButton.setDisplay(hasInline);
            configurator.label.setText(hasInline ?
                    configurator.label.getText().copy().withStyle(style -> style.withColor(ColorPattern.ORANGE.color)) :
                    configurator.label.getText().copy().withStyle(style -> style.withColor(-1)));
        });
        return configurator;
    }

    protected Configurator createConfiguratorInternal(String name, Supplier<VALUE> getter, Consumer<VALUE> setter) {
        IConfiguratorAccessor accessor = ConfiguratorAccessors.findByClass(type);
        return accessor.create(name, getter, setter, true, getVALUE_FIELD(), this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Property<?> property = (Property<?>) o;
        return id == property.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return name;
    }
}
