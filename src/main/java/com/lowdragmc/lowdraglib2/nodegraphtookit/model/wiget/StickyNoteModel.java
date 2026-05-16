package com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wiget.StickyNoteElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.List;
import java.util.Objects;

public class StickyNoteModel extends GraphElementModel implements IMovable, IHasElementColor, IGraphElementUIModel {
    @Persisted @Getter
    private Vector2f position = new Vector2f(0);
    @Persisted @Getter
    private Vector2f size = new Vector2f(150, 100);
    @Persisted @Getter
    private String content = "";
    @Persisted @Getter
    private int elementColor = 0xFFFFEB3B;
    @Persisted @Getter
    private boolean userColor = false;
    @Persisted @Getter
    private int fontSize = 11;
    @Persisted @Getter
    private boolean collapsed = false;

    public StickyNoteModel() {
        capabilities.addAll(List.of(
                Capabilities.SELECTABLE,
                Capabilities.MOVABLE,
                Capabilities.RESIZABLE,
                Capabilities.DELETABLE,
                Capabilities.COLORABLE,
                Capabilities.COPIABLE
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

    public void setContent(String value) {
        if (Objects.equals(content, value)) return;
        content = value;
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
        return 0xFFFFEB3B;
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

    public void setFontSize(int value) {
        if (fontSize == value) return;
        fontSize = value;
        GraphModel gm = getGraphModel();
        if (gm != null) gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.STYLE);
    }

    public void setCollapsed(boolean value) {
        if (collapsed == value) return;
        collapsed = value;
        GraphModel gm = getGraphModel();
        if (gm != null) gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.LAYOUT);
    }

    @Override
    public @Nullable GraphElement<?> createElementUI() {
        return new StickyNoteElement(this);
    }
}
