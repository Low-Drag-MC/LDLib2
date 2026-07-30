package com.lowdragmc.lowdraglib2.client.font;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A single-channel (R8) glyph atlas made of one or more square pages.
 * <p>
 * Compared to vanilla's {@link net.minecraft.client.gui.font.FontTexture} (fixed 256x256, one texture per page,
 * one {@link net.minecraft.client.renderer.RenderType} per texture) the pages here are much larger, so a whole
 * screen worth of text usually lives on a single page and therefore collapses into a single draw call.
 * <p>
 * Distance field pages are filtered with {@code GL_LINEAR}, which the field reconstruction depends on;
 * pages holding glyphs rasterized at their drawn size use {@code GL_NEAREST}, since one texel is one device
 * pixel there and filtering would only soften them.
 */
@OnlyIn(Dist.CLIENT)
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

    private final ResourceLocation namePrefix;
    private final int pageSize;
    private final boolean linear;
    private final Function<ResourceLocation, GlyphRenderTypes> renderTypesFactory;
    private final List<Page> pages = new ArrayList<>();
    private final int[] scratch = new int[2];

    @Nullable
    private Slot whiteSlot;

    /**
     * @param namePrefix         texture id prefix, pages are registered as {@code <prefix>/<index>}
     * @param pageSize           edge length of a page in pixels
     * @param linear             whether pages should be sampled with linear filtering (true for SDF pages,
     *                           false for exact-size raster pages)
     * @param renderTypesFactory builds the render types used by glyphs baked into a given page texture
     */
    public LDGlyphAtlas(ResourceLocation namePrefix, int pageSize, boolean linear,
                        Function<ResourceLocation, GlyphRenderTypes> renderTypesFactory) {
        this.namePrefix = namePrefix;
        this.pageSize = pageSize;
        this.linear = linear;
        this.renderTypesFactory = renderTypesFactory;
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
        var page = new Page(name, pageSize, linear, renderTypesFactory.apply(name));
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
            textureManager.release(page.name);
            page.close();
        }
        pages.clear();
        whiteSlot = null;
    }

    /**
     * One atlas page, backed by a GL_R8 texture.
     */
    @OnlyIn(Dist.CLIENT)
    public class Page extends AbstractTexture {
        private final ResourceLocation name;
        private final int size;
        private final GlyphRenderTypes renderTypes;
        private final SkylinePacker packer;

        private Page(ResourceLocation name, int size, boolean linear, GlyphRenderTypes renderTypes) {
            this.name = name;
            this.size = size;
            this.renderTypes = renderTypes;
            this.packer = new SkylinePacker(size, size);
            TextureUtil.prepareImage(NativeImage.InternalGlFormat.RED, this.getId(), size, size);
            this.bind();
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            this.setFilter(linear, false);
            // prepareImage leaves the content undefined, clear it so unused areas never bleed into a glyph
            var zero = MemoryUtil.memCalloc(size * size);
            try {
                upload(0, 0, size, size, MemoryUtil.memAddress(zero));
            } finally {
                MemoryUtil.memFree(zero);
            }
        }

        public ResourceLocation name() {
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
                this.bind();
                upload(x, y, width, height, MemoryUtil.memAddress(buffer));
            } finally {
                MemoryUtil.memFree(buffer);
            }
            return new Slot(this, x, y, width, height);
        }

        private void upload(int x, int y, int width, int height, long pixels) {
            // R8 rows are not 4 byte aligned, so the default unpack alignment would shear the upload
            GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 1);
            GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GlStateManager._texSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, width, height,
                    GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, pixels);
            GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);
        }

        @Override
        public void load(ResourceManager resourceManager) {
        }

        @Override
        public void close() {
            this.releaseId();
        }
    }
}
