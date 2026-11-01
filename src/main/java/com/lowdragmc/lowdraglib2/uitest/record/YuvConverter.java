package com.lowdragmc.lowdraglib2.uitest.record;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;

import java.util.OptionalInt;

/**
 * Converts a region of a colour texture into one I420 frame (BT.709, limited range) on the GPU, so the
 * CPU downloads 1.5 bytes a pixel instead of 4 and ffmpeg has no colour conversion left to do.
 *
 * <p>The frame is laid out exactly as ffmpeg's {@code yuv420p} expects it in memory, as the bytes of an
 * {@link TextureFormat#RED8} texture read back row after row: each fragment computes which byte of the
 * Y, U or V plane it is from its linear index. The texture is {@link #layoutWidth} wide rather than the
 * frame's width, a multiple of four, because {@code CommandEncoder#copyTextureToBuffer} reads with GL's
 * default pack alignment of four and would pad every row of an odd-multiple-of-two width; the planes
 * simply wrap across its rows, and the bytes past {@link #frameBytes} are never read.
 *
 * <p>Goes through the GPU abstraction rather than raw GL: a pipeline compiled from the shader sources
 * below, a render pass into a texture of its own, and a uniform buffer. Nothing it does touches state
 * the game's own passes rely on, so there is nothing to save and restore around it. The pipeline is not
 * registered as a resource: its sources live here and are handed to the device on every use, which is a
 * cache hit after the first, and survives a resource reload clearing the pipeline cache.
 */
final class YuvConverter implements AutoCloseable {

    private static final Identifier VERTEX = LDLib2.id("uitest/i420_quad");
    private static final Identifier FRAGMENT = LDLib2.id("uitest/i420");

    private static final String VERTEX_SOURCE = """
            #version 330
            void main() {
                vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
            }
            """;

    private static final String FRAGMENT_SOURCE = """
            #version 330
            uniform sampler2D Source;

            layout(std140) uniform I420Params {
                vec4 Region;
                ivec2 OutSize;
                int Identity;
                int LayoutWidth;
            };

            out vec4 fragColor;

            vec3 texel(ivec2 p) {
                ivec2 size = textureSize(Source, 0);
                p = clamp(p, ivec2(0), size - 1);
                return texelFetch(Source, ivec2(p.x, size.y - 1 - p.y), 0).rgb;
            }

            vec3 at(vec2 o) {
                if (Identity != 0) return texel(ivec2(Region.xy) + ivec2(o));
                vec2 p = Region.xy + (o + 0.5) * Region.zw / vec2(OutSize) - 0.5;
                ivec2 i = ivec2(floor(p));
                vec2 f = p - vec2(i);
                return mix(mix(texel(i), texel(i + ivec2(1, 0)), f.x),
                           mix(texel(i + ivec2(0, 1)), texel(i + ivec2(1, 1)), f.x), f.y);
            }

            void main() {
                int w = OutSize.x;
                int h = OutSize.y;
                int index = int(gl_FragCoord.y) * LayoutWidth + int(gl_FragCoord.x);
                int lumaSize = w * h;
                int cw = w / 2;
                int chromaSize = cw * (h / 2);
                float value;
                if (index < lumaSize) {
                    vec3 c = at(vec2(index % w, index / w));
                    value = 16.0 + 219.0 * dot(c, vec3(0.2126, 0.7152, 0.0722));
                } else if (index < lumaSize + 2 * chromaSize) {
                    int j = index - lumaSize;
                    bool isV = j >= chromaSize;
                    if (isV) j -= chromaSize;
                    vec2 o = vec2(j % cw, j / cw) * 2.0;
                    vec3 c = (at(o) + at(o + vec2(1.0, 0.0)) + at(o + vec2(0.0, 1.0)) + at(o + vec2(1.0, 1.0))) * 0.25;
                    value = isV ? 128.0 + 224.0 * dot(c, vec3(0.5, -0.45415, -0.04585))
                                : 128.0 + 224.0 * dot(c, vec3(-0.11457, -0.38543, 0.5));
                } else {
                    value = 0.0;
                }
                fragColor = vec4(value / 255.0, 0.0, 0.0, 1.0);
            }
            """;

    private static final ShaderSource SOURCES = (id, type) -> {
        if (type == ShaderType.VERTEX && id.equals(VERTEX)) return VERTEX_SOURCE;
        if (type == ShaderType.FRAGMENT && id.equals(FRAGMENT)) return FRAGMENT_SOURCE;
        return null;
    };

    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
            .withLocation(LDLib2.id("pipeline/uitest_i420"))
            .withVertexShader(VERTEX)
            .withFragmentShader(FRAGMENT)
            .withSampler("Source")
            .withUniform("I420Params", UniformType.UNIFORM_BUFFER)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
            .build();

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
        if (layoutRows > device.getMaxTextureSize()) {
            throw new IllegalArgumentException("a " + width + "x" + height + " video does not fit in one texture here");
        }
        texture = device.createTexture(() -> "ldlib2 recording i420",
                GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC, TextureFormat.RED8, layoutWidth, layoutRows, 1, 1);
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
        // the pipeline cache is keyed by the pipeline object, so this compiles once and is a lookup after
        if (!device.precompilePipeline(PIPELINE, SOURCES).isValid()) {
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
        try (var pass = encoder.createRenderPass(() -> "ldlib2 recording i420", textureView, OptionalInt.empty())) {
            pass.setPipeline(PIPELINE);
            pass.bindTexture("Source", source, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.setUniform("I420Params", params);
            pass.draw(0, 3);
        }
    }

    @Override
    public void close() {
        textureView.close();
        texture.close();
        params.close();
    }
}
