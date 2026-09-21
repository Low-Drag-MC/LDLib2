package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import org.joml.Vector2f;

/**
 * Finding a spot on a node graph canvas that has nothing on it.
 *
 * <p>Harder than picking a corner, which is what the scenarios here tried first. A
 * {@link GraphView} is not a bare rectangle: the blackboard, inspector and preview panels are docked
 * over three of its corners and the graph log footer runs along the bottom, so a fixed inset lands
 * on a panel as often as not — and a right-click that misses the canvas opens no menu at all rather
 * than failing loudly. Where the nodes themselves ended up is not knowable in advance either, since
 * it depends on how the canvas framed the graph.</p>
 *
 * <p>So the spot is searched for rather than computed, and the search is what states the
 * requirement: inside the canvas, and not on top of anything the graph put there.</p>
 */
public final class CanvasPoints {

    private CanvasPoints() {
    }

    /**
     * A screen point inside the canvas where the hit test resolves to the canvas itself rather than
     * to any graph element.
     *
     * @throws IllegalStateException when the canvas is completely covered — better than silently
     *                               clicking a node and leaving the caller to wonder why its
     *                               selection changed
     */
    public static Vector2f emptySpot(TestContext ctx, GraphView view) {
        var canvas = view.graphView;
        var bounds = ElementBounds.of(canvas);
        var ui = ctx.requireUI();
        for (var fy = 0.8f; fy >= 0.15f; fy -= 0.05f) {
            for (var fx = 0.1f; fx <= 0.9f; fx += 0.05f) {
                var x = bounds.x() + bounds.width() * fx;
                var y = bounds.y() + bounds.height() * fy;
                var hit = ui.hitTestAtScreen(x, y);
                if (hit == null) continue;
                var path = hit.getStructurePath();
                // Inside the canvas — a docked panel or the log footer is drawn over it, not in it.
                if (!path.contains(canvas)) continue;
                // ModelElement covers nodes, wires, placemats and sticky notes, including their
                // insides: a port row's path still runs through the node it belongs to.
                if (path.stream().anyMatch(ModelElement.class::isInstance)) continue;
                return new Vector2f(x, y);
            }
        }
        throw new IllegalStateException("no empty spot on the graph canvas " + bounds);
    }
}
