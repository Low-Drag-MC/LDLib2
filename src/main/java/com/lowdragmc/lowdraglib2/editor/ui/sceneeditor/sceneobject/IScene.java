package com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject;

import org.jetbrains.annotations.Nullable;
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
        sceneObject.setScene(this);
    }

    default void removeSceneObject(ISceneObject sceneObject) {
        if (sceneObject.getScene() == this) {
            sceneObject.setScene(null);
        }
    }

    void addSceneObjectInternal(ISceneObject sceneObject);

    /**
     * Remove a scene object from the scene root.
     */
    void removeSceneObjectInternal(ISceneObject sceneObject);

}
