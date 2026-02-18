package com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor;
import com.lowdragmc.lowdraglib2.math.Ray;
import com.lowdragmc.lowdraglib2.math.Transform;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneInteractable;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneRendering;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.SceneObject;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;

public class TransformGizmo extends SceneObject implements ISceneRendering, ISceneInteractable {
    public enum Mode {
        TRANSLATE,
        ROTATE,
        SCALE
    }
    private static final VoxelShape xAxisCollider = Shapes.box(0, -0.1, -0.1, 1.2, 0.1, 0.1);
    private static final VoxelShape xPlaneCollider = Shapes.box(0, 0.1, 0.1, 0.01, 0.3, 0.3);
    private static final VoxelShape yAxisCollider = Shapes.box(-0.1, 0, -0.1, 0.1, 1.2, 0.1);
    private static final VoxelShape yPlaneCollider = Shapes.box(0.1, 0, 0.1, 0.3, 0.01, 0.3);
    private static final VoxelShape zAxisCollider = Shapes.box(-0.1, -0.1, 0, 0.1, 0.1, 1.2);
    private static final VoxelShape zPlaneCollider = Shapes.box(0.1, 0.1, 0, 0.3, 0.3, 0.01);
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
    @Nullable
    @Setter
    private Runnable onTransformChanged;

    //runtime
    @Getter
    @Setter
    @Nonnull
    private Mode mode = Mode.TRANSLATE;
    private boolean isMovingX, isMovingY, isMovingZ, isMovingXPlane, isMovingYPlane, isMovingZPlane;
    private Vector3f moveDirection;
    private Vector3f startPosition;
    private Vector3f diffPosition;
    private Vector2f startMouse;

    public void setTargetTransform(@Nullable Transform targetTransform) {
        if (this.targetTransform == targetTransform) return;
        this.targetTransform = targetTransform;
        if (targetTransform != null) {
            transform().position(targetTransform.position());
            transform().rotation(targetTransform.rotation());
        }
    }

    public boolean isMoving() {
        return isMovingX || isMovingY || isMovingZ;
    }

    public boolean isMovingPlane() {
        return isMovingXPlane || isMovingYPlane || isMovingZPlane;
    }

    public boolean hasTargetTransform() {
        return targetTransform != null;
    }

    public boolean isHoverPlane(Direction.Axis axis) {
        var scene = getScene();
        if (scene instanceof SceneEditor editor && targetTransform != null) {
            return editor.getMouseRay()
                    .map(ray -> ray.worldToLocal(transform()).toInfinite())
                    .map(ray -> switch (axis) {
                        case X -> ray.clip(xPlaneCollider) != null;
                        case Y -> ray.clip(yPlaneCollider) != null;
                        case Z -> ray.clip(zPlaneCollider) != null;
                    }).orElse(false);
        }
        return false;
    }

    public boolean isHoverAxis(Direction.Axis axis) {
        var scene = getScene();
        if (scene instanceof SceneEditor editor && targetTransform != null) {
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
    @OnlyIn(Dist.CLIENT)
    public void updateFrame(float partialTicks) {
        super.updateFrame(partialTicks);
        if (getScene() instanceof SceneEditor editor && editor.getModularUI() != null) {
            var renderer = editor.scene.getRenderer();
            if (renderer == null) return;
            var distance = renderer.getEyePos().distance(transform().position());
            float baseScale = 0.23F;
            float gizmoScale = distance * (float) Math.tan(renderer.getFov() * 0.5f * Math.PI / 180) * baseScale;
//            if (lastScale != gizmoScale) {
//                transform().scale(new Vector3f(gizmoScale));
//                lastScale = gizmoScale;
//            }
            if (targetTransform == null) return;
            var hasChanged = false;
            if (!transform().position().equals(targetTransform.position())) {
                transform().position(targetTransform.position());
            }
            if (!transform().rotation().equals(targetTransform.rotation())) {
                transform().rotation(targetTransform.rotation());
            }
            if (isMovingPlane()) {
                Vector3f planeNormal = moveDirection;
                Vector3f point = startPosition;

                var ray = editor.getMouseRay().orElse(null);
                if (ray == null) return;
                var origin = ray.startPos();
                var direction = ray.getDirection();

                float denominator = direction.dot(planeNormal);
                if (Math.abs(denominator) < 1e-6f) {
                    return;
                }
                Vector3f originToPoint = new Vector3f(point).sub(origin);
                float t = originToPoint.dot(planeNormal) / denominator;
                Vector3f intersectionPoint = new Vector3f(origin).add(new Vector3f(direction).mul(t)).sub(startPosition);
                Vector3f newPosition = new Vector3f(startPosition).add(intersectionPoint);
                if (diffPosition == null) {
                    diffPosition = new Vector3f(newPosition).sub(transform().position());
                }
                newPosition = newPosition.sub(diffPosition);
                transform().position(newPosition);
                targetTransform.position(newPosition);
                hasChanged = true;
            } else if (isMoving()) {
                var lastMouseX = editor.getModularUI().getLastMouseX();
                var lastMouseY = editor.getModularUI().getLastMouseY();
                var currentPosition = transform().position();

                var moveD = transform().localToWorldMatrix().transformDirection(new Vector3f(moveDirection));
                var screenStart = editor.project(currentPosition);
                var screenEnd = editor.project(new Vector3f(currentPosition).add(moveD));
                if (screenStart.isEmpty() || screenEnd.isEmpty()) return;
                var screenAxis = new Vector2f(screenEnd.get()).sub(screenStart.get());

                if (screenAxis.length() > 0) {
                    screenAxis.normalize();
                } else {
                    return;
                }

                assert editor.getModularUI() != null;
                var mouseDelta = new Vector2f(lastMouseX, lastMouseY).sub(startMouse);
                if (mode == Mode.ROTATE) {
                    mouseDelta.set(-mouseDelta.y, mouseDelta.x);
                }
                var projectedLength = mouseDelta.dot(screenAxis);
                if (projectedLength == 0) {
                    return;
                }
                var distanceToCamera = renderer.getEyePos().distance(currentPosition);

                var fov = renderer.getFov();
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
                    hasChanged = true;
                } else if (mode == Mode.SCALE) {
                    var localScale = targetTransform.localScale().add(new Vector3f(moveDirection).mul(scaleDelta));
                    targetTransform.localScale(localScale);
                    hasChanged = true;
                } else if (mode == Mode.ROTATE) {
                    var localRotation = new Quaternionf(transform().localRotation());
                    localRotation.rotateAxis(-scaleDelta, moveDirection);
                    transform().localRotation(localRotation);
                    targetTransform.rotation(transform().rotation());
                    hasChanged = true;
                }

                startMouse.set(lastMouseX, lastMouseY);
            }
            if (hasChanged && onTransformChanged != null) {
                onTransformChanged.run();
            }
        }
    }

    @Override
    public void preDraw(float partialTicks) {
        RenderSystem.disableDepthTest();
    }

    @Override
    public void postDraw(float partialTicks) {
        RenderSystem.enableDepthTest();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInternal(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks) {
        if (targetTransform == null) return;
        var buffer = bufferSource.getBuffer(LDLibRenderTypes.noDepthLines());
        RenderSystem.disableDepthTest();

        var isHoverXPlane = isHoverPlane(Direction.Axis.X);
        var isHoverYPlane = !isHoverXPlane && isHoverPlane(Direction.Axis.Y);
        var isHoverZPlane = !isHoverXPlane && !isHoverYPlane && isHoverPlane(Direction.Axis.Z);
        var isHoverPlane = isHoverXPlane || isHoverYPlane || isHoverZPlane;

        var isHoverX = !isHoverPlane && isHoverAxis(Direction.Axis.X);
        var isHoverY = !isHoverPlane && !isHoverX && isHoverAxis(Direction.Axis.Y);
        var isHoverZ = !isHoverPlane && !isHoverX && !isHoverY && isHoverAxis(Direction.Axis.Z);

        var xColor = 0xFFFF0000;
        var yColor = 0xFF00FF00;
        var zColor = 0xFF0000FF;
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
            if (isMovingX || isHoverX) {
                xR = 1;
                xG = 1;
                xB = 1;
                xA = 1;
            }
            if (isMovingY || isHoverY) {
                yR = 1;
                yG = 1;
                yB = 1;
                yA = 1;
            }
            if (isMovingZ || isHoverZ) {
                zR = 1;
                zG = 1;
                zB = 1;
                zA = 1;
            }
            // draw x axis
            RenderBufferUtils.drawLine(poseStack.last(), buffer, new Vector3f(0, 0, 0), new Vector3f(mode == Mode.TRANSLATE ? 1 : scale.x, 0, 0),
                    xR, xG, xB, xA, xR, xG, xB, xA);
            if (isMovingX) {
                RenderBufferUtils.drawLine(poseStack.last(), buffer, new Vector3f(-50, 0, 0), new Vector3f(50, 0, 0),
                        1, 1, 1, 1, 1, 1, 1, 1);
            }
            // draw y axis
            RenderBufferUtils.drawLine(poseStack.last(), buffer, new Vector3f(0, 0, 0), new Vector3f(0, mode == Mode.TRANSLATE ? 1 : scale.y, 0),
                    yR, yG, yB, yA, yR, yG, yB, yA);
            if (isMovingY) {
                RenderBufferUtils.drawLine(poseStack.last(), buffer, new Vector3f(0, -50, 0), new Vector3f(0, 50, 0),
                        1, 1, 1, 1, 1, 1, 1, 1);
            }
            // draw z axis
            RenderBufferUtils.drawLine(poseStack.last(), buffer, new Vector3f(0, 0, 0), new Vector3f(0, 0, mode == Mode.TRANSLATE ? 1 : scale.z),
                    zR, zG, zB, zA, zR, zG, zB, zA);
            if (isMovingZ) {
                RenderBufferUtils.drawLine(poseStack.last(), buffer, new Vector3f(0, 0, -50), new Vector3f(0, 0, 50),
                        1, 1, 1, 1, 1, 1, 1, 1);
            }

            if (mode == Mode.TRANSLATE) {
                // draw arrow
                buffer = bufferSource.getBuffer(LDLibRenderTypes.positionColorNoDepth());
                RenderSystem.disableDepthTest();

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
                xR = 1f;
                xG = 1f;
                xB = 1f;
                xA = 1f;
                yR = 1f;
                yG = 1f;
                yB = 1f;
                yA = 1f;
                zR = 1f;
                zG = 1f;
                zB = 1f;
                zA = 1f;
                if (!isMovingXPlane && !isHoverXPlane || isMoving() || isHoverX) {
                    xG = 0;
                    xB = 0;
                }
                if (!isMovingYPlane && !isHoverYPlane || isMoving() || isHoverY) {
                    yR = 0;
                    yB = 0;
                }
                if (!isMovingZPlane && !isHoverZPlane || isMoving() || isHoverZ) {
                    zR = 0;
                    zG = 0;
                }
                RenderSystem.depthMask(true);
                // draw x surface
                RenderBufferUtils.drawCubeFace(poseStack, buffer, 0, 0.1f, 0.1f, 0, 0.3f, 0.3f,
                        xR, xG, xB, xA, false);
                // draw y surface
                RenderBufferUtils.drawCubeFace(poseStack, buffer, 0.1f, 0, 0.1f, 0.3f, 0, 0.3f,
                        yR, yG, yB, yA, false);
                // draw z surface
                RenderBufferUtils.drawCubeFace(poseStack, buffer, 0.1f, 0.1f, 0, 0.3f, 0.3f, 0,
                        zR, zG, zB, zA, false);
            }

            if (mode == Mode.SCALE) {
                // draw box
                buffer = bufferSource.getBuffer(LDLibRenderTypes.positionColorNoDepth());
                RenderSystem.disableDepthTest();

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
            if (isMovingX || isHoverX) {
                xR = 1;
                xG = 1;
                xB = 1;
                xA = 1;
            }
            if (isMovingY || isHoverY) {
                yR = 1;
                yG = 1;
                yB = 1;
                yA = 1;
            }
            if (isMovingZ || isHoverZ) {
                zR = 1;
                zG = 1;
                zB = 1;
                zA = 1;
            }
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
            RenderSystem.disableDepthTest();

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

        if (bufferSource instanceof MultiBufferSource.BufferSource source) {
            source.endLastBatch();
        }
    }

    @Override
    public boolean onMouseClick(Ray mouseRay) {
        if (getScene() instanceof SceneEditor editor && editor.getModularUI() != null) {
            var lastMouseX = editor.getModularUI().getLastMouseX();
            var lastMouseY = editor.getModularUI().getLastMouseY();
            if (mode == Mode.TRANSLATE) {
                if (isHoverPlane(Direction.Axis.X)) {
                    isMovingXPlane = true;
                    startPosition = transform().position();
                    moveDirection = transform().localToWorldMatrix().transformDirection(new Vector3f(1, 0, 0));
                } else if (isHoverPlane(Direction.Axis.Y)) {
                    isMovingYPlane = true;
                    startPosition = transform().position();
                    moveDirection = transform().localToWorldMatrix().transformDirection(new Vector3f(0, 1, 0));
                } else if (isHoverPlane(Direction.Axis.Z)) {
                    isMovingZPlane = true;
                    startPosition = transform().position();
                    moveDirection = transform().localToWorldMatrix().transformDirection(new Vector3f(0, 0, 1));
                }
                if (isMovingXPlane || isMovingYPlane || isMovingZPlane) {
                    startMouse = new Vector2f(lastMouseX, lastMouseY);
                    return true;
                }
            }
            if (isHoverAxis(Direction.Axis.X)) {
                isMovingX = true;
                moveDirection = new Vector3f(1, 0, 0);
                startMouse = new Vector2f(lastMouseX, lastMouseY);
                return true;
            } else if (isHoverAxis(Direction.Axis.Y)) {
                isMovingY = true;
                moveDirection = new Vector3f(0, 1, 0);
                startMouse = new Vector2f(lastMouseX, lastMouseY);
                return true;
            } else if (isHoverAxis(Direction.Axis.Z)) {
                isMovingZ = true;
                moveDirection = new Vector3f(0, 0, 1);
                startMouse = new Vector2f(lastMouseX, lastMouseY);
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
        isMovingXPlane = false;
        isMovingYPlane = false;
        isMovingZPlane = false;
        startMouse = null;
        moveDirection = null;
        startPosition = null;
        diffPosition = null;
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
