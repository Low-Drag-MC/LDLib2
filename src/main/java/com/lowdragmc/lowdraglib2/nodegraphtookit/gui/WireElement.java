package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
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
    protected ModelElement lastUsedFromPort;
    protected ModelElement lastUsedToPort;
    protected WireModel lastWireModel;

    public WireElement(WireModel wireModel) {
        super(wireModel);
        addClass("__wire__");
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
        return lastUsedFromPort != modelElements.get(getModel().getFromPort()) || lastUsedToPort != modelElements.get(getModel().getToPort());
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
        return lastWireModel != getModel();
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

    protected void updatePortPosition() {
        var graphView = getGraphView();
        if (graphView == null) return;
        var model = getModel();
        var fromPort = model.getFromPort();
        var toPort = model.getToPort();
        var dirty = rawPoints.isEmpty();

        Vector2f fromWorldPos = new Vector2f();
        if (fromPort == null) {
            if (model instanceof IGhostWireModel ghostWire) {
                fromWorldPos = ghostWire.getFromWorldPoint();
            }
        } else {
            fromWorldPos = resolvePortEndpoint(fromPort);
        }

        Vector2f toWorldPos = new Vector2f();
        if (toPort == null) {
            if (model instanceof IGhostWireModel ghostWire) {
                toWorldPos = ghostWire.getToWorldPoint();
            }
        } else {
            toWorldPos = resolvePortEndpoint(toPort);
        }

        if (getParent() == null) return;

        var fromPos = getParent().worldToLocalLayoutOffset(fromWorldPos);
        if (!fromPos.equals(from)) {
            dirty = true;
            this.from = fromPos;
        }
        var toPos = getParent().worldToLocalLayoutOffset(toWorldPos);
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

        if (dirty) {
            // Control points leave the port along its orientation: horizontal ports exit sideways
            // (±x), vertical ports exit up/down (±y). The output endpoint (`from`) pushes in the
            // exit direction (+), the input endpoint (`to`) pulls back from its approach side (-).
            var fromPoint2 = controlPoint(fromPort, from, fromOffset, true);
            var toPoint2 = controlPoint(toPort, to, toOffset, false);

            var minX = Math.min(Math.min(from.x, to.x), Math.min(fromPoint2.x, toPoint2.x));
            var minY = Math.min(Math.min(from.y, to.y), Math.min(fromPoint2.y, toPoint2.y));
            var maxX = Math.max(Math.max(from.x, to.x), Math.max(fromPoint2.x, toPoint2.x));
            var maxY = Math.max(Math.max(from.y, to.y), Math.max(fromPoint2.y, toPoint2.y));
            var border = 2;

            var x = minX - border;
            var y = minY - border;
            float fX = x, fY = y, fW = maxX - minX + 2 * border, fH = maxY - minY + 2 * border;
            // Wire bounds computed from connected port positions — pin via IMPORTANT.
            Style.importantPipeline(getLayout(), l -> l
                    .left(fX)
                    .top(fY)
                    .width(fW)
                    .height(fH));
            var offset = new Vector2f(getParent().getPositionX(), getParent().getPositionY());
            var realFrom = from.add(offset, new Vector2f());
            var realTo = to.add(offset, new Vector2f());
            fromPoint2 = fromPoint2.add(offset);
            toPoint2 = toPoint2.add(offset);
            rawPoints = List.of(realFrom, fromPoint2, toPoint2, realTo);
            drawPoints = roundCorners(rawPoints, 6, 8);
        }
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

    @Override
    public void drawBackgroundAdditional(@NotNull GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
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
        DrawerHelper.drawTexLines(guiContext.graphics,
                LDLibRenderTypes.graphWire(),
                drawPoints,
                fromColor,
                toColor,
                (isHover ? 1.1f : 0.7f) * 7);
    }

    @Override
    public boolean isIntersectWithPoint(double localX, double localY) {
        // check element rect first
        if (!super.isIntersectWithPoint(localX, localY)) return false;
        var localMouse = new Vector2f((float) localX, (float) localY);
        // line1
        if (isMouseOverLine(localMouse, rawPoints.get(0), rawPoints.get(1), 2)) return true;
        // line2
        if (isMouseOverLine(localMouse, rawPoints.get(1), rawPoints.get(2), 2)) return true;
        // line3
        return isMouseOverLine(localMouse, rawPoints.get(2), rawPoints.get(3), 2);
    }

    @Override
    public boolean isOverlapping(float localX, float localY, float localWidth, float localHeight) {
        if (!super.isOverlapping(localX, localY, localWidth, localHeight)) return false;
        var localRect = new Vector4f(localX, localY, localWidth, localHeight);
        // line1
        if (isRectOverlapping(localRect, rawPoints.get(0), rawPoints.get(1), 2)) return true;
        // line2
        if (isRectOverlapping(localRect, rawPoints.get(1), rawPoints.get(2), 2)) return true;
        // line3
        return isRectOverlapping(localRect, rawPoints.get(2), rawPoints.get(3), 2);
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
