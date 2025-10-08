package com.lowdragmc.lowdraglib2.gui.ui.data;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector2f;


/**
 * Lightweight 2D transform for UIElement: translate + rotate + scale around an origin (pivot).
 * Does not affect Yoga layout; only rendering and hit-testing.
 */
@Accessors(chain = true, fluent = true)
public final class Transform2D implements IConfigurable, IPersistedSerializable {
    @Getter
    @Configurable(name = "Transform2D.translate")
    private Vector2f translate = new Vector2f();
    @Getter
    @Configurable(name = "Transform2D.scale")
    private Vector2f scale = new Vector2f(1f);
    @Getter
    @Configurable(name = "Transform2D.rotation")
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private float rotation = 0f;   // Z-axis degree

    /**
     * Transform origin as ratio of element size:
     * 0=left/top, 0.5=center, 1=right/bottom.
     */
    @Getter @Setter
    @Configurable(name = "Transform2D.pivot")
    private Pivot pivot = Pivot.CENTER;

    // runtime
    private float rotationRad;


    public boolean isIdentity() {
        return translate.x == 0f && translate.y == 0f
                && rotationRad == 0f
                && scale.x == 1f && scale.y == 1f;
    }

    public Transform2D translate(float x, float y) {
        translate.x = x;
        translate.y = y;
        return this;
    }

    public Transform2D rotationRad(float rad) {
        this.rotationRad = rad;
        this.rotation = (float) Math.toDegrees(rad);
        return this;
    }

    @ConfigSetter(field = "rotation")
    public Transform2D rotation(float deg) {
        this.rotation = deg;
        this.rotationRad = (float) Math.toRadians(deg);
        return this;
    }

    public Transform2D scale(float sx, float sy) {
        scale.x = sx;
        scale.y = sy;
        return this;
    }

    public Transform2D scale(float s) {
        return scale(s, s);
    }

    public Transform2D pivot(float oxRatio, float oyRatio) {
        this.pivot = Pivot.of(oxRatio, oyRatio);
        return this;
    }

    // Apply to pose for rendering: T -> pivot -> R -> S -> -pivot
    public void pushToPose(GUIContext ctx, UIElement e) {
        if (isIdentity()) return;
        float px = e.getPositionX() + e.getSizeWidth() * pivot.x;
        float py = e.getPositionY() + e.getSizeHeight() * pivot.y;

        ctx.pose.pushPose();

        if (translate.x != 0f || translate.y != 0f) {
            ctx.pose.translate(translate.x, translate.y, 0);
        }

        if (rotationRad != 0f || scale.x != 1f || scale.y != 1f) {
            ctx.pose.translate(px, py, 0);
            if (rotationRad != 0f) {
                ctx.pose.mulPose(new Quaternionf().rotateLocalZ(rotationRad));
            }
            if (scale.x != 1f || scale.y != 1f) {
                ctx.pose.scale(scale.x, scale.y, 1);
            }
            ctx.pose.translate(-px, -py, 0);
        }
    }

    public void popPose(GUIContext ctx) {
        if (!isIdentity()) {
            ctx.pose.popPose();
        }
    }

    // Inverse-transform a screen point back into the element's pre-transform space:
    // inverse order: pivot -> inv(S) -> inv(R) -> -pivot -> inv(T)
    public void inversePoint(UIElement e, double[] p /* [x,y] */) {
        if (isIdentity()) return;

        // Inverse translation
        p[0] -= translate.x;
        p[1] -= translate.y;

        float px = e.getPositionX() + e.getSizeWidth() * pivot.x;
        float py = e.getPositionY() + e.getSizeHeight() * pivot.y;

        // Translate to pivot
        p[0] -= px;
        p[1] -= py;

        // Inverse rotation
        if (rotationRad != 0f) {
            double cos = Math.cos(-rotationRad);
            double sin = Math.sin(-rotationRad);
            double x = p[0] * cos - p[1] * sin;
            double y = p[0] * sin + p[1] * cos;
            p[0] = x; p[1] = y;
        }

        // Inverse scale
        if (scale.x != 1f || scale.y != 1f) {
            double invSx = (scale.x == 0f ? 1e-6 : 1.0 / scale.x);
            double invSy = (scale.y == 0f ? 1e-6 : 1.0 / scale.y);
            p[0] *= invSx;
            p[1] *= invSy;
        }

        // Translate back from pivot
        p[0] += px;
        p[1] += py;
    }

    public void copyFrom(@NotNull Transform2D transform2D) {
        this.translate = new Vector2f(transform2D.translate);
        this.scale = new Vector2f(transform2D.scale);
        this.rotation = transform2D.rotation;
        this.pivot = transform2D.pivot;
        this.rotationRad = transform2D.rotationRad;
    }
}