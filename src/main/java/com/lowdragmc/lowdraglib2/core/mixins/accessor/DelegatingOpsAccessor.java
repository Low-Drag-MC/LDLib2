package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.DelegatingOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DelegatingOps.class)
public interface DelegatingOpsAccessor<T> {
    @Accessor DynamicOps<T> getDelegate();
}
