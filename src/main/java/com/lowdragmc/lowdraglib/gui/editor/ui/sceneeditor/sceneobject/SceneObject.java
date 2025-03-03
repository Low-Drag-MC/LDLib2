package com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.sceneobject;

import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.data.Transform;
import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nullable;
import java.util.List;

public class SceneObject implements ISceneObject {
    @Getter
    @Nullable
    private IScene scene;
    @Getter
    @Accessors(fluent = true)
    private final Transform transform;

    // runtime
    @Nullable
    private List<ISceneObject> children = null;

    public SceneObject() {
        this.transform = new Transform(this);
    }

    public SceneObject(Transform transform) {
        this();
        this.transform.set(transform);
    }

    @Override
    public final void setSceneInternal(IScene scene) {
        this.scene = scene;
    }

    @Override
    public List<ISceneObject> children() {
        if (children == null) {
            children = ISceneObject.super.children();
        }
        return children;
    }

    @Override
    public void onChildChanged() {
        children = null;
    }
}
