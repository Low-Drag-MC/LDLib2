package com.lowdragmc.lowdraglib2.math;

import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.util.Objects;

/**
 * A high-dynamic-range color: an LDR base color {@code (r, g, b, a)} in 0..1 plus a linear
 * {@code intensity} multiplier applied to <b>rgb only</b>.
 *
 * <p><b>The convention</b> (this is the one place it is defined, everything else follows):
 * {@link #toVector4f()} returns {@code (r*i, g*i, b*i, a)} — the intensity is <em>premultiplied</em>
 * into rgb. Shaders therefore consume the value as plain {@code .rgba}; they must <b>not</b> do
 * {@code rgb * a} (that was the old, alpha-less {@code Vector4f} encoding). Note that for the
 * common "HDR emission offset" uniforms alpha is pinned to 1 (see {@link #toVector4fOpaque()}), in
 * which case both readings coincide — which is exactly what keeps existing hand-written shaders
 * working across the convention change.
 *
 * <p>Because the intensity is folded into rgb on the way out, the pair {@code (base, intensity)}
 * cannot be recovered from a {@code vec4} exactly. Anything that needs to round-trip the authored
 * value (configurators, serialized uniforms) must store the {@code HDRColor} itself; anything that
 * only needs the final color can reconstruct an equivalent one with
 * {@link #fromPremultiplied(Vector4f)}.
 */
public class HDRColor {

    /**
     * Reads both the current shape ({@code {color:[r,g,b,a], intensity:f}}) and the legacy
     * {@code @ConfigHDR Vector4f} shape (a bare 4-float list whose {@code w} was the intensity and
     * which could not express alpha). Always <b>writes</b> the current shape.
     */
    public static final Codec<HDRColor> CODEC = Codec.withAlternative(
            RecordCodecBuilder.create(instance -> instance.group(
                    ExtraCodecs.VECTOR4F.fieldOf("color").forGetter(HDRColor::toRawVector4f),
                    Codec.FLOAT.optionalFieldOf("intensity", 1f).forGetter(HDRColor::getIntensity)
            ).apply(instance, HDRColor::new)),
            ExtraCodecs.VECTOR4F,
            legacy -> new HDRColor(legacy.x(), legacy.y(), legacy.z(), 1f, legacy.w()));

    public static final StreamCodec<ByteBuf, HDRColor> STREAM_CODEC = StreamCodec.of(
            (byteBuf, color) -> {
                byteBuf.writeFloat(color.r);
                byteBuf.writeFloat(color.g);
                byteBuf.writeFloat(color.b);
                byteBuf.writeFloat(color.a);
                byteBuf.writeFloat(color.intensity);
            },
            byteBuf -> new HDRColor(byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(),
                    byteBuf.readFloat(), byteBuf.readFloat()));

    @Getter
    private float r, g, b, a;
    /** Linear multiplier on rgb; never negative. */
    @Getter
    private float intensity;

    public HDRColor() {
        this(1, 1, 1, 1, 1);
    }

    public HDRColor(float r, float g, float b, float a, float intensity) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.intensity = Math.max(0, intensity);
    }

    public HDRColor(Vector4fc color, float intensity) {
        this(color.x(), color.y(), color.z(), color.w(), intensity);
    }

    /** Copy constructor — required by the syncdata {@code copyMark} dirty detection. */
    public HDRColor(HDRColor other) {
        this(other.r, other.g, other.b, other.a, other.intensity);
    }

    public static HDRColor white() {
        return new HDRColor(1, 1, 1, 1, 1);
    }

    public static HDRColor black() {
        return new HDRColor(0, 0, 0, 1, 1);
    }

    /** An LDR color, i.e. intensity 1. */
    public static HDRColor fromARGB(int argb) {
        return new HDRColor(ColorUtils.red(argb), ColorUtils.green(argb), ColorUtils.blue(argb),
                ColorUtils.alpha(argb), 1f);
    }

    /**
     * Recover a {@code (base, intensity)} pair from an already-premultiplied {@code vec4}, using the
     * canonicalisation Unity's HDR color field uses: {@code m = max(rgb)}; if {@code m > 1} the color
     * is {@code rgb/m} at intensity {@code m}, otherwise it is {@code rgb} at intensity 1.
     *
     * <p>This is idempotent and always reproduces the same premultiplied value, but it is <em>not</em>
     * a faithful inverse of an arbitrary authored pair: intensities below 1 are not representable, and
     * e.g. {@code (0.5, 0.25, 0) @ 2} comes back as {@code (1, 0.5, 0) @ 1}. Identical pixels, but the
     * editor state is normalised.
     */
    public static HDRColor fromPremultiplied(Vector4fc premultiplied) {
        return fromPremultiplied(premultiplied.x(), premultiplied.y(), premultiplied.z(), premultiplied.w());
    }

    public static HDRColor fromPremultiplied(float r, float g, float b, float a) {
        var max = Math.max(r, Math.max(g, b));
        if (max > 1) {
            return new HDRColor(r / max, g / max, b / max, a, max);
        }
        return new HDRColor(r, g, b, a, 1f);
    }

    public void set(float r, float g, float b, float a, float intensity) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.intensity = Math.max(0, intensity);
    }

    public void set(HDRColor other) {
        set(other.r, other.g, other.b, other.a, other.intensity);
    }

    public void setIntensity(float intensity) {
        this.intensity = Math.max(0, intensity);
    }

    public HDRColor withIntensity(float intensity) {
        return new HDRColor(r, g, b, a, intensity);
    }

    /** The base color's packed ARGB int (intensity <b>not</b> applied) — for driving a color picker. */
    public int baseARGB() {
        return pack(a, r, g, b);
    }

    /** The final color, intensity premultiplied into rgb. This is the shader-facing value. */
    public Vector4f toVector4f() {
        return new Vector4f(r * intensity, g * intensity, b * intensity, a);
    }

    /**
     * As {@link #toVector4f()} but with alpha forced to 1 — the form used for "HDR emission offset"
     * uniforms, where alpha carries no meaning and pinning it keeps shaders written against the old
     * {@code rgb * a} encoding numerically identical.
     */
    public Vector4f toVector4fOpaque() {
        return new Vector4f(r * intensity, g * intensity, b * intensity, 1f);
    }

    /** The un-premultiplied {@code (r, g, b, a)}; this is what {@link #CODEC} persists. */
    public Vector4f toRawVector4f() {
        return new Vector4f(r, g, b, a);
    }

    /** The final color clamped down to a packed ARGB int — the LDR fallback for legacy consumers. */
    public int toARGB() {
        return pack(a, r * intensity, g * intensity, b * intensity);
    }

    /**
     * Pack 0..1 floats to ARGB, rounding rather than truncating (which {@code ColorUtils.color(float…)}
     * does) so that {@code fromARGB(x).toARGB() == x} exactly — record mode and the gradient stop editor
     * both round-trip a colour through this and would otherwise drift a bit per edit.
     */
    private static int pack(float a, float r, float g, float b) {
        return (round255(a) << 24) | (round255(r) << 16) | (round255(g) << 8) | round255(b);
    }

    private static int round255(float value) {
        return Math.clamp(Math.round(value * 255f), 0, 255);
    }

    public HDRColor copy() {
        return new HDRColor(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof HDRColor other
                && Float.compare(r, other.r) == 0
                && Float.compare(g, other.g) == 0
                && Float.compare(b, other.b) == 0
                && Float.compare(a, other.a) == 0
                && Float.compare(intensity, other.intensity) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(r, g, b, a, intensity);
    }

    @Override
    public String toString() {
        return "HDRColor[%.3f, %.3f, %.3f, %.3f x%.3f]".formatted(r, g, b, a, intensity);
    }
}
