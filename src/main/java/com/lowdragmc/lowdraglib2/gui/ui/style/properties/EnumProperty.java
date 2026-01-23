package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.accessors.EnumAccessor;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.EnumValue;
import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Accessors(chain = true)
public class EnumProperty<T extends Enum<T>> extends Property<T> {
    @Setter
    private List<T> candidates;
    @Setter
    @Nullable
    private Function<T, IGuiTexture> iconProvider;

    public EnumProperty(String name, Class<T> clazz, T initialValue) {
        this(name, clazz, initialValue, List.of(clazz.getEnumConstants()));
    }

    public EnumProperty(String name, Class<T> clazz, T initialValue, List<T> candidates) {
        super(name, clazz, LDLibExtraCodecs.enumCodec(clazz, initialValue), initialValue, EnumValue.of(clazz));
        this.candidates = Collections.unmodifiableList(candidates);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<T> getter, Consumer<T> setter) {
        if (iconProvider != null) {
            return EnumAccessor.create(name, candidates, getter, setter, initialValue, true, iconProvider);
        }
        return EnumAccessor.create(name, candidates, getter, setter, initialValue, true);
    }
}
