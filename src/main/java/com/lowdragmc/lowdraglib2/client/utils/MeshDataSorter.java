package com.lowdragmc.lowdraglib2.client.utils;

import com.mojang.blaze3d.vertex.*;
import org.joml.Vector3f;

import org.jetbrains.annotations.Nullable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class MeshDataSorter {

    @Nullable
    public static MeshDataSortResult sortPrimitives(MeshData meshData, ByteBufferBuilder bufferBuilder, VertexSorting sorting) {
        var drawState = meshData.drawState();
        VertexFormat.Mode mode = drawState.mode();
        int verticesPerPrimitive = getVerticesPerPrimitive(mode);
        
        if (verticesPerPrimitive == 0) {
            return null;
        }
        
        Vector3f[] centroids = extractPrimitiveCentroids(
            meshData.vertexBuffer(),
            drawState.vertexCount(),
            drawState.format(),
            mode
        );
        
        if (centroids.length == 0) {
            return null;
        }
        
        MeshDataSortResult sortResult = new MeshDataSortResult(centroids, drawState.indexType(), mode);
        ByteBufferBuilder.Result indexBuffer = sortResult.buildSortedIndexBuffer(bufferBuilder, sorting);
        
        return new MeshDataSortResult(centroids, drawState.indexType(), mode, indexBuffer);
    }
    
    private static int getVerticesPerPrimitive(VertexFormat.Mode mode) {
        return switch (mode) {
            case TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN -> 3;
            case QUADS -> 4;
            case LINES, DEBUG_LINE_STRIP, DEBUG_LINES -> 2;
            default -> 0;
        };
    }
    
    private static Vector3f[] extractPrimitiveCentroids(ByteBuffer byteBuffer, int vertexCount,
                                                       VertexFormat format, VertexFormat.Mode mode) {
        int positionOffset = format.getOffset(VertexFormatElement.POSITION);
        if (positionOffset == -1) {
            throw new IllegalArgumentException("Cannot identify primitive centers with no position element");
        }
        
        int verticesPerPrimitive = getVerticesPerPrimitive(mode);
        if (verticesPerPrimitive == 0 || vertexCount < verticesPerPrimitive) {
            return new Vector3f[0];
        }
        
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        int vertexSizeInFloats = format.getVertexSize() / 4;
        int primitiveCount = vertexCount / verticesPerPrimitive;
        
        return switch (mode) {
            case TRIANGLES -> extractTriangleCentroids(floatBuffer, primitiveCount, vertexSizeInFloats, positionOffset);
            case QUADS -> extractQuadCentroids(floatBuffer, primitiveCount, vertexSizeInFloats, positionOffset);
            case LINES, DEBUG_LINES -> extractLineCentroids(floatBuffer, primitiveCount, vertexSizeInFloats, positionOffset);
            default -> extractGenericCentroids(floatBuffer, primitiveCount, vertexSizeInFloats, positionOffset, verticesPerPrimitive);
        };
    }
    
    private static Vector3f[] extractTriangleCentroids(FloatBuffer floatBuffer, int primitiveCount,
                                                      int vertexSizeInFloats, int positionOffset) {
        Vector3f[] centroids = new Vector3f[primitiveCount];
        int primitiveStride = vertexSizeInFloats * 3;
        
        for (int i = 0; i < primitiveCount; i++) {
            int baseIndex = i * primitiveStride + positionOffset;
            
            // p0
            float x1 = floatBuffer.get(baseIndex);
            float y1 = floatBuffer.get(baseIndex + 1);
            float z1 = floatBuffer.get(baseIndex + 2);
            
            // p1
            int v2Index = baseIndex + vertexSizeInFloats;
            float x2 = floatBuffer.get(v2Index);
            float y2 = floatBuffer.get(v2Index + 1);
            float z2 = floatBuffer.get(v2Index + 2);
            
            // p2
            int v3Index = v2Index + vertexSizeInFloats;
            float x3 = floatBuffer.get(v3Index);
            float y3 = floatBuffer.get(v3Index + 1);
            float z3 = floatBuffer.get(v3Index + 2);
            
            // centroids =：(v1 + v2 + v3) / 3
            centroids[i] = new Vector3f(
                (x1 + x2 + x3) * 0.33333334f,
                (y1 + y2 + y3) * 0.33333334f,
                (z1 + z2 + z3) * 0.33333334f
            );
        }
        
        return centroids;
    }
    
    private static Vector3f[] extractQuadCentroids(FloatBuffer floatBuffer, int primitiveCount,
                                                  int vertexSizeInFloats, int positionOffset) {
        Vector3f[] centroids = new Vector3f[primitiveCount];
        int primitiveStride = vertexSizeInFloats * 4;
        
        for (int i = 0; i < primitiveCount; i++) {
            int baseIndex = i * primitiveStride + positionOffset;
            
            // p0
            float x1 = floatBuffer.get(baseIndex);
            float y1 = floatBuffer.get(baseIndex + 1);
            float z1 = floatBuffer.get(baseIndex + 2);
            
            // p3
            int v3Index = baseIndex + vertexSizeInFloats * 2;
            float x3 = floatBuffer.get(v3Index);
            float y3 = floatBuffer.get(v3Index + 1);
            float z3 = floatBuffer.get(v3Index + 2);
            
            centroids[i] = new Vector3f(
                (x1 + x3) * 0.5f,
                (y1 + y3) * 0.5f,
                (z1 + z3) * 0.5f
            );
        }
        
        return centroids;
    }
    
    private static Vector3f[] extractLineCentroids(FloatBuffer floatBuffer, int primitiveCount,
                                                  int vertexSizeInFloats, int positionOffset) {
        Vector3f[] centroids = new Vector3f[primitiveCount];
        int primitiveStride = vertexSizeInFloats * 2;
        
        for (int i = 0; i < primitiveCount; i++) {
            int baseIndex = i * primitiveStride + positionOffset;
            
            // p0
            float x1 = floatBuffer.get(baseIndex);
            float y1 = floatBuffer.get(baseIndex + 1);
            float z1 = floatBuffer.get(baseIndex + 2);
            
            // p1
            int v2Index = baseIndex + vertexSizeInFloats;
            float x2 = floatBuffer.get(v2Index);
            float y2 = floatBuffer.get(v2Index + 1);
            float z2 = floatBuffer.get(v2Index + 2);
            
            // center
            centroids[i] = new Vector3f(
                (x1 + x2) * 0.5f,
                (y1 + y2) * 0.5f,
                (z1 + z2) * 0.5f
            );
        }
        
        return centroids;
    }
    
    // others
    private static Vector3f[] extractGenericCentroids(FloatBuffer floatBuffer, int primitiveCount,
                                                     int vertexSizeInFloats, int positionOffset,
                                                     int verticesPerPrimitive) {
        Vector3f[] centroids = new Vector3f[primitiveCount];
        int primitiveStride = vertexSizeInFloats * verticesPerPrimitive;
        float invVertexCount = 1.0f / verticesPerPrimitive;
        
        for (int i = 0; i < primitiveCount; i++) {
            int baseIndex = i * primitiveStride + positionOffset;
            
            float sumX = 0, sumY = 0, sumZ = 0;
            for (int v = 0; v < verticesPerPrimitive; v++) {
                int vertexIndex = baseIndex + v * vertexSizeInFloats;
                sumX += floatBuffer.get(vertexIndex);
                sumY += floatBuffer.get(vertexIndex + 1);
                sumZ += floatBuffer.get(vertexIndex + 2);
            }
            
            centroids[i] = new Vector3f(
                sumX * invVertexCount,
                sumY * invVertexCount,
                sumZ * invVertexCount
            );
        }
        
        return centroids;
    }
}