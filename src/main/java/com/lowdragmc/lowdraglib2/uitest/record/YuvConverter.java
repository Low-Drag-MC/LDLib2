package com.lowdragmc.lowdraglib2.uitest.record;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderPipelines;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import org.lwjgl.system.MemoryStack;

import java.util.Optional;

/**
 * Converts a region of a colour texture into one I420 frame (BT.709, limited range) on the GPU, so the
 * CPU downloads 1.5 bytes a pixel instead of 4 and ffmpeg has no colour conversion left to do.
 *
 * <p>The frame is laid out exactly as ffmpeg's {@code yuv420p} expects it in memory, as the bytes of an
 * {@link GpuFormat#R8_UNORM} texture read back row after row: each fragment computes which byte of the
 * Y, U or V plane it is from its linear index. The texture is {@link #layoutWidth} wide rather than the
 * frame's width, a multiple of four, because {@code CommandEncoder#copyTextureToBuffer} reads with GL's
 * default pack alignment of four under OpenGL and would pad every row of an odd-multiple-of-two width; the planes
 * simply wrap across its rows, and the bytes past {@link #frameBytes} are never read.
 *
 * <p>Goes through the GPU abstraction rather than raw GL: {@link LDLibRenderPipelines#UITEST_I420} (shader
 * {@code ldlib2:uitest/i420}, after the game's own {@code core/screenquad}), a render pass into a texture of its own,
 * and a uniform buffer. Nothing it does touches state the game's own passes rely on, so there is nothing to save and
 * restore around it. Rows are read bottom-up, as the game's own {@code Screenshot} reads its target on either backend.
 */
final class YuvConverter implements AutoCloseable {

    /** vec4 + ivec2 + int + int, std140. */
    private static final int PARAMS_SIZE = 32;

    final int width;
    final int height;
    /** Bytes of one I420 frame, what ffmpeg reads per frame. */
    final int frameBytes;
    /** Width of the texture the frame is laid out in, a multiple of four. */
    final int layoutWidth;
    final int layoutRows;
    /** Bytes {@link #texture()} reads back as, at least {@link #frameBytes}. */
    final int readbackBytes;

    private final GpuTexture texture;
    private final GpuTextureView textureView;
    private final GpuBuffer params;

    /** @param width output width, even */
    YuvConverter(int width, int height) {
        if (width <= 0 || height <= 0 || (width & 1) != 0 || (height & 1) != 0) {
            throw new IllegalArgumentException("I420 needs a positive, even frame size, got " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.frameBytes = width * height * 3 / 2;
        this.layoutWidth = (width + 3) & ~3;
        this.layoutRows = (frameBytes + layoutWidth - 1) / layoutWidth;
        this.readbackBytes = layoutWidth * layoutRows;

        var device = RenderSystem.getDevice();
        if (layoutRows > device.getDeviceInfo().limits().maxTextureSizeForFormat(GpuFormat.R8_UNORM)) {
            throw new IllegalArgumentException("a " + width + "x" + height + " video does not fit in one texture here");
        }
        texture = device.createTexture(() -> "ldlib2 recording i420",
                GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC, GpuFormat.R8_UNORM, layoutWidth, layoutRows, 1, 1);
        textureView = device.createTextureView(texture);
        params = device.createBuffer(() -> "ldlib2 recording i420 params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, PARAMS_SIZE);
    }

    /** The converted frame, to be read back with {@code copyTextureToBuffer}. */
    GpuTexture texture() {
        return texture;
    }

    /**
     * Converts {@code region} of {@code source}, scaled to this converter's size, into {@link #texture()}.
     *
     * @param regionX from the source's top-left corner, in texels; fractional for a moving camera
     */
    void convert(GpuTextureView source, float regionX, float regionY, float regionWidth, float regionHeight) {
        var device = RenderSystem.getDevice();
        var pipeline = RenderSystem.getCompiledPipelineNullable(LDLibRenderPipelines.UITEST_I420);
        if (pipeline == null) {
            throw new IllegalStateException("the I420 conversion shader did not compile, see the log");
        }

        boolean identity = regionWidth == width && regionHeight == height
                && regionX == Math.floor(regionX) && regionY == Math.floor(regionY);
        var encoder = device.createCommandEncoder();
        try (var stack = MemoryStack.stackPush()) {
            var data = Std140Builder.onStack(stack, PARAMS_SIZE)
                    .putVec4(regionX, regionY, regionWidth, regionHeight)
                    .putIVec2(width, height)
                    .putInt(identity ? 1 : 0)
                    .putInt(layoutWidth)
                    .get();
            encoder.writeToBuffer(params.slice(), data);
        }
        try (var pass = encoder.createRenderPass(() -> "ldlib2 recording i420", textureView, Optional.empty())) {
            pass.setPipeline(pipeline);
            pass.setUniform("Source", source, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.setUniform("I420Params", params);
            pass.draw(3, 1, 0, 0);
        }
    }

    @Override
    public void close() {
        textureView.close();
        texture.close();
        params.close();
    }
}
