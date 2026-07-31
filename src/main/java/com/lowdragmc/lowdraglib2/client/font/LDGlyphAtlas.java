package com.lowdragmc.lowdraglib2.client.font;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A single-channel (R8) glyph atlas made of one or more square pages.
 * <p>
 * Compared to vanilla's {@link net.minecraft.client.gui.font.FontTexture} (fixed 256x256, one texture per page,
 * one render type per texture) the pages here are much larger, so a whole screen worth of text usually lives on
 * a single page and therefore collapses into a single draw call.
 * <p>
 * Distance field pages are filtered linearly, which the field reconstruction depends on; pages holding glyphs
 * rasterized at their drawn size use nearest sampling, since one texel is one device pixel there and filtering
 * would only soften them.
 */
public class LDGlyphAtlas implements AutoCloseable {
    /**
     * Gutter kept between neighbouring glyphs so linear filtering never blends two glyphs together.
     */
    private static final int GUTTER = 1;
    /**
     * Size of the fully opaque block reserved on the first page, used for text effects (underline,
     * strikethrough, background) so they batch together with the glyphs instead of needing their own texture.
     * <p>
     * It is claimed as the very first allocation on page 0 rather than on the first underlined string, so it
     * cannot fail to be placed once that page has filled up with glyphs.
     */
    private static final int WHITE_BLOCK = 8;

    private final Identifier namePrefix;
    private final int pageSize;
    private final boolean linear;
    private final List<Page> pages = new ArrayList<>();
    private final int[] scratch = new int[2];

    @Nullable
    private Slot whiteSlot;

    /**
     * @param namePrefix texture id prefix, pages are registered as {@code <prefix>/<index>}
     * @param pageSize   edge length of a page in pixels
     * @param linear     whether pages should be sampled with linear filtering (true for SDF pages, false for
     *                   exact-size raster pages)
     */
    public LDGlyphAtlas(Identifier namePrefix, int pageSize, boolean linear) {
        this.namePrefix = namePrefix;
        this.pageSize = pageSize;
        this.linear = linear;
    }

    /**
     * A region of a page holding one glyph bitmap.
     */
    public record Slot(Page page, int x, int y, int width, int height) {
        public float u0() {
            return (float) x / page.size;
        }

        public float u1() {
            return (float) (x + width) / page.size;
        }

        public float v0() {
            return (float) y / page.size;
        }

        public float v1() {
            return (float) (y + height) / page.size;
        }
    }

    /**
     * Uploads a single channel bitmap and returns the region it was placed in.
     *
     * @param width  bitmap width
     * @param height bitmap height
     * @param data   {@code width * height} bytes, row major, top row first
     * @return the allocated slot, or null if the bitmap does not fit on an empty page at all
     */
    @Nullable
    public Slot add(int width, int height, byte[] data) {
        RenderSystem.assertOnRenderThread();
        if (width <= 0 || height <= 0) return null;
        // a bitmap too large for an empty page can never be placed, and falling through to newPage() would
        // leak one full size texture per attempt
        if (width + GUTTER > pageSize || height + GUTTER > pageSize) return null;
        for (var page : pages) {
            var slot = page.tryAdd(width, height, data);
            if (slot != null) return slot;
        }
        var page = newPage();
        return page.tryAdd(width, height, data);
    }

    /**
     * The fully opaque region used to render text effects. Lazily creates the first page, which is what
     * reserves the block.
     */
    public Slot whiteSlot() {
        if (whiteSlot == null) {
            // page 0 is the one that reserves it, and it only stays null while there are no pages at all
            newPage();
        }
        return Objects.requireNonNull(whiteSlot);
    }

    public int pageCount() {
        return pages.size();
    }

    /**
     * @return bytes of texture memory this atlas is holding, one byte per texel since pages are single channel
     */
    public long byteSize() {
        return (long) pages.size() * pageSize * pageSize;
    }

    private Page newPage() {
        var name = namePrefix.withSuffix("/" + pages.size());
        var page = new Page(name, pageSize, linear);
        var first = pages.isEmpty();
        pages.add(page);
        Minecraft.getInstance().getTextureManager().register(name, page);
        if (first) {
            reserveWhiteBlock(page);
        }
        return page;
    }

    /**
     * Claims the white block on a freshly created page 0, where 8x8 pixels always fit.
     */
    private void reserveWhiteBlock(Page page) {
        var data = new byte[WHITE_BLOCK * WHITE_BLOCK];
        Arrays.fill(data, (byte) 0xFF);
        var slot = page.tryAdd(WHITE_BLOCK, WHITE_BLOCK, data);
        if (slot == null) {
            throw new IllegalStateException("Could not reserve the white block on an empty glyph atlas page");
        }
        // inset by 2px so linear filtering never reaches the transparent gutter
        whiteSlot = new Slot(slot.page(), slot.x() + 2, slot.y() + 2, WHITE_BLOCK - 4, WHITE_BLOCK - 4);
    }

    @Override
    public void close() {
        var textureManager = Minecraft.getInstance().getTextureManager();
        for (var page : pages) {
            // release() closes the texture it is holding, so the page must not be closed again here
            textureManager.release(page.name);
        }
        pages.clear();
        whiteSlot = null;
    }

    /**
     * One atlas page, backed by an R8 texture.
     */
    public class Page extends AbstractTexture {
        private final Identifier name;
        private final int size;
        private final GlyphRenderTypes renderTypes;
        private final SkylinePacker packer;

        private Page(Identifier name, int size, boolean linear) {
            this.name = name;
            this.size = size;
            this.renderTypes = LDTextRenderTypes.of(name, linear);
            this.packer = new SkylinePacker(size, size);
            var device = RenderSystem.getDevice();
            var filter = linear ? FilterMode.LINEAR : FilterMode.NEAREST;
            this.texture = device.createTexture(name::toString,
                    GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                    GpuFormat.R8_UNORM, size, size, 1, 1);
            this.textureView = device.createTextureView(this.texture);
            // clamp so a glyph on the edge of a page never wraps around to the opposite side
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(filter);
            // a freshly created texture has undefined contents, so clear it or unused areas bleed into a glyph
            var zero = MemoryUtil.memCalloc(size * size);
            try {
                upload(0, 0, size, size, zero);
            } finally {
                MemoryUtil.memFree(zero);
            }
        }

        public Identifier name() {
            return name;
        }

        public GlyphRenderTypes renderTypes() {
            return renderTypes;
        }

        @Nullable
        private Slot tryAdd(int width, int height, byte[] data) {
            if (!packer.insert(width + GUTTER, height + GUTTER, scratch)) {
                return null;
            }
            var x = scratch[0];
            var y = scratch[1];
            var buffer = MemoryUtil.memAlloc(data.length);
            try {
                buffer.put(data);
                buffer.flip();
                upload(x, y, width, height, buffer);
            } finally {
                MemoryUtil.memFree(buffer);
            }
            return new Slot(this, x, y, width, height);
        }

        private void upload(int x, int y, int width, int height, ByteBuffer pixels) {
            // 26.2 takes no source format: the layout is read off the destination, and an R8_UNORM page
            // is one byte per texel, which is exactly what the single channel bitmaps are.
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(
                    getTexture(), pixels, 0, 0, x, y, width, height);
        }
    }
}
