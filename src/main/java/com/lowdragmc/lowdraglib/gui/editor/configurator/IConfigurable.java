package com.lowdragmc.lowdraglib.gui.editor.configurator;

import com.lowdragmc.lowdraglib.gui.editor.ILDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.runtime.ConfiguratorParser;

/**
 * @author KilaBash
 * @date 2022/12/3
 * @implNote IConfigurable
 *
 * You may need to register it as a {@link LDLRegister}.
 * <br>
 * to de/serialize it.
 */
public interface IConfigurable extends ILDLRegister {

    /**
     * Add configurators into given group
     * @param father father group
     */
    default void buildConfigurator(ConfiguratorGroup father) {
        ConfiguratorParser.createConfigurators(father, this);
    }

}
