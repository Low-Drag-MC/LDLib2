package com.lowdragmc.lowdraglib2.gui.ui;

import com.google.common.collect.ImmutableList;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.style.*;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class Style implements IConfigurable, IPersistedSerializable {
    public final UIElement holder;
    protected final StyleBag styleBag;
    // runtime
    @Getter @Setter
    private StyleOrigin pipelineState = StyleOrigin.INLINE;

    public Style(UIElement holder) {
        this.holder = holder;
        this.holder._addStyleInternal(this);
        this.styleBag = holder.getStyleBag();
    }

    /**
     * Retrieves the array of properties associated with this implementation.
     * The method defines the properties relevant for the extending class.
     *
     * @return an array of Property instances, representing the set of properties applicable to the implementing class.
     */
    protected abstract Property<?>[] getProperties();

    public final ImmutableList<Property<?>> getPropertiesList() {
        return ImmutableList.copyOf(getProperties());
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) { return (T) o; }

    /**
     * Sets the specified property to the provided value within the style system.
     * Replaces or adds the candidate property value into the style bag using
     * the current pipeline state.
     *
     * @param <T> the type of the property value
     * @param property the property to be updated
     * @param value the new value to set for the specified property
     */
    public <T> void set(Property<T> property, T value) {
        set(pipelineState, property, value);
    }

    public <T> void set(StyleOrigin origin, Property<T> property, T value) {
        if (value == null) {
            styleBag.removeCandidates(property, slot -> slot.origin() == origin &&
                    slot.specificity() == 0 &&
                    slot.sourceOrder() == 0);
            return;
        }
        styleBag.replaceOrPutCandidate(property, StyleSlot.of(
                property,
                origin,
                0, 0,
                value
        ));
    }

    public <T> void setDefault(Property<T> property, T value) {
        set(StyleOrigin.DEFAULT, property, value);
    }

    /**
     * Updates the pipeline state of the provided style temporarily, performs an operation
     * on the style via the specified consumer, and then restores the original pipeline state.
     * During the pipeline, all inline style setters will be redirected to the specified pipeline state.
     *
     * @param pipelineState the {@link StyleOrigin} to temporarily assign as the pipeline state
     * @param style the style instance whose pipeline state will be modified
     * @param styleConsumer a consumer that performs operations on the style instance
     * @param <T> the type of the style, extending {@link Style}
     * @return the modified style instance with its original pipeline state restored
     */
    public static <T extends Style> T pipeline(StyleOrigin pipelineState, T style, Consumer<T> styleConsumer) {
        var previousPipeline = style.getPipelineState();
        style.setPipelineState(pipelineState);
        styleConsumer.accept(style);
        style.setPipelineState(previousPipeline);
        return style;
    }

    public static <T extends Style> T importantPipeline(T style, Consumer<T> styleConsumer) {
        return pipeline(StyleOrigin.IMPORTANT, style, styleConsumer);
    }

    public static <T extends Style> T inlinePipeline(T style, Consumer<T> styleConsumer) {
        return pipeline(StyleOrigin.INLINE, style, styleConsumer);
    }

    public static <T extends Style> T defaultPipeline(T style, Consumer<T> styleConsumer) {
        return pipeline(StyleOrigin.DEFAULT, style, styleConsumer);
    }

    @Nullable
    public <T> T getDefault(Property<T> property) {
        return getValue(property, StyleOrigin.DEFAULT);
    }

    public <T> void setInline(Property<T> property, T value) {
        set(StyleOrigin.INLINE, property, value);
    }

    @Nullable
    public <T> T getInline(Property<T> property) {
        return getValue(property, StyleOrigin.INLINE);
    }

    public <T> void setImportant(Property<T> property, T value) {
        set(StyleOrigin.IMPORTANT, property, value);
    }

    @Nullable
    public <T> T getImportant(Property<T> property) {
        return getValue(property, StyleOrigin.IMPORTANT);
    }

    @Nullable
    public <T> T getValue(Property<T> property, StyleOrigin origin) {
        if (!styleBag.candidates.containsKey(property)) return null;
        return cast(styleBag.candidates.get(property).stream()
                .filter(slot -> slot.origin() == origin)
                .sorted(((a, b) -> StyleSlot.compare(b, a)))
                .map(StyleSlot::value)
                .findFirst()
                .orElse(null));
    }

    public <T> Optional<T> getValue(Property<T> property) {
        return Optional.ofNullable(styleBag.getComputed(property));
    }

    public <T> T getValueSave(Property<T> property) {
        var value = styleBag.getComputed(property);
        if (value != null) return value;
        return property.initialValue;
    }

    public void copyFrom(Style other) {
        var properties = new HashSet<>(Arrays.asList(getProperties()));
        styleBag.removeCandidates(slot ->
                properties.contains(slot.property()) &&
                slot.origin() == StyleOrigin.INLINE);
        for (var property : other.getProperties()) {
            if (properties.contains(property)) {
                var otherSlots = other.styleBag.candidates.get(property);
                if (otherSlots != null) {
                    for (var slot : otherSlots) {
                        if (slot.origin() == StyleOrigin.INLINE) {
                            set(property, cast(slot.value()));
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieves a supplier that can provide the inline value of the specified property.
     *
     * @param property the property whose inline value getter is to be created
     * @return a {@link Supplier} that provides the inline value of the given property, or null if the property has no inline value.
     */
    public <T> Supplier<T> valueGetter(Property<T> property) {
        return () -> cast(getInline(property));
    }

    /**
     * Creates a consumer that sets the value of the specified property.
     * If the value is null, any inline style values for the property are removed.
     * Otherwise, the provided value is set as an inline style.
     *
     * @param property the property whose value is to be set
     * @return a consumer that accepts the new value to set for the specified property, or null to reset property.
     */
    public <T> Consumer<T> valueSetter(Property<T> property) {
        return value -> {
            if (value == null) {
                styleBag.removeCandidates(property, slot -> slot.origin() == StyleOrigin.INLINE);
            } else {
                set(property, cast(value));
            }
        };
    }

    /**
     * Constructs configurators for each property returned by {@code getProperties()}
     * and adds them to the specified {@code ConfiguratorGroup}.
     *
     * @param father the {@link ConfiguratorGroup} to which the created configurators are added
     */
    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        if (this.getClass().isAnnotationPresent(Configurable.class)) {
            var configurable = this.getClass().getAnnotation(Configurable.class);
            var group = new ConfiguratorGroup(configurable.name(), configurable.collapse());
            group.setCanCollapse(configurable.canCollapse());
            group.setTips(configurable.tips());
            father.addConfigurator(group);
            father = group;
        }
        for (var property : getProperties()) {
            var configurator = property.createConfigurator(
                    cast(valueGetter(property)),
                    cast(valueSetter(property)),
                    cast(Optional.ofNullable(getDefault(property)).orElse(cast(property.initialValue)))
            );
            father.addConfigurator(configurator);
        }
    }


    @Override
    public void beforeDeserialize() {
        for (Property<?> property : getProperties()) {
            styleBag.removeCandidates(property, slot -> slot.origin() == StyleOrigin.INLINE);
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        var tag = IPersistedSerializable.super.serializeNBT(provider);
        for (Property<?> property : getProperties()) {
            var inline = getInline(property);
            if (inline != null) {
                tag.put(property.name, property.codec.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), cast(inline)).result().orElseThrow());
            }
        }
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
        IPersistedSerializable.super.deserializeNBT(provider, tag);
        for (Property<?> property : getProperties()) {
            if (!tag.contains(property.name)) continue;
            property.codec.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag.get(property.name)).result()
                    .ifPresent(value -> set(property, cast(value)));
        }
    }
}
