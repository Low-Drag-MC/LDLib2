package com.lowdragmc.lowdraglib2.math.curve;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.math.Interpolations;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.joml.Vector2f;

/**
 * @author KilaBash
 * @date 2022/6/17
 * @implNote ExplicitCubicBezierCurve2
 */
public class ExplicitCubicBezierCurve2 extends Curve<Vector2f> implements INBTSerializable<ListTag> {
    public Vector2f p0, c0, c1, p1;

    public ExplicitCubicBezierCurve2(Vector2f start, Vector2f control1, Vector2f control2, Vector2f end) {
        this.p0 = start;
        this.c0 = control1;
        this.c1 = control2;
        this.p1 = end;
    }

    public ExplicitCubicBezierCurve2(HolderLookup.Provider provider, ListTag list) {
        deserializeNBT(provider, list);
    }

    public ExplicitCubicBezierCurve2(ListTag list) {
        deserializeNBT(Platform.getFrozenRegistry(), list);
    }

    @Override
    public Vector2f getPoint(float t) {
        if (c0.x == p0.x) {
            return new Vector2f(p0.x + t * (p1.x - p0.x), c0.y > p0.y ? p0.y : p1.y);
        }
        if (c1.x == p1.x) {
            return new Vector2f(p0.x + t * (p1.x - p0.x), c1.y > p1.y ? p1.y : p0.y);
        }
        return new Vector2f(
                p0.x + t * (p1.x - p0.x),
                (float) Interpolations.CubicBezier(t, p0.y, c0.y, c1.y, p1.y)
        );
    }

    @Override
    public ListTag serializeNBT(HolderLookup.Provider provider) {
        var list = new ListTag();
        list.add(FloatTag.valueOf(p0.x));
        list.add(FloatTag.valueOf(p0.y));

        list.add(FloatTag.valueOf(c0.x));
        list.add(FloatTag.valueOf(c0.y));

        list.add(FloatTag.valueOf(c1.x));
        list.add(FloatTag.valueOf(c1.y));

        list.add(FloatTag.valueOf(p1.x));
        list.add(FloatTag.valueOf(p1.y));
        return list;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, ListTag list) {
        p0 = new Vector2f(list.getFloat(0), list.getFloat(1));
        c0 = new Vector2f(list.getFloat(2), list.getFloat(3));
        c1 = new Vector2f(list.getFloat(4), list.getFloat(5));
        p1 = new Vector2f(list.getFloat(6), list.getFloat(7));
    }

    public ExplicitCubicBezierCurve2 copy() {
        return new ExplicitCubicBezierCurve2(new Vector2f(p0), new Vector2f(c0), new Vector2f(c1), new Vector2f(p1));
    }
}
