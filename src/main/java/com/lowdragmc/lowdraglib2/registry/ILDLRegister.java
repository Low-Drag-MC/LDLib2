package com.lowdragmc.lowdraglib2.registry;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * @author KilaBash
 * @date 2022/12/17
 * @implNote ILDLRegister
 */
public interface ILDLRegister<T extends ILDLRegister<T, V>, V> {

    /**
     * Whether element is registered
     */
    default boolean isLDLRegister() {
        return getClass().isAnnotationPresent(LDLRegister.class);
    }

    default LDLRegister getRegisterUI() {
        return getClass().getAnnotation(LDLRegister.class);
    }

    default String name() {
        if (isLDLRegister()) {
            return getRegisterUI().name();
        }
        throw new RuntimeException("not registered %s".formatted(getClass()));
    }

    default String group() {
        if (isLDLRegister()) {
            return getRegisterUI().group();
        }
        throw new RuntimeException("not registered %s".formatted(getClass()));
    }

    default ResourceLocation registryName() {
        if (isLDLRegister()) {
            return ResourceLocation.parse(getRegisterUI().registry());
        }
        throw new RuntimeException("not registered %s".formatted(getClass()));
    }

    @SuppressWarnings("unchecked")
    default AutoRegistry.LDLibRegister<T, V> getRegistry() {
        if (isLDLRegister()) {
            if (AutoRegistry.REGISTERED.get(registryName()) instanceof AutoRegistry.LDLibRegister<?,?> registry) {
                return (AutoRegistry.LDLibRegister<T, V>) registry;
            }
        }
        throw new RuntimeException("not registered %s".formatted(getClass()));
    }

    default AutoRegistry.Holder<LDLRegister, T, V> getRegistryHolder() {
        if (isLDLRegister()) {
            return getRegistry().get(name());
        }
        throw new RuntimeException("not registered %s".formatted(getClass()));
    }

    default Optional<AutoRegistry.Holder<LDLRegister, T, V>> getRegistryHolderOptional() {
        if (isLDLRegister()) {
            return Optional.ofNullable(getRegistry().get(name()));
        }
        return Optional.empty();
    }

    default String getTranslateKey() {
        return group().isEmpty() ? name() : "%s.%s".formatted(group(), name());
    }

    default Component getChatComponent() {
        return Component.translatable(getTranslateKey());
    }
}
