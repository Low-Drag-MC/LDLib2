package com.lowdragmc.lowdraglib2.gui.ui.data;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;


/**
 * Lightweight 2D transform for UIElement: translate + rotate + scale around an origin (pivot).
 * Does not affect Yoga layout; only rendering and hit-testing.
 */
@Accessors(chain = true, fluent = true)
@EqualsAndHashCode
public final class Transform2D implements IConfigurable, IPersistedSerializable {
    public final static Codec<Transform2D> CODEC = PersistedParser.createCodec(Transform2D::new);

    @Getter
    @Configurable(name = "Transform2D.translate")
    private Translate2D translate = Translate2D.ZERO;
    @Getter
    @Configurable(name = "Transform2D.scale")
    private Vector2f scale = new Vector2f(1f);
    @Getter
    @Configurable(name = "Transform2D.rotation")
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE}, wheel = 1f)
    private float rotation = 0f;   // Z-axis degree

    /**
     * Transform origin as ratio of element size:
     * 0=left/top, 0.5=center, 1=right/bottom.
     */
    @Getter @Setter
    @Configurable(name = "Transform2D.pivot")
    private Pivot pivot = Pivot.CENTER;

    // runtime
    @EqualsAndHashCode.Exclude
    private float rotationRad;

    public static Transform2D identity() {
        return new Transform2D();
    }

    public boolean isIdentity() {
        return translate.isZero()
                && rotationRad == 0f
                && scale.x == 1f && scale.y == 1f;
    }

    public Transform2D setIdentity() {
        translate = Translate2D.ZERO;
        scale.set(1f, 1f);
        rotationRad = 0f;
        rotation = 0f;
        pivot = Pivot.CENTER;
        return this;
    }

    public Transform2D translate(float x, float y) {
        translate = Translate2D.px(x, y);
        return this;
    }

    public Transform2D translatePercent(float x, float y) {
        translate = Translate2D.percent(x, y);
        return this;
    }

    public Transform2D translate(Translate2D t) {
        this.translate = t;
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

    @Override
    public void beforeDeserialize() {
        setIdentity();
    }

    @SkipPersistedValue(field = "translate")
    private boolean skipTranslatePersisted(Translate2D translate) {
        return translate.isZero();
    }

    @SkipPersistedValue(field = "rotation")
    private boolean skipRotationPersisted(float rotation) {
        return rotation == 0f;
    }

    @SkipPersistedValue(field = "scale")
    private boolean skipScalePersisted(Vector2f scale) {
        return scale.x == 1f && scale.y == 1f;
    }

    @SkipPersistedValue(field = "pivot")
    private boolean skipPivotPersisted(Pivot pivot) {
        return pivot.equals(Pivot.CENTER);
    }

    public void pushPose(PoseStack poseStack, float x, float y, float width, float height) {
        if (isIdentity()) return;
        poseStack.pushPose();
        float tx = translate.resolveX(width);
        float ty = translate.resolveY(height);
        poseStack.translate(tx, ty, 0);

        var xPivot = pivot.x * width;
        var yPivot = pivot.y * height;
        var translationX = x + xPivot;
        var translationY = y + yPivot;

        if (rotationRad != 0f || scale.x != 1f || scale.y != 1f) {
            poseStack.translate(translationX, translationY, 0);
            if (rotationRad != 0f) {
                poseStack.mulPose(new Quaternionf().rotateLocalZ(rotationRad));
            }
            if (scale.x != 1f || scale.y != 1f) {
                poseStack.scale(scale.x, scale.y, 1);
            }
            poseStack.translate(-translationX, -translationY, 0);
        }
    }

    public void popPose(PoseStack poseStack) {
        if (!isIdentity()) {
            poseStack.popPose();
        }
    }

    // Apply to pose for rendering: T -> pivot -> R -> S -> -pivot
    public void pushPose(GUIContext ctx, UIElement e) {
        if (isIdentity()) return;
        float px = e.getPositionX() + e.getSizeWidth() * pivot.x;
        float py = e.getPositionY() + e.getSizeHeight() * pivot.y;

        ctx.pose.pushPose();

        float tx = translate.resolveX(e.getSizeWidth());
        float ty = translate.resolveY(e.getSizeHeight());
        if (tx != 0f || ty != 0f) {
            ctx.pose.translate(tx, ty, 0);
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

    public void pushPose(Matrix4f pose, UIElement e) {
        if (isIdentity()) return;
        float px = e.getPositionX() + e.getSizeWidth() * pivot.x;
        float py = e.getPositionY() + e.getSizeHeight() * pivot.y;

        float tx = translate.resolveX(e.getSizeWidth());
        float ty = translate.resolveY(e.getSizeHeight());
        if (tx != 0f || ty != 0f) {
            pose.translate(tx, ty, 0);
        }

        if (rotationRad != 0f || scale.x != 1f || scale.y != 1f) {
            pose.translate(px, py, 0);
            if (rotationRad != 0f) {
                pose.rotate(new Quaternionf().rotateLocalZ(rotationRad));
            }
            if (scale.x != 1f || scale.y != 1f) {
                pose.scale(scale.x, scale.y, 1);
            }
            pose.translate(-px, -py, 0);
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
        float tx = translate.resolveX(e.getSizeWidth());
        float ty = translate.resolveY(e.getSizeHeight());
        p[0] -= tx;
        p[1] -= ty;

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

    // Transform a point from the element's pre-transform space to screen space:
    // order: pivot -> S -> R -> -pivot -> T
    public void forwardPoint(UIElement e, double[] p /* [x,y] */) {
        if (isIdentity()) return;

        // Translation
        float tx = translate.resolveX(e.getSizeWidth());
        float ty = translate.resolveY(e.getSizeHeight());
        p[0] += tx;
        p[1] += ty;

        float px = e.getPositionX() + e.getSizeWidth()  * pivot.x;
        float py = e.getPositionY() + e.getSizeHeight() * pivot.y;

        // Translate to pivot
        p[0] += px;
        p[1] += py;

        // rotation
        if (rotationRad != 0f) {
            double cos = Math.cos(rotationRad);
            double sin = Math.sin(rotationRad);
            double x = p[0] * cos - p[1] * sin;
            double y = p[0] * sin + p[1] * cos;
            p[0] = x; p[1] = y;
        }

        // scale
        if (scale.x != 1f || scale.y != 1f) {
            p[0] *= scale.x;
            p[1] *= scale.y;
        }

        // return
        p[0] -= px;
        p[1] -= py;
    }

    public Transform2D copy() {
        var copied = new Transform2D();
        copied.copyFrom(this);
        return copied;
    }

    public void copyFrom(@NotNull Transform2D transform2D) {
        this.translate = transform2D.translate;
        this.scale = new Vector2f(transform2D.scale);
        this.rotation = transform2D.rotation;
        this.pivot = transform2D.pivot;
        this.rotationRad = transform2D.rotationRad;
    }

    public static Transform2D interpolate(Transform2D a, Transform2D b, float t) {
        var copied = new Transform2D();
        copied.translate = Translate2D.lerp(a.translate, b.translate, t);
        copied.scale = a.scale.lerp(b.scale, t, new Vector2f());
        copied.rotation = a.rotation + (b.rotation - a.rotation) * t;
        copied.pivot = Pivot.of(a.pivot.x + (b.pivot.x - a.pivot.x) * t, a.pivot.y + (b.pivot.y - a.pivot.y) * t);
        copied.rotationRad = a.rotationRad + (b.rotationRad - a.rotationRad) * t;
        return copied;
    }
}
