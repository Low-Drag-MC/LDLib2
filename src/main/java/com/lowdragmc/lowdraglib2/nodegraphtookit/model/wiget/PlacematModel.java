package com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wiget.PlacematElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlacematModel extends GraphElementModel implements IMovable, IHasName, IHasElementColor, IGraphElementUIModel {
    @Persisted @Getter
    private Vector2f position = new Vector2f(0);
    @Persisted @Getter
    private Vector2f size = new Vector2f(200, 150);
    @Persisted @Getter
    private String name = "Placemat";
    @Persisted @Getter
    private int elementColor = 0x90606aee;
    @Persisted @Getter
    private boolean userColor = false;
    @Persisted @Getter
    private int zOrder = 0;

    public PlacematModel() {
        capabilities.addAll(List.of(
                Capabilities.SELECTABLE,
                Capabilities.MOVABLE,
                Capabilities.RESIZABLE,
                Capabilities.DELETABLE,
                Capabilities.COLORABLE,
                Capabilities.RENAMABLE,
                Capabilities.COPIABLE,
                Capabilities.ASCENDABLE
        ));
    }

    @Override
    public void setPosition(Vector2f value) {
        if (!isMovable()) return;
        if (Objects.equals(position, value)) return;
        position = value;
        GraphModel gm = getGraphModel();
        if (gm != null) gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.LAYOUT);
    }

    @Override
    public void move(Vector2f delta) {
        if (!isMovable()) return;
        setPosition(getPosition().add(delta, new Vector2f()));
    }

    public void setSize(Vector2f value) {
        if (Objects.equals(size, value)) return;
        size = value;
        GraphModel gm = getGraphModel();
        if (gm != null) gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.LAYOUT);
    }

    @Override
    public void setName(String value) {
        if (Objects.equals(name, value)) return;
        name = value;
        GraphModel gm = getGraphModel();
        if (gm != null) gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
    }

    @Override
    public void setColor(int color) {
        if (elementColor == color) return;
        elementColor = color;
        userColor = true;
        GraphModel gm = getGraphModel();
        if (gm != null) gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.STYLE);
    }

    @Override
    public int getDefaultColor() {
        return 0xFF2E2E2E;
    }

    @Override
    public boolean hasUserColor() {
        return userColor;
    }

    @Override
    public void resetColor() {
        if (!userColor) return;
        userColor = false;
        elementColor = getDefaultColor();
        GraphModel gm = getGraphModel();
        if (gm != null) gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.STYLE);
    }

    public void setZOrder(int value) {
        if (zOrder == value) return;
        zOrder = value;
        GraphModel gm = getGraphModel();
        if (gm != null) gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.STYLE);
    }

    /**
     * Returns all nodes that are fully contained within this placemat's bounds.
     * Uses the provided size lookup to determine each node's rendered size.
     * @param nodeSizeLookup provides (width, height) for a given node, or null if unknown
     */
    public List<AbstractNodeModel> getContainedNodes(@Nullable java.util.function.Function<AbstractNodeModel, Vector2f> nodeSizeLookup) {
        var result = new ArrayList<AbstractNodeModel>();
        GraphModel gm = getGraphModel();
        if (gm == null) return result;
        float px = position.x;
        float py = position.y;
        float px2 = position.x + size.x;
        float py2 = position.y + size.y;
        for (var node : gm.getNodeModels()) {
            if (node == null) continue;
            var np = node.getPosition();
            Vector2f ns = nodeSizeLookup != null ? nodeSizeLookup.apply(node) : null;
            if (ns != null) {
                // fully inside: all four corners within placemat bounds
                if (np.x >= px && np.y >= py && np.x + ns.x <= px2 && np.y + ns.y <= py2) {
                    result.add(node);
                }
            } else {
                // fallback: position-only check
                if (np.x >= px && np.x <= px2 && np.y >= py && np.y <= py2) {
                    result.add(node);
                }
            }
        }
        return result;
    }

    @Override
    public @Nullable GraphElement<?> createElementUI() {
        return new PlacematElement(this);
    }
}
