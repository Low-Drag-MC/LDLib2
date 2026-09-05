package com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils;

import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneInteractable;
import com.lowdragmc.lowdraglib2.math.Ray;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.joml.Vector3f;

import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.SceneObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who a click in the scene belongs to.
 *
 * <p>The old dispatch OR-ed every interactable's answer together and never passed anything on, so
 * two overlapping objects were both told they had been clicked and the one that acted last won.
 * That is invisible with one object and arbitrary with two, which is why it survived: it is not a
 * crash, it is the wrong thing being selected, occasionally.
 */
class ScenePickingTest {

    /**
     * A stand-in that reports a hit at a fixed distance and records whether it was asked.
     *
     * <p>Extends {@code SceneObject} rather than implementing the interfaces by hand: the scene's
     * identity, its child list and its transform are all things that already work, and faking them
     * would be testing the fake.
     */
    private static final class Target extends SceneObject implements ISceneInteractable {
        private final String name;
        private final double distance;
        private final boolean consumes;
        private final List<String> log;
        private boolean asked;

        Target(String name, double distance, boolean consumes, List<String> log) {
            this.name = name;
            this.distance = distance;
            this.consumes = consumes;
            this.log = log;
        }

        @Override
        public boolean onMouseClick(Ray ray) {
            asked = true;
            log.add(name);
            return consumes;
        }

        /** A negative distance stands for "no shape at all", which is the interface's default. */
        @Override
        public VoxelShape getCollisionShape() {
            return distance < 0 ? Shapes.empty() : Shapes.block();
        }

        @Override
        public BlockHitResult clip(Ray ray, boolean transform) {
            return distance < 0 ? null
                    : new BlockHitResult(new Vec3(distance, 0, 0), Direction.UP, BlockPos.ZERO, false);
        }
    }

    private static Ray fromOrigin() {
        return new Ray(new Vector3f(0, 0, 0), new Vector3f(100, 0, 0));
    }

    @Test
    @DisplayName("the nearest hit is asked first, however the objects were stored")
    void nearestFirst() {
        List<String> log = new ArrayList<>();
        // deliberately stored far-then-near, which is the order the old dispatch would have used
        var far = new Target("far", 9.0, false, log);
        var near = new Target("near", 1.0, false, log);

        ScenePicking.click(List.of(far, near), fromOrigin());

        assertEquals(List.of("near", "far"), log);
    }

    /**
     * ⭐ The contract itself: {@code onMouseClick} returning true means the event was consumed, and
     * nothing behind it hears about the click at all.
     */
    @Test
    @DisplayName("a consumed click does not reach anything further away")
    void consumingStopsTheDispatch() {
        List<String> log = new ArrayList<>();
        var behind = new Target("behind", 9.0, false, log);
        var infront = new Target("infront", 1.0, true, log);

        assertTrue(ScenePicking.click(List.of(behind, infront), fromOrigin()));

        assertEquals(List.of("infront"), log);
        assertFalse(behind.asked, "the object behind the one that consumed the click was told anyway");
    }

    /** Nothing hit means nothing consumed, which is what lets the scene fall back to its own drag. */
    @Test
    @DisplayName("a click that hits nothing is not consumed")
    void nothingHitIsNotConsumed() {
        assertFalse(ScenePicking.click(List.<Target>of(), fromOrigin()));
    }

    /**
     * ⚠️ An interactable with no shape is still offered the click, last. It could always exist —
     * the shape is empty by default — and dropping it would be a silent behaviour change rather
     * than a fix.
     */
    @Test
    @DisplayName("something with no shape at all still gets a turn, after everything that was hit")
    void shapelessGoesLast() {
        List<String> log = new ArrayList<>();
        var shapeless = new Target("shapeless", -1, false, log);
        var hit = new Target("hit", 5.0, false, log);

        ScenePicking.click(List.of(shapeless, hit), fromOrigin());

        assertEquals(List.of("hit", "shapeless"), log);
    }

    /**
     * ⭐ The ordering has to be done in one space.
     *
     * <p>{@link ISceneInteractable#clip(Ray)} resolves the ray into the object's own local space before
     * clipping, so the hit comes back in <em>local</em> coordinates. Measuring that against the world
     * ray's start compares two different spaces, and these two objects are what that looks like: both
     * are hit half a block from their own origin, so both report the same local hit, and the only thing
     * that separates them is a transform the distance never looked at. Ninety-eight blocks apart, and
     * the arbitrary answer is the one the old code gave.
     *
     * <p>The other cases in this class cannot catch it — {@code Target} sits at the origin, where local
     * and world are the same thing.
     */
    @Test
    @DisplayName("distance is measured in world space, not in each object's own")
    void movedObjectsSortByWhereTheyActuallyAre() {
        List<String> log = new ArrayList<>();
        var far = new Target("far", 0.5, false, log);
        var near = new Target("near", 0.5, false, log);
        far.transform().localPosition(new Vector3f(99, 0, 0));
        near.transform().localPosition(new Vector3f(1, 0, 0));

        // stored near-last, so an implementation that does not sort at all would also fail this
        ScenePicking.click(List.of(far, near), fromOrigin());

        assertEquals(List.of("near", "far"), log);
    }
}
