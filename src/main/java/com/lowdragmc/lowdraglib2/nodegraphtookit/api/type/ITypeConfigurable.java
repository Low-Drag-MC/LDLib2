package com.lowdragmc.lowdraglib2.nodegraphtookit.api.type;

import com.lowdragmc.lowdraglib2.configurator.ConfiguratorAccessors;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.accessors.EnumAccessor;
import com.lowdragmc.lowdraglib2.configurator.ui.ValueConfigurator;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldValueConfigurable;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public interface ITypeConfigurable {
    ITypeConfigurable NOP = (valueConfigurable, typeHandle) -> null;
    ITypeConfigurable DEFAULT = (valueConfigurable, typeHandle) -> {
        if (typeHandle == null) return null;
        var type = typeHandle.resolve();
        // for enum
        if (type instanceof Class<?> clazz && clazz.isEnum()) {
            var candidates = Arrays.stream(clazz.getEnumConstants()).map(Enum.class::cast).toList();
            return IConfigurable.create(father ->
                father.addConfigurator(EnumAccessor.<Enum>create(
                        "",
                        candidates,
                        valueConfigurable::getValue,
                        valueConfigurable::setValue,
                        candidates.getFirst(),
                        valueConfigurable.forceUpdate()
                        ))
            );
        }
        // others
        var accessor = ConfiguratorAccessors.findByType(type);
        var configurator = IConfigurable.create(father -> father.addConfigurator(accessor.create(
                "",
                valueConfigurable::getValue,
                valueConfigurable::setValue,
                valueConfigurable.forceUpdate(),
                valueConfigurable.getValueField(),
                valueConfigurable.getValueOwer()
        )));
        if (configurator instanceof ValueConfigurator<?> valueConfigurator) {
            valueConfigurator.setDefaultValue(valueConfigurable.getDefaultValue());
        }
        return configurator;
    };

    @Nullable
    IConfigurable createConfigurable(IFieldValueConfigurable valueConfigurable, TypeHandle typeHandle);
}
