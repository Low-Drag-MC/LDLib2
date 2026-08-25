package com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.WireReroutePointElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Capabilities;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ContextualMenuHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ContextualMenuItem;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IGraphElementUIModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IMovable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A reroute point: the graph-editor equivalent of Unreal's reroute node or Unity ShaderGraph's
 * redirect node — a wire portal's entry and exit merged into one draggable dot, with no label.
 *
 * <p>Like a portal it <b>fans out</b>: any number of wires may leave the same reroute point, and they
 * all share the routing that leads up to it. Unlike a portal it is <b>not a node</b> — it has no
 * ports, never appears in {@code getNodeModels()}, and is invisible to every connection-side API. A
 * wire that travels {@code out → r0 → r1 → in} still reports {@code getFromPort() == out} and
 * {@code getToPort() == in}; three wires leaving {@code r1} are simply three wires on the same output
 * port. Reroute points only decide where those wires are <em>drawn</em>.</p>
 *
 * <p>Structurally the points form a tree rooted at a source port: each one stores only its
 * {@link #getUpstream() upstream} point ({@code null} when it hangs directly off the source port),
 * and each wire stores the last point it passes through ({@link WireModel#getRouteVia()}). Splicing a
 * point into the middle of a chain therefore re-routes every wire and branch below it at once, which
 * is exactly what dragging a shared trunk should do.</p>
 */
public class WireReroutePointModel extends GraphElementModel implements IMovable, IGraphElementUIModel {
    /**
     * Box size in graph content pixels. {@link #getPosition()} is the top-left corner, matching every
     * other {@link IMovable}. It is drawn as a miniature node — an input dot, a divider, an output
     * dot — so wires enter on the left and leave on the right instead of piling onto one spot.
     */
    public static final float WIDTH = 24f;
    public static final float HEIGHT = 12f;
    /** Diameter of the two connector dots. */
    public static final float DOT_SIZE = 7f;
    /** Inset of each dot from its side of the box. */
    public static final float DOT_INSET = 2f;

    /** Guards {@link #chainFrom} against a corrupt save that links a point into a cycle. */
    private static final int MAX_CHAIN = 256;

    /**
     * Reroute points are never inspected, renamed or coloured — a dot has no data of its own.
     */
    private static final List<ContextualMenuItem> MENU_ITEMS = List.of(
            ContextualMenuHelpers.DELETE_ITEM,
            ContextualMenuHelpers.FRAME_SELECTION_ITEM
    );

    @Getter
    private Vector2f position = new Vector2f(0);
    /**
     * The point this one hangs off, or {@code null} when it is the first point after the source port.
     * Managed by {@link com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel}, which owns
     * the points and keeps the tree consistent.
     */
    @Getter
    private @Nullable WireReroutePointModel upstream;

    public WireReroutePointModel() {
        capabilities.addAll(List.of(
                Capabilities.SELECTABLE,
                Capabilities.MOVABLE,
                Capabilities.DELETABLE
        ));
    }

    /**
     * @param position top-left in canvas content coordinates. Assigned directly rather than through
     *                 {@link #setPosition}, which is guarded by {@link #isMovable()} and would emit a
     *                 change hint for a point the graph is about to report as new anyway.
     */
    public WireReroutePointModel(Vector2f position) {
        this();
        this.position = new Vector2f(position);
    }

    public void setUpstream(@Nullable WireReroutePointModel upstream) {
        if (this.upstream == upstream || upstream == this) return;
        this.upstream = upstream;
        var gm = getGraphModel();
        if (gm != null) {
            // Everything downstream is drawn relative to this chain, so the whole branch re-routes.
            gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.LAYOUT);
        }
    }

    @Override
    public void setPosition(Vector2f value) {
        if (!isMovable()) return;
        if (Objects.equals(position, value)) return;
        position = new Vector2f(value);
        var gm = getGraphModel();
        if (gm != null) {
            gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.LAYOUT);
        }
    }

    @Override
    public void move(Vector2f delta) {
        if (!isMovable()) return;
        setPosition(getPosition().add(delta, new Vector2f()));
    }

    /**
     * The centre of the box. Used to place a point on the spot the user clicked; the wires themselves
     * attach to {@link #getInputAnchor()} and {@link #getOutputAnchor()}.
     */
    public Vector2f getCenter() {
        return new Vector2f(position.x + WIDTH / 2f, position.y + HEIGHT / 2f);
    }

    /**
     * Places the point so that its centre lands on {@code center}.
     */
    public void setCenter(Vector2f center) {
        setPosition(centerToPosition(center));
    }

    /**
     * Converts a centre position into the top-left {@link #getPosition()} it implies.
     */
    public static Vector2f centerToPosition(Vector2f center) {
        return new Vector2f(center.x - WIDTH / 2f, center.y - HEIGHT / 2f);
    }

    /**
     * Where the incoming trunk lands: the centre of the left-hand dot.
     */
    public Vector2f getInputAnchor() {
        return new Vector2f(position.x + DOT_INSET + DOT_SIZE / 2f, position.y + HEIGHT / 2f);
    }

    /**
     * Where every branch leaves from: the centre of the right-hand dot.
     */
    public Vector2f getOutputAnchor() {
        return new Vector2f(position.x + WIDTH - DOT_INSET - DOT_SIZE / 2f, position.y + HEIGHT / 2f);
    }

    /**
     * The source port every wire through this point comes from, or {@code null} when the point is
     * orphaned (no wire routes through it — the graph sweeps those away).
     *
     * <p>Derived rather than stored: a wire endpoint can be re-dragged onto a different port, and a
     * cached copy would silently go stale.</p>
     */
    public @Nullable PortModel getSourcePort() {
        var gm = getGraphModel();
        if (gm == null) return null;
        for (var wire : gm.getWiresThroughReroutePoint(this)) {
            if (wire.getFromPort() != null) return wire.getFromPort();
        }
        return null;
    }

    /**
     * Whether the chain ending at {@code head} passes through {@code point}. The allocation-free
     * counterpart of {@code chainFrom(head).contains(point)}, because this runs once per wire every
     * time anything asks which wires travel through a point.
     */
    public static boolean chainContains(@Nullable WireReroutePointModel head,
                                        @Nullable WireReroutePointModel point) {
        if (point == null) return false;
        var depth = 0;
        for (var current = head; current != null && depth < MAX_CHAIN; current = current.upstream, depth++) {
            if (current == point) return true;
        }
        return false;
    }

    /**
     * The chain ending at {@code point}, ordered from the one nearest the source port down to
     * {@code point} itself — the route a wire leaving it is drawn along. Stops at {@link #MAX_CHAIN}
     * so a cyclic link from a corrupt save cannot hang the renderer.
     */
    public static List<WireReroutePointModel> chainFrom(@Nullable WireReroutePointModel point) {
        if (point == null) return List.of();
        var chain = new ArrayList<WireReroutePointModel>();
        for (var current = point; current != null && chain.size() < MAX_CHAIN; current = current.upstream) {
            chain.add(current);
        }
        Collections.reverse(chain);
        return chain;
    }

    @Override
    public List<ContextualMenuItem> getContextualMenuItems() {
        return MENU_ITEMS;
    }

    @Override
    public @Nullable GraphElement<?> createElementUI() {
        return new WireReroutePointElement(this);
    }

    @Override
    public String toString() {
        return "WireReroutePoint(" + position.x + ", " + position.y + ")";
    }
}
