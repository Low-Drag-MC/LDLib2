package com.lowdragmc.lowdraglib2.uitest.record;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.RenderTargetScope;
import com.lowdragmc.lowdraglib2.client.font.LDFonts;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGuiRendererExt;
import com.lowdragmc.lowdraglib2.uitest.input.InputDriver;
import com.lowdragmc.lowdraglib2.uitest.input.InputListener;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * What a recorded take shows on top of the UI: the pointer, a ripple where each click lands, the keys
 * being pressed, a burned-in caption and a spotlight. Drawn only into the video frame, never into the
 * game's own framebuffer, so screenshots and the live window are unaffected.
 *
 * <p>The pointer and ripples live in screen space, so a camera zoom magnifies them with the UI. The key
 * display and caption live in the camera's frame and keep their size in the video. Sizes follow the
 * height of the recorded region, within bounds in GUI pixels so they stay in proportion to the UI, and
 * everything animates on video time, so it is identical every time the take is recorded.
 *
 * <p>The game's gui rendering is deferred, so drawing here is two passes, as everywhere else LDLib2 renders a
 * UI off-screen ({@code ModularUIWindow} for one): the overlay is recorded into a {@link GuiRenderState} of
 * its own through an ordinary {@link GUIContext}, then flushed by a gui renderer of its own
 * ({@link IGuiRendererExt#ldlib2$createSubRenderer}) into the composite target the recorder hands over. The
 * game's renderer and its state belong to the frame being presented and are never touched.
 */
public final class RecordingOverlay implements InputListener, AutoCloseable {

    private static final Identifier ARROW_TEXTURE = LDLib2.id("uitest/recording_cursor");
    private static final Identifier IBEAM_TEXTURE = LDLib2.id("uitest/recording_ibeam");
    /** The classic arrow, tip at the origin, in units of a 19-unit-tall pointer. */
    private static final float[] ARROW = {0, 0, 0, 16.4f, 3.7f, 12.9f, 6.4f, 18.9f, 8.9f, 17.8f, 6.3f, 12.1f, 11.3f, 12.1f};
    private static final float ARROW_HEIGHT = 18.9f;
    /** An I-beam as three boxes, {@code centreX, centreY, halfWidth, halfHeight}, centred on the origin. */
    private static final float[] IBEAM = {0, 0, 0.6f, 8.4f, 0, -8.4f, 2.6f, 0.6f, 0, 8.4f, 2.6f, 0.6f};
    private static final float IBEAM_HEIGHT = 18f;

    private static final long RIPPLE_NANOS = 420_000_000L;
    private static final float SPOTLIGHT_SECONDS = 0.22f;

    private final InputDriver driver;
    private final List<Ripple> ripples = new ArrayList<>();
    private final KeyDisplay keys = new KeyDisplay();
    private final SDFRectTexture shape = new SDFRectTexture();
    private int buttonsDown;
    private final CursorSprite arrow = new CursorSprite(ARROW_TEXTURE, false);
    private final CursorSprite ibeam = new CursorSprite(IBEAM_TEXTURE, true);
    private long lastDrawNanos;
    private float baseHeight;
    private float regionX;
    private float regionY;
    private float regionWidth;
    private float regionHeight;
    private boolean showCursor = true;
    private boolean showClicks = true;
    private boolean showKeys = true;
    @Nullable
    private String caption;
    private float captionAlpha;
    @Nullable
    private String fadingCaption;
    @Nullable
    private Supplier<float[]> spotlight;
    @Nullable
    private float[] spotlightBounds;
    private float spotlightAlpha;
    /** Flushes the overlay's own render state; built on first draw, closed with the overlay. */
    @Nullable
    private GuiRenderer renderer;

    private record Ripple(float x, float y, int button, long startedAt) {
    }

    public RecordingOverlay(InputDriver driver) {
        this.driver = driver;
    }

    /**
     * The part of the screen the video shows, in GUI-scaled coordinates. The first call fixes the size the
     * overlay is designed for; later ones, from a moving camera, keep the key display and caption the same
     * size in the video.
     */
    public void setRegion(float x, float y, float width, float height) {
        if (baseHeight <= 0) baseHeight = height;
        regionX = x;
        regionY = y;
        regionWidth = width;
        regionHeight = height;
    }

    public void show(boolean cursor, boolean clicks, boolean keys) {
        this.showCursor = cursor;
        this.showClicks = clicks;
        this.showKeys = keys;
    }

    /** Burns this text into the video until the next call; {@code null} or empty clears it. */
    public void setCaption(@Nullable String caption) {
        if (caption != null && caption.isEmpty()) caption = null;
        if (Objects.equals(this.caption, caption)) return;
        if (this.caption != null) fadingCaption = this.caption;
        this.caption = caption;
        if (caption != null) captionAlpha = 0;
    }

    /**
     * Dims everything but a rectangle, {@code x, y, width, height} in GUI-scaled coordinates, read every
     * frame so it follows a moving element; {@code null} lifts the spotlight. A supplier returning
     * {@code null} keeps the last rectangle.
     */
    public void setSpotlight(@Nullable Supplier<float[]> bounds) {
        this.spotlight = bounds;
    }

    // region input

    @Override
    public void mouseDown(float x, float y, int button) {
        buttonsDown |= 1 << button;
        long now = Util.getNanos();
        ripples.add(new Ripple(x, y, button, now));
        keys.mouseDown(button, now);
    }

    @Override
    public void mouseUp(float x, float y, int button) {
        buttonsDown &= ~(1 << button);
    }

    @Override
    public void scroll(float x, float y, double amount) {
        keys.scroll(amount, Util.getNanos());
    }

    @Override
    public void keyDown(int keyCode, int modifiers) {
        keys.keyDown(keyCode, Util.getNanos());
    }

    @Override
    public void keyUp(int keyCode, int modifiers) {
        keys.keyUp(keyCode);
    }

    @Override
    public void charTyped(char codePoint) {
        keys.charTyped(codePoint, Util.getNanos());
    }

    // endregion

    // region drawing

    /**
     * Draws over {@code target}, which must be the size of the game's frame: the overlay is laid out in the
     * game window's GUI-scaled space, which is what the gui renderer projects with when nothing overrides it.
     */
    public void draw(RenderTarget target) {
        var colorView = target.getColorTextureView();
        if (colorView == null) return;
        var minecraft = Minecraft.getInstance();
        double guiScale = minecraft.getWindow().getGuiScale();
        float width = (float) (target.width / guiScale);
        float height = (float) (target.height / guiScale);
        if (regionWidth <= 0 || regionHeight <= 0) setRegion(0, 0, width, height);
        float screenUnit = Framing.keyHeight(baseHeight);
        float frameUnit = screenUnit * regionHeight / baseHeight;
        long now = Util.getNanos();
        float dt = lastDrawNanos == 0 ? 0 : (now - lastDrawNanos) / 1e9f;
        lastDrawNanos = now;

        // pass one: record
        var state = new GuiRenderState();
        var context = GUIContext.of(new GuiGraphicsExtractor(minecraft, state, 0, 0), 0, 0, 0);
        drawSpotlight(context, width, height, screenUnit, dt);
        drawCaption(context, frameUnit, dt);
        if (showKeys) drawKeys(context, frameUnit, now, dt);
        if (showClicks) drawRipples(context, screenUnit, now);
        if (showCursor) drawCursor(context, guiScale, screenUnit);

        // pass two: flush into the target. The target override says where pixels land; the output override is
        // what the render passes actually attach. No window override: the target is the game frame's size,
        // so the game window's metrics already are the right ones for projection and scissor.
        var gui = ensureRenderer();
        ((IGuiRendererExt) (Object) gui).ldlib2$setRenderState(state);
        try (var ignoredOutput = RenderTargetScope.redirect(colorView, target.getDepthTextureView());
             var ignoredTarget = IGuiRendererExt.ldlib2$targetOverride(target)) {
            try {
                gui.render();
            } finally {
                // our own renderer, so our own frame to end
                gui.endFrame();
            }
        }
    }

    /** A gui renderer on the game renderer's shared plumbing, as {@code ModularUIWindow} builds one. */
    private GuiRenderer ensureRenderer() {
        if (renderer == null) {
            renderer = IGuiRendererExt.ldlib2$createSubRenderer(new GuiRenderState());
        }
        return renderer;
    }

    private void drawSpotlight(GUIContext graphics, float width, float height, float unit, float dt) {
        if (spotlight != null) {
            var bounds = spotlight.get();
            if (bounds != null) spotlightBounds = bounds;
        }
        float target = spotlight != null && spotlightBounds != null ? 1 : 0;
        float step = dt <= 0 ? 1 : dt / SPOTLIGHT_SECONDS;
        spotlightAlpha = target > spotlightAlpha ? Math.min(target, spotlightAlpha + step) : Math.max(target, spotlightAlpha - step);
        if (spotlightAlpha <= 0 || spotlightBounds == null) return;
        float a = spotlightAlpha * spotlightAlpha * (3 - 2 * spotlightAlpha);
        var b = spotlightBounds;
        float pad = unit * 0.3f;
        float left = b[0] - pad, top = b[1] - pad, right = b[0] + b[2] + pad, bottom = b[1] + b[3] + pad;
        int dim = withAlpha(0x000000, 0.55f * a);
        fillRect(graphics, 0, 0, width, top, dim);
        fillRect(graphics, 0, bottom, width, height - bottom, dim);
        fillRect(graphics, 0, top, left, bottom - top, dim);
        fillRect(graphics, right, top, width - right, bottom - top, dim);
        shape.setRadius(unit * 0.15f).setStroke(unit * 0.07f).setColor(0).setBorderColor(withAlpha(0xFFFFFF, 0.75f * a));
        graphics.drawTexture(shape, left, top, right - left, bottom - top);
    }

    /** An exact rectangle without anti-aliased edges, so adjacent bands meet without a seam. */
    private static void fillRect(GUIContext graphics, float x, float y, float width, float height, int color) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(RenderPipelines.GUI, x, y, x + width, y + height, color, color, color, color);
    }

    private void drawCaption(GUIContext graphics, float unit, float dt) {
        float step = dt <= 0 ? 1 : dt / 0.15f;
        String text;
        if (caption != null) {
            captionAlpha = Math.min(1, captionAlpha + step);
            text = caption;
        } else if (fadingCaption != null) {
            captionAlpha = Math.max(0, captionAlpha - step);
            if (captionAlpha <= 0) {
                fadingCaption = null;
                return;
            }
            text = fadingCaption;
        } else {
            return;
        }
        var font = LDFonts.font();
        float boxHeight = unit * 1.3f;
        float textScale = unit * 0.62f / font.lineHeight;
        float padding = unit * 0.5f;
        float textWidth = font.width(text) * textScale;
        float maxWidth = regionWidth * 0.92f - padding * 2;
        if (textWidth > maxWidth) {
            textScale *= maxWidth / textWidth;
            textWidth = maxWidth;
        }
        float boxWidth = textWidth + padding * 2;
        float bottom = regionY + regionHeight - (showKeys ? unit * 1.75f : unit * 0.5f);
        float left = regionX + (regionWidth - boxWidth) / 2;
        float top = bottom - boxHeight;
        shape.setRadius(unit * 0.25f).setStroke(0).setColor(withAlpha(0x101216, 0.85f * captionAlpha));
        graphics.drawTexture(shape, left, top, boxWidth, boxHeight);
        if (captionAlpha < 0.03f) return;
        var pose = graphics.pose;
        pose.pushPose();
        pose.translate(left + padding, top + (boxHeight - font.lineHeight * textScale) / 2 + textScale);
        pose.scale(textScale, textScale);
        LDFonts.drawText(graphics, font, Component.literal(text), 0, 0, withAlpha(0xFFFFFF, captionAlpha), false);
        pose.popPose();
    }

    private void drawCursor(GUIContext graphics, double guiScale, float unit) {
        float x = driver.cursorX();
        float y = driver.cursorY();
        if (x < 0 || y < 0) return;
        var sprite = overTextInput(x, y) ? ibeam : arrow;
        sprite.ensure(Math.round(unit * 0.7f * (float) guiScale));
        // rasterised at twice the size it is shown: crisp at 1x, and still sharp under a 2x zoom
        float scale = (float) ((buttonsDown != 0 ? 0.88 : 1.0) / guiScale / 2);
        var pose = graphics.pose;
        pose.pushPose();
        pose.translate(x, y);
        pose.scale(scale, scale);
        pose.translate(-sprite.tipX, -sprite.tipY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, sprite.location, 0, 0, sprite.width, sprite.height, 0, 0, 1, 1, -1);
        pose.popPose();
    }

    private static boolean overTextInput(float x, float y) {
        var ui = ModularUIClientAccess.of(Minecraft.getInstance().gui.screen());
        if (ui == null) return false;
        var hit = ui.hitTestAtScreen(x, y);
        for (int depth = 0; hit != null && depth < 4; depth++, hit = hit.getParent()) {
            if (hit instanceof TextField || hit instanceof TextArea || hit instanceof CodeEditor) return true;
        }
        return false;
    }

    private void drawRipples(GUIContext graphics, float unit, long now) {
        ripples.removeIf(ripple -> now - ripple.startedAt >= RIPPLE_NANOS);
        for (var ripple : ripples) {
            float t = (now - ripple.startedAt) / (float) RIPPLE_NANOS;
            float grow = 1 - (1 - t) * (1 - t) * (1 - t);
            int color = switch (ripple.button) {
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> 0xFFA94D;
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> 0x8BE38B;
                default -> 0x62B0FF;
            };
            if (t < 0.5f) {
                float radius = unit * 0.32f;
                shape.setRadius(radius).setStroke(0).setColor(withAlpha(color, 0.55f * (1 - t / 0.5f)));
                graphics.drawTexture(shape, ripple.x - radius, ripple.y - radius, radius * 2, radius * 2);
            }
            float radius = unit * (0.25f + 0.6f * grow);
            shape.setRadius(radius).setStroke(unit * 0.11f).setColor(0)
                    .setBorderColor(withAlpha(color, (float) Math.pow(1 - t, 1.3)));
            graphics.drawTexture(shape, ripple.x - radius, ripple.y - radius, radius * 2, radius * 2);
        }
    }

    private void drawKeys(GUIContext graphics, float keyHeight, long now, float dt) {
        var visible = keys.visible(now);
        if (visible.isEmpty()) return;
        var font = LDFonts.font();
        float textScale = keyHeight * 0.5f / font.lineHeight;
        float padding = keyHeight * 0.42f;
        float gap = keyHeight * 0.25f;
        float top = regionY + regionHeight - keyHeight * 1.5f;

        var widths = new float[visible.size()];
        float total = gap * (visible.size() - 1);
        for (int i = 0; i < visible.size(); i++) {
            widths[i] = font.width(visible.get(i).text()) * textScale + padding * 2;
            total += widths[i];
        }
        float follow = dt <= 0 ? 1 : (float) (1 - Math.exp(-dt / 0.07));
        float x = regionX + (regionWidth - total) / 2;
        for (int i = 0; i < visible.size(); i++) {
            var key = visible.get(i);
            key.x = Float.isNaN(key.x) ? x : key.x + (x - key.x) * follow;
            x += widths[i] + gap;

            float alpha = key.alpha(now);
            if (alpha <= 0.02f) continue;
            float appear = key.appear(now);
            float pop = 0.85f + 0.15f * (1 - (1 - appear) * (1 - appear));

            var pose = graphics.pose;
            pose.pushPose();
            pose.translate(key.x + widths[i] / 2, top + keyHeight / 2);
            pose.scale(pop, pop);
            shape.setRadius(keyHeight * 0.28f).setStroke(keyHeight * 0.03f)
                    .setColor(withAlpha(0x181A1F, 0.88f * alpha)).setBorderColor(withAlpha(0xFFFFFF, 0.22f * alpha));
            graphics.drawTexture(shape, -widths[i] / 2, -keyHeight / 2, widths[i], keyHeight);
            pose.translate(-widths[i] / 2 + padding, -font.lineHeight * textScale / 2 + textScale);
            pose.scale(textScale, textScale);
            LDFonts.drawText(graphics, font, Component.literal(key.text()), 0, 0, withAlpha(0xFFFFFF, alpha), false);
            pose.popPose();
        }
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** A pointer image, rasterised on demand at the size it is drawn at. */
    private static final class CursorSprite {
        private final Identifier location;
        private final boolean beam;
        private int heightPx = -1;
        private int width;
        private int height;
        private float tipX;
        private float tipY;

        private CursorSprite(Identifier location, boolean beam) {
            this.location = location;
            this.beam = beam;
        }

        private void ensure(int shownHeightPx) {
            int px = Math.max(16, shownHeightPx) * 2;
            if (px == heightPx) return;
            heightPx = px;
            var image = rasterize(px);
            width = image.getWidth();
            height = image.getHeight();
            Minecraft.getInstance().getTextureManager().register(location, new LinearTexture(location, image));
        }

        private void release() {
            if (heightPx > 0) {
                Minecraft.getInstance().getTextureManager().release(location);
                heightPx = -1;
            }
        }

        /** White with a dark outline and a soft shadow, anti-aliased from the shape's signed distance. */
        private NativeImage rasterize(int heightPx) {
            float shapeHeight = beam ? IBEAM_HEIGHT : ARROW_HEIGHT;
            float shapeWidth = beam ? 5.2f : 11.3f;
            float k = heightPx / shapeHeight;
            float outline = beam ? 0.9f : 1.15f;
            float pad = (outline + 2.4f) * k;
            int imageWidth = (int) Math.ceil(shapeWidth * k + pad * 2);
            int imageHeight = (int) Math.ceil(shapeHeight * k + pad * 2);
            tipX = beam ? pad + shapeWidth * k / 2 : pad;
            tipY = beam ? pad + shapeHeight * k / 2 : pad;
            var image = new NativeImage(imageWidth, imageHeight, true);
            float pixel = 1 / k;
            for (int py = 0; py < imageHeight; py++) {
                for (int px = 0; px < imageWidth; px++) {
                    float ux = (px + 0.5f - tipX) / k;
                    float uy = (py + 0.5f - tipY) / k;
                    float d = distance(ux, uy);
                    float shapeAlpha = clamp01((outline - d) / pixel + 0.5f);
                    float fill = clamp01(-d / pixel + 0.5f);
                    float shadowAlpha = 0.38f * smoothstep(outline + 1.4f, outline - 0.6f, distance(ux - 0.5f, uy - 0.9f));
                    float alpha = shapeAlpha + shadowAlpha * (1 - shapeAlpha);
                    int grey = alpha <= 0 ? 0 : Math.round(255 * fill * shapeAlpha / alpha);
                    int a = Math.round(alpha * 255);
                    // grey in every channel, so the ARGB and ABGR readings of this agree
                    image.setPixel(px, py, (a << 24) | (grey << 16) | (grey << 8) | grey);
                }
            }
            return image;
        }

        private float distance(float x, float y) {
            return beam ? beamDistance(x, y) : arrowDistance(x, y);
        }
    }

    /** Distance to the arrow's outline in arrow units, negative inside. */
    private static float arrowDistance(float x, float y) {
        float best = Float.MAX_VALUE;
        boolean inside = false;
        int n = ARROW.length / 2;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            float ax = ARROW[j * 2], ay = ARROW[j * 2 + 1];
            float bx = ARROW[i * 2], by = ARROW[i * 2 + 1];
            if ((by > y) != (ay > y) && x < (ax - bx) * (y - by) / (ay - by) + bx) inside = !inside;
            float ex = bx - ax, ey = by - ay;
            float h = clamp01(((x - ax) * ex + (y - ay) * ey) / (ex * ex + ey * ey));
            float dx = x - ax - ex * h, dy = y - ay - ey * h;
            best = Math.min(best, dx * dx + dy * dy);
        }
        float distance = (float) Math.sqrt(best);
        return inside ? -distance : distance;
    }

    /** Distance to the I-beam, the union of its boxes, negative inside. */
    private static float beamDistance(float x, float y) {
        float best = Float.MAX_VALUE;
        for (int i = 0; i < IBEAM.length; i += 4) {
            float dx = Math.abs(x - IBEAM[i]) - IBEAM[i + 2];
            float dy = Math.abs(y - IBEAM[i + 1]) - IBEAM[i + 3];
            float outside = (float) Math.hypot(Math.max(dx, 0), Math.max(dy, 0));
            best = Math.min(best, outside + Math.min(Math.max(dx, dy), 0));
        }
        return best;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = clamp01((value - edge0) / (edge1 - edge0));
        return t * t * (3 - 2 * t);
    }

    /** A texture sampled linearly: the pointer is drawn at half the size it was rasterised at. */
    private static final class LinearTexture extends DynamicTexture {
        private LinearTexture(Identifier location, NativeImage image) {
            super(location::toString, image);
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }
    }

    // endregion

    @Override
    public void close() {
        arrow.release();
        ibeam.release();
        if (renderer != null) {
            renderer.close();
            renderer = null;
        }
    }
}
