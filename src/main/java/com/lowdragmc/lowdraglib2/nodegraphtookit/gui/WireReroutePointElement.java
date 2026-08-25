package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphViewLod;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.WireCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireReroutePointModel;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A reroute point, drawn as a miniature node: an input dot on the left, a divider, an output dot on
 * the right. That shape is not decoration — it is what the thing is. A wire portal's entry and exit
 * collapsed into one body, so wires land on the left dot and every branch leaves from the right one.
 *
 * <p>Both dots are real connection handles, and each stands in for the real port on its side of the
 * connection: dragging off the output dot offers compatible <em>input</em> ports and adds a branch,
 * dragging off the input dot offers compatible <em>output</em> ports and re-sources every branch.
 * Either dot also accepts a wire dropped on it, with the same two meanings picked from which end the
 * user grabbed. Dragging the <em>body</em> between them moves the point, and every wire follows.</p>
 *
 * <p>Lives on the {@link WireElement#WIRE_LAYER} with a raised z-index so it wins hit-testing over
 * the wires that run underneath it, while still passing below nodes.</p>
 */
public class WireReroutePointElement extends GraphElement<WireReroutePointModel> {

    @Getter
    private UIElement inputDot;
    @Getter
    private UIElement outputDot;
    // runtime
    @Nullable
    private WireDragHelper wireDragHelper;
    private boolean isWireDragging;
    /**
     * The source port, cached. Resolving it scans the graph's wires, and drawing asks for it three
     * times a frame (body plus both dots) — so it is refreshed on model updates and on the client
     * tick instead, which is far more often than a wire's type can visibly change.
     */
    @Nullable
    private PortModel cachedSourcePort;

    public WireReroutePointElement(WireReroutePointModel model) {
        super(model);
        addClass("__wire-reroute-point__");
    }

    @Override
    public String getLayerName() {
        return WireElement.WIRE_LAYER;
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        var model = getModel();
        // Position and size come from the model — pin via IMPORTANT.
        Style.importantPipeline(getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE)
                .left(model.getPosition().x)
                .top(model.getPosition().y)
                .width(WireReroutePointModel.WIDTH)
                .height(WireReroutePointModel.HEIGHT));
        // Sits on the wire layer: without this the wires drawn after it would swallow the clicks.
        Style.defaultPipeline(getStyle(), s -> s.zIndex(1));

        inputDot = buildDot(true);
        outputDot = buildDot(false);
        addChildren(inputDot, outputDot);
    }

    /**
     * One of the two connector dots. They are separate child elements rather than painted decoration
     * so that a press on one starts a wire drag while a press on the body between them still starts a
     * move — and so the drag can be started <em>on the dot</em>, which is what carries the drag
     * listeners. Their positions mirror {@link WireReroutePointModel#getInputAnchor()} and
     * {@link WireReroutePointModel#getOutputAnchor()}, which is where wires are drawn to.
     */
    private UIElement buildDot(boolean isInput) {
        float left = isInput
                ? WireReroutePointModel.DOT_INSET
                : WireReroutePointModel.WIDTH - WireReroutePointModel.DOT_INSET - WireReroutePointModel.DOT_SIZE;
        var dot = new UIElement().addClass(isInput
                ? "__wire-reroute-point_input__"
                : "__wire-reroute-point_output__");
        Style.defaultPipeline(dot.getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE)
                .left(left)
                .top((WireReroutePointModel.HEIGHT - WireReroutePointModel.DOT_SIZE) / 2f)
                .width(WireReroutePointModel.DOT_SIZE)
                .height(WireReroutePointModel.DOT_SIZE));
        Style.defaultPipeline(dot.getStyle(), s -> s
                .zIndex(1)
                .background(DynamicTexture.of(() -> dotTexture(dot)))
                .tooltips(isInput ? "graph.reroute_point.input" : "graph.reroute_point.output"));
        dot.addEventListener(UIEvents.MOUSE_DOWN, event -> onDotMouseDown(event, dot, isInput));
        dot.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDotDragSourceUpdate);
        dot.addEventListener(UIEvents.DRAG_END, this::onDotDragEnd);
        return dot;
    }

    @Override
    public void setGraphView(@Nullable GraphView graphView) {
        super.setGraphView(graphView);
        // Rebuilt per drag, because which of the two operations it performs depends on the dot.
        wireDragHelper = null;
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
        if (visitor.hasHint(ChangeHint.LAYOUT)) {
            var model = getModel();
            Style.importantPipeline(getLayout(), l -> l.left(model.getPosition().x).top(model.getPosition().y));
        }
        cachedSourcePort = getModel().getSourcePort();
    }

    @Override
    public void screenTick() {
        super.screenTick();
        // Re-sourcing a point and retyping its source both change the colour without producing a
        // change hint here, so the cache is refreshed on the tick rather than only on updates.
        cachedSourcePort = getModel().getSourcePort();
    }

    /**
     * Whether either connector dot is under the cursor. {@link WireDragHelper} asks this to decide
     * whether a wire being dropped landed on the point — the body between the dots is the move
     * handle, so a drop aimed there is not a connection.
     */
    public boolean isConnectorHovered() {
        return inputDot != null && outputDot != null
                && (inputDot.isSelfOrChildHover() || outputDot.isSelfOrChildHover());
    }

    // region drag out

    /**
     * Starts a wire drag from one of the two dots. Which end of a connection the user is holding
     * follows from which dot they grabbed, so the drag offers the ports that side can actually reach:
     *
     * <ul>
     *   <li><b>output dot</b> — stands in for the source port the branches come from, so compatible
     *       <em>input</em> ports light up and dropping on one adds a branch.</li>
     *   <li><b>input dot</b> — stands in for what the branches feed, so compatible <em>output</em>
     *       ports light up and dropping on one re-sources the whole branch.</li>
     * </ul>
     *
     * <p>Either way the candidate is routed through this point first, so the rubber band leaves the
     * dot instead of jumping back to a far-away port.</p>
     */
    protected void onDotMouseDown(UIEvent event, UIElement dot, boolean isInput) {
        if (isWireDragging) {
            event.stopImmediatePropagation();
            return;
        }
        if (event.button != 0 || graphView == null) return;
        var draggedPort = isInput ? anyBranchTarget() : getModel().getSourcePort();
        // Orphaned point (no wire through it): nothing to branch from or re-source. The graph sweeps
        // these away, so this only happens for a frame at most.
        if (draggedPort == null) return;

        var helper = new ReroutePointWireDragHelper(graphView, isInput);
        wireDragHelper = helper;
        helper.createWireCandidate();
        helper.setDraggedPort(draggedPort);
        var candidate = helper.getWireCandidateModel();
        if (candidate != null) candidate.setRouteVia(getModel());

        if (helper.handleMouseDown(event, null)) {
            WireDragHelper.enableAllWires(graphView, false, Set.of(helper.getWireCandidateModel()));
            isWireDragging = true;
            event.stopPropagation();
            dot.startDrag(helper, null);
        } else {
            helper.reset();
            wireDragHelper = null;
        }
    }

    /**
     * One of the input ports the branches currently feed, used as the held end when dragging from the
     * input dot. Any of them will do: they all take what the source port produces, so they all agree
     * on which output ports could legally replace it.
     */
    @Nullable
    private PortModel anyBranchTarget() {
        if (graphView == null || graphView.getGraph() == null) return null;
        for (var wire : graphView.getGraph().graphModel.getWiresThroughReroutePoint(getModel())) {
            if (wire.getToPort() != null) return wire.getToPort();
        }
        return null;
    }

    protected void onDotDragSourceUpdate(UIEvent event) {
        if (isWireDragging && wireDragHelper != null && graphView != null && graphView.getGraph() != null
                && event.dragHandler.draggingObject == wireDragHelper) {
            wireDragHelper.handleMouseMove(event);
            event.stopPropagation();
        }
    }

    protected void onDotDragEnd(UIEvent event) {
        if (!isWireDragging) return;
        if (wireDragHelper != null && graphView != null && graphView.getGraph() != null
                && event.dragHandler.draggingObject == wireDragHelper) {
            if (canPerformConnection(getLocalMouse(event.x, event.y))) {
                wireDragHelper.handleMouseUp(event, true, Collections.emptyList(), Collections.emptyList());
            } else {
                wireDragHelper.reset();
            }
        }
        isWireDragging = false;
        wireDragHelper = null;
        event.stopPropagation();
    }

    /** Mirrors {@code PortElement}: a twitch during a click must not create a wire. */
    protected boolean canPerformConnection(Vector2f localMouse) {
        var mui = getModularUI();
        if (mui == null) return false;
        var lastMouseDown = getLocalMouse(mui.getLastMouseDownX(), mui.getLastMouseDownY());
        return localMouse.distance(lastMouseDown) > WireDragHelper.DISTANCE_THRESHOLD;
    }

    /**
     * Turns the drop into the edit the grabbed dot implies. Everything else about the drag —
     * compatible port highlighting, the ghost preview, dropping on empty canvas to open the item
     * library — is the stock port-drag behaviour.
     */
    private class ReroutePointWireDragHelper extends WireDragHelper {
        private final boolean reSource;

        ReroutePointWireDragHelper(GraphView graphView, boolean reSource) {
            super(graphView);
            this.reSource = reSource;
        }

        @Override
        protected void createNewWire(PortModel fromPort, PortModel toPort) {
            if (reSource) {
                // Dragged from the input dot onto an output port: that port now feeds every branch.
                graphView.dispatchCommand(new WireCommands.ReSourceReroutePointCommand(getModel(), fromPort));
            } else {
                graphView.dispatchCommand(new WireCommands.CreateWireCommand(toPort, fromPort, getModel()));
            }
        }
    }

    // endregion

    @Override
    protected void drawBackgroundAdditional(@NotNull IGUIContext guiContext) {
        if (!(guiContext instanceof GUIContext context)) return;
        super.drawBackgroundAdditional(context);
        // At BLOCK zoom the body is sub-pixel and the wires themselves aren't drawn either.
        if (lod() == GraphViewLod.BLOCK) return;

        var selected = isSelected() || isUnderRegionSelection();
        var border = selected ? ColorPattern.BLUE.color
                : isSelfOrChildHover() ? ColorPattern.WHITE.color : 0xFF7F8084;
        var body = 0xFF2B2D30;
        if (!isActive()) {
            border &= 0x77FFFFFF;
            body &= 0x77FFFFFF;
        }
        context.drawTexture(new SDFRectTexture()
                        .setRadius(WireReroutePointModel.HEIGHT / 2f)
                        .setStroke(1f)
                        .setColor(body)
                        .setBorderColor(border),
                getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());

        // The divider that makes it read as a node with two sides rather than one blob. Skipped at
        // reduced detail, where it is thinner than a pixel.
        if (lod() == GraphViewLod.FULL) {
            float x = getPositionX() + getSizeWidth() / 2f;
            float inset = 2.5f;
            DrawerHelperClient.drawLines(context,
                    List.of(new Vector2f(x, getPositionY() + inset),
                            new Vector2f(x, getPositionY() + getSizeHeight() - inset)),
                    border, border, 1f);
        }
    }

    /**
     * The dots carry the wire's type colour, so a reroute point reads as part of the wires through
     * it. Hovering either one highlights it the way a port connector does.
     */
    protected IGuiTexture dotTexture(UIElement dot) {
        if (lod() != GraphViewLod.FULL) return IGuiTexture.EMPTY;
        var color = wireColor();
        if (!isActive()) color &= 0x77FFFFFF;
        return new SDFRectTexture()
                .setRadius(WireReroutePointModel.DOT_SIZE / 2f)
                .setStroke(1f)
                .setColor(color)
                .setBorderColor(dot.isSelfOrChildHover() ? ColorPattern.WHITE.color : 0xFF1E1F22);
    }

    /**
     * The colour of the wires through this point. Falls back to white when there is no resolvable
     * typed source (an orphan, or a missing port).
     */
    protected int wireColor() {
        return cachedSourcePort == null ? -1 : cachedSourcePort.getDataTypeHandle().getTypeColor();
    }
}
