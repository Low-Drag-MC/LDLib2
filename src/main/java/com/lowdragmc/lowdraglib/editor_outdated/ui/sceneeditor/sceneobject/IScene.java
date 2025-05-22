package com.lowdragmc.lowdraglib.editor_outdated.ui.sceneeditor.sceneobject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;

public interface IScene {
    @Nullable
    ISceneObject getSceneObject(UUID uuid);

    Collection<ISceneObject> getAllSceneObjects();

    /**
     * Add a scene object to the scene root.
     */
    default void addSceneObject(ISceneObject sceneObject) {
        sceneObject.destroy();
        sceneObject.transform().parent(null);
        addSceneObjectInternal(sceneObject);
    }

    default void removeSceneObject(ISceneObject sceneObject) {
        sceneObject.destroy();
    }

    void addSceneObjectInternal(ISceneObject sceneObject);

    /**
     * Remove a scene object from the scene root.
     */
    void removeSceneObjectInternal(ISceneObject sceneObject);

    /**
     * it will be called when the scene objects are all added, but before the scene object is ready for used.
     */
    default void awake() {
        for (var sceneObject : getAllSceneObjects()) {
            sceneObject.awake();
        }
    }

}
