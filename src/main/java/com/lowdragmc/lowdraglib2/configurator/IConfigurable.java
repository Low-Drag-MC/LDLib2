package com.lowdragmc.lowdraglib2.configurator;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface IConfigurable {

    /**
     * Add configurators into given group
     * @param father father group
     */
    @OnlyIn(Dist.CLIENT)
    default void buildConfigurator(ConfiguratorGroup father) {
        ConfiguratorParser.createConfigurators(father, this);
    }

    /**
     * Creates and returns a configurator directly instead of build it.
     */
    @OnlyIn(Dist.CLIENT)
    default Configurator createDirectConfigurator() {
        var group = new ConfiguratorGroup();
        buildConfigurator(group);
        return group;
    }

}
