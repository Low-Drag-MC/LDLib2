package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortOrientation;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout.GraphLayoutAlgorithm;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout.LayoutBox;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout.LayoutDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout.LayoutEdge;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout.LayoutOptions;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wiget.PlacematElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.BlockNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.PlacematModel;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LayoutCommands {

    /** Gap kept between a grown placemat's border and the nodes it holds. */
    public static final float PLACEMAT_PADDING = 20f;
    /** Size used for a node whose UI element has not been built yet. */
    private static final Vector2f FALLBACK_NODE_SIZE = new Vector2f(150f, 60f);

    private LayoutCommands() {
    }

    /**
     * Rearranges a set of nodes with one of the {@link GraphLayoutAlgorithm} strategies.
     *
     * <h2>What counts as one thing to move</h2>
     * <p>Placemats make "the selection" ambiguous, and the two readings are both useful, so which
     * one applies is decided by what is selected rather than by a setting:</p>
     * <ul>
     *     <li><b>The placemat itself is selected</b> — it is one box. It and everything it holds
     *     move together, and the wires its contents have to the outside become the box's wires.
     *     The contents keep their relative arrangement.</li>
     *     <li><b>Only nodes inside a placemat are selected</b> — those nodes are laid out on their
     *     own, anchored inside the placemat, and the placemat is grown afterwards if the result no
     *     longer fits. It is never shrunk: a placemat is also a label for an area the user drew.</li>
     * </ul>
     *
     * <p>With nothing selected at all, the whole graph is arranged, placemats as units.</p>
     */
    public static class AutoLayoutCommand extends UndoableGraphCommand {
        public static final Component NAME = Component.translatable("graph.commands.auto_layout");

        private final List<GraphElementModel> selection;
        private final GraphLayoutAlgorithm algorithm;

        public AutoLayoutCommand(Collection<? extends GraphElementModel> selection, GraphLayoutAlgorithm algorithm) {
            this.selection = List.copyOf(selection);
            this.algorithm = algorithm;
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }

        @Override
        public void execute() {
            // Containment is a geometric test, so it stops being answerable the moment anything
            // moves. Snapshot it before touching a single position.
            var membership = new LinkedHashMap<PlacematModel, List<AbstractNodeModel>>();
            for (var placemat : graphModel.getPlacematModels()) {
                membership.put(placemat, placemat.getContainedNodes(this::elementSize));
            }

            var units = collectUnits(membership);
            if (units.size() < 2) return;

            var unitOf = new IdentityHashMap<AbstractNodeModel, Integer>();
            for (var i = 0; i < units.size(); i++) {
                for (var node : units.get(i).nodes()) unitOf.put(node, i);
            }

            var boxes = new ArrayList<LayoutBox>(units.size());
            for (var i = 0; i < units.size(); i++) {
                var unit = units.get(i);
                var position = unit.position();
                var size = unit.size();
                boxes.add(new LayoutBox(i, size.x, size.y, position.x, position.y));
            }

            // The direction has to be settled before the edges are built: a port offset is a
            // cross-axis distance, and which axis that is comes out of this.
            var direction = detectDirection(unitOf);
            var edges = collectEdges(unitOf, units, direction);

            var before = topLeftOf(boxes);
            algorithm.create().layout(boxes, edges, options(direction));
            var after = topLeftOf(boxes);

            // The one placemat, if any, whose contents this is a layout *of* — which decides both
            // where the result is anchored and whether anything gets resized afterwards.
            var host = soleHostPlacemat(units, membership);
            var anchor = resolveAnchor(host, before);
            var offset = new Vector2f(anchor).sub(after);

            // Apply, remembering how far each node travelled: reroute points have to follow the
            // wire they bend or they end up dragging it back across the canvas.
            var deltas = new IdentityHashMap<AbstractNodeModel, Vector2f>();
            for (var i = 0; i < units.size(); i++) {
                var target = new Vector2f(boxes.get(i).x, boxes.get(i).y).add(offset);
                if (view != null) target = view.snapPosition(target);
                units.get(i).moveTo(target, deltas);
            }

            nudgeReroutePoints(deltas);
            growPlacemat(host, membership);
        }

        // region units

        /** One movable thing in the layout: a node on its own, or a placemat with its contents. */
        private interface Unit {
            /** Top-left corner, in canvas content coordinates. */
            Vector2f position();

            Vector2f size();

            /** Every node whose wires should count as this unit's wires. */
            List<AbstractNodeModel> nodes();

            /** Moves this unit's top-left to {@code target}, recording each node's travel. */
            void moveTo(Vector2f target, Map<AbstractNodeModel, Vector2f> deltas);
        }

        private record NodeUnit(AbstractNodeModel node, Vector2f size) implements Unit {
            @Override
            public Vector2f position() {
                return new Vector2f(node.getPosition());
            }

            @Override
            public List<AbstractNodeModel> nodes() {
                return List.of(node);
            }

            @Override
            public void moveTo(Vector2f target, Map<AbstractNodeModel, Vector2f> deltas) {
                var delta = new Vector2f(target).sub(node.getPosition());
                node.setPosition(target);
                deltas.put(node, delta);
            }
        }

        private record PlacematUnit(PlacematModel placemat, List<AbstractNodeModel> contents) implements Unit {
            @Override
            public Vector2f position() {
                return new Vector2f(placemat.getPosition());
            }

            @Override
            public Vector2f size() {
                return new Vector2f(placemat.getSize());
            }

            @Override
            public List<AbstractNodeModel> nodes() {
                return contents;
            }

            @Override
            public void moveTo(Vector2f target, Map<AbstractNodeModel, Vector2f> deltas) {
                var delta = new Vector2f(target).sub(placemat.getPosition());
                placemat.setPosition(target);
                for (var node : contents) {
                    node.move(delta);
                    deltas.put(node, delta);
                }
            }
        }

        private List<Unit> collectUnits(Map<PlacematModel, List<AbstractNodeModel>> membership) {
            var placemats = new ArrayList<PlacematModel>();
            var nodes = new ArrayList<AbstractNodeModel>();
            if (selection.isEmpty()) {
                placemats.addAll(graphModel.getPlacematModels());
                nodes.addAll(graphModel.getNodeModels());
            } else {
                for (var model : selection) {
                    if (model instanceof PlacematModel placemat) placemats.add(placemat);
                    else if (model instanceof AbstractNodeModel node) nodes.add(node);
                }
            }

            var units = new ArrayList<Unit>();
            // Nodes riding along inside a placemat unit must not also become units of their own,
            // or they would be positioned twice and tear the placemat's contents apart.
            var claimed = new HashSet<AbstractNodeModel>();
            for (var placemat : placemats) {
                if (!placemat.isMovable()) continue;
                var contents = membership.getOrDefault(placemat, List.of());
                units.add(new PlacematUnit(placemat, contents));
                claimed.addAll(contents);
            }
            for (var node : nodes) {
                if (!node.isMovable() || claimed.contains(node)) continue;
                units.add(new NodeUnit(node, elementSize(node)));
            }
            return units;
        }

        // endregion

        /**
         * Which way the graph flows, taken from the ports actually being laid out: nodes with ports
         * on their sides read left to right, nodes with ports on top and bottom read downwards.
         * A mixed graph goes with the majority — there is one answer to give and no better one.
         */
        private LayoutDirection detectDirection(Map<AbstractNodeModel, Integer> unitOf) {
            var vertical = 0;
            var horizontal = 0;
            for (var wire : graphModel.getWireModels()) {
                var fromPort = wire.getFromPort();
                var toPort = wire.getToPort();
                if (fromPort == null || toPort == null) continue;
                if (unitOf.get(layoutNodeOf(fromPort)) == null || unitOf.get(layoutNodeOf(toPort)) == null) continue;
                for (var port : List.of(fromPort, toPort)) {
                    if (port.getOrientation() == PortOrientation.Vertical) vertical++;
                    else horizontal++;
                }
            }
            return vertical > horizontal ? LayoutDirection.TOP_BOTTOM : LayoutDirection.LEFT_RIGHT;
        }

        private List<LayoutEdge> collectEdges(Map<AbstractNodeModel, Integer> unitOf,
                                              List<Unit> units,
                                              LayoutDirection direction) {
            var edges = new ArrayList<LayoutEdge>();
            for (var wire : graphModel.getWireModels()) {
                var fromPort = wire.getFromPort();
                var toPort = wire.getToPort();
                if (fromPort == null || toPort == null) continue;
                var fromUnit = unitOf.get(layoutNodeOf(fromPort));
                var toUnit = unitOf.get(layoutNodeOf(toPort));
                if (fromUnit == null || toUnit == null || fromUnit.equals(toUnit)) continue;
                edges.add(new LayoutEdge(fromUnit, toUnit,
                        portOffset(fromPort, units.get(fromUnit), direction),
                        portOffset(toPort, units.get(toUnit), direction)));
            }
            return edges;
        }

        /**
         * The node a port's wires should be attributed to. A block's ports belong, as far as layout
         * is concerned, to the context node that draws it.
         */
        private static @Nullable AbstractNodeModel layoutNodeOf(PortModel port) {
            AbstractNodeModel node = port.getNodeModel();
            var guard = 0;
            while (node instanceof BlockNodeModel block && block.getContextNodeModel() != null && guard++ < 16) {
                node = block.getContextNodeModel();
            }
            return node;
        }

        /**
         * How far along the unit's cross axis the wire attaches. {@link Float#NaN} when the UI
         * cannot say, which the layout reads as "the middle".
         *
         * <p>Both coordinates come from {@code getPositionX/Y}, which is the cumulative sum up the
         * parent chain — the same absolute layout space for a port in the node layer and a placemat
         * in the placemat layer, which is what makes subtracting them meaningful.</p>
         */
        private float portOffset(PortModel port, Unit unit, LayoutDirection direction) {
            if (view == null) return Float.NaN;
            var portElement = view.getModelElement(port);
            var originModel = unit instanceof PlacematUnit placematUnit
                    ? (GraphElementModel) placematUnit.placemat()
                    : ((NodeUnit) unit).node();
            var originElement = view.getModelElement(originModel);
            if (portElement == null || originElement == null) return Float.NaN;
            return direction.isHorizontal()
                    ? portElement.getPositionY() + portElement.getSizeHeight() / 2f - originElement.getPositionY()
                    : portElement.getPositionX() + portElement.getSizeWidth() / 2f - originElement.getPositionX();
        }

        private LayoutOptions options(LayoutDirection direction) {
            var spacing = view != null && view.isSnapToGrid() ? Math.max(16f, view.getGridSnapSize() * 2f) : 32f;
            return LayoutOptions.defaults()
                    .withDirection(direction)
                    .withSpacing(direction.isHorizontal() ? 96f : 72f, spacing);
        }

        /**
         * Normally the selection's own top-left, so the graph does not jump out from under the
         * user. The exception is the "lay out the inside of a placemat" case — every unit is a bare
         * node and they all belong to one placemat nobody selected — where anchoring inside that
         * placemat is the whole point of the gesture.
         */
        private Vector2f resolveAnchor(@Nullable PlacematModel host, Vector2f selectionTopLeft) {
            if (host == null) return selectionTopLeft;
            return new Vector2f(
                    host.getPosition().x + PLACEMAT_PADDING,
                    host.getPosition().y + PLACEMAT_PADDING + PlacematElement.TITLE_BAR_HEIGHT);
        }

        /** The one unselected placemat that holds every unit, or {@code null} if there isn't one. */
        private @Nullable PlacematModel soleHostPlacemat(List<Unit> units,
                                                         Map<PlacematModel, List<AbstractNodeModel>> membership) {
            for (var unit : units) {
                if (!(unit instanceof NodeUnit)) return null;
            }
            var moving = new HashSet<AbstractNodeModel>();
            for (var unit : units) moving.addAll(unit.nodes());
            PlacematModel host = null;
            for (var entry : membership.entrySet()) {
                if (!new HashSet<>(entry.getValue()).containsAll(moving)) continue;
                // More than one candidate means nested placemats; the innermost is the useful one.
                if (host == null || entry.getValue().size() < membership.get(host).size()) host = entry.getKey();
            }
            return host;
        }

        /**
         * Slides each reroute point by the average of how far the two nodes its wire connects have
         * moved. Rough by construction — a reroute point has no layer of its own — but it keeps a
         * hand-routed wire pointing roughly where it used to, which leaving it pinned would not.
         */
        private void nudgeReroutePoints(Map<AbstractNodeModel, Vector2f> deltas) {
            for (var wire : graphModel.getWireModels()) {
                var points = wire.getReroutePoints();
                if (points.isEmpty()) continue;
                var fromPort = wire.getFromPort();
                var toPort = wire.getToPort();
                if (fromPort == null || toPort == null) continue;
                var fromDelta = deltas.get(layoutNodeOf(fromPort));
                var toDelta = deltas.get(layoutNodeOf(toPort));
                if (fromDelta == null || toDelta == null) continue;
                var delta = new Vector2f(fromDelta).add(toDelta).mul(0.5f);
                if (delta.lengthSquared() < 1e-6f) continue;
                for (var point : points) point.move(delta);
            }
        }

        /**
         * Only grows — a placemat is also a label for an area somebody drew, and shrinking it to
         * hug its contents is a different gesture.
         *
         * <p>Only the <em>host</em>, deliberately. An earlier version grew every placemat that had
         * lost a member, which sounds more helpful and is not: laying out a wide selection that
         * happens to include some of a placemat's contents scatters them, and the placemat then
         * stretches to chase them until it has swallowed the whole graph. Membership here is
         * geometric — a node dragged out of a placemat simply stops being in it — so following it
         * is the odd behaviour, not letting it go.</p>
         */
        private void growPlacemat(@Nullable PlacematModel host,
                                  Map<PlacematModel, List<AbstractNodeModel>> membership) {
            if (host == null) return;
            var contents = membership.getOrDefault(host, List.of());
            if (contents.isEmpty()) return;

            var minX = host.getPosition().x;
            var minY = host.getPosition().y;
            var maxX = minX + host.getSize().x;
            var maxY = minY + host.getSize().y;
            for (var node : contents) {
                var position = node.getPosition();
                var size = elementSize(node);
                minX = Math.min(minX, position.x - PLACEMAT_PADDING);
                minY = Math.min(minY, position.y - PLACEMAT_PADDING - PlacematElement.TITLE_BAR_HEIGHT);
                maxX = Math.max(maxX, position.x + size.x + PLACEMAT_PADDING);
                maxY = Math.max(maxY, position.y + size.y + PLACEMAT_PADDING);
            }
            host.setPosition(new Vector2f(minX, minY));
            host.setSize(new Vector2f(maxX - minX, maxY - minY));
        }

        private Vector2f elementSize(AbstractNodeModel node) {
            var element = view == null ? null : view.getModelElement(node);
            if (element == null || element.getSizeWidth() <= 0 || element.getSizeHeight() <= 0) {
                return new Vector2f(FALLBACK_NODE_SIZE);
            }
            return new Vector2f(element.getSizeWidth(), element.getSizeHeight());
        }

        private static Vector2f topLeftOf(List<LayoutBox> boxes) {
            var minX = Float.MAX_VALUE;
            var minY = Float.MAX_VALUE;
            for (var box : boxes) {
                minX = Math.min(minX, box.x);
                minY = Math.min(minY, box.y);
            }
            return new Vector2f(minX, minY);
        }
    }
}
