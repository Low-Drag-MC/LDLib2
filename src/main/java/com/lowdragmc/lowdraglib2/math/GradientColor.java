package com.lowdragmc.lowdraglib2.math;

import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import com.mojang.serialization.Codec;
import lombok.Getter;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GradientColor implements ValueIOSerializable {
    /** Legacy 1.21 layout: a flat float list {@code [t,a, t,a, ...]} — kept for decoding old saves. */
    @Deprecated(since = "1.22")
    private static final Codec<List<Vector2fc>> LEGACY_ALPHA_POINTS = Codec.FLOAT.listOf().xmap(
            flat -> {
                List<Vector2fc> points = new ArrayList<>();
                for (int i = 0; i + 1 < flat.size(); i += 2) {
                    points.add(new Vector2f(flat.get(i), flat.get(i + 1)));
                }
                return points;
            },
            points -> {
                List<Float> flat = new ArrayList<>();
                points.forEach(p -> { flat.add(p.x()); flat.add(p.y()); });
                return flat;
            });

    /** Legacy 1.21 layout: a flat float list {@code [t,r,g,b, t,r,g,b, ...]} — kept for decoding old saves. */
    @Deprecated(since = "1.22")
    private static final Codec<List<Vector4fc>> LEGACY_RGB_POINTS = Codec.FLOAT.listOf().xmap(
            flat -> {
                List<Vector4fc> points = new ArrayList<>();
                for (int i = 0; i + 3 < flat.size(); i += 4) {
                    points.add(new Vector4f(flat.get(i), flat.get(i + 1), flat.get(i + 2), flat.get(i + 3)));
                }
                return points;
            },
            points -> {
                List<Float> flat = new ArrayList<>();
                points.forEach(p -> { flat.add(p.x()); flat.add(p.y()); flat.add(p.z()); flat.add(p.w()); });
                return flat;
            });

    /** Encode the current list-of-vectors form; decode either layout. */
    private static final Codec<List<Vector2fc>> ALPHA_POINTS =
            Codec.withAlternative(Codec.list(ExtraCodecs.VECTOR2F), LEGACY_ALPHA_POINTS);
    private static final Codec<List<Vector4fc>> RGB_POINTS =
            Codec.withAlternative(Codec.list(ExtraCodecs.VECTOR4F), LEGACY_RGB_POINTS);

    @Getter
    protected List<Vector2fc> aP;
    @Getter
    protected List<Vector4fc> rgbP;

    public GradientColor() {
        this.aP = new ArrayList<>(List.of(new Vector2f(0, 1), new Vector2f(1, 1)));
        this.rgbP = new ArrayList<>(List.of(new Vector4f(0, 1, 1, 1), new Vector4f(1, 1, 1, 1)));
    }

    public GradientColor(int... colors) {
        this.aP = new ArrayList<>();
        this.rgbP = new ArrayList<>();
        if (colors.length == 1) {
            this.aP.add(new Vector2f(0.5f, ColorUtils.alpha(colors[0])));
            this.rgbP.add(new Vector4f(0.5f, ColorUtils.red(colors[0]), ColorUtils.green(colors[0]), ColorUtils.blue(colors[0])));
        } else {
            for (int i = 0; i < colors.length; i++) {
                var t = i / (colors.length - 1f);
                this.aP.add(new Vector2f(t, ColorUtils.alpha(colors[i])));
                this.rgbP.add(new Vector4f(t, ColorUtils.red(colors[i]), ColorUtils.green(colors[i]), ColorUtils.blue(colors[i])));
            }
        }
    }

    public float getAlpha(float t) {
        if (aP.isEmpty()) {
            return 1; // broken/empty gradient data must degrade, not crash the tick
        }
        var value = aP.getFirst().y();
        var found = t < aP.getFirst().x();
        if (!found) {
            for (int i = 0; i < aP.size() - 1; i++) {
                var s = aP.get(i);
                var e = aP.get(i + 1);
                if (t >= s.x() && t <= e.x()) {
                    value = s.y() * (e.x() - t) / (e.x() - s.x()) + e.y() * (t - s.x()) / (e.x() - s.x());
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            value = aP.getLast().y();
        }
        return value;
    }

    public Vector3f getRGB(float t) {
        if (rgbP.isEmpty()) {
            return new Vector3f(1, 1, 1); // broken/empty gradient data must degrade, not crash the tick
        }
        var value = new Vector3f(rgbP.getFirst().y(), rgbP.getFirst().z(), rgbP.getFirst().w());
        var found = t < rgbP.getFirst().x();
        if (!found) {
            for (int i = 0; i < rgbP.size() - 1; i++) {
                var s = rgbP.get(i);
                var e = rgbP.get(i + 1);
                if (t >= s.x() && t <= e.x()) {
                    value = new Vector3f(
                            s.y() * (e.x() - t) / (e.x() - s.x()) + e.y() * (t - s.x()) / (e.x() - s.x()),
                            s.z() * (e.x() - t) / (e.x() - s.x()) + e.z() * (t - s.x()) / (e.x() - s.x()),
                            s.w() * (e.x() - t) / (e.x() - s.x()) + e.w() * (t - s.x()) / (e.x() - s.x())
                    );
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            value = new Vector3f(rgbP.getLast().y(), rgbP.getLast().z(), rgbP.getLast().w());
        }
        return value;
    }

    public int getColor(float t) {
        var alpha = getAlpha(t);
        var rgb = getRGB(t);
        return ColorUtils.color(alpha, rgb.x, rgb.y, rgb.z);
    }

    public int getRGBColor(float t) {
        var rgb = getRGB(t);
        return ColorUtils.color(1, rgb.x, rgb.y, rgb.z);
    }

    public int addAlpha(float t, float value) {
        if (aP.isEmpty()) {
            aP.add(new Vector2f(t, value));
            return 0;
        }
        if (t < aP.getFirst().x()) {
            aP.addFirst(new Vector2f(t, value));
            return 0;
        }
        for (int i = 0; i < aP.size() - 1; i++) {
            if (t >= aP.get(i).x() && t <=  aP.get(i + 1).x()) {
                aP.add(i + 1, new Vector2f(t, value));
                return i + 1;
            }
        }
        aP.add(new Vector2f(t, value));
        return aP.size() - 1;
    }

    public int addRGB(float t, float r, float g, float b) {
        if (rgbP.isEmpty()) {
            rgbP.add(new Vector4f(t, r, g, b));
            return 0;
        }
        if (t < rgbP.getFirst().x()) {
            rgbP.addFirst(new Vector4f(t, r, g, b));
            return 0;
        }
        for (int i = 0; i < rgbP.size() - 1; i++) {
            if (t >= rgbP.get(i).x() && t <=  rgbP.get(i + 1).x()) {
                rgbP.add(i + 1, new Vector4f(t, r, g, b));
                return i + 1;
            }
        }
        rgbP.add(new Vector4f(t, r, g, b));
        return rgbP.size() - 1;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("a", ALPHA_POINTS, aP);
        output.store("rgb", RGB_POINTS, rgbP);
    }

    @Override
    public void deserialize(ValueInput input) {
        aP.clear();
        rgbP.clear();
        input.read("a", ALPHA_POINTS).ifPresent(aP::addAll);
        input.read("rgb", RGB_POINTS).ifPresent(rgbP::addAll);
    }
    
    public GradientColor copy() {
        var copy = new GradientColor();
        copy.aP.clear();
        copy.rgbP.clear();
        this.aP.forEach(vec2 -> copy.aP.add(new Vector2f(vec2.x(), vec2.y())));
        this.rgbP.forEach(vec4 -> copy.rgbP.add(new Vector4f(vec4.x(), vec4.y(), vec4.z(), vec4.w())));
        return copy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(aP, rgbP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GradientColor that = (GradientColor) o;
        return Objects.equals(aP, that.aP) && Objects.equals(rgbP, that.rgbP);
    }
}
