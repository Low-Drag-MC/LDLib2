package com.lowdragmc.lowdraglib2.client.utils;

import com.lowdragmc.lowdraglib2.core.mixins.accessor.MeshDataAccessor;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import org.apache.commons.lang3.mutable.MutableLong;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import org.jetbrains.annotations.Nullable;

public class MeshDataSortResult {
    public final Vector3f[] centroids;
    public final VertexFormat.IndexType indexType;
    public final VertexFormat.Mode mode;
    public final ByteBufferBuilder.Result indexBuffer;

    public MeshDataSortResult(Vector3f[] centroids, VertexFormat.IndexType indexType, VertexFormat.Mode mode) {
        this(centroids, indexType, mode, null);
    }

    public MeshDataSortResult(Vector3f[] centroids, VertexFormat.IndexType indexType,
                              VertexFormat.Mode mode, ByteBufferBuilder.Result indexBuffer) {
        this.centroids = centroids;
        this.indexType = indexType;
        this.mode = mode;
        this.indexBuffer = indexBuffer;
    }

    public void applyTo(MeshData meshData) {
        if (meshData instanceof MeshDataAccessor accessor) {
            accessor.setIndexBuffer(indexBuffer);
        }
    }

    @Nullable
    public ByteBufferBuilder.Result buildSortedIndexBuffer(ByteBufferBuilder bufferBuilder, VertexSorting sorting) {
        int[] primitiveIndices = sorting.sort(this.centroids);
        
        return switch (mode) {
            case TRIANGLES -> buildTriangleIndexBuffer(bufferBuilder, primitiveIndices);
            case QUADS -> buildQuadIndexBuffer(bufferBuilder, primitiveIndices);
            case LINES, DEBUG_LINES -> buildLineIndexBuffer(bufferBuilder, primitiveIndices);
            default -> null;
        };
    }
    
    private ByteBufferBuilder.Result buildTriangleIndexBuffer(ByteBufferBuilder bufferBuilder, int[] primitiveIndices) {
        long memoryAddress = bufferBuilder.reserve(primitiveIndices.length * 3 * this.indexType.bytes);
        IntConsumer indexWriter = createIndexWriter(memoryAddress, this.indexType);
        
        for (int primitiveIndex : primitiveIndices) {
            int baseVertexIndex = primitiveIndex * 3;
            indexWriter.accept(baseVertexIndex);
            indexWriter.accept(baseVertexIndex + 1);
            indexWriter.accept(baseVertexIndex + 2);
        }
        
        return bufferBuilder.build();
    }
    
    private ByteBufferBuilder.Result buildQuadIndexBuffer(ByteBufferBuilder bufferBuilder, int[] primitiveIndices) {
        long memoryAddress = bufferBuilder.reserve(primitiveIndices.length * 6 * this.indexType.bytes);
        IntConsumer indexWriter = createIndexWriter(memoryAddress, this.indexType);
        
        for (int primitiveIndex : primitiveIndices) {
            int baseVertexIndex = primitiveIndex * 4;
            // 两个三角形：(0,1,2) 和 (2,3,0)
            indexWriter.accept(baseVertexIndex);
            indexWriter.accept(baseVertexIndex + 1);
            indexWriter.accept(baseVertexIndex + 2);
            indexWriter.accept(baseVertexIndex + 2);
            indexWriter.accept(baseVertexIndex + 3);
            indexWriter.accept(baseVertexIndex);
        }
        
        return bufferBuilder.build();
    }
    
    private ByteBufferBuilder.Result buildLineIndexBuffer(ByteBufferBuilder bufferBuilder, int[] primitiveIndices) {
        long memoryAddress = bufferBuilder.reserve(primitiveIndices.length * 2 * this.indexType.bytes);
        IntConsumer indexWriter = createIndexWriter(memoryAddress, this.indexType);
        
        for (int primitiveIndex : primitiveIndices) {
            int baseVertexIndex = primitiveIndex * 2;
            indexWriter.accept(baseVertexIndex);
            indexWriter.accept(baseVertexIndex + 1);
        }
        
        return bufferBuilder.build();
    }
    
    private IntConsumer createIndexWriter(long index, VertexFormat.IndexType type) {
        MutableLong mutableLong = new MutableLong(index);
        
        return switch (type) {
            case SHORT -> value -> MemoryUtil.memPutShort(mutableLong.getAndAdd(2L), (short) value);
            case INT -> value -> MemoryUtil.memPutInt(mutableLong.getAndAdd(4L), value);
        };
    }
}