package com.lowdragmc.lowdraglib2.math;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneObject;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author KilaBash
 * @date 2024/06/26
 * @implNote A transform that represents the position, rotation, and scale of a scene object.
 */
@Accessors(fluent = true)
public final class Transform implements IPersistedSerializable, IConfigurable {
    @Getter
    @Accessors(fluent = true)
    @Persisted
    private UUID id = UUID.randomUUID();
    /**
     * Position of the transform relative to the parent transform.
     */
    @Getter
    @Configurable(name = "transform.position", tips = "transform.position.tips")
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private Vector3f localPosition = new Vector3f();

    /**
     * Rotation of the transform relative to the parent transform.
     */
    @Getter
    @Configurable(name = "transform.rotation", tips = "transform.rotation.tips", forceUpdate = false)
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE}, wheel = 1)
    private Quaternionf localRotation = new Quaternionf();

    /**
     * Scale of the transform relative to the parent transform.
     */
    @Getter
    @Configurable(name = "transform.scale", tips = "transform.scale.tips")
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private Vector3f localScale = new Vector3f(1, 1, 1);

    /**
     * The parent transform of the transform.
     */
    @Nullable
    @Getter
    private Transform parent;
    @Persisted
    private UUID _parentId;

    /**
     * The children transforms of the transform.
     */
    @Getter
    private List<Transform> children = new ArrayList<>();

    /**
     * The transform owner.
     */
    @Getter
    @Nonnull
    private final ISceneObject sceneObject;

    // runtime
    @Nullable
    private Vector3f position = null;
    @Nullable
    private Quaternionf rotation = null;
    @Nullable
    private Vector3f scale = null;
    @Nullable
    private Matrix4f localTransformMatrix = null;
    @Nullable
    private Matrix4f worldToLocalMatrix = null;
    private Matrix4f localToWorldMatrix = null;

    public Transform(@Nonnull ISceneObject sceneObject) {
        this.sceneObject = sceneObject;
    }

    /**
     * Notify the transform that the transform has changed.
     * This will clean cache of the world space position, rotation, scale, and matrices.
     */
    private void onTransformChanged() {
        position = null;
        rotation = null;
        scale = null;
        localTransformMatrix = null;
        worldToLocalMatrix = null;
        localToWorldMatrix = null;
        for (Transform child : children) {
            child.onTransformChanged();
        }
        sceneObject.onTransformChanged();
    }

    public void parent(@Nullable Transform parent) {
        parent(parent, true);
    }

    /**
     * Set the parent transform of the transform.
     * @param parent The parent transform.
     *               If the parent is null, the transform will be the root transform.
     * @param keepWorldTransform If true, the world space position, rotation, and scale of the transform will be kept.
     */
    public void parent(@Nullable Transform parent, boolean keepWorldTransform) {
        if (this.parent == parent) {
            return;
        }
        if (parent != null) {
            if (parent.isInheritedParent(this)) {
                throw new IllegalArgumentException("Cannot set parent to a child transform.");
            }
        }

        var lastPosition = keepWorldTransform ? position() : null;
        var lastRotation = keepWorldTransform ? rotation() : null;
        var lastScale = keepWorldTransform ? scale() : null;

        if (this.parent != null) {
            this.parent.children.remove(this);
            this.parent.sceneObject.onChildChanged();
        }

        this.parent = parent;
        this._parentId = parent == null ? null : parent.id();
        if (parent != null) {
            parent.children.add(this);
            this.sceneObject.setScene(parent.sceneObject.getScene());
            parent.sceneObject.onChildChanged();
        }
        if (keepWorldTransform) {
            onTransformChanged();
            position(lastPosition);
            rotation(lastRotation);
            scale(lastScale);
        } else {
            onTransformChanged();
        }
        this.sceneObject.onParentChanged();
    }

    public boolean isInheritedParent(Transform parent) {
        if (this.parent == null) {
            return false;
        }
        if (this.parent == parent) {
            return true;
        }
        return this.parent.isInheritedParent(parent);
    }

    /**
     * Matrix that represents the local transform of the transform.
     */
    public Matrix4f localTransformMatrix() {
        if (localTransformMatrix == null) {
            localTransformMatrix = new Matrix4f().translate(localPosition).rotate(localRotation).scale(localScale);
        }
        return localTransformMatrix;
    }

    /**
     * Matrix that transforms from local space to world space.
     */
    public Matrix4f localToWorldMatrix() {
        if (localToWorldMatrix == null) {
            localToWorldMatrix = parent == null ?
                    localTransformMatrix() :
                    new Matrix4f(parent.localToWorldMatrix()).mul(localTransformMatrix());
        }
        return localToWorldMatrix;
    }

    /**
     * Matrix that transforms from world space to local space.
     */
    public Matrix4f worldToLocalMatrix() {
        if (worldToLocalMatrix == null) {
            worldToLocalMatrix = localToWorldMatrix().invert(new Matrix4f());
        }
        return worldToLocalMatrix;
    }

    /**
     * Set the position, rotation, and scale of the transform.
     */
    public Transform set(Transform transform) {
        return set(transform, false);
    }

    public Transform set(Transform transform, boolean local) {
        if (local) {
            localPosition(transform.localPosition());
            localRotation(transform.localRotation());
            localScale(transform.localScale());
        } else {
            position(transform.position());
            rotation(transform.rotation());
            scale(transform.scale());
        }
        return this;
    }

    /**
     * The world space position of the transform.
     */
    public Vector3f position() {
        if (position == null) {
            position = parent == null ?
                    localPosition :
                    parent.localToWorldMatrix().transformPosition(new Vector3f(localPosition));
        }
        return new Vector3f(position);
    }

    public void position(Vector3f position) {
        onTransformChanged();
        this.position = new Vector3f(position);
        if (parent == null) {
            this.localPosition = new Vector3f(position);
        } else {
            this.localPosition = parent.worldToLocalMatrix().transformPosition(new Vector3f(position));
        }
    }

    @ConfigSetter(field = "localPosition")
    public void localPosition(Vector3f localPosition) {
        this.localPosition = localPosition;
        onTransformChanged();
    }

    /**
     * The world space rotation of the transform.
     */
    public Quaternionf rotation() {
        if (rotation == null) {
            rotation = parent == null ?
                    localRotation :
                    parent.rotation().mul(localRotation);
        }
        return new Quaternionf(rotation);
    }

    public void rotation(Quaternionf rotation) {
        onTransformChanged();
        this.rotation = new Quaternionf(rotation);
        if (parent == null) {
            this.localRotation = new Quaternionf(rotation);
        } else {
            this.localRotation = parent.rotation().invert().mul(rotation);
        }
    }

    @ConfigSetter(field = "localRotation")
    public void localRotation(Quaternionf localRotation) {
        this.localRotation = localRotation;
        onTransformChanged();
    }

    /**
     * The world space scale of the transform.
     */
    public Vector3f scale() {
        if (scale == null) {
            scale = parent == null ?
                    localScale :
                    new Vector3f(localScale).mul(parent.scale());
        }
        return new Vector3f(scale);
    }

    public void scale(Vector3f scale) {
        onTransformChanged();
        this.scale = new Vector3f(scale);
        if (parent == null) {
            this.localScale = new Vector3f(scale);
        } else {
            this.localScale = new Vector3f(scale).div(parent.scale());
        }
    }

    @ConfigSetter(field = "localScale")
    public void localScale(Vector3f localScale) {
        this.localScale = localScale;
        onTransformChanged();
    }

    public void awake() {
        if (_parentId != null && sceneObject.getScene() != null) {
            var parent = sceneObject.getScene().getSceneObject(_parentId);
            if (parent != null) {
                parent(parent.transform(), false);
            } else {
                LDLib2.LOGGER.warn("Parent transform {} not found.", _parentId);
            }
        }
    }

    /**
     * Set the ID of the transform.
     * Do not call this method unless you know what you are doing.
     */
    public void _setInternalID(UUID uuid) {
        id = uuid;
    }

    /**
     * Refresh the ID of the transform. This will generate a new random UUID.
     * Do not call this method unless you know what you are doing.
     */
    public void _refreshInternalID() {
        id = UUID.randomUUID();
    }

    /**
     * Set the parent ID of the transform.
     * Do not call this method unless you know what you are doing.
     */
    public void _setInternalParentID(UUID uuid) {
        _parentId = uuid;
    }
}
