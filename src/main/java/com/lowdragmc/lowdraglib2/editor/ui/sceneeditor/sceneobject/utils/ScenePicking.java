package com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils;

import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneInteractable;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneObject;
import com.lowdragmc.lowdraglib2.math.Ray;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Which interactable a click in the scene belongs to.
 *
 * <p>{@link ISceneInteractable#onMouseClick} has always documented what it returns —
 * <i>"true to consume the event, false to pass it to the next interactable"</i> — and the dispatch
 * did not implement it: it visited every object in map order and OR-ed the answers together, so
 * nothing was ever passed to a <i>next</i> one and nothing could consume anything. With one
 * interactable that is invisible. With two overlapping ones, every one of them is told it was
 * clicked, and which of them "wins" is whichever happens to act last.
 *
 * <p>This is that contract: <b>nearest first, stop at the first that consumes</b>.
 *
 * <h2>⚠️ Objects with no shape are still offered the click</h2>
 *
 * <p>{@link ISceneInteractable#getCollisionShape} is empty by default, so an interactable can exist
 * to receive clicks without describing a volume — and dropping those would be a silent behaviour
 * change for anything relying on the old broadcast. They go last, after everything the ray actually
 * hit, in the order they were visited.
 */
public final class ScenePicking {

    private ScenePicking() {
    }

    /**
     * Dispatches a click, nearest hit first.
     *
     * @return whether anything consumed it — what the caller needs to decide about the event
     */
    public static boolean click(Iterable<? extends ISceneObject> roots, Ray ray) {
        for (ISceneInteractable candidate : order(roots, ray)) {
            if (candidate.onMouseClick(ray)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The interactables under {@code roots}, ordered as a click should reach them.
     *
     * <p>Separated from {@link #click} so the ordering can be checked without a scene: it is the
     * part that can be wrong on its own, and "somebody was told" is not an assertion about who.
     */
    public static List<ISceneInteractable> order(Iterable<? extends ISceneObject> roots, Ray ray) {
        record Hit(ISceneInteractable target, double distanceSqr) {
        }
        List<Hit> hits = new ArrayList<>();
        List<ISceneInteractable> shapeless = new ArrayList<>();
        for (ISceneObject root : roots) {
            root.executeAll(object -> {
                if (!(object instanceof ISceneInteractable interactable)) {
                    return;
                }
                if (interactable.getCollisionShape().isEmpty()) {
                    shapeless.add(interactable);
                    return;
                }
                BlockHitResult hit = interactable.clip(ray);
                if (hit != null) {
                    hits.add(new Hit(interactable, hit.getLocation().distanceToSqr(
                            ray.startPos().x, ray.startPos().y, ray.startPos().z)));
                }
            });
        }
        hits.sort(Comparator.comparingDouble(Hit::distanceSqr));
        List<ISceneInteractable> ordered = new ArrayList<>(hits.size() + shapeless.size());
        for (Hit hit : hits) {
            ordered.add(hit.target());
        }
        ordered.addAll(shapeless);
        return ordered;
    }
}
