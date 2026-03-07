package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.google.common.util.concurrent.Runnables;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

public class EnhancedPoseStack {
    public final Matrix3x2fStack pose;
    @Setter @Accessors(chain = true)
    private Runnable onTransform = Runnables.doNothing();

    public EnhancedPoseStack(Matrix3x2fStack pose) {
        this.pose = pose;
    }

    public void translate(float x, float y) {
        pose.translate(x, y);
        onTransform.run();
    }

    public void scale(float x, float y) {
        pose.scale(x, y);
        onTransform.run();
    }

    public void rotate(float ang) {
        pose.rotate(ang);
        onTransform.run();
    }

    public void pushPose() {
        pose.pushMatrix();
    }

    public void popPose() {
        pose.popMatrix();
        onTransform.run();
    }

    public void clear() {
        pose.clear();
    }

    public void setIdentity() {
        pose.identity();
        onTransform.run();
    }

    public void mulPose(Matrix3x2f matrix) {
        pose.mul(matrix);
        onTransform.run();
    }

    public Matrix3x2f copyPose() {
        return new Matrix3x2f(pose);
    }
}
