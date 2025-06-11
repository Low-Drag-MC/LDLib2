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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GradientColor implements INBTSerializable<CompoundTag> {
    @Getter
    protected List<Vector2f> aP, rP, gP, bP;

    public GradientColor() {
        this.aP = new ArrayList<>(List.of(new Vector2f(0, 1), new Vector2f(1, 1)));
        this.rP = new ArrayList<>(List.of(new Vector2f(0, 1), new Vector2f(1, 1)));
        this.gP = new ArrayList<>(List.of(new Vector2f(0, 1), new Vector2f(1, 1)));
        this.bP = new ArrayList<>(List.of(new Vector2f(0, 1), new Vector2f(1, 1)));
    }

    public GradientColor(int... colors) {
        this.aP = new ArrayList<>();
        this.rP = new ArrayList<>();
        this.gP = new ArrayList<>();
        this.bP = new ArrayList<>();
        if (colors.length == 1) {
            this.aP.add(new Vector2f(0.5f, ColorUtils.alpha(colors[0])));
            this.rP.add(new Vector2f(0.5f, ColorUtils.red(colors[0])));
            this.gP.add(new Vector2f(0.5f, ColorUtils.green(colors[0])));
            this.bP.add(new Vector2f(0.5f, ColorUtils.blue(colors[0])));
        }
        for (int i = 0; i < colors.length; i++) {
            var t = i / (colors.length - 1f);
            this.aP.add(new Vector2f(t, ColorUtils.alpha(colors[i])));
            this.rP.add(new Vector2f(t, ColorUtils.red(colors[i])));
            this.gP.add(new Vector2f(t, ColorUtils.green(colors[i])));
            this.bP.add(new Vector2f(t, ColorUtils.blue(colors[i])));
        }
    }

    public float get(List<Vector2f> data, float t) {
        var value = data.getFirst().y;
        var found = t < data.getFirst().x;
        if (!found) {
            for (int i = 0; i < data.size() - 1; i++) {
                var s = data.get(i);
                var e = data.get(i + 1);
                if (t >= s.x && t <= e.x) {
                    value = s.y * (e.x - t) / (e.x - s.x) + e.y * (t - s.x) / (e.x - s.x);
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            value = data.getLast().y;
        }
        return value;
    }

    public int getColor(float t) {
        return ColorUtils.color(get(aP, t), get(rP, t), get(gP, t), get(bP, t));
    }

    public int getRGBColor(float t) {
        return ColorUtils.color(1, get(rP, t), get(gP, t), get(bP, t));
    }

    public int add(List<Vector2f> data, float t, float value) {
        if (data.isEmpty()) {
            data.add(new Vector2f(t, value));
            return 0;
        }
        if (t < data.getFirst().x) {
            data.addFirst(new Vector2f(t, value));
            return 0;
        }
        for (int i = 0; i < data.size() - 1; i++) {
            if (t >= data.get(i).x && t <=  data.get(i + 1).x) {
                data.add(i + 1, new Vector2f(t, value));
                return i + 1;
            }
        }
        data.add(new Vector2f(t, value));
        return data.size() - 1;
    }

    public int addAlpha(float t, float value) {
        return add(aP, t, value);
    }

    public int addRGB(float t, float r, float g, float b) {
        add(rP, t, r);
        add(gP, t, g);
        return add(bP, t, b);
    }

    private ListTag saveAsTag(List<Vector2f> data) {
        var list = new ListTag();
        for (Vector2f Vector2f : data) {
            list.add(FloatTag.valueOf(Vector2f.x));
            list.add(FloatTag.valueOf(Vector2f.y));
        }
        return list;
    }

    private void loadFromTag(List<Vector2f> data, ListTag list) {
        data.clear();
        for (int i = 0; i < list.size(); i += 2) {
            data.add(new Vector2f(list.getFloat(i), list.getFloat(i + 1)));
        }
    }

    @Override
    public CompoundTag serializeNBT(@Nonnull HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        tag.put("a", saveAsTag(aP));
        tag.put("r", saveAsTag(rP));
        tag.put("g", saveAsTag(gP));
        tag.put("b", saveAsTag(bP));
        return tag;
    }

    @Override
    public void deserializeNBT(@Nonnull HolderLookup.Provider provider, CompoundTag nbt) {
        loadFromTag(aP, nbt.getList("a", Tag.TAG_FLOAT));
        loadFromTag(rP, nbt.getList("r", Tag.TAG_FLOAT));
        loadFromTag(gP, nbt.getList("g", Tag.TAG_FLOAT));
        loadFromTag(bP, nbt.getList("b", Tag.TAG_FLOAT));
    }
    
    public GradientColor copy() {
        var copy = new GradientColor();
        copy.aP.clear();
        copy.rP.clear();
        copy.gP.clear();
        copy.bP.clear();
        this.aP.forEach(Vector2f -> copy.aP.add(new Vector2f(Vector2f.x, Vector2f.y)));
        this.rP.forEach(Vector2f -> copy.rP.add(new Vector2f(Vector2f.x, Vector2f.y)));
        this.gP.forEach(Vector2f -> copy.gP.add(new Vector2f(Vector2f.x, Vector2f.y)));
        this.bP.forEach(Vector2f -> copy.bP.add(new Vector2f(Vector2f.x, Vector2f.y)));
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GradientColor that = (GradientColor) o;
        return Objects.equals(aP, that.aP) && Objects.equals(rP, that.rP) && Objects.equals(gP, that.gP) && Objects.equals(bP, that.bP);
    }
}
