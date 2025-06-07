package com.lowdragmc.lowdraglib2.editor_outdated.configurator;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.ConfiguratorParser;

/**
 * @author KilaBash
 * @date 2022/12/3
 * @implNote IConfigurable
 *
 * You may need to register it as a {@link LDLRegister}.
 * <br>
 * to de/serialize it.
 */
public interface IConfigurable {

    /**
     * Add configurators into given group
     * @param father father group
     */
    default void buildConfigurator(ConfiguratorGroup father) {
        ConfiguratorParser.createConfigurators(father, this);
    }

}
