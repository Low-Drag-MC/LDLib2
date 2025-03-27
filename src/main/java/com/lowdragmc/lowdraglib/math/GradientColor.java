package com.lowdragmc.lowdraglib.math;

import com.lowdragmc.lowdraglib.utils.ColorUtils;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author KilaBash
 * @date 2023/5/30
 * @implNote Gradient
 */
public class GradientColor implements INBTSerializable<CompoundTag> {
    @Getter
    protected List<Vec2> aP, rP, gP, bP;

    public GradientColor() {
        this.aP = new ArrayList<>(List.of(new Vec2(0, 1), new Vec2(1, 1)));
        this.rP = new ArrayList<>(List.of(new Vec2(0, 1), new Vec2(1, 1)));
        this.gP = new ArrayList<>(List.of(new Vec2(0, 1), new Vec2(1, 1)));
        this.bP = new ArrayList<>(List.of(new Vec2(0, 1), new Vec2(1, 1)));
    }

    public GradientColor(int... colors) {
        this.aP = new ArrayList<>();
        this.rP = new ArrayList<>();
        this.gP = new ArrayList<>();
        this.bP = new ArrayList<>();
        if (colors.length == 1) {
            this.aP.add(new Vec2(0.5f, ColorUtils.alpha(colors[0])));
            this.rP.add(new Vec2(0.5f, ColorUtils.red(colors[0])));
            this.gP.add(new Vec2(0.5f, ColorUtils.green(colors[0])));
            this.bP.add(new Vec2(0.5f, ColorUtils.blue(colors[0])));
        }
        for (int i = 0; i < colors.length; i++) {
            var t = i / (colors.length - 1f);
            this.aP.add(new Vec2(t, ColorUtils.alpha(colors[i])));
            this.rP.add(new Vec2(t, ColorUtils.red(colors[i])));
            this.gP.add(new Vec2(t, ColorUtils.green(colors[i])));
            this.bP.add(new Vec2(t, ColorUtils.blue(colors[i])));
        }
    }

    public float get(List<Vec2> data, float t) {
        var value = data.get(0).y;
        var found = t < data.get(0).x;
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
            value = data.get(data.size() - 1).y;
        }
        return value;
    }

    public int getColor(float t) {
        return ColorUtils.color(get(aP, t), get(rP, t), get(gP, t), get(bP, t));
    }

    public int getRGBColor(float t) {
        return ColorUtils.color(1, get(rP, t), get(gP, t), get(bP, t));
    }

    public int add(List<Vec2> data, float t, float value) {
        if (data.size() == 0) {
            data.add(new Vec2(t, value));
            return 0;
        }
        if (t < data.get(0).x) {
            data.add(0, new Vec2(t, value));
            return 0;
        }
        for (int i = 0; i < data.size() - 1; i++) {
            if (t >= data.get(i).x && t <=  data.get(i + 1).x) {
                data.add(i + 1, new Vec2(t, value));
                return i + 1;
            }
        }
        data.add(new Vec2(t, value));
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

    private ListTag saveAsTag(List<Vec2> data) {
        var list = new ListTag();
        for (Vec2 vec2 : data) {
            list.add(FloatTag.valueOf(vec2.x));
            list.add(FloatTag.valueOf(vec2.y));
        }
        return list;
    }

    private void loadFromTag(List<Vec2> data, ListTag list) {
        data.clear();
        for (int i = 0; i < list.size(); i += 2) {
            data.add(new Vec2(list.getFloat(i), list.getFloat(i + 1)));
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        tag.put("a", saveAsTag(aP));
        tag.put("r", saveAsTag(rP));
        tag.put("g", saveAsTag(gP));
        tag.put("b", saveAsTag(bP));
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        loadFromTag(aP, nbt.getList("a", Tag.TAG_FLOAT));
        loadFromTag(rP, nbt.getList("r", Tag.TAG_FLOAT));
        loadFromTag(gP, nbt.getList("g", Tag.TAG_FLOAT));
        loadFromTag(bP, nbt.getList("b", Tag.TAG_FLOAT));
    }
}
