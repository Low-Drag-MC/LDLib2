package com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils;

import com.lowdragmc.lowdraglib2.math.Ray;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.Optional;

/**
 * The camera a {@link TransformGizmo} is seen through and the cursor's ray into it. A gizmo in a {@code SceneEditor} uses
 * the editor's own; one drawn anywhere else is given one with {@link TransformGizmo#setView}.
 */
@OnlyIn(Dist.CLIENT)
public interface GizmoView {

    Vector3f eyePos();

    Vector3f lookAt();

    Vector3f worldUp();

    boolean isOrtho();

    /** How much world half the view's height covers at {@code distance} from the eye. */
    float viewHalfHeight(float distance);

    Optional<Ray> mouseRay();
}
