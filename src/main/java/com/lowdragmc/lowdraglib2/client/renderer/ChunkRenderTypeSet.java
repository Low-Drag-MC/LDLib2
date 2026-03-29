package com.lowdragmc.lowdraglib2.client.renderer;

import net.minecraft.client.renderer.RenderType;

import java.util.Collections;
import java.util.Iterator;

/**
 * Stub for NeoForge ChunkRenderTypeSet.
 * In Fabric, the render types are typically retrieved differently or emitted directly via QuadEmitter.
 */
public class ChunkRenderTypeSet implements Iterable<RenderType> {

    public static ChunkRenderTypeSet all() {
        return new ChunkRenderTypeSet(null);
    }

    public static ChunkRenderTypeSet of(RenderType type) {
        return new ChunkRenderTypeSet(type);
    }

    private final RenderType type;

    private ChunkRenderTypeSet(RenderType type) {
        this.type = type;
    }

    @Override
    public Iterator<RenderType> iterator() {
        return Collections.singleton(type).iterator();
    }
}
