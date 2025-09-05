package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class EnhancedPoseStack {
    public final PoseStack pose;
    @Setter @Accessors(chain = true)
    private Runnable onTransform = Runnables.doNothing();

    public EnhancedPoseStack(PoseStack pose) {
        this.pose = pose;
    }

    public void translate(double x, double y, double z) {
        pose.translate(x, y, z);
        onTransform.run();
    }

    public void translate(float x, float y, float z) {
        pose.translate(x, y, z);
        onTransform.run();
    }

    public void scale(float x, float y, float z) {
        pose.scale(x, y, z);
        onTransform.run();
    }

    public void mulPose(Quaternionf quaternion) {
       pose.mulPose(quaternion);
        onTransform.run();
    }

    public void rotateAround(Quaternionf quaternion, float x, float y, float z) {
        pose.rotateAround(quaternion, x, y, z);
        onTransform.run();
    }

    public void pushPose() {
        pose.pushPose();
    }

    public void popPose() {
        pose.popPose();
        onTransform.run();
    }

    public PoseStack.Pose last() {
        return pose.last();
    }

    public boolean clear() {
        return pose.clear();
    }

    public void setIdentity() {
        pose.setIdentity();
        onTransform.run();
    }

    public void mulPose(Matrix4f pose) {
       this.pose.mulPose(pose);
        onTransform.run();
    }

    public void pushTransformation(Transformation transformation) {
        pose.pushTransformation(transformation);
        onTransform.run();
    }
}
