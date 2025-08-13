package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MeshData.class)
public interface MeshDataAccessor {
    @Accessor
    ByteBufferBuilder.Result getIndexBuffer();
    @Accessor @Mutable
    void setIndexBuffer(ByteBufferBuilder.Result indexBuffer);
}
