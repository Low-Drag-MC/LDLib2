package com.lowdragmc.lowdraglib2.math;

import com.google.common.collect.ImmutableList;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneObject;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
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
    @Configurable(name = "transform.rotation", tips = "transform.rotation.tips")
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
    private final List<Transform> children = new ArrayList<>();
    @Persisted
    private final List<UUID> _childrenId = new ArrayList<>();

    /**
     * The transform owner.
     */
    @Getter
    @Nonnull
    private final ISceneObject sceneObject;

    // runtime
    @Getter
    private boolean isValid = false;
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
            this.parent.removeChildInternal(this);
        }

        this.parent = parent;
        this._parentId = parent == null ? null : parent.id();
        if (parent != null) {
            parent.addChildInternal(this);
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

    private void addChildInternal(Transform transform) {
        this.children.add(transform);
        if (!this._childrenId.contains(transform.id)) {
            this._childrenId.add(transform.id());
        }
        transform.sceneObject.setScene(this.sceneObject.getScene());
        this.sceneObject.onChildChanged();
    }

    private void removeChildInternal(Transform transform) {
        this.children.remove(transform);
        this._childrenId.remove(transform.id());
        this.sceneObject.onChildChanged();
    }

    public int getSiblingIndex() {
        return parent == null ? -1 : parent.children.indexOf(this);
    }

    public void setSiblingIndex(int newIndex) {
        if (parent == null) return;

        List<Transform> siblings = parent.children;
        int currentIndex = siblings.indexOf(this);
        if (currentIndex == -1 || currentIndex == newIndex) return;

        siblings.remove(currentIndex);
        parent._childrenId.remove(currentIndex);

        int clampedIndex = Math.max(0, Math.min(newIndex, siblings.size()));

        siblings.add(clampedIndex, this);
        parent._childrenId.add(clampedIndex, this.id());

        parent.sceneObject.onChildChanged();
    }

    public void identity() {
        position(new Vector3f());
        rotation(new Quaternionf());
        scale(new Vector3f(1, 1, 1));
    }

    public void identityLocal() {
        localPosition(new Vector3f());
        localRotation(new Quaternionf());
        localScale(new Vector3f(1, 1, 1));
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

    // Add these methods after the existing methods, before the destroy() method

    /**
     * The right direction vector of the transform in world space.
     * In a standard coordinate system, this represents the positive X axis.
     */
    public Vector3f right() {
        return rotation().transform(new Vector3f(1, 0, 0));
    }

    /**
     * The left direction vector of the transform in world space.
     * This is the opposite of right().
     */
    public Vector3f left() {
        return rotation().transform(new Vector3f(-1, 0, 0));
    }

    /**
     * The up direction vector of the transform in world space.
     * In a standard coordinate system, this represents the positive Y axis.
     */
    public Vector3f up() {
        return rotation().transform(new Vector3f(0, 1, 0));
    }

    /**
     * The down direction vector of the transform in world space.
     * This is the opposite of up().
     */
    public Vector3f down() {
        return rotation().transform(new Vector3f(0, -1, 0));
    }

    /**
     * The forward direction vector of the transform in world space.
     * In a standard coordinate system, this represents the negative Z axis.
     */
    public Vector3f forward() {
        return rotation().transform(new Vector3f(0, 0, -1));
    }

    /**
     * The back direction vector of the transform in world space.
     * This is the opposite of forward().
     */
    public Vector3f back() {
        return rotation().transform(new Vector3f(0, 0, 1));
    }

    /**
     * Rotate the transform to look at a target position.
     * @param target The world position to look at
     */
    public void lookAt(Vector3f target) {
        lookAt(target, new Vector3f(0, 1, 0));
    }

    /**
     * Rotate the transform to look at a target position with a specific up direction.
     * @param target The world position to look at
     * @param up The up direction vector
     */
    public void lookAt(Vector3f target, Vector3f up) {
        Vector3f direction = new Vector3f(target).sub(position()).normalize();
        if (direction.lengthSquared() > 0) {
            Quaternionf lookRotation = new Quaternionf().lookAlong(direction, up).conjugate();
            rotation(lookRotation);
        }
    }

    /**
     * Translate the transform in the specified direction.
     * @param direction The direction vector in world space
     * @param distance The distance to translate
     */
    public void translate(Vector3f direction, float distance) {
        Vector3f translation = new Vector3f(direction).normalize().mul(distance);
        position(new Vector3f(position()).add(translation));
    }

    /**
     * Translate the transform relative to its local coordinate system.
     * @param localDirection The direction vector in local space
     * @param distance The distance to translate
     */
    public void translateLocal(Vector3f localDirection, float distance) {
        Vector3f worldDirection = rotation().transform(new Vector3f(localDirection).normalize());
        translate(worldDirection, distance);
    }

    /**
     * Rotate the transform around a specific axis by the given angle.
     * @param axis The rotation axis in world space
     * @param angle The rotation angle in radians
     */
    public void rotate(Vector3f axis, float angle) {
        Quaternionf deltaRotation = new Quaternionf().rotateAxis(angle, axis);
        rotation(new Quaternionf(rotation()).mul(deltaRotation));
    }

    /**
     * Rotate the transform around its local axes.
     * @param eulerAngles The rotation angles in radians (x, y, z)
     */
    public void rotateLocal(Vector3f eulerAngles) {
        Quaternionf deltaRotation = new Quaternionf().rotateXYZ(eulerAngles.x, eulerAngles.y, eulerAngles.z);
        rotation(new Quaternionf(rotation()).mul(deltaRotation));
    }

    public void destroy() {
        if (this.parent != null && this.parent.isValid) {
            this.parent.removeChildInternal(this);
            this.parent = null;
        }
        isValid = false;
    }

    public void awake() {
        if (isValid) return;
        if (sceneObject.getScene() == null) {
            throw new RuntimeException("trying to awake transform before set scene");
        }
        if ( _parentId != null && parent == null) {
            var parent = sceneObject.getScene().getSceneObject(_parentId);
            if (parent != null) {
                parent(parent.transform(), false);
            } else {
                LDLib2.LOGGER.warn("Parent transform {} not found.", _parentId);
            }
        }
        isValid = true;
    }

    public void rebuildChildOrder() {
        if (children.size() > 1 && !_childrenId.isEmpty()) {
            children.sort((a, b) -> {
                int ai = _childrenId.indexOf(a.id());
                int bi = _childrenId.indexOf(b.id());
                return Integer.compare(ai, bi);
            });
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

    /**
     * Get the parent ID
     */
    public UUID _getInternalParentID() {
        return _parentId;
    }

    /**
     * Set the children IDs of the transform.
     * Do not call this method unless you know what you are doing.
     */
    public void _setInternalChildID(List<UUID> uuids) {
        _childrenId.clear();
        _childrenId.addAll(uuids);
    }

    /**
     * Get children IDs
     */
    public List<UUID> _getInternalChildID() {
        return ImmutableList.copyOf(_childrenId);
    }

    public void copyTransformFrom(Transform transform, boolean local, boolean copyHierarchy) {
        this.set(transform, local);
        if (copyHierarchy) {
            this.parent(transform.parent());
            this._setInternalParentID(transform._getInternalParentID());
            this._setInternalChildID(transform._getInternalChildID());
        }
    }
}