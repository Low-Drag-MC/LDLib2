package com.lowdragmc.lowdraglib2.configurator;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.lowdraglib2.registry.ILDLRegisterClient;

import java.util.function.Consumer;

public interface IConfigurable {
    static IConfigurable create(Consumer<ConfiguratorGroup> consumer) {
        return new IConfigurable() {
            @Override
            public void buildConfigurator(ConfiguratorGroup father) {
                consumer.accept(father);
            }
        };
    }

    /**
     * Add configurators into given group
     * @param father father group
     */
    default void buildConfigurator(ConfiguratorGroup father) {
        ConfiguratorParser.createConfigurators(father, this);
    }

    /**
     * Creates and returns a configurator directly instead of build it.
     */
    default Configurator createDirectConfigurator() {
        var group = new ConfiguratorGroup();
        buildConfigurator(group);
        return group;
    }

    /**
     * Obtain the name of this configurable
     */
    default String getConfigurableName() {
        if (this instanceof ILDLRegister<?,?> register) return register.name();
        if (this instanceof ILDLRegisterClient<?,?> register) return register.name();
        return getClass().getSimpleName();
    }

}
