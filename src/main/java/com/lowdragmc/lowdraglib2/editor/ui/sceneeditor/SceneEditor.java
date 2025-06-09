package com.lowdragmc.lowdraglib2.editor.ui.sceneeditor;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.editor_outdated.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.math.Ray;
import com.lowdragmc.lowdraglib2.math.Transform;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.IScene;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneInteractable;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneObject;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneRendering;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils.TransformGizmo;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.appliedenergistics.yoga.*;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A scene which provides editable features as a unity scene.
 */
public class SceneEditor extends UIElement implements IScene {
    public static final Object SCENE_OBJECT_DRAGGING = new Object();
    public static final Object CAMERA_MOVING = new Object();
    public final UIElement topBar;
    public final Scene scene;
    public final UIElement gizmoBar;
    public final TextElement screenTips;

    protected float moveSpeed = 0.1f;
    protected boolean isCameraMoving = false;
    protected int tipsDuration = 0;
    @Getter
    protected Map<UUID, ISceneObject> sceneObjects = new LinkedHashMap<>();
    @Getter
    protected final TransformGizmo transformGizmo;

    public SceneEditor() {
        this.topBar = new UIElement();
        topBar.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeight(16);
            layout.setPadding(YogaEdge.ALL, 1);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID));

        this.scene = new Scene() {
            @Override
            protected void renderBeforeBatchEnd(MultiBufferSource bufferSource, float partialTicks) {
                SceneEditor.this.renderBeforeBatchEnd(bufferSource, partialTicks);
            }
        };
        this.scene.setRenderFacing(false);
        this.scene.setRenderSelect(false);
        this.scene.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
        });

        this.gizmoBar = new UIElement();
        gizmoBar.layout(layout -> {
            layout.setPositionType(YogaPositionType.ABSOLUTE);
            layout.setPosition(YogaEdge.TOP, 18);
            layout.setWidth(16);
            layout.setPadding(YogaEdge.ALL, 1);
            layout.setGap(YogaGutter.ALL, 1);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID));

        this.screenTips = new TextElement();
        screenTips.textStyle(style -> {
            style.textAlignHorizontal(Horizontal.CENTER);
            style.textAlignVertical(Vertical.CENTER);
        }).layout(layout -> {
            layout.setPositionType(YogaPositionType.ABSOLUTE);
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        });
//        this.scene.addChild(screenTips);

        transformGizmo = new TransformGizmo();
        transformGizmo.setScene(this);

        initGizmos();

        addChildren(topBar, scene, gizmoBar);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown, true);
        addEventListener(UIEvents.MOUSE_UP, this::onMouseUp, true);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onMouseDrag);
        addEventListener(UIEvents.MOUSE_WHEEL, this::onMouseWheel, true);
    }

    public void disableTransformGizmo() {
        gizmoBar.setDisplay(YogaDisplay.NONE);
    }

    public void enableTransformGizmo() {
        gizmoBar.setDisplay(YogaDisplay.FLEX);
    }

    public void setTransformGizmoTarget(@Nullable Transform transform) {
        transformGizmo.setTargetTransform(transform);
        gizmoBar.setActive(transform != null);
    }

    public void initGizmos() {
        // translate
        var toggleGroup = new Toggle.ToggleGroup();
        gizmoBar.addChild(new Toggle().setToggleGroup(toggleGroup)
                .setText("")
                .setOn(transformGizmo.getMode() == TransformGizmo.Mode.TRANSLATE, false)
                .toggleButton(button -> button.layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeightPercent(100);
                }))
                .setOnToggleChanged(isOn -> {
                    transformGizmo.setMode(TransformGizmo.Mode.TRANSLATE);
                }).toggleStyle(style -> {
                    style.baseTexture(IGuiTexture.EMPTY);
                    style.hoverTexture(ColorPattern.T_BLUE.rectTexture());
                    style.unmarkTexture(Icons.TRANSFORM_TRANSLATE);
                    style.markTexture(new GuiTextureGroup(ColorPattern.T_BLUE.rectTexture(), Icons.TRANSFORM_TRANSLATE));
                }).layout(layout -> {
                    layout.setPadding(YogaEdge.ALL, 0);
                    layout.setWidthPercent(100);
                    layout.setAspectRatio(1f);
                }));

        // rotation
        gizmoBar.addChild(new Toggle().setToggleGroup(toggleGroup)
                .setText("")
                .setOn(transformGizmo.getMode() == TransformGizmo.Mode.ROTATE, false)
                .toggleButton(button -> button.layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeightPercent(100);
                }))
                .setOnToggleChanged(isOn -> {
                    transformGizmo.setMode(TransformGizmo.Mode.ROTATE);
                }).toggleStyle(style -> {
                    style.baseTexture(IGuiTexture.EMPTY);
                    style.hoverTexture(ColorPattern.T_BLUE.rectTexture());
                    style.unmarkTexture(Icons.TRANSFORM_ROTATE);
                    style.markTexture(new GuiTextureGroup(ColorPattern.T_BLUE.rectTexture(), Icons.TRANSFORM_ROTATE));
                }).layout(layout -> {
                    layout.setPadding(YogaEdge.ALL, 0);
                    layout.setWidthPercent(100);
                    layout.setAspectRatio(1f);
                }));

        // scale
        gizmoBar.addChild(new Toggle().setToggleGroup(toggleGroup)
                .setText("")
                .setOn(transformGizmo.getMode() == TransformGizmo.Mode.SCALE, false)
                .toggleButton(button -> button.layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeightPercent(100);
                }))
                .setOnToggleChanged(isOn -> {
                    transformGizmo.setMode(TransformGizmo.Mode.SCALE);
                }).toggleStyle(style -> {
                    style.baseTexture(IGuiTexture.EMPTY);
                    style.hoverTexture(ColorPattern.T_BLUE.rectTexture());
                    style.unmarkTexture(Icons.TRANSFORM_SCALE);
                    style.markTexture(new GuiTextureGroup(ColorPattern.T_BLUE.rectTexture(), Icons.TRANSFORM_SCALE));
                }).layout(layout -> {
                    layout.setPadding(YogaEdge.ALL, 0);
                    layout.setWidthPercent(100);
                    layout.setAspectRatio(1f);
                }));
    }

    public Optional<Ray> getMouseRay() {
        var renderer = scene.getRenderer();
        if (renderer == null) return Optional.empty();
        var lastHit = renderer.getLastHit();
        return lastHit == null ? Optional.empty() : Optional.of(Ray.create(renderer.getEyePos(), lastHit));
    }

    public Optional<Ray> unProject(int mouseX, int mouseY) {
        var renderer = scene.getRenderer();
        if (renderer == null) return Optional.empty();
        var mouse = renderer.getPositionedRect(mouseX, mouseY, 0, 0);
        return Optional.of(new Ray(renderer.getEyePos(), renderer.unProject(mouse.position.x, mouse.position.y, false)));
    }

    public Optional<Vector2f> project(Vector3f pos) {
        var renderer = scene.getRenderer();
        if (renderer == null) return Optional.empty();
        var window = Minecraft.getInstance().getWindow();
        var result = renderer.project(pos);
        var x = result.x() * window.getGuiScaledWidth() / window.getWidth();
        var y = (window.getHeight() - result.y()) * window.getGuiScaledHeight() / window.getHeight();
        return Optional.of(new Vector2f(x, y));
    }

    public void setScreenTips(String tips) {
        this.screenTips.setText(tips);
        tipsDuration = 20;
    }

    @Override
    @Nullable
    public ISceneObject getSceneObject(UUID uuid) {
        return sceneObjects.get(uuid);
    }

    @Override
    public Collection<ISceneObject> getAllSceneObjects() {
        return sceneObjects.values();
    }

    @Override
    public void addSceneObjectInternal(ISceneObject sceneObject) {
        sceneObject.setScene(this);
        sceneObjects.put(sceneObject.id(), sceneObject);
    }

    @Override
    public void removeSceneObjectInternal(ISceneObject sceneObject) {
        sceneObjects.remove(sceneObject.id(), sceneObject);
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (tipsDuration > 0) {
            tipsDuration--;
            if (tipsDuration == 0) {
                screenTips.setText("");
            }
        }
        for (ISceneObject sceneObject : sceneObjects.values()) {
            sceneObject.executeAll(ISceneObject::updateTick);
        }
        if (transformGizmo.hasTargetTransform()) {
            transformGizmo.updateTick();
        }
    }

    protected void onMouseDown(UIEvent event) {
        if (event.button == 0 && event.target == scene) {
            if (getMouseRay().map(ray -> {
                var result = new AtomicBoolean(false);
                for (ISceneObject sceneObject : sceneObjects.values()) {
                    sceneObject.executeAll(so -> {
                        if (so instanceof ISceneInteractable sceneInteractable) {
                            result.set(result.get() | sceneInteractable.onMouseClick(ray));
                        }
                    });
                }
                if (transformGizmo.hasTargetTransform()) {
                    result.set(result.get() | transformGizmo.onMouseClick(ray));
                }
                return result.get();
            }).orElse(false)) {
                // block scene event
                startDrag(SCENE_OBJECT_DRAGGING, null);
                event.stopPropagation();
            }
        } else if (event.button == 1 && event.target == scene) {
            isCameraMoving = true;
            startDrag(CAMERA_MOVING, null);
            event.stopPropagation();
        }
    }

    protected void onMouseUp(UIEvent event) {
        if (event.button == 0 && event.target == scene) {
            getMouseRay().ifPresent(ray -> {
                for (ISceneObject sceneObject : sceneObjects.values()) {
                    sceneObject.executeAll(so -> {
                        if (so instanceof ISceneInteractable sceneInteractable) {
                            sceneInteractable.onMouseRelease(ray);
                        }
                    });
                }
                if (transformGizmo.hasTargetTransform()) {
                    transformGizmo.onMouseRelease(ray);
                }
            });
        } else if (event.button == 1 && event.target == scene) {
            isCameraMoving = false;
        }
    }

    protected void onMouseDrag(UIEvent event) {
        if (event.target == this) {
            if (event.dragHandler.getDraggingObject() == SCENE_OBJECT_DRAGGING) {
                getMouseRay().ifPresent(ray -> {
                    for (ISceneObject sceneObject : sceneObjects.values()) {
                        sceneObject.executeAll(so -> {
                            if (so instanceof ISceneInteractable sceneInteractable) {
                                sceneInteractable.onMouseDrag(ray);
                            }
                        });
                    }
                    if (transformGizmo.hasTargetTransform()) {
                        transformGizmo.onMouseDrag(ray);
                    }
                });
            } else if (event.dragHandler.getDraggingObject() == CAMERA_MOVING) {
                var renderer = scene.getRenderer();
                if (renderer == null) return;
                var eyePos = renderer.getEyePos();
                var lookAt = renderer.getLookAt();
                var worldUp = renderer.getWorldUp();
                var lookDir = new Vector3f(lookAt).sub(eyePos);
                var cross = new Vector3f(lookDir).cross(worldUp).normalize();
                lookDir = new Vector3f(lookDir).rotate(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-event.deltaY + 360), cross)));
                lookDir = new Vector3f(lookDir).rotate(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-event.deltaX + 360), worldUp)));
                scene.setCenter(new Vector3f(eyePos).add(new Vector3f(lookDir)));
                Vector3f pos = new Vector3f(eyePos).sub(lookAt);
                scene.setCameraYawAndPitch(
                        (float) Math.toDegrees(Math.atan2(pos.z, pos.x)),
                        (float) Math.toDegrees(Math.atan2(pos.y, Math.sqrt(pos.x * pos.x + pos.z * pos.z)))
                );
//                renderer.setCameraLookAt(eyePos, scene.getCenter(), worldUp);
            }
        }
    }

    protected void onMouseWheel(UIEvent event) {
        if (isCameraMoving) {
            if (event.deltaY > 0) {
                moveSpeed = Mth.clamp(moveSpeed + 0.01f, 0.02f, 10);
            } else {
                moveSpeed = Mth.clamp(moveSpeed - 0.01f, 0.02f, 10);
            }
            setScreenTips("Move Speed: x%.2f".formatted(moveSpeed));
            // block scene events
            event.stopPropagation();
        }
    }

    protected void renderBeforeBatchEnd(MultiBufferSource bufferSource, float partialTicks) {
        var poseStack = new PoseStack();
        for (ISceneObject sceneObject : sceneObjects.values()) {
            sceneObject.executeAll(so -> so.updateFrame(partialTicks));
            sceneObject.executeAll(so -> {
                if (so instanceof ISceneRendering sceneRendering) {
                    sceneRendering.draw(poseStack, bufferSource, partialTicks);
                }
            }, so -> { // before
                if (so instanceof ISceneRendering sceneRendering) {
                    sceneRendering.preDraw(partialTicks);
                }
            }, so -> { // after
                if (so instanceof ISceneRendering sceneRendering) {
                    sceneRendering.postDraw(partialTicks);
                }
            });
        }
        if (bufferSource instanceof MultiBufferSource.BufferSource buffer) {
            buffer.endBatch();
        }
        if (transformGizmo.hasTargetTransform()) {
            transformGizmo.updateFrame(partialTicks);
            transformGizmo.preDraw(partialTicks);
            transformGizmo.draw(poseStack, bufferSource, partialTicks);
            transformGizmo.postDraw(partialTicks);
        }
    }

    @Override
    public void drawBackgroundAdditional(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackgroundAdditional(graphics, mouseX, mouseY, partialTicks);
        var renderer = scene.getRenderer();
        if (isCameraMoving && renderer != null) {
            var _forward = isKeyDown(GLFW.GLFW_KEY_W);
            var _backward = isKeyDown(GLFW.GLFW_KEY_S);
            var _left = isKeyDown(GLFW.GLFW_KEY_A);
            var _right = isKeyDown(GLFW.GLFW_KEY_D);
            var _up = isKeyDown(GLFW.GLFW_KEY_E);
            var _down = isKeyDown(GLFW.GLFW_KEY_Q);
            if (_forward || _backward || _left || _right || _up || _down) {
                var eyePos = renderer.getEyePos();
                var lookAt = renderer.getLookAt();
                var worldUp = renderer.getWorldUp();
                var lookDir = new Vector3f(lookAt).sub(eyePos);
                var realMoveSpeed = moveSpeed * partialTicks * (isShiftDown() ? 5 : 1);
                var forward = new Vector3f(lookDir).normalize().mul(realMoveSpeed);
                var right = new Vector3f(lookDir).cross(worldUp).normalize().mul(realMoveSpeed);
                var up = new Vector3f(worldUp).normalize().mul(realMoveSpeed);
                if (_forward) { // move forward
                    eyePos.add(forward);
                    lookAt.add(forward);
                }
                if (_backward) { // move backward
                    eyePos.sub(forward);
                    lookAt.sub(forward);
                }
                if (_left) { // move left
                    eyePos.sub(right);
                    lookAt.sub(right);
                }
                if (_right) { // move right
                    eyePos.add(right);
                    lookAt.add(right);
                }
                if (_up) { // move up
                    eyePos.add(up);
                    lookAt.add(up);
                }
                if (_down) { // move down
                    eyePos.sub(up);
                    lookAt.sub(up);
                }
                // update renderer
                renderer.setCameraLookAt(eyePos, lookAt, worldUp);
            }
        }
    }
}
