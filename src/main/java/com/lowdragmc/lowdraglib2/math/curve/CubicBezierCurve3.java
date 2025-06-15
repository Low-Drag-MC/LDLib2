package com.lowdragmc.lowdraglib2.math.curve;

import com.lowdragmc.lowdraglib2.math.Interpolations;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;
import org.joml.Vector3f;

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2022/6/17
 * @implNote CubicBezierCurve3
 */
public class CubicBezierCurve3 extends Curve<Vector3f> implements INBTSerializable<ListTag> {
    public Vector3f p0, c0, c1, p1;

    public CubicBezierCurve3(Vector3f start, Vector3f control1, Vector3f control2, Vector3f end) {
        this.p0 = start;
        this.c0 = control1;
        this.c1 = control2;
        this.p1 = end;
    }

    @Override
    public Vector3f getPoint(float t) {
        return new Vector3f(
                (float) Interpolations.CubicBezier(t, p0.x, c0.x, c1.x, p1.x),
                (float) Interpolations.CubicBezier(t, p0.y, c0.y, c1.y, p1.y),
                (float) Interpolations.CubicBezier(t, p0.z, c0.z, c1.z, p1.z)
        );
    }

    @Override
    public @UnknownNullability ListTag serializeNBT(@Nonnull HolderLookup.Provider provider) {
        var list = new ListTag();
        list.add(FloatTag.valueOf(p0.x));
        list.add(FloatTag.valueOf(p0.y));
        list.add(FloatTag.valueOf(p0.z));

        list.add(FloatTag.valueOf(c0.x));
        list.add(FloatTag.valueOf(c0.y));
        list.add(FloatTag.valueOf(c0.z));

        list.add(FloatTag.valueOf(c1.x));
        list.add(FloatTag.valueOf(c1.y));
        list.add(FloatTag.valueOf(c1.z));

        list.add(FloatTag.valueOf(p1.x));
        list.add(FloatTag.valueOf(p1.y));
        list.add(FloatTag.valueOf(p1.z));
        return list;
    }

    @Override
    public void deserializeNBT(@Nonnull HolderLookup.Provider provider, ListTag list) {
        p0 = new Vector3f(list.getFloat(0), list.getFloat(1), list.getFloat(2));
        c0 = new Vector3f(list.getFloat(3), list.getFloat(4), list.getFloat(5));
        c1 = new Vector3f(list.getFloat(6), list.getFloat(7), list.getFloat(8));
        p1 = new Vector3f(list.getFloat(9), list.getFloat(10), list.getFloat(11));
    }

    public CubicBezierCurve3 copy() {
        return new CubicBezierCurve3(new Vector3f(p0), new Vector3f(c0), new Vector3f(c1), new Vector3f(p1));
    }
}
