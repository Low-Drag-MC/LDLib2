package com.lowdragmc.lowdraglib2.registry;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * @author KilaBash
 * @date 2023/6/13
 * @implNote ILDLRegisterClient
 */
public interface ILDLRegisterClient<T extends ILDLRegisterClient<T, V>, V> {
    default boolean isLDLRegister() {
        return getClass().isAnnotationPresent(LDLRegisterClient.class);
    }

    default LDLRegisterClient getRegisterUIClient() {
        return getClass().getAnnotation(LDLRegisterClient.class);
    }

    default String name() {
        if (isLDLRegister()) {
            return getRegisterUIClient().name();
        }
        throw new RuntimeException("not registered %s".formatted(getClass()));
    }

    default String group() {
        if (isLDLRegister()) {
            return getRegisterUIClient().group();
        }
        throw new RuntimeException("not registered ui %s".formatted(getClass()));
    }

    default ResourceLocation registryName() {
        if (isLDLRegister()) {
            return ResourceLocation.parse(getRegisterUIClient().registry());
        }
        throw new RuntimeException("not registered %s".formatted(getClass()));
    }

    @SuppressWarnings("unchecked")
    default AutoRegistry.LDLibRegisterClient<T, V> getRegistry() {
        if (isLDLRegister()) {
            if (AutoRegistry.REGISTERED.get(registryName()) instanceof AutoRegistry.LDLibRegisterClient<?,?> registry) {
                return (AutoRegistry.LDLibRegisterClient<T, V>) registry;
            }
        }
        throw new RuntimeException("not registered %s".formatted(getClass()));
    }

    default AutoRegistry.Holder<LDLRegisterClient, T, V> getRegistryHolder() {
        if (isLDLRegister()) {
            return getRegistry().get(name());
        }
        throw new RuntimeException("not registered %s".formatted(getClass()));
    }

    default Optional<AutoRegistry.Holder<LDLRegisterClient, T, V>> getRegistryHolderOptional() {
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
