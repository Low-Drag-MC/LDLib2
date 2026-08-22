package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphViewLod;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.WireCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.DependencyTypes;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortOrientation;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.PortElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.IGhostWireModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireReroutePointModel;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.*;

public class WireElement extends GraphElement<WireModel> {
    public final static String WIRE_LAYER = "Wire";
    // runtime
    @Getter
    protected Vector2f from = new Vector2f();
    @Getter
    protected Vector2f to = new Vector2f();
    protected float fromOffset = 15;
    protected float toOffset = 15;
    protected List<Vector2f> rawPoints = Collections.emptyList();
    protected List<Vector2f> drawPoints = Collections.emptyList();
    /** Reroute connector anchors, in the same parent-local layout space as {@link #from} / {@link #to}. */
    protected List<Vector2f> reroutePositions = Collections.emptyList();
    protected ModelElement lastUsedFromPort;
    protected ModelElement lastUsedToPort;
    /** The point elements {@link #addBackwardDependencies()} last hooked, in wire order. */
    protected List<ModelElement> lastUsedReroutePoints = Collections.emptyList();
    protected WireModel lastWireModel;
    protected WireReroutePointModel lastRouteVia;
    protected int lastReroutePointCount = -1;
    /**
     * LOD the current {@link #drawPoints} were built for. Changing level changes the point list
     * (rounded vs. straight), and {@link GraphViewLod#BLOCK} skips {@link #updatePortPosition()}
     * altogether — so geometry has to be forced stale whenever the level moves.
     */
    protected GraphViewLod lastLod = GraphViewLod.FULL;
    protected boolean geometryDirty = true;

    public WireElement(WireModel wireModel) {
        super(wireModel);
        addClass("__wire__");
        addEventListener(UIEvents.DOUBLE_CLICK, this::onDoubleClick);
    }

    /**
     * Double-clicking a wire drops a reroute point where the cursor is — the same gesture Unreal and
     * Unity use. It is a layout edit only: the wire keeps both of its ports.
     */
    protected void onDoubleClick(UIEvent event) {
        if (event.button != 0) return;
        var graphView = getGraphView();
        // A ghost wire is transient drag feedback, not a real connection — nothing to bend.
        if (graphView == null || getModel() instanceof IGhostWireModel) return;
        var center = graphView.getContentViewContainer()
                .worldToLocalLayoutOffset(new Vector2f(event.x, event.y));
        graphView.dispatchCommand(new WireCommands.InsertReroutePointCommand(
                getModel(), center, reroutePointInsertIndex(center)));
        event.stopPropagation();
    }

    @Override
    public String getLayerName() {
        return WIRE_LAYER;
    }

    // region build ui

    @Override
    protected void buildUI() {
        super.buildUI();
        // Wire is absolutely positioned at coordinates computed from connected port positions — IMPORTANT.
        Style.importantPipeline(getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE));
    }

    // endregion

    @Override
    public boolean hasBackwardsDependenciesChanged() {
        if (graphView == null) return false;
        var modelElements = graphView.getModelElements();
        if (lastUsedFromPort != modelElements.get(getModel().getFromPort())
                || lastUsedToPort != modelElements.get(getModel().getToPort())) {
            return true;
        }
        // A point added, removed or (re)built its UI: the LAYOUT hooks that keep the polyline glued
        // to the points have to be re-attached, or dragging one would leave the wire behind.
        var points = getModel().getReroutePoints();
        if (lastUsedReroutePoints.size() != points.size()) return true;
        for (int i = 0; i < points.size(); i++) {
            if (lastUsedReroutePoints.get(i) != modelElements.get(points.get(i))) return true;
        }
        return false;
    }

    @Override
    public void addBackwardDependencies() {
        super.addBackwardDependencies();
        if (graphView == null) return;

        // When the ports move, the wire should be redrawn.
        addDependencies(getModel().getFromPort());
        addDependencies(getModel().getToPort());

        var modelElements = graphView.getModelElements();
        lastUsedFromPort = modelElements.get(getModel().getFromPort());
        lastUsedToPort = modelElements.get(getModel().getToPort());

        // Same for the reroute points: the wire follows them while they are dragged.
        var points = getModel().getReroutePoints();
        var used = new ArrayList<ModelElement>(points.size());
        for (var point : points) {
            var ui = modelElements.get(point);
            used.add(ui);
            if (ui != null) {
                getDependencies().addBackwardDependency(ui, DependencyTypes.LAYOUT);
            }
        }
        lastUsedReroutePoints = used;
    }

    private void addDependencies(PortModel portModel) {
        if (portModel == null || graphView == null)
            return;

        var modelElements = graphView.getModelElements();
        var ui = modelElements.get(portModel);
        if (ui != null) {
            // Wire color changes with port color.
            getDependencies().addBackwardDependency(ui, DependencyTypes.STYLE);

            // When port LAYOUT changes, the wire should follow.
            getDependencies().addBackwardDependency(ui, DependencyTypes.LAYOUT);
        }

        ui = modelElements.get(portModel.getNodeModel());
        if (ui != null) {
            // Wire position changes with node position.
            getDependencies().addBackwardDependency(ui, DependencyTypes.LAYOUT);
        }
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        updatePortPosition();
    }

    @Override
    public boolean hasModelDependenciesChanged() {
        var model = getModel();
        // routeVia catches the branch being re-pointed, the count catches a splice further upstream
        // that leaves routeVia alone — a shared trunk bending affects every wire below it.
        return lastWireModel != model
                || lastRouteVia != model.getRouteVia()
                || lastReroutePointCount != model.getReroutePoints().size();
    }

    @Override
    public void addModelDependencies() {
        if (graphView == null) return;
        var model = getModel();
        var fromPort = model.getFromPort();
        if (fromPort != null && graphView.getModelElement(fromPort) instanceof PortElement portElement) {
            portElement.addDependencyToWireModel(model);
        }
        var toPort = model.getToPort();
        if (toPort != null && graphView.getModelElement(toPort) instanceof PortElement portElement) {
            portElement.addDependencyToWireModel(model);
        }
        // Moving a reroute point changes the wire's geometry, so a change on the point model must
        // reach us even when its element is rebuilt rather than merely re-laid-out.
        var chain = model.getReroutePoints();
        for (var reroutePoint : chain) {
            getDependencies().addModelDependency(reroutePoint);
        }
        lastReroutePointCount = chain.size();
        lastRouteVia = model.getRouteVia();
        lastWireModel = model;
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
        if (visitor.hasHint(ChangeHint.LAYOUT)) {
            updatePortPosition();
        }
    }

    public static List<WireCommands.ConvertWiresToPortalsCommand.PortalData> getPortalsWireData(ArrayList<WireModel> wires, GraphView graphView) {
        return wires.stream().map(wireModel -> {
            var outputPort = graphView.getModelElement(wireModel.getFromPort());
            var inputPort =graphView.getModelElement(wireModel.getToPort());
            var outputNode = wireModel.getFromPort() == null ? null : graphView.getModelElement(wireModel.getFromPort().getNodeModel());
            var inputNode = wireModel.getToPort() == null ? null : graphView.getModelElement(wireModel.getToPort().getNodeModel());;
            var wire = graphView.getModelElement(wireModel);

            if (outputNode == null || inputNode == null || outputPort == null || inputPort == null || wire == null)
                return null;

            var outputPos = graphView.getContentViewContainer().worldToLocal(
                    outputPort.localToWorld(new Vector2f(outputPort.getPositionX(), outputPort.getPositionY()))
            );
            var inputPos = graphView.getContentViewContainer().worldToLocal(
                    inputPort.localToWorld(new Vector2f(inputPort.getPositionX(), inputPort.getPositionY()))
            );
            return new WireCommands.ConvertWiresToPortalsCommand.PortalData(wireModel,
                    outputPos,
                    inputPos
            );
        }).filter(Objects::nonNull).toList();
    }

    /**
     * Resolves a port's wire-endpoint position in world coordinates. Normally projects to the
     * port connector's centre; when the owning node is collapsed, projects to the node title
     * bar's edge instead (left edge for INPUT, right edge for OUTPUT) so wires stay visually
     * attached after the port row is hidden.
     *
     * <p>Note: {@code getWorldMouse} expects <em>absolute</em> layout coords (the cumulative sum
     * up the parent chain that {@link com.lowdragmc.lowdraglib2.gui.ui.UIElement#getPositionX()}
     * returns) — not coords local to the element. Passing local coords here would make the wire
     * land near the global origin.</p>
     */
    private Vector2f resolvePortEndpoint(PortModel port) {
        var graphView = getGraphView();
        if (graphView == null) return new Vector2f();
        var nodeModel = port.getNodeModel();
        boolean collapsed = nodeModel instanceof AbstractNodeModel anm && anm.isCollapsed();
        if (collapsed && graphView.getModelElement(nodeModel) instanceof NodeElement nodeElement
                && nodeElement.getNodeTittle() != null) {
            var title = nodeElement.getNodeTittle();
            boolean isOutput = port.getDirection() == PortDirection.OUTPUT;
            float absX = title.getPositionX() + (isOutput ? title.getSizeWidth() : 0f);
            float absY = title.getPositionY() + title.getSizeHeight() / 2f;
            return title.getWorldMouse(absX, absY);
        }
        if (graphView.getModelElement(port) instanceof PortElement portElement) {
            var portConnector = portElement.getConnector().getConnectorIcon();
            return portElement.getWorldMouse(
                    portConnector.getPositionX() + portConnector.getSizeWidth() / 2,
                    portConnector.getPositionY() + portConnector.getSizeHeight() / 2
            );
        }
        return new Vector2f();
    }

    /**
     * One of the wire's two ends in world coordinates: the port's endpoint, or — while the wire is a
     * ghost being dragged out of a port — the loose end following the cursor.
     *
     * @param isFrom which end, used only to pick the ghost's loose end
     */
    private Vector2f endpointWorldPosition(@org.jetbrains.annotations.Nullable PortModel port, boolean isFrom) {
        if (port != null) return resolvePortEndpoint(port);
        if (getModel() instanceof IGhostWireModel ghostWire) {
            return isFrom ? ghostWire.getFromWorldPoint() : ghostWire.getToWorldPoint();
        }
        return new Vector2f();
    }

    protected void updatePortPosition() {
        var graphView = getGraphView();
        if (graphView == null) return;
        var model = getModel();
        var fromPort = model.getFromPort();
        var toPort = model.getToPort();
        var dirty = rawPoints.isEmpty() || geometryDirty;

        if (getParent() == null) return;

        var fromPos = getParent().worldToLocalLayoutOffset(endpointWorldPosition(fromPort, true));
        if (!fromPos.equals(from)) {
            dirty = true;
            this.from = fromPos;
        }
        var toPos = getParent().worldToLocalLayoutOffset(endpointWorldPosition(toPort, false));
        if (!toPos.equals(to)) {
            dirty = true;
            this.to = toPos;
        }
        var fromPortOffset = Optional.ofNullable(fromPort).map(PortModel::getNodeModel).map(PortNodeModel::getPortWireOffset).orElse(15F);
        if (fromPortOffset != fromOffset) {
            dirty = true;
            this.fromOffset = fromPortOffset;
        }
        var toPortOffset = Optional.ofNullable(toPort).map(PortModel::getNodeModel).map(PortNodeModel::getPortWireOffset).orElse(15F);
        if (toPortOffset != toOffset) {
            dirty = true;
            this.toOffset = toPortOffset;
        }

        // Reroute points, resolved from their UI so the wire tracks one mid-drag (the drag preview
        // moves the element's layout without touching the model). This runs per wire per frame and
        // most wires are straight, so that case is short-circuited before the chain walk: neither the
        // chain list nor the per-point matrix work happens.
        List<Vector2f> anchors = Collections.emptyList();
        if (model.getRouteVia() != null) {
            var chain = model.getReroutePoints();
            // Two points each: a reroute point is drawn as a little node, so the wire lands on its
            // input dot and leaves from its output dot rather than crossing a single spot.
            anchors = new ArrayList<>(chain.size() * 2);
            for (var reroutePoint : chain) {
                anchors.add(getParent().worldToLocalLayoutOffset(resolveReroutePointAnchor(reroutePoint, true)));
                anchors.add(getParent().worldToLocalLayoutOffset(resolveReroutePointAnchor(reroutePoint, false)));
            }
        }
        if (!anchors.equals(reroutePositions)) {
            dirty = true;
            this.reroutePositions = anchors;
        }

        if (dirty) {
            // Control points leave the port along its orientation: horizontal ports exit sideways
            // (±x), vertical ports exit up/down (±y). The output endpoint (`from`) pushes in the
            // exit direction (+), the input endpoint (`to`) pulls back from its approach side (-).
            // Reroute anchors sit between the two control points, in wire order, and are passed through
            // verbatim — the corner rounding below turns them into smooth bends.
            var localPoints = new ArrayList<Vector2f>(4 + anchors.size());
            localPoints.add(from);
            localPoints.add(controlPoint(fromPort, from, fromOffset, true));
            localPoints.addAll(anchors);
            localPoints.add(controlPoint(toPort, to, toOffset, false));
            localPoints.add(to);

            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (var point : localPoints) {
                minX = Math.min(minX, point.x);
                minY = Math.min(minY, point.y);
                maxX = Math.max(maxX, point.x);
                maxY = Math.max(maxY, point.y);
            }
            var border = 2;
            // Copied into effectively-final locals for the style lambda.
            float boxLeft = minX - border, boxTop = minY - border;
            float boxWidth = maxX - minX + 2 * border, boxHeight = maxY - minY + 2 * border;
            // Wire bounds computed from connected port positions — pin via IMPORTANT.
            Style.importantPipeline(getLayout(), l -> l
                    .left(boxLeft)
                    .top(boxTop)
                    .width(boxWidth)
                    .height(boxHeight));
            var offset = new Vector2f(getParent().getPositionX(), getParent().getPositionY());
            rawPoints = localPoints.stream().map(point -> point.add(offset, new Vector2f())).toList();
            // Rounding a corner costs 8 extra points each; at reduced LOD the fillet is smaller
            // than a pixel, so the raw polyline is used verbatim.
            drawPoints = effectiveLod() == GraphViewLod.SIMPLIFIED ? rawPoints : roundCorners(rawPoints, 6, 8);
            geometryDirty = false;
        }
    }

    /**
     * Resolves one of a reroute point's two connector anchors in world coordinates — the centre of
     * the matching dot element when one exists, otherwise the model anchor projected out of canvas
     * content space (the element is built one frame behind the model on creation and after a rebuild).
     *
     * @param input {@code true} for the incoming side, {@code false} for the side branches leave from
     */
    private Vector2f resolveReroutePointAnchor(WireReroutePointModel reroutePoint, boolean input) {
        var graphView = getGraphView();
        if (graphView == null) return new Vector2f();
        if (graphView.getModelElement(reroutePoint) instanceof WireReroutePointElement element) {
            var dot = input ? element.getInputDot() : element.getOutputDot();
            if (dot != null) {
                return dot.getWorldMouse(
                        dot.getPositionX() + dot.getSizeWidth() / 2,
                        dot.getPositionY() + dot.getSizeHeight() / 2);
            }
        }
        var container = graphView.getContentViewContainer();
        var anchor = input ? reroutePoint.getInputAnchor() : reroutePoint.getOutputAnchor();
        // Inverse of worldToLocalLayoutOffset: add the container's own position back before projecting.
        return container.getWorldMouse(
                anchor.x + container.getPositionX(),
                anchor.y + container.getPositionY());
    }

    /**
     * The reroute index a point dropped at {@code contentLocalPosition} (canvas content coordinates)
     * should take, i.e. the index of the polyline segment it is closest to.
     *
     * <p>Segments are counted along {@code fromPort → reroute[0] → … → reroute[n-1] → toPort}, so
     * segment {@code i} is exactly the slot a new point must occupy to bend it.</p>
     */
    public int reroutePointInsertIndex(Vector2f contentLocalPosition) {
        var route = routeContentPositions();
        if (route.size() < 2) return getModel().getReroutePoints().size();
        var best = 0;
        var bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < route.size() - 1; i++) {
            var distance = distanceToSegment(contentLocalPosition, route.get(i), route.get(i + 1));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    /**
     * The wire's route as {@code [fromPort, point…, toPort]} in canvas content coordinates — the same
     * space model positions live in. Control points are deliberately left out: they are cosmetic
     * tangents, and including them would bias segment picking near the ports.
     */
    protected List<Vector2f> routeContentPositions() {
        var graphView = getGraphView();
        if (graphView == null) return List.of();
        var container = graphView.getContentViewContainer();
        var model = getModel();
        var route = new ArrayList<Vector2f>(2 + model.getReroutePoints().size());
        route.add(container.worldToLocalLayoutOffset(endpointWorldPosition(model.getFromPort(), true)));
        for (var reroutePoint : model.getReroutePoints()) {
            route.add(reroutePoint.getCenter());
        }
        route.add(container.worldToLocalLayoutOffset(endpointWorldPosition(model.getToPort(), false)));
        return route;
    }

    private static float distanceToSegment(Vector2f point, Vector2f a, Vector2f b) {
        float dx = b.x - a.x, dy = b.y - a.y;
        float length2 = dx * dx + dy * dy;
        float t = length2 < 1e-6f ? 0f : ((point.x - a.x) * dx + (point.y - a.y) * dy) / length2;
        t = Math.clamp(t, 0f, 1f);
        float ex = point.x - (a.x + t * dx);
        float ey = point.y - (a.y + t * dy);
        return (float) Math.sqrt(ex * ex + ey * ey);
    }

    /**
     * Computes a wire control point offset from {@code endpoint} in the direction the wire should
     * leave/approach the port. Horizontal ports offset along x, vertical ports along y. {@code isFrom}
     * (the output endpoint) offsets in the positive exit direction; the input endpoint offsets back
     * along the negative approach direction — matching the node layout (horizontal: out right / in
     * left; vertical: out bottom / in top).
     */
    private Vector2f controlPoint(@org.jetbrains.annotations.Nullable PortModel port, Vector2f endpoint, float offset, boolean isFrom) {
        float sign = isFrom ? 1f : -1f;
        boolean vertical = port != null && port.getOrientation() == PortOrientation.Vertical;
        return vertical
                ? endpoint.add(0, sign * offset, new Vector2f())
                : endpoint.add(sign * offset, 0, new Vector2f());
    }

    public static List<Vector2f> roundCorners(List<Vector2f> input, float radius, int cornerSegments) {
        if (input == null || input.size() < 3) return input;

        var out = new ArrayList<Vector2f>(input.size() * (cornerSegments + 1));
        out.add(new Vector2f(input.getFirst()));

        for (var i = 1; i < input.size() - 1; i++) {
            Vector2f A = input.get(i - 1);
            Vector2f B = input.get(i);
            Vector2f C = input.get(i + 1);

            Vector2f BA = new Vector2f(A).sub(B);
            Vector2f BC = new Vector2f(C).sub(B);

            float lenBA = BA.length();
            float lenBC = BC.length();

            if (lenBA < 1e-4f || lenBC < 1e-4f) {
                out.add(new Vector2f(B));
                continue;
            }

            Vector2f d1 = BA.div(lenBA);
            Vector2f d2 = BC.div(lenBC);

            // if almost parallel, no need to round corners
            float cross = d1.x * d2.y - d1.y * d2.x;
            float dot = d1.dot(d2);
            if (Math.abs(cross) < 1e-4f && dot < -0.999f) {
                // 180 degree turn (very sharp), still roundable; here we don't special handle either way'
            } else if (Math.abs(cross) < 1e-4f && dot > 0.999f) {
                out.add(new Vector2f(B));
                continue;
            }

            float r = Math.min(radius, Math.min(lenBA, lenBC) * 0.5f);

            // Points at which to enter the corner
            Vector2f P1 = new Vector2f(B).add(new Vector2f(d1).mul(r));
            Vector2f P2 = new Vector2f(B).add(new Vector2f(d2).mul(r));

            // Replace corner with Bezier: first put P1, then sample to P2 (control point is B)
            out.add(P1);

            int seg = Math.max(1, cornerSegments);
            for (int s = 1; s < seg; s++) {
                float t = s / (float) seg;

                // Quadratic Bezier: (1-t)^2 P1 + 2(1-t)t B + t^2 P2
                float u = 1f - t;
                float w1 = u * u;
                float w2 = 2f * u * t;
                float w3 = t * t;

                out.add(new Vector2f(
                        w1 * P1.x + w2 * B.x + w3 * P2.x,
                        w1 * P1.y + w2 * B.y + w3 * P2.y
                ));
            }

            out.add(P2);
        }

        out.add(new Vector2f(input.getLast()));
        return out;
    }

    @Override
    public void screenTick() {
        super.screenTick();
        var mui = getModularUI();
        if (mui == null) return;
    }

    /**
     * The level this wire draws at.
     *
     * <p>Ghost wires ignore it entirely: one is live feedback for a drag the user is performing right
     * now, and dropping it at low zoom would make the interaction look broken. There is at most a
     * handful of them, so the cost is irrelevant.
     */
    protected GraphViewLod effectiveLod() {
        return getModel() instanceof IGhostWireModel ? GraphViewLod.FULL : lod();
    }

    @Override
    public void drawBackgroundAdditional(@NotNull GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        var lod = effectiveLod();
        if (lod != lastLod) {
            lastLod = lod;
            geometryDirty = true;
        }
        // BLOCK returns before updatePortPosition(): resolving both endpoints costs ~4 matrix ops
        // per wire per frame, which is the real cost of a large graph, not the draw call.
        if (lod == GraphViewLod.BLOCK) return;
        updatePortPosition();
        if (drawPoints.isEmpty()) return;
        // couldn't be clicking state
        var isSelected = isSelected() || isUnderRegionSelection();
        var isHover = isHover();

        var fromColor = -1;
        var toColor = -1;

        if (isSelected) {
            fromColor = ColorPattern.BLUE.color;
            toColor = ColorPattern.BLUE.color;
        } else {
            var fromPort = getModel().getFromPort();
            if (fromPort != null) {
                fromColor = fromPort.getDataTypeHandle().getTypeColor();
            }
            var toPort = getModel().getToPort();
            if (toPort != null) {
                toColor = toPort.getDataTypeHandle().getTypeColor();
            }
        }

        if (!isActive()) {
            fromColor &= 0x77FFFFFF;
            toColor &= 0x77FFFFFF;
        }
        if (lod == GraphViewLod.SIMPLIFIED) {
            // Flat quad strip, no beam shader: the per-fragment falloff is invisible at this zoom.
            DrawerHelper.drawLines(guiContext.graphics, drawPoints, fromColor, toColor, 3f);
        } else {
            DrawerHelper.drawTexLines(guiContext.graphics,
                    LDLibRenderTypes.graphWire(),
                    drawPoints,
                    fromColor,
                    toColor,
                    (isHover ? 1.1f : 0.7f) * 7);
        }
    }

    @Override
    public boolean isIntersectWithPoint(double localX, double localY) {
        // check element rect first
        if (!super.isIntersectWithPoint(localX, localY)) return false;
        // No geometry yet: the wire has never been drawn (culled since creation, or the graph is
        // zoomed out far enough that BLOCK LOD skips the endpoint resolve). Nothing to hit.
        if (rawPoints.size() < 2) return false;
        var localMouse = new Vector2f((float) localX, (float) localY);
        // One test per polyline segment: four for a straight wire, plus two per reroute point.
        for (int i = 0; i < rawPoints.size() - 1; i++) {
            if (isMouseOverLine(localMouse, rawPoints.get(i), rawPoints.get(i + 1), 2)) return true;
        }
        return false;
    }

    @Override
    public boolean isOverlapping(float localX, float localY, float localWidth, float localHeight) {
        if (!super.isOverlapping(localX, localY, localWidth, localHeight)) return false;
        if (rawPoints.size() < 2) return false;
        var localRect = new Vector4f(localX, localY, localWidth, localHeight);
        for (int i = 0; i < rawPoints.size() - 1; i++) {
            if (isRectOverlapping(localRect, rawPoints.get(i), rawPoints.get(i + 1), 2)) return true;
        }
        return false;
    }

    @Override
    public boolean canBeRegionSelected(Vector4f region) {
        var graphEditor = getGraphView();
        if (graphEditor == null) return false;
        var model = getModel();
        if (model.getFromPort() == null || model.getToPort() == null) return false;
        var fromNode = model.getFromPort().getNodeModel();
        var toNode = model.getToPort().getNodeModel();
        if (fromNode == null || toNode == null) return false;
        var fromElement = graphEditor.getModelElements().get(fromNode);
        var toElement = graphEditor.getModelElements().get(toNode);
        if (fromElement == null || toElement == null) return false;
        var isFromRegionSelected = fromElement.canBeRegionSelected(region);
        var isToRegionSelected = toElement.canBeRegionSelected(region);
        return (isFromRegionSelected && isToRegionSelected)
                || !isFromRegionSelected && !isToRegionSelected && super.canBeRegionSelected(region);
    }

    private boolean isMouseOverLine(Vector2f mouse, Vector2f point1, Vector2f point2, float width) {
        // Treat width as the full stroke width; hit radius is half (add small epsilon for usability)
        final float radius = Math.max(0.5f, width * 0.5f);

        float x = mouse.x, y = mouse.y;
        float x1 = point1.x, y1 = point1.y;
        float x2 = point2.x, y2 = point2.y;

        float dx = x2 - x1;
        float dy = y2 - y1;

        // Degenerate segment (point)
        float len2 = dx * dx + dy * dy;
        if (len2 < 1e-6f) {
            float px = x - x1;
            float py = y - y1;
            return (px * px + py * py) <= radius * radius;
        }

        // Project mouse onto segment, clamp t to [0,1]
        float t = ((x - x1) * dx + (y - y1) * dy) / len2;
        if (t < 0f) t = 0f;
        else if (t > 1f) t = 1f;

        // Closest point on segment
        float cx = x1 + t * dx;
        float cy = y1 + t * dy;

        float ex = x - cx;
        float ey = y - cy;

        return (ex * ex + ey * ey) <= radius * radius;
    }

    private boolean isRectOverlapping(Vector4f rect, Vector2f point1, Vector2f point2, float width) {
        final float r = Math.max(0.5f, width * 0.5f);

        // Normalize rect to (minX,minY,maxX,maxY)
        float x1 = rect.x, y1 = rect.y, w = rect.z, h = rect.w;
        float minX = Math.min(x1, x1 + w);
        float maxX = Math.max(x1, x1 + w);
        float minY = Math.min(y1, y1 + h);
        float maxY = Math.max(y1, y1 + h);

        // Expand rect by radius r
        float ex0 = minX - r, ey0 = minY - r;
        float ex1 = maxX + r, ey1 = maxY + r;

        // If either endpoint is inside expanded rect, overlap
        if (pointInAabb(point1.x, point1.y, ex0, ey0, ex1, ey1) ||
                pointInAabb(point2.x, point2.y, ex0, ey0, ex1, ey1)) {
            return true;
        }

        // Segment intersects expanded AABB?
        return segmentIntersectsAabb(point1.x, point1.y, point2.x, point2.y, ex0, ey0, ex1, ey1);
    }

    private static boolean pointInAabb(float px, float py, float minX, float minY, float maxX, float maxY) {
        return px >= minX && px <= maxX && py >= minY && py <= maxY;
    }

    /**
     * Liang-Barsky style / parametric slab test:
     * segment P(t)=P0 + t*(P1-P0), t in [0,1]
     */
    private static boolean segmentIntersectsAabb(
            float x0, float y0, float x1, float y1,
            float minX, float minY, float maxX, float maxY
    ) {
        float dx = x1 - x0;
        float dy = y1 - y0;

        float tMin = 0f;
        float tMax = 1f;

        // X slab
        if (Math.abs(dx) < 1e-8f) {
            if (x0 < minX || x0 > maxX) return false;
        } else {
            float inv = 1f / dx;
            float t1 = (minX - x0) * inv;
            float t2 = (maxX - x0) * inv;
            if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        // Y slab
        if (Math.abs(dy) < 1e-8f) {
            if (y0 < minY || y0 > maxY) return false;
        } else {
            float inv = 1f / dy;
            float t1 = (minY - y0) * inv;
            float t2 = (maxY - y0) * inv;
            if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        return true;
    }
}
