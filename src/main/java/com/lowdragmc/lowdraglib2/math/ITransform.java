package com.lowdragmc.lowdraglib2.math;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * <b>Something that has a position, a rotation and a scale, and can be moved.</b>
 *
 * <p>The contract the scene editor's gizmo drives, separated from {@link Transform} so that it is not
 * the only thing that can be dragged. {@link Transform} implements it as it stands — every method
 * here already existed on it with these exact signatures — and anything else with a transform of its
 * own can implement it instead of mirroring one.
 *
 * <p>That mirroring is the reason this exists. A caller whose objects are <i>not</i> scene objects —
 * because their data has to load on a dedicated server, or because it already has its own hierarchy —
 * previously had to keep a proxy {@link Transform} beside every one of its objects and reconcile the
 * two every frame. Reconciling in both directions without a change signal is guesswork, and the
 * guesses are subtle: the gizmo writes world position and world rotation but <i>local</i> scale, so a
 * naive "copy everything back" turns a rotated parent into a wrong local scale.
 *
 * <h2>⚠️ What the getters return</h2>
 *
 * <p>The two halves have deliberately different rules, and getting them the wrong way round is silent:
 *
 * <ul>
 *   <li><b>World</b> ({@link #position()}, {@link #rotation()}) — a <b>snapshot you own</b>. It is
 *       derived rather than stored, so an implementation computes it on demand; callers keep these as
 *       drag anchors and would otherwise be holding a value that moves under them as they drag.
 *   <li><b>Local</b> ({@link #localPosition()} and friends) — a <b>read-only view</b>. It may be the
 *       live field. Do not mutate it and do not retain it: use the setter, which is what tells the
 *       implementation that something changed. An implementation that caches world matrices has no
 *       other way to know.
 * </ul>
 *
 * <h2>Why there is no world scale</h2>
 *
 * <p>Scale is local only, because that is the honest shape of it: a world-space scale applied through
 * a rotated parent is not expressible as a parent-relative TRS at all, so a setter for it would have
 * to either lie or shear. The gizmo scales locally for the same reason.
 */
public interface ITransform {

    // ---- local: parent-relative, and the values that are actually stored ----

    /** ⚠️ A read-only view — see the class javadoc. Use {@link #localPosition(Vector3f)} to change it. */
    Vector3f localPosition();

    /** ⚠️ A read-only view — see the class javadoc. Use {@link #localRotation(Quaternionf)} to change it. */
    Quaternionf localRotation();

    /** ⚠️ A read-only view — see the class javadoc. Use {@link #localScale(Vector3f)} to change it. */
    Vector3f localScale();

    void localPosition(Vector3f localPosition);

    void localRotation(Quaternionf localRotation);

    void localScale(Vector3f localScale);

    // ---- world: derived, and the space a gizmo drags in ----

    /** The world-space position — <b>a snapshot you own</b>. */
    Vector3f position();

    /** The world-space rotation — <b>a snapshot you own</b>. */
    Quaternionf rotation();

    /**
     * Moves this to a world-space position.
     *
     * <p>An implementation with a parent converts; one whose world matrix comes from somewhere else
     * entirely — a bone driven by an animation — is entitled to refuse, and refusing is the honest
     * answer: whatever is driving it would overwrite the write on the next frame anyway.
     */
    void position(Vector3f position);

    /** Rotates this to a world-space rotation. See {@link #position(Vector3f)} about refusing. */
    void rotation(Quaternionf rotation);
}
