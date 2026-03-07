package com.lowdragmc.lowdraglib2.math.curve;

import com.lowdragmc.lowdraglib2.math.Interpolations;
import lombok.EqualsAndHashCode;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.joml.Vector3f;
import org.joml.Vector3fc;


@EqualsAndHashCode(callSuper = false)
public class CubicBezierCurve3 extends Curve<Vector3f> implements ValueIOSerializable {
    public Vector3fc p0, c0, c1, p1;

    public CubicBezierCurve3(Vector3f start, Vector3f control1, Vector3f control2, Vector3f end) {
        this.p0 = start;
        this.c0 = control1;
        this.c1 = control2;
        this.p1 = end;
    }

    @Override
    public Vector3f getPoint(float t) {
        return new Vector3f(
                (float) Interpolations.CubicBezier(t, p0.x(), c0.x(), c1.x(), p1.x()),
                (float) Interpolations.CubicBezier(t, p0.y(), c0.y(), c1.y(), p1.y()),
                (float) Interpolations.CubicBezier(t, p0.z(), c0.z(), c1.z(), p1.z())
        );
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("p0", ExtraCodecs.VECTOR3F, p0);
        output.store("c0", ExtraCodecs.VECTOR3F, c0);
        output.store("c1", ExtraCodecs.VECTOR3F, c1);
        output.store("p1", ExtraCodecs.VECTOR3F, p1);
    }

    @Override
    public void deserialize(ValueInput input) {
        p0 = input.read("p0", ExtraCodecs.VECTOR3F).orElseGet(Vector3f::new);
        c0 = input.read("c0", ExtraCodecs.VECTOR3F).orElseGet(Vector3f::new);
        c1 = input.read("c1", ExtraCodecs.VECTOR3F).orElseGet(Vector3f::new);
        p1 = input.read("p1", ExtraCodecs.VECTOR3F).orElseGet(Vector3f::new);
    }

    public CubicBezierCurve3 copy() {
        return new CubicBezierCurve3(new Vector3f(p0), new Vector3f(c0), new Vector3f(c1), new Vector3f(p1));
    }
}
