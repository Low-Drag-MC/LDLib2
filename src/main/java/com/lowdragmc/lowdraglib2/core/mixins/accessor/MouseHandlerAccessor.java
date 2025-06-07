package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author KilaBash
 * @date 2023/7/1
 * @implNote MouseHandlerMixin
 */
@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor int getActiveButton();
}
