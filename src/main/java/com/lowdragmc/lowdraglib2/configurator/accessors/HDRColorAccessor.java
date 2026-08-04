package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigHDR;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.HDRColorConfigurator;
import com.lowdragmc.lowdraglib2.math.HDRColor;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Configurator for {@link HDRColor} fields.
 *
 * <p>{@code @DefaultValue(numberValue = {r, g, b, a, intensity})} seeds the default; the trailing
 * components may be omitted (alpha and intensity then default to 1).
 *
 * <p>Marking the field {@link ConfigHDR} declares it an "emission offset" (alpha carries no meaning)
 * and hides the alpha slider — see {@link HDRColor#toVector4fOpaque()}.
 */
@LDLRegisterClient(name = "hdr_color", registry = "ldlib2:configurator_accessor")
public class HDRColorAccessor extends TypesAccessor<HDRColor> {

    public HDRColorAccessor() {
        super(HDRColor.class);
    }

    @Override
    public HDRColor defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            var values = field.getAnnotation(DefaultValue.class).numberValue();
            return new HDRColor(component(values, 0, 0f), component(values, 1, 0f), component(values, 2, 0f),
                    component(values, 3, 1f), component(values, 4, 1f));
        }
        // an emission offset defaults to "no emission", a plain colour to white
        return field != null && field.isAnnotationPresent(ConfigHDR.class) ? HDRColor.black() : HDRColor.white();
    }

    private static float component(double[] values, int index, float fallback) {
        return index < values.length ? (float) values[index] : fallback;
    }

    @Override
    public Configurator create(String name, Supplier<HDRColor> supplier, Consumer<HDRColor> consumer,
                               boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        var showAlpha = field == null || !field.isAnnotationPresent(ConfigHDR.class);
        return new HDRColorConfigurator(name, supplier, consumer,
                defaultValue(field, field == null ? null : field.getType()), forceUpdate, showAlpha);
    }
}
