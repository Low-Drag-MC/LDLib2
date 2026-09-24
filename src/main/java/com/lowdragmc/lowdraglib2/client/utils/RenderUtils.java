package com.lowdragmc.lowdraglib2.client.utils;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderPipelines;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.StagedVertexBuffer;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

public class RenderUtils {
    private static final RenderType BLOCK_OVERLAY = RenderType.create(
            "ldlib_block_overlay",
            RenderSetup.builder(LDLibRenderPipelines.BLOCK_OVERLAY)
                    .createRenderSetup()
    );

    /**
     * Immediate-mode drawing for a handful of render types at once, into a render pass that is already
     * open: geometry is collected per {@link RenderType} into its own {@link BufferBuilder}, and on
     * {@link #flush()} each mesh is uploaded to a transient {@link GpuBuffer} and drawn through
     * {@code RenderType.prepare().drawFromBuffer(...)}, which captures the current model-view and
     * projection. The pass decides where it lands — a scene hook passes
     * {@code SceneRenderContext#renderPass()}. Each render type gets its own backing
     * {@link ByteBufferBuilder}, so interleaved {@link #getBuffer} calls cannot clobber each other.
     *
     * <p><b>Submission order is preserved, and callers may rely on it.</b> Within a render type,
     * because a {@link BufferBuilder} appends in call order and the mesh is drawn in that order;
     * across render types, because {@code builders} is insertion-ordered and {@link #flush()} walks
     * it. That is what makes this usable for translucent geometry drawn with the depth test off,
     * where back-to-front is the caller's to choose — see
     * {@code TransformGizmo#POSITION_COLOR_NO_DEPTH}.
     *
     * <p>⚠️ Consequently {@code RenderType.sortOnUpload()} is <b>not</b> honoured here. Vanilla's
     * batching sorts quads by distance from the origin of the space the vertices were written in,
     * which is only meaningful when that space is the camera's; for world-space geometry it reorders
     * by a number that means nothing. A render type drawn through this class should not ask for it.
     */
    public static final class ImmediateDraw implements AutoCloseable {
        private final RenderPass renderPass;
        private final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
        private final Map<RenderType, ByteBufferBuilder> scratch = new LinkedHashMap<>();

        public ImmediateDraw(RenderPass renderPass) {
            this.renderPass = renderPass;
        }

        public VertexConsumer getBuffer(RenderType renderType) {
            return builders.computeIfAbsent(renderType, rt -> {
                var bb = new ByteBufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE);
                scratch.put(rt, bb);
                return new BufferBuilder(bb, rt.primitiveTopology(), rt.format());
            });
        }

        public void flush() {
            for (var entry : builders.entrySet()) {
                MeshData mesh = entry.getValue().build();
                if (mesh != null) {
                    drawMesh(renderPass, entry.getKey(), mesh);
                }
            }
            builders.clear();
            scratch.values().forEach(ByteBufferBuilder::close);
            scratch.clear();
        }

        @Override
        public void close() {
            flush();
        }
    }

    /**
     * Upload a built {@link MeshData} and draw it once through {@code renderType} into {@code renderPass}
     * (closes the mesh). The transient buffers are released straight after: the draw has been issued,
     * and the device keeps what an in-flight command still reads.
     */
    public static void drawMesh(RenderPass renderPass, RenderType renderType, MeshData mesh) {
        try (mesh) {
            ByteBuffer vb = mesh.vertexBuffer();
            if (vb == null) return;
            var drawState = mesh.drawState();
            var device = RenderSystem.getDevice();
            GpuBuffer vertexBuffer = device.createBuffer(() -> "ldlib immediate vbo", GpuBuffer.USAGE_VERTEX, vb);
            GpuBuffer ownIndexBuffer = null;
            try {
                GpuBuffer indexBuffer;
                IndexType indexType;
                ByteBuffer ib = mesh.indexBuffer();
                if (ib != null) {
                    ownIndexBuffer = device.createBuffer(() -> "ldlib immediate ibo", GpuBuffer.USAGE_INDEX, ib);
                    indexBuffer = ownIndexBuffer;
                    indexType = drawState.indexType();
                } else {
                    var seq = RenderSystem.getSequentialBuffer(drawState.primitiveTopology());
                    indexBuffer = seq.getBuffer(drawState.indexCount());
                    indexType = seq.type();
                }
                renderType.prepare().drawFromBuffer(new StagedVertexBuffer.ExecuteInfo(vertexBuffer, indexBuffer, indexType,
                        0, 0, drawState.indexCount(), drawState.primitiveTopology()), renderPass);
            } finally {
                vertexBuffer.close();
                if (ownIndexBuffer != null) ownIndexBuffer.close();
            }
        }
    }

    /** Emit geometry into a single {@code renderType} and draw it immediately into {@code renderPass}. */
    public static void drawImmediate(RenderPass renderPass, RenderType renderType, Consumer<VertexConsumer> emit) {
        try (var draw = new ImmediateDraw(renderPass)) {
            emit.accept(draw.getBuffer(renderType));
        }
    }

    public static void renderBlockOverLay(RenderPass renderPass, @Nonnull PoseStack poseStack, BlockPos pos, float r, float g, float b, float scale) {
        if (pos == null) return;

        poseStack.pushPose();
        poseStack.translate((pos.getX() + 0.5), (pos.getY() + 0.5), (pos.getZ() + 0.5));
        poseStack.scale(scale, scale, scale);

        drawImmediate(renderPass, BLOCK_OVERLAY, buffer ->
                RenderUtils.renderCubeFace(poseStack, buffer, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, r, g, b, 1));

        poseStack.popPose();
    }

    public static void renderCubeFace(PoseStack poseStack, VertexConsumer buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a) {
        Matrix4f mat = poseStack.last().pose();
        buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, minX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, minX, maxY, minZ).setColor(r, g, b, a);

        buffer.addVertex(mat, maxX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, minY, maxZ).setColor(r, g, b, a);

        buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, minX, minY, maxZ).setColor(r, g, b, a);

        buffer.addVertex(mat, minX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(r, g, b, a);

        buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, minX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, minY, minZ).setColor(r, g, b, a);

        buffer.addVertex(mat, minX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(r, g, b, a);
    }

    public static void moveToFace(PoseStack poseStack, double x, double y, double z, Direction face) {
        poseStack.translate(x + 0.5 + face.getStepX() * 0.5, y + 0.5 + face.getStepY() * 0.5, z + 0.5 + face.getStepZ() * 0.5);
    }

    public static void rotateToFace(PoseStack poseStack, Direction face, @Nullable Direction spin) {
        float angle = spin == Direction.EAST ? Mth.HALF_PI : spin == Direction.SOUTH ? Mth.PI : spin == Direction.WEST ? -Mth.HALF_PI : 0;
        switch (face) {
            case UP -> {
                poseStack.scale(1.0f, -1.0f, 1.0f);
                poseStack.rotate(new Quaternionf().rotateAxis(Mth.HALF_PI, new Vector3f(1, 0, 0)));
                poseStack.rotate(new Quaternionf().rotateAxis(angle, new Vector3f(0, 0, 1)));
            }
            case DOWN -> {
                poseStack.scale(1.0f, -1.0f, 1.0f);
                poseStack.rotate(new Quaternionf().rotateAxis(-Mth.HALF_PI, new Vector3f(1, 0, 0)));
                poseStack.rotate(new Quaternionf().rotateAxis(spin == Direction.EAST ? Mth.HALF_PI : spin == Direction.NORTH ? Mth.PI : spin == Direction.WEST ? -Mth.HALF_PI : 0, new Vector3f(0, 0, 1)));
            }
            case EAST -> {
                poseStack.scale(-1.0f, -1.0f, -1.0f);
                poseStack.rotate(new Quaternionf().rotateAxis(-Mth.HALF_PI, new Vector3f(0, 1, 0)));
                poseStack.rotate(new Quaternionf().rotateAxis(angle, new Vector3f(0, 0, 1)));
            }
            case WEST -> {
                poseStack.scale(-1.0f, -1.0f, -1.0f);
                poseStack.rotate(new Quaternionf().rotateAxis(Mth.HALF_PI, new Vector3f(0, 1, 0)));
                poseStack.rotate(new Quaternionf().rotateAxis(angle, new Vector3f(0, 0, 1)));
            }
            case NORTH -> {
                poseStack.scale(-1.0f, -1.0f, -1.0f);
                poseStack.rotate(new Quaternionf().rotateAxis(angle, new Vector3f(0, 0, 1)));
            }
            case SOUTH -> {
                poseStack.scale(-1.0f, -1.0f, -1.0f);
                poseStack.rotate(new Quaternionf().rotateAxis(Mth.PI, new Vector3f(0, 1, 0)));
                poseStack.rotate(new Quaternionf().rotateAxis(angle, new Vector3f(0, 0, 1)));
            }
            default -> {
            }
        }
    }
}
