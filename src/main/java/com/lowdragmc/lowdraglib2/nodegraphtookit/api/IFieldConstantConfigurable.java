package com.lowdragmc.lowdraglib2.nodegraphtookit.api;

import com.lowdragmc.lowdraglib2.configurator.ConfiguratorAccessors;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import org.jetbrains.annotations.Nullable;

public interface IFieldConstantConfigurable extends IFieldValueConfigurable {
    @Nullable Constant getConfigurableConstant();

    @Override
    default void setValue(Object value) {
        var constant = getConfigurableConstant();
        if (constant != null) constant.setValue(value);
    }

    @Override
    default <T> T getValue() {
        var constant = getConfigurableConstant();
        if (constant == null) return null;
        return (T) constant.getValue();
    }

    @Override
    default <T> T getDefaultValue() {
        var constant = getConfigurableConstant();
        if (constant == null) return null;
        return (T) constant.getDefaultValue();
    }

    @Override
    default void notifyValueChanged() {
        var constant = getConfigurableConstant();
        if (constant != null) {
            constant.notifyListeners();
        }
    }

    @Override
    default void buildConfigurator(ConfiguratorGroup father) {
        var constant = getConfigurableConstant();
        var group = new ConfiguratorGroup();
        if (constant != null) {
            var typeHandle = constant.getTypeHandle();
            if (typeHandle != null) {
                var configurable = typeHandle.resolveConfigurable().createConfigurable(this, typeHandle);
                if (configurable != null) {
                    configurable.buildConfigurator(group);
                }
            } else {
                var type = constant.getType();
                var accessor = ConfiguratorAccessors.findByType(type);
                group.addConfigurator(accessor.create(
                        "",
                        this::getValue,
                        this::setValue,
                        this.forceUpdate(),
                        this.getValueField(),
                        this.getValueOwer()
                ));
            }
        }
        for (var configurator : group.getConfigurators()) {
            configurator.addEventListener(Configurator.CHANGE_EVENT, e -> notifyValueChanged());
            father.addConfigurator(configurator);
        }
    }

}
