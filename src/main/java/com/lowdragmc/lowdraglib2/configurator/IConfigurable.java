package com.lowdragmc.lowdraglib2.configurator;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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
    @OnlyIn(Dist.CLIENT)
    default void buildConfigurator(ConfiguratorGroup father) {
        ConfiguratorParser.createConfigurators(father, this);
    }

}
