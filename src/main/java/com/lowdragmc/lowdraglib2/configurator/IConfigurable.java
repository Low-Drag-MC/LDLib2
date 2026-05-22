package com.lowdragmc.lowdraglib2.configurator;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.lowdraglib2.registry.ILDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface IConfigurable {
    static IConfigurable create(Consumer<ConfiguratorGroup> consumer) {
        return new IConfigurable() {
            @Override
            @OnlyIn(Dist.CLIENT)
            public void buildConfigurator(ConfiguratorGroup father) {
                consumer.accept(father);
            }
        };
    }

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

    /**
     * Creates a history recorder for this configurable. Returning {@code null} disables
     * history tracking for this configurable in the inspector.
     * <p>
     * Default implementation returns {@link IConfigurableHistory#ofSerializable(INBTSerializable)}
     * when this instance implements {@link INBTSerializable}, otherwise {@code null}.
     */
    @Nullable
    default IConfigurableHistory createHistoryRecorder() {
        if (this instanceof INBTSerializable<?> serializable) {
            return IConfigurableHistory.ofSerializable(serializable);
        }
        return null;
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
