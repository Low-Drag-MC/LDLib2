package com.lowdragmc.lowdraglib2.math.curve;

import com.lowdragmc.lowdraglib2.math.Interpolations;
import lombok.EqualsAndHashCode;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.joml.Vector2f;
import org.joml.Vector2fc;


@EqualsAndHashCode(callSuper = false)
public class ExplicitCubicBezierCurve2 extends Curve<Vector2f> implements ValueIOSerializable {
    public Vector2fc p0, c0, c1, p1;

    public ExplicitCubicBezierCurve2(Vector2fc start, Vector2fc control1, Vector2fc control2, Vector2fc end) {
        this.p0 = start;
        this.c0 = control1;
        this.c1 = control2;
        this.p1 = end;
    }

    public ExplicitCubicBezierCurve2(ValueInput input) {
        deserialize(input);
    }

    @Override
    public Vector2f getPoint(float t) {
        if (c0.x() == p0.x()) {
            return new Vector2f(p0.x() + t * (p1.x() - p0.x()), c0.y() > p0.y() ? p0.y() : p1.y());
        }
        if (c1.x() == p1.x()) {
            return new Vector2f(p0.x() + t * (p1.x() - p0.x()), c1.y() > p1.y() ? p1.y() : p0.y());
        }
        return new Vector2f(
                p0.x() + t * (p1.x() - p0.x()),
                (float) Interpolations.CubicBezier(t, p0.y(), c0.y(), c1.y(), p1.y())
        );
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("p0", net.minecraft.util.ExtraCodecs.VECTOR2F, p0);
        output.store("c0", net.minecraft.util.ExtraCodecs.VECTOR2F, c0);
        output.store("c1", net.minecraft.util.ExtraCodecs.VECTOR2F, c1);
        output.store("p1", net.minecraft.util.ExtraCodecs.VECTOR2F, p1);
    }

    @Override
    public void deserialize(ValueInput input) {
        p0 = input.read("p0", net.minecraft.util.ExtraCodecs.VECTOR2F).orElseGet(Vector2f::new);
        c0 = input.read("c0", net.minecraft.util.ExtraCodecs.VECTOR2F).orElseGet(Vector2f::new);
        c1 = input.read("c1", net.minecraft.util.ExtraCodecs.VECTOR2F).orElseGet(Vector2f::new);
        p1 = input.read("p1", net.minecraft.util.ExtraCodecs.VECTOR2F).orElseGet(Vector2f::new);
    }

    public ExplicitCubicBezierCurve2 copy() {
        return new ExplicitCubicBezierCurve2(new Vector2f(p0), new Vector2f(c0), new Vector2f(c1), new Vector2f(p1));
    }
}
