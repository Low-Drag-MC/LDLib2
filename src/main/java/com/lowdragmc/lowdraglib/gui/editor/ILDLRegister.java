package com.lowdragmc.lowdraglib.gui.editor;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import net.minecraft.network.chat.Component;

/**
 * @author KilaBash
 * @date 2022/12/17
 * @implNote ILDLRegister
 */
public interface ILDLRegister {

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

    default String getTranslateKey() {
        return "ldlib.gui.editor.register.%s.%s".formatted(group(), name());
    }

    default Component getChatComponent() {
        return Component.translatable(getTranslateKey());
    }
}
