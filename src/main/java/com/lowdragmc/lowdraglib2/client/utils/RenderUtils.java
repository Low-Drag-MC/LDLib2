package com.lowdragmc.lowdraglib2.client.utils;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderPipelines;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

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
     * 26.2 immediate-draw replacement for the removed {@code MultiBufferSource.BufferSource}: collects
     * geometry per {@link RenderType} into its own {@link BufferBuilder}, then on {@link #flush()}
     * uploads each mesh to a transient {@link GpuBuffer} and draws it via
     * {@code RenderType.prepare().drawFromBuffer(...)} — which captures the current model-view/projection
     * and honors {@code RenderSystem.outputColor/DepthTextureOverride} (so scene-FBO draws land correctly).
     * Each RenderType gets its own backing {@link ByteBufferBuilder} so interleaved {@code getBuffer}
     * calls don't clobber each other.
     */
    public static final class ImmediateDraw implements AutoCloseable {
        private final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
        private final Map<RenderType, ByteBufferBuilder> scratch = new LinkedHashMap<>();

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
                    drawMesh(entry.getKey(), mesh);
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

    /** Upload a built {@link MeshData} and draw it once through {@code renderType} (closes the mesh). */
    public static void drawMesh(RenderType renderType, MeshData mesh) {
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
                renderType.prepare().drawFromBuffer(vertexBuffer, indexBuffer, indexType, 0, 0, drawState.indexCount());
            } finally {
                vertexBuffer.close();
                if (ownIndexBuffer != null) ownIndexBuffer.close();
            }
        }
    }

    /** Emit geometry into a single {@code renderType} and draw it immediately. */
    public static void drawImmediate(RenderType renderType, Consumer<VertexConsumer> emit) {
        try (var draw = new ImmediateDraw()) {
            emit.accept(draw.getBuffer(renderType));
        }
    }

    /***
     * used to render pixels in stencil mask. (e.g. Restrict rendering results to be displayed only in Monitor Screens)
     * if you want to do the similar things in Gui(2D) not World(3D)
     * that you don't need to draw mask to build a rect mask easily.
     * @param mask draw mask
     * @param renderInMask rendering in the mask
     * @param renderMaskVisible should mask be rendered too
     *
     * @deprecated Drives the stencil through raw {@code GL11} calls against whatever framebuffer
     *             happens to be bound. Neither half of that survives 26.2: stencil state belongs to
     *             the pipeline and the render pass now, and on the Vulkan backend there is no GL
     *             context on the render thread, so LWJGL throws out of the first call rather than
     *             quietly doing nothing. Mask a UI with a clip rectangle, and anything in the world
     *             through a render pipeline that declares its own stencil state.
     */
    @Deprecated(since = "26.2.2.35", forRemoval = true)
    public static void useStencil(Runnable mask, Runnable renderInMask, boolean renderMaskVisible) {
        GL11.glStencilMask(0xFF);
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glEnable(GL11.GL_STENCIL_TEST);

        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        if (!renderMaskVisible) {
            GL11.glColorMask(false, false, false, false);
            GL11.glDepthMask(false);
        }

        mask.run();

        if (!renderMaskVisible) {
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
        }

        GL11.glStencilMask(0x00);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        renderInMask.run();

        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    public static void renderBlockOverLay(@Nonnull PoseStack poseStack, BlockPos pos, float r, float g, float b, float scale) {
        if (pos == null) return;

        poseStack.pushPose();
        poseStack.translate((pos.getX() + 0.5), (pos.getY() + 0.5), (pos.getZ() + 0.5));
        poseStack.scale(scale, scale, scale);

        drawImmediate(BLOCK_OVERLAY, buffer ->
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
                poseStack.mulPose(new Quaternionf().rotateAxis(Mth.HALF_PI, new Vector3f(1, 0, 0)));
                poseStack.mulPose(new Quaternionf().rotateAxis(angle, new Vector3f(0, 0, 1)));
            }
            case DOWN -> {
                poseStack.scale(1.0f, -1.0f, 1.0f);
                poseStack.mulPose(new Quaternionf().rotateAxis(-Mth.HALF_PI, new Vector3f(1, 0, 0)));
                poseStack.mulPose(new Quaternionf().rotateAxis(spin == Direction.EAST ? Mth.HALF_PI : spin == Direction.NORTH ? Mth.PI : spin == Direction.WEST ? -Mth.HALF_PI : 0, new Vector3f(0, 0, 1)));
            }
            case EAST -> {
                poseStack.scale(-1.0f, -1.0f, -1.0f);
                poseStack.mulPose(new Quaternionf().rotateAxis(-Mth.HALF_PI, new Vector3f(0, 1, 0)));
                poseStack.mulPose(new Quaternionf().rotateAxis(angle, new Vector3f(0, 0, 1)));
            }
            case WEST -> {
                poseStack.scale(-1.0f, -1.0f, -1.0f);
                poseStack.mulPose(new Quaternionf().rotateAxis(Mth.HALF_PI, new Vector3f(0, 1, 0)));
                poseStack.mulPose(new Quaternionf().rotateAxis(angle, new Vector3f(0, 0, 1)));
            }
            case NORTH -> {
                poseStack.scale(-1.0f, -1.0f, -1.0f);
                poseStack.mulPose(new Quaternionf().rotateAxis(angle, new Vector3f(0, 0, 1)));
            }
            case SOUTH -> {
                poseStack.scale(-1.0f, -1.0f, -1.0f);
                poseStack.mulPose(new Quaternionf().rotateAxis(Mth.PI, new Vector3f(0, 1, 0)));
                poseStack.mulPose(new Quaternionf().rotateAxis(angle, new Vector3f(0, 0, 1)));
            }
            default -> {
            }
        }
    }
}
