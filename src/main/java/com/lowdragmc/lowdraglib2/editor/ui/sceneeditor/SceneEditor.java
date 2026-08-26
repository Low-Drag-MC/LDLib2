package com.lowdragmc.lowdraglib2.editor.ui.sceneeditor;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.math.Ray;
import com.lowdragmc.lowdraglib2.math.ITransform;
import com.lowdragmc.lowdraglib2.math.Transform;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.IScene;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneInteractable;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneObject;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneRendering;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils.TransformGizmo;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.appliedenergistics.yoga.*;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A scene which provides editable features as a unity scene.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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
            layout.flexDirection(FlexDirection.ROW);
            layout.widthPercent(100);
            layout.height(16);
            layout.paddingAll(1);
            layout.gapAll(1);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID)).moveInlineAsDefault().addClass("__ui-editor-view_header__");

        this.scene = new Scene();
        this.scene.setRenderFacing(false);
        this.scene.setRenderSelect(false);
        this.scene.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });
        this.scene.setAfterWorldRender(scene -> {
            var mc = Minecraft.getInstance();
            var partialTicks = mc.getTimer().getGameTimeDeltaPartialTick(false);
            SceneEditor.this.renderAfterWorld(mc.renderBuffers().bufferSource(), partialTicks);
        });

        this.gizmoBar = new UIElement();
        gizmoBar.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.top(18);
            layout.width(20);
            layout.paddingAll(3);
            layout.gapAll(1);
        }).style(style -> style.backgroundTexture(Sprites.BORDER_RT0)).moveInlineAsDefault().addClass("__editor-gizmo-bar__");

        this.screenTips = new TextElement();
        screenTips.textStyle(style -> {
            style.textAlignHorizontal(Horizontal.CENTER);
            style.textAlignVertical(Vertical.CENTER);
        }).layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.widthPercent(100);
            layout.heightPercent(100);
        }).moveInlineAsDefault();
//        this.scene.addChild(screenTips);

        transformGizmo = new TransformGizmo();
        transformGizmo.setSceneInternal(this);

        initTopBar();
        initGizmos();

        addChildren(topBar, scene, gizmoBar);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown, true);
        addEventListener(UIEvents.MOUSE_UP, this::onMouseUp, true);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onMouseDrag);
        addEventListener(UIEvents.MOUSE_WHEEL, this::onMouseWheel, true);
    }

    public void disableTransformGizmo() {
        transformGizmo.setEnabled(false);
        gizmoBar.setDisplay(false);
    }

    public void enableTransformGizmo() {
        transformGizmo.setEnabled(true);
        gizmoBar.setDisplay(true);
    }

    /**
     * What the gizmo drags.
     *
     * <p>Takes an {@link ITransform}, so an editor can drive something that has a transform without
     * that thing having to <b>be</b> a scene object. The {@link Transform} overloads below are the
     * same method and are kept so existing callers do not have to change.
     */
    public void setTransformGizmoTarget(@Nullable ITransform transform) {
        setTransformGizmoTarget(transform, null);
    }

    public void setTransformGizmoTarget(@Nullable ITransform transform, @Nullable Runnable onTransformUpdated) {
        transformGizmo.setTargetTransform(transform);
        transformGizmo.setOnTransformChanged(onTransformUpdated);
        gizmoBar.setActive(transform != null);
        if (transform == null) {
            transformGizmo.setMode(TransformGizmo.Mode.NONE);
        }
    }

    public void setTransformGizmoTarget(@Nullable Transform transform) {
        setTransformGizmoTarget((ITransform) transform, null);
    }

    public void setTransformGizmoTarget(@Nullable Transform transform, @Nullable Runnable onTransformUpdated) {
        setTransformGizmoTarget((ITransform) transform, onTransformUpdated);
    }

    public TransformGizmo.Mode getTransformGizmoMode() {
        return transformGizmo.getMode();
    }

    public void setTransformGizmoMode(TransformGizmo.Mode mode) {
        transformGizmo.setMode(mode);
    }

    public void initTopBar() {
        topBar.addChild(new Selector<Boolean>()
                .setCandidates(List.of(true, false))
                .setValue(scene.isUseOrtho(), false)
                .setOnValueChanged(scene::useOrtho)
                .setCandidateUIProvider(candidate -> new Label()
                        .textStyle(style -> style
                                .textAlignHorizontal(Horizontal.LEFT)
                                .textAlignVertical(Vertical.CENTER))
                        .setText(candidate == null ? "---" : candidate ? "editor.camera.ortho" : "editor.camera.perspective"))
                .layout(layout -> layout.width(50))
                .style(style -> style.tooltips("editor.camera.mode"))
                .moveInlineAsDefault()
                .addClass("__ui-editor-view_header-projection-mode__")
        );
    }

    public void initGizmos() {
        var toggleGroup = new Toggle.ToggleGroup().setAllowEmpty(true);
        // translate
        gizmoBar.addChild(createTransformToggle(toggleGroup, TransformGizmo.Mode.TRANSLATE, Icons.TRANSFORM_TRANSLATE));
        // rotation
        gizmoBar.addChild(createTransformToggle(toggleGroup, TransformGizmo.Mode.ROTATE, Icons.TRANSFORM_ROTATE));
        // scale
        gizmoBar.addChild(createTransformToggle(toggleGroup, TransformGizmo.Mode.SCALE, Icons.TRANSFORM_SCALE));
        // local / global space toggle
        gizmoBar.addChild(createSpaceToggle());
    }


    private Toggle createTransformToggle(Toggle.ToggleGroup toggleGroup, TransformGizmo.Mode mode, IGuiTexture icon) {
        return (Toggle) new Toggle()
                .setToggleGroup(toggleGroup)
                .setText("")
                .setOn(transformGizmo.getMode() == mode, false)
                .toggleButton(button -> button.layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                }))
                .setOnToggleChanged(isOn -> setTransformGizmoMode(isOn ? mode : TransformGizmo.Mode.NONE))
                .toggleStyle(style -> {
                    style.baseTexture(IGuiTexture.EMPTY);
                    style.hoverTexture(ColorPattern.T_BLUE.rectTexture());
                    style.unmarkTexture(icon);
                    style.markTexture(new GuiTextureGroup(ColorPattern.T_BLUE.rectTexture(), icon));
                })
                .layout(layout -> {
                    layout.paddingAll(0);
                    layout.widthPercent(100);
                    layout.setAspectRatio(1f);
                }).addEventListener(UIEvents.TICK, event -> {
                    if (event.currentElement instanceof Toggle toggle) {
                        if (toggle.getValue() != (transformGizmo.getMode() == mode)) {
                            toggle.setValue(transformGizmo.getMode() == mode, false);
                        }
                    }
                }).addClass("__editor-gizmo-bar-toggle__");
    }

    private Toggle createSpaceToggle() {
        return (Toggle) new Toggle()
                .setText("")
                .setOn(transformGizmo.getSpace() == TransformGizmo.Space.GLOBAL, false)
                .toggleButton(button -> button.layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                }))
                .setOnToggleChanged(isOn ->
                        transformGizmo.setSpace(isOn ? TransformGizmo.Space.GLOBAL : TransformGizmo.Space.LOCAL))
                .toggleStyle(style -> {
                    style.baseTexture(IGuiTexture.EMPTY);
                    style.hoverTexture(ColorPattern.T_BLUE.rectTexture());
                    style.unmarkTexture(Icons.LOCAL);
                    style.markTexture(new GuiTextureGroup(ColorPattern.T_BLUE.rectTexture(), Icons.GLOBAL));
                })
                .layout(layout -> {
                    layout.paddingAll(0);
                    layout.widthPercent(100);
                    layout.setAspectRatio(1f);
                }).style(style -> style.tooltips("editor.gizmo.space"))
                .addEventListener(UIEvents.TICK, event -> {
                    if (event.currentElement instanceof Toggle toggle) {
                        var isGlobal = transformGizmo.getSpace() == TransformGizmo.Space.GLOBAL;
                        if (toggle.getValue() != isGlobal) {
                            toggle.setValue(isGlobal, false);
                        }
                    }
                }).addClass("__editor-gizmo-bar-toggle__");
    }


    public Optional<Ray> getMouseRay() {
        var renderer = scene.getRenderer();
        if (renderer == null) return Optional.empty();
        var lastHit = renderer.getLastHit();
        var startPos = renderer.getEyePos();
        if (scene.isUseOrtho()) {
            var lookAt = renderer.getLookAt();
            startPos = new Vector3f(startPos).add(new Vector3f(startPos.x - lookAt.x(), startPos.y - lookAt.y(), startPos.z - lookAt.z()).mul(500, 500, 500));
        }
        return lastHit == null ? Optional.empty() : Optional.of(Ray.create(startPos, lastHit));
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
        if (transformGizmo.isActive()) {
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
                if (transformGizmo.isActive()) {
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
                if (transformGizmo.isActive()) {
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
                    if (transformGizmo.isActive()) {
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
                var cross = new Vector3f(lookDir).cross(worldUp);
                if (cross.lengthSquared() < 1.0e-6f) {
                    // looking (near) straight up/down: cross is degenerate, recover with a stable horizontal axis
                    cross.set(1, 0, 0);
                }
                cross.normalize();
                // clamp pitch so the look direction never reaches the poles (avoids gimbal-lock flicker/spin)
                var minPitchAngle = 0.5f;
                var pitchToUp = (float) Math.toDegrees(lookDir.angle(worldUp));
                var newPitchToUp = Mth.clamp(pitchToUp + event.deltaY, minPitchAngle, 180f - minPitchAngle);
                var pitchAngle = pitchToUp - newPitchToUp;
                lookDir = new Vector3f(lookDir).rotate(new Quaternionf(new AxisAngle4f((float) Math.toRadians(pitchAngle), cross)));
                lookDir = new Vector3f(lookDir).rotate(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-event.deltaX), worldUp)));
                var center = new Vector3f(eyePos).add(new Vector3f(lookDir));
                scene.setCenter(center);
                Vector3f pos = new Vector3f(eyePos).sub(lookAt);
                scene.setCameraYawAndPitch(
                        (float) Math.toDegrees(Math.atan2(pos.z, pos.x)),
                        (float) Math.toDegrees(Math.atan2(pos.y, Math.sqrt(pos.x * pos.x + pos.z * pos.z)))
                );
                renderer.setCameraLookAt(eyePos, center, worldUp);
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

    protected void renderAfterWorld(MultiBufferSource bufferSource, float partialTicks) {
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
        if (transformGizmo.isActive()) {
            transformGizmo.updateFrame(partialTicks);
            transformGizmo.preDraw(partialTicks);
            transformGizmo.draw(poseStack, bufferSource, partialTicks);
            transformGizmo.postDraw(partialTicks);
        }
    }

    @Override
    public void drawBackgroundOverlay(GUIContext guiContext) {
        super.drawBackgroundOverlay(guiContext);
        // Show the transform readout (offset / degrees / scale) next to the cursor while dragging.
        // It must be drawn in the overlay pass (after the scene child renders its 3D world) and with
        // SEE_THROUGH so the scene's depth buffer can't occlude it.
        if (transformGizmo.isActive() && transformGizmo.isDragging() && transformGizmo.getReadoutText() != null) {
            var font = guiContext.mc.font;
            font.drawInBatch(transformGizmo.getReadoutText(),
                    (int) guiContext.localMouseX + 8, (int) guiContext.localMouseY - 12, 0xFFFFFF00, true,
                    guiContext.graphics.pose().last().pose(), guiContext.graphics.bufferSource(),
                    Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
            guiContext.graphics.flush();
        }
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
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
                var realMoveSpeed = moveSpeed * guiContext.partialTick * (isShiftDown() ? 5 : 1);
                var forward = new Vector3f(lookDir).normalize().mul(realMoveSpeed);
                var right = new Vector3f(lookDir).cross(worldUp).normalize().mul(realMoveSpeed);
                // camera up (screen up), perpendicular to the look direction, so it tilts with the camera pitch
                var up = new Vector3f(right).cross(forward).normalize().mul(realMoveSpeed);
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
