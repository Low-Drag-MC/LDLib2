package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {
    /**
     * Base pointer of the vertex currently being written. In 26.2 {@code BufferBuilder.beginElement}
     * is private and only indexes a fixed 7-element array (Position..LineWidth), so custom vertex
     * attributes can no longer be written by semantic id. Instead we expose the raw vertex pointer
     * and write custom attributes directly at {@code pointer + element.offset()} (offset resolved by
     * name from the {@link com.mojang.blaze3d.vertex.VertexFormat}). Returns {@code -1L} when not
     * currently building a vertex.
     */
    @Accessor("vertexPointer")
    long getVertexPointer();
}
