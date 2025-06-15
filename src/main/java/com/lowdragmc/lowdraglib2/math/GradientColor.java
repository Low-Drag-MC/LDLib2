package com.lowdragmc.lowdraglib2.math;

import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GradientColor implements INBTSerializable<CompoundTag> {
    @Getter
    protected List<Vector2f> aP;
    @Getter
    protected List<Vector4f> rgbP;

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
        var value = aP.getFirst().y;
        var found = t < aP.getFirst().x;
        if (!found) {
            for (int i = 0; i < aP.size() - 1; i++) {
                var s = aP.get(i);
                var e = aP.get(i + 1);
                if (t >= s.x && t <= e.x) {
                    value = s.y * (e.x - t) / (e.x - s.x) + e.y * (t - s.x) / (e.x - s.x);
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            value = aP.getLast().y;
        }
        return value;
    }

    public Vector3f getRGB(float t) {
        var value = new Vector3f(rgbP.getFirst().y, rgbP.getFirst().z, rgbP.getFirst().w);
        var found = t < rgbP.getFirst().x;
        if (!found) {
            for (int i = 0; i < rgbP.size() - 1; i++) {
                var s = rgbP.get(i);
                var e = rgbP.get(i + 1);
                if (t >= s.x && t <= e.x) {
                    value = new Vector3f(
                            s.y * (e.x - t) / (e.x - s.x) + e.y * (t - s.x) / (e.x - s.x),
                            s.z * (e.x - t) / (e.x - s.x) + e.z * (t - s.x) / (e.x - s.x),
                            s.w * (e.x - t) / (e.x - s.x) + e.w * (t - s.x) / (e.x - s.x)
                    );
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            value = new Vector3f(rgbP.getLast().y, rgbP.getLast().z, rgbP.getLast().w);
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
        if (t < aP.getFirst().x) {
            aP.addFirst(new Vector2f(t, value));
            return 0;
        }
        for (int i = 0; i < aP.size() - 1; i++) {
            if (t >= aP.get(i).x && t <=  aP.get(i + 1).x) {
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
        if (t < rgbP.getFirst().x) {
            rgbP.addFirst(new Vector4f(t, r, g, b));
            return 0;
        }
        for (int i = 0; i < rgbP.size() - 1; i++) {
            if (t >= rgbP.get(i).x && t <=  rgbP.get(i + 1).x) {
                rgbP.add(i + 1, new Vector4f(t, r, g, b));
                return i + 1;
            }
        }
        rgbP.add(new Vector4f(t, r, g, b));
        return rgbP.size() - 1;
    }

    private ListTag saveAlpha(List<Vector2f> data) {
        var list = new ListTag();
        for (var Vector2f : data) {
            list.add(FloatTag.valueOf(Vector2f.x));
            list.add(FloatTag.valueOf(Vector2f.y));
        }
        return list;
    }

    private ListTag saveRGB(List<Vector4f> data) {
        var list = new ListTag();
        for (var Vector4f : data) {
            list.add(FloatTag.valueOf(Vector4f.x));
            list.add(FloatTag.valueOf(Vector4f.y));
            list.add(FloatTag.valueOf(Vector4f.z));
            list.add(FloatTag.valueOf(Vector4f.w));
        }
        return list;
    }

    private void loadAlphaFromTag(List<Vector2f> data, ListTag list) {
        data.clear();
        for (int i = 0; i < list.size(); i += 2) {
            data.add(new Vector2f(list.getFloat(i), list.getFloat(i + 1)));
        }
    }

    private void loadRGBFromTag(List<Vector4f> data, ListTag list) {
        data.clear();
        for (int i = 0; i < list.size(); i += 4) {
            data.add(new Vector4f(list.getFloat(i), list.getFloat(i + 1), list.getFloat(i + 2), list.getFloat(i + 3)));
        }
    }

    @Override
    public CompoundTag serializeNBT(@Nonnull HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        tag.put("a", saveAlpha(aP));
        tag.put("rgb", saveRGB(rgbP));
        return tag;
    }

    @Override
    public void deserializeNBT(@Nonnull HolderLookup.Provider provider, CompoundTag nbt) {
        loadAlphaFromTag(aP, nbt.getList("a", Tag.TAG_FLOAT));
        loadRGBFromTag(rgbP, nbt.getList("rgb", Tag.TAG_FLOAT));
    }
    
    public GradientColor copy() {
        var copy = new GradientColor();
        copy.aP.clear();
        copy.rgbP.clear();
        this.aP.forEach(Vector2f -> copy.aP.add(new Vector2f(Vector2f.x, Vector2f.y)));
        this.rgbP.forEach(Vector2f -> copy.rgbP.add(new Vector4f(Vector2f.x, Vector2f.y, Vector2f.z, Vector2f.w)));
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
