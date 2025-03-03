package com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.sceneobject.utils;

import com.lowdragmc.lowdraglib.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.SceneEditorWidget;
import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.data.Ray;
import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.data.Transform;
import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.sceneobject.ISceneInteractable;
import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.sceneobject.ISceneRendering;
import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.sceneobject.SceneObject;
import com.lowdragmc.lowdraglib.utils.ColorUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TransformGizmo extends SceneObject implements ISceneRendering, ISceneInteractable {
    public enum Mode {
        TRANSLATE,
        ROTATE,
        SCALE
    }
    private static final VoxelShape xAxisCollider = Shapes.box(0, -0.1, -0.1, 1.2, 0.1, 0.1);
    private static final VoxelShape yAxisCollider = Shapes.box(-0.1, 0, -0.1, 0.1, 1.2, 0.1);
    private static final VoxelShape zAxisCollider = Shapes.box(-0.1, -0.1, 0, 0.1, 0.1, 1.2);
    private static final VoxelShape xRingCollider = createRingCollisionBox(
            new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), 1.0, 16, 0.1
    );
    private static final VoxelShape yRingCollider = createRingCollisionBox(
            new Vector3f(0, 0, 0), new Vector3f(0, 1, 0), 1.0, 16, 0.1
    );
    private static final VoxelShape zRingCollider = createRingCollisionBox(
            new Vector3f(0, 0, 0), new Vector3f(0, 0, 1), 1.0, 16, 0.1
    );


    @Nullable
    @Getter
    private Transform targetTransform;

    //runtime
    @Getter
    @Setter
    @Nonnull
    private Mode mode = Mode.TRANSLATE;
    private boolean isMovingX, isMovingY, isMovingZ;
    private Vector3f moveDirection;
    private Vector2f startMouse;
    private float lastScale = 1;

    public void setTargetTransform(@Nullable Transform targetTransform) {
        if (this.targetTransform == targetTransform) return;
        this.targetTransform = targetTransform;
        if (targetTransform != null) {
            transform().position(targetTransform.position());
            transform().rotation(targetTransform.rotation());
        }
    }

    public boolean hasTargetTransform() {
        return targetTransform != null;
    }

    public boolean isHoverAxis(Direction.Axis axis) {
        var scene = getScene();
        if (scene instanceof SceneEditorWidget editor && targetTransform != null) {
            return switch (mode) {
                case TRANSLATE -> editor.getMouseRay()
                        .map(ray -> ray.worldToLocal(transform()).toInfinite())
                        .map(ray -> switch (axis) {
                            case X -> ray.clip(xAxisCollider) != null;
                            case Y -> ray.clip(yAxisCollider) != null;
                            case Z -> ray.clip(zAxisCollider) != null;
                        }).orElse(false);
                case ROTATE -> editor.getMouseRay()
                        .map(ray -> ray.worldToLocal(transform()).toInfinite())
                        .map(ray -> switch (axis) {
                            case X -> ray.clip(xRingCollider) != null;
                            case Y -> ray.clip(yRingCollider) != null;
                            case Z -> ray.clip(zRingCollider) != null;
                        }).orElse(false);
                case SCALE -> editor.getMouseRay()
                        .map(ray -> ray.worldToLocal(transform()).toInfinite())
                        .map(ray -> {
                            if (targetTransform == null) return false;
                            var scale = targetTransform.scale();
                            return switch (axis) {
                                case X -> ray.clip(Shapes.box(
                                        Math.min(0.2 + scale.x, 0), -0.1, -0.1,
                                        Math.max(0.2 + scale.x, 0), 0.1, 0.1)) != null;
                                case Y -> ray.clip(Shapes.box(
                                        -0.1, Math.min(0.2 + scale.y, 0), -0.1,
                                        0.1, Math.max(0.2 + scale.y, 0), 0.1)) != null;
                                case Z -> ray.clip(Shapes.box(
                                        -0.1, -0.1, Math.min(0.2 + scale.z, 0),
                                        0.1, 0.1, Math.max(0.2 + scale.z, 0))) != null;
                            };
                        }).orElse(false);
            };
        }
        return false;
    }

    @Override
    public void onTransformChanged() {
        super.onTransformChanged();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void updateFrame(float partialTicks) {
        super.updateFrame(partialTicks);
        if (getScene() instanceof SceneEditorWidget editor) {
            var distance = editor.getRenderer().getEyePos().distance(transform().position());
            float baseScale = 0.23F;
            float gizmoScale = distance * (float) Math.tan(editor.getRenderer().getFov() * 0.5f * Math.PI / 180) * baseScale;
            if (lastScale != gizmoScale) {
                transform().scale(new Vector3f(gizmoScale));
                lastScale = gizmoScale;
            }
            if (targetTransform == null) return;
            if (!transform().position().equals(targetTransform.position())) {
                transform().position(targetTransform.position());
            }
            if (!transform().rotation().equals(targetTransform.rotation())) {
                transform().rotation(targetTransform.rotation());
            }
            if (isMovingX || isMovingY || isMovingZ) {
                var currentPosition = transform().position();

                var moveD = transform().localToWorldMatrix().transformDirection(new Vector3f(moveDirection));
                var screenStart = editor.project(currentPosition);
                var screenEnd = editor.project(new Vector3f(currentPosition).add(moveD));
                var screenAxis = new Vector2f(screenEnd).sub(screenStart);

                if (screenAxis.length() > 0) {
                    screenAxis.normalize();
                } else {
                    return;
                }

                var mouseDelta = new Vector2f(editor.getLastMouseX(), editor.getLastMouseY()).sub(startMouse);
                if (mode == Mode.ROTATE) {
                    mouseDelta.set(-mouseDelta.y, mouseDelta.x);
                }
                var projectedLength = mouseDelta.dot(screenAxis);
                if (projectedLength == 0) {
                    return;
                }
                var distanceToCamera = editor.getRenderer().getEyePos().distance(currentPosition);

                var fov = editor.getRenderer().getFov();
                var screenHeight = editor.getSizeHeight();
                float worldHeight = 2.0f * distanceToCamera * (float) Math.tan(Math.toRadians(fov / 2));
                float pixelToWorldScale = worldHeight / screenHeight;
                var scaleDelta = projectedLength * pixelToWorldScale;
                if (scaleDelta == 0) {
                    return;
                }

                if (mode == Mode.TRANSLATE) {
                    var position = currentPosition.add(new Vector3f(moveD).mul(scaleDelta));
                    transform().position(position);
                    targetTransform.position(position);
                } else if (mode == Mode.SCALE) {
                    var localScale = targetTransform.localScale().add(new Vector3f(moveDirection).mul(scaleDelta));
                    targetTransform.localScale(localScale);
                } else if (mode == Mode.ROTATE) {
                    var localRotation = transform().localRotation();
                    localRotation.rotateAxis(scaleDelta, moveDirection);
                    transform().localRotation(localRotation);
                    targetTransform.localRotation(localRotation);
                }

                startMouse.set(editor.getLastMouseX(), editor.getLastMouseY());
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInternal(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks) {
        if (targetTransform == null) return;
        var buffer = bufferSource.getBuffer(LDLibRenderTypes.noDepthLines());
        var pose = poseStack.last().pose();
        var hoverColor = 0xFFFFFFFF;
        var isHoverX = isHoverAxis(Direction.Axis.X);
        var isHoverY = !isHoverX && isHoverAxis(Direction.Axis.Y);
        var isHoverZ = !isHoverX && !isHoverY && isHoverAxis(Direction.Axis.Z);
        var xColor = !isHoverX ? 0xFFFF0000 : hoverColor;
        var yColor = !isHoverY ? 0xFF00FF00 : hoverColor;
        var zColor = !isHoverZ ? 0xFF0000FF : hoverColor;
        var xR = ColorUtils.red(xColor);
        var xG = ColorUtils.green(xColor);
        var xB = ColorUtils.blue(xColor);
        var xA = ColorUtils.alpha(xColor);
        var yR = ColorUtils.red(yColor);
        var yG = ColorUtils.green(yColor);
        var yB = ColorUtils.blue(yColor);
        var yA = ColorUtils.alpha(yColor);
        var zR = ColorUtils.red(zColor);
        var zG = ColorUtils.green(zColor);
        var zB = ColorUtils.blue(zColor);
        var zA = ColorUtils.alpha(zColor);
        if (mode == Mode.SCALE || mode == Mode.TRANSLATE) {
            var scale = targetTransform.scale();
            // draw x axis
            RenderBufferUtils.drawLine(pose, buffer, new Vector3f(0, 0, 0), new Vector3f(mode == Mode.TRANSLATE ? 1 : scale.x, 0, 0),
                    xR, xG, xB, xA, xR, xG, xB, xA);
            if (isMovingX) {
                RenderBufferUtils.drawLine(pose, buffer, new Vector3f(-50, 0, 0), new Vector3f(50, 0, 0),
                        xR, xG, xB, xA, xR, xG, xB, xA);
            }
            // draw y axis
            RenderBufferUtils.drawLine(pose, buffer, new Vector3f(0, 0, 0), new Vector3f(0, mode == Mode.TRANSLATE ? 1 : scale.y, 0),
                    yR, yG, yB, yA, yR, yG, yB, yA);
            if (isMovingY) {
                RenderBufferUtils.drawLine(pose, buffer, new Vector3f(0, -50, 0), new Vector3f(0, 50, 0),
                        yR, yG, yB, yA, yR, yG, yB, yA);
            }
            // draw z axis
            RenderBufferUtils.drawLine(pose, buffer, new Vector3f(0, 0, 0), new Vector3f(0, 0, mode == Mode.TRANSLATE ? 1 : scale.z),
                    zR, zG, zB, zA, zR, zG, zB, zA);
            if (isMovingZ) {
                RenderBufferUtils.drawLine(pose, buffer, new Vector3f(0, 0, -50), new Vector3f(0, 0, 50),
                        zR, zG, zB, zA, zR, zG, zB, zA);
            }

            if (mode == Mode.TRANSLATE) {
                // draw arrow
                buffer = bufferSource.getBuffer(LDLibRenderTypes.positionColorNoDepth());
                // draw x arrow
                RenderBufferUtils.shapeCone(poseStack, buffer, 1, 0, 0, 0.05f, 0.15f, 10,
                        xR, xG, xB, xA, Direction.Axis.X);
                RenderBufferUtils.shapeCircle(poseStack, buffer, 1, 0, 0, 0.05f, 10,
                        xR, xG, xB, xA, Direction.Axis.X);
                // draw y arrow
                RenderBufferUtils.shapeCone(poseStack, buffer, 0, 1, 0, 0.05f, 0.15f, 10,
                        yR, yG, yB, yA, Direction.Axis.Y);
                RenderBufferUtils.shapeCircle(poseStack, buffer, 0, 1, 0, 0.05f, 10,
                        yR, yG, yB, yA, Direction.Axis.Y);
                // draw z arrow
                RenderBufferUtils.shapeCone(poseStack, buffer, 0, 0, 1, 0.05f, 0.15f, 10,
                        zR, zG, zB, zA, Direction.Axis.Z);
                RenderBufferUtils.shapeCircle(poseStack, buffer, 0, 0, 1, 0.05f, 10,
                        zR, zG, zB, zA, Direction.Axis.Z);
            }

            if (mode == Mode.SCALE) {
                // draw box
                buffer = bufferSource.getBuffer(LDLibRenderTypes.positionColorNoDepth());
                // draw x box
                RenderBufferUtils.drawCubeFace(poseStack, buffer, scale.x - 0.05f, -0.05f, -0.05f, scale.x + 0.05f, 0.05f, 0.05f,
                        xR, xG, xB, xA, true);
                // draw y box
                RenderBufferUtils.drawCubeFace(poseStack, buffer, -0.05f, scale.y - 0.05f, -0.05f, 0.05f, scale.y + 0.05f, 0.05f,
                        yR, yG, yB, yA, true);
                // draw z box
                RenderBufferUtils.drawCubeFace(poseStack, buffer, -0.05f, -0.05f, scale.z - 0.05f, 0.05f, 0.05f, scale.z + 0.05f,
                        zR, zG, zB, zA, true);
            }
        }

        if (mode == Mode.ROTATE) {
            // draw x ring
            RenderBufferUtils.drawCircleLine(poseStack, buffer, new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), 50,
                    1f, xR, xG, xB, xA);

            // draw y ring
            RenderBufferUtils.drawCircleLine(poseStack, buffer, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0), 50,
                    1f, yR, yG, yB, yA);

            // draw z ring
            RenderBufferUtils.drawCircleLine(poseStack, buffer, new Vector3f(0, 0, 0), new Vector3f(0, 0, 1), 50,
                    1f, zR, zG, zB, zA);

            // draw box
            buffer = bufferSource.getBuffer(LDLibRenderTypes.positionColorNoDepth());
            // draw x box
            RenderBufferUtils.drawCubeFace(poseStack, buffer, 0.95f, -0.05f, -0.05f, 1.05f, 0.05f, 0.05f,
                    zR, zG, zB, zA, true);
            // draw y box
            RenderBufferUtils.drawCubeFace(poseStack, buffer, -0.05f, 0.95f, -0.05f, 0.05f, 1.05f, 0.05f,
                    xR, xG, xB, xA, true);
            // draw z box
            RenderBufferUtils.drawCubeFace(poseStack, buffer, -0.05f, -0.05f, 0.95f, 0.05f, 0.05f, 1.05f,
                    yR, yG, yB, yA, true);
        }
    }

    @Override
    public boolean onMouseClick(Ray mouseRay) {
        if (getScene() instanceof SceneEditorWidget editor) {
            if (isHoverAxis(Direction.Axis.X)) {
                isMovingX = true;
                moveDirection = new Vector3f(1, 0, 0);
                startMouse = new Vector2f(editor.getLastMouseX(), editor.getLastMouseY());
                return true;
            } else if (isHoverAxis(Direction.Axis.Y)) {
                isMovingY = true;
                moveDirection = new Vector3f(0, 1, 0);
                startMouse = new Vector2f(editor.getLastMouseX(), editor.getLastMouseY());
                return true;
            } else if (isHoverAxis(Direction.Axis.Z)) {
                isMovingZ = true;
                moveDirection = new Vector3f(0, 0, 1);
                startMouse = new Vector2f(editor.getLastMouseX(), editor.getLastMouseY());
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMouseRelease(Ray mouseRay) {
        isMovingX = false;
        isMovingY = false;
        isMovingZ = false;
        startMouse = null;
        moveDirection = null;
    }

    public static VoxelShape createRingCollisionBox(Vector3f center, Vector3f normal, double radius, int segments, double thickness) {
        VoxelShape ringShape = Shapes.empty();
        double angleStep = 2 * Math.PI / segments;

        Vector3f u = new Vector3f();
        Vector3f v = new Vector3f();

        if (normal.equals(new Vector3f(0, 0, 1)) || normal.equals(new Vector3f(0, 0, -1))) {
            u.set(1, 0, 0);
            v.set(0, 1, 0);
        } else {
            if (Math.abs(normal.x) < Math.abs(normal.y) && Math.abs(normal.x) < Math.abs(normal.z)) {
                u.set(0, -normal.z, normal.y).normalize();
            } else if (Math.abs(normal.y) < Math.abs(normal.x) && Math.abs(normal.y) < Math.abs(normal.z)) {
                u.set(-normal.z, 0, normal.x).normalize();
            } else {
                u.set(-normal.y, normal.x, 0).normalize();
            }
            v.set(normal).cross(u).normalize();
            u.cross(normal, v).normalize();
        }

        for (int i = 0; i < segments; i++) {
            double angle = i * angleStep;
            double nextAngle = (i + 1) * angleStep;

            Vector3f start = new Vector3f(center)
                    .add((float) (radius * Math.cos(angle) * u.x + radius * Math.sin(angle) * v.x),
                            (float) (radius * Math.cos(angle) * u.y + radius * Math.sin(angle) * v.y),
                            (float) (radius * Math.cos(angle) * u.z + radius * Math.sin(angle) * v.z));

            Vector3f end = new Vector3f(center)
                    .add((float) (radius * Math.cos(nextAngle) * u.x + radius * Math.sin(nextAngle) * v.x),
                            (float) (radius * Math.cos(nextAngle) * u.y + radius * Math.sin(nextAngle) * v.y),
                            (float) (radius * Math.cos(nextAngle) * u.z + radius * Math.sin(nextAngle) * v.z));

            double minX = Math.min(start.x, end.x) - thickness / 2;
            double maxX = Math.max(start.x, end.x) + thickness / 2;
            double minY = Math.min(start.y, end.y) - thickness / 2;
            double maxY = Math.max(start.y, end.y) + thickness / 2;
            double minZ = Math.min(start.z, end.z) - thickness / 2;
            double maxZ = Math.max(start.z, end.z) + thickness / 2;

            VoxelShape segmentBox = Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);

            ringShape = Shapes.or(ringShape, segmentBox);
        }

        return ringShape;
    }
}
