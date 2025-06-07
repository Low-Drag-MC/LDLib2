package com.lowdragmc.lowdraglib2.editor_outdated.ui.sceneeditor.sceneobject;

import com.lowdragmc.lowdraglib2.editor_outdated.ui.sceneeditor.data.Transform;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author KilaBash
 * @date 2024/06/26
 * @implNote A scene object that can be placed in the scene editor.
 */
@OnlyIn(Dist.CLIENT)
public interface ISceneObject {
    /**
     * Get the unique id of the object.
     */
    default UUID id() {
        return transform().id();
    }

    /**
     * Get the transform of the object.
     */
    Transform transform();

    default void setTransform(Transform transform) {
        transform.set(transform);
    }

    /**
     * Get the scene.
     */
    @Nullable
    IScene getScene();

    /**
     * Set the scene internal. you should not call this method directly.
     */
    void setSceneInternal(IScene scene);

    /**
     * Set the scene. you should not call this method directly.
     */
    default void setScene(IScene scene) {
        if (getScene() != scene) {
            setSceneInternal(scene);
            children().forEach(child -> child.setScene(scene));
        }
    }

    /**
     * Destroy the object.
     */
    default void destroy() {
        transform().parent(null);
        Optional.ofNullable(getScene()).ifPresent(scene -> scene.removeSceneObjectInternal(this));
        children().forEach(ISceneObject::destroy);
    }

    /**
     * Get the children of the object. (read-only)
     * if possible, please cache the children list. and update it when the children is changed. see {@link #onChildChanged()}
     */
    default List<ISceneObject> children() {
        return transform().children().stream().map(Transform::sceneObject).toList();
    }

    /**
     * Called when the transform of the object is changed.
     */
    default void onTransformChanged() {
    }

    /**
     * Called when the children of the object is changed.
     */
    default void onChildChanged() {
    }

    /**
     * Called when the parent of the object is changed.
     */
    default void onParentChanged() {

    }

    /**
     * Update the interactable per tick.
     */
    default void updateTick() {
    }

    /**
     * Update the interactable per frame.
     */
    default void updateFrame(float partialTicks) {
    }

    /**
     * Execute the consumer for the object and all children.
     */
    default void executeAll(Consumer<ISceneObject> consumer) {
        consumer.accept(this);
        children().forEach(child -> child.executeAll(consumer));
    }

    /**
     * Execute the consumer for the object and all children.
     */
    default void executeAll(Consumer<ISceneObject> consumer, @Nullable Consumer<ISceneObject> before, @Nullable Consumer<ISceneObject> after) {
        if (before != null) before.accept(this);
        consumer.accept(this);
        children().forEach(child -> child.executeAll(consumer));
        if (after != null) after.accept(this);
    }

    /**
     * it will be called when the scene objects are all added, but before the scene object is ready for used.
     */
    default void awake() {
        transform().awake();
    }

}
