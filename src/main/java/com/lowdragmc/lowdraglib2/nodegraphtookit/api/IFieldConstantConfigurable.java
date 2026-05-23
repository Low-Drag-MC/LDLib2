package com.lowdragmc.lowdraglib2.nodegraphtookit.api;

import com.lowdragmc.lowdraglib2.configurator.ConfiguratorAccessors;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ValueConfigurator;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.ITypeConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import org.jetbrains.annotations.Nullable;

public interface IFieldConstantConfigurable extends IFieldValueConfigurable {
    @Nullable Constant getConfigurableConstant();
    void onValueChanged();

    /**
     * Per-instance override for {@link #buildConfigurator}. Returning a non-null value bypasses
     * the {@code TypeHandle}-based resolution and uses the supplied configurable instead. Used
     * to plug in custom configurators for individual options/ports without having to register a
     * new {@code ITypeConfigurable} against the type identification — useful when two ports
     * share the same {@code TypeHandle} but want different UIs.
     */
    @Nullable
    default ITypeConfigurable getCustomTypeConfigurable() {
        return null;
    }

    /**
     * Hard opt-out for the inspector field. When {@code false}, {@link #buildConfigurator} is a
     * no-op — no UI row is generated for this port/option even though its underlying value still
     * exists on the model. Default {@code true} keeps every existing call site behaving as today.
     */
    default boolean isConfiguratorEnabled() {
        return true;
    }

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
        onValueChanged();
    }

    @Override
    default void buildConfigurator(ConfiguratorGroup father) {
        if (!isConfiguratorEnabled()) return;
        var constant = getConfigurableConstant();
        var group = new ConfiguratorGroup();
        if (constant != null) {
            var typeHandle = constant.getTypeHandle();
            // Per-instance override takes precedence over typeHandle-based resolution. This lets
            // two ports/options that share a TypeHandle use different configurator UIs.
            ITypeConfigurable resolved = getCustomTypeConfigurable();
            if (resolved == null && typeHandle != null) {
                resolved = typeHandle.resolveConfigurable();
            }
            if (resolved != null) {
                var configurable = resolved.createConfigurable(this, typeHandle);
                if (configurable != null) {
                    configurable.buildConfigurator(group);
                }
            } else {
                var type = constant.getType();
                var accessor = ConfiguratorAccessors.findByType(type);
                var configurator = accessor.create(
                        "",
                        this::getValue,
                        this::setValue,
                        this.forceUpdate(),
                        this.getValueField(),
                        this.getValueOwer()
                );
                if (configurator instanceof ValueConfigurator<?> valueConfigurator) {
                    valueConfigurator.setDefaultValue(getDefaultValue());
                }
                group.addConfigurator(configurator);
            }
        }
        for (var configurator : group.getConfigurators()) {
            configurator.addEventListener(Configurator.CHANGE_EVENT, e -> notifyValueChanged());
            father.addConfigurator(configurator);
        }
    }

}
