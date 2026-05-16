package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * A simple horizontal breadcrumb: {@code root &gt; subA &gt; subB}. Each segment is a clickable
 * {@link Button} whose action pops the editor's subgraph stack to that level. The deepest segment
 * (current level) is rendered as a plain label.
 */
public class GraphBreadcrumb extends UIElement {

    private final List<Component> labels = new ArrayList<>();
    private IntConsumer onJump = level -> {};

    public GraphBreadcrumb() {
        layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.heightPercent(100);
        });
    }

    /** Wires the click handler. The argument passed is the depth (0 = root). */
    public GraphBreadcrumb setOnJump(IntConsumer onJump) {
        this.onJump = onJump == null ? level -> {} : onJump;
        return this;
    }

    public void setPath(List<Component> path) {
        this.labels.clear();
        this.labels.addAll(path);
        rebuild();
    }

    private void rebuild() {
        clearAllChildren();
        for (int i = 0; i < labels.size(); i++) {
            final int level = i;
            var label = labels.get(i);
            if (i < labels.size() - 1) {
                addChild(new Button().setText(label.getString())
                        .setOnClick(e -> onJump.accept(level))
                        .layout(layout -> layout.heightPercent(100)));
                addChild(new Button().setText(">").setActive(false)
                        .layout(layout -> layout.width(8).heightPercent(100)));
            } else {
                // deepest: not clickable
                addChild(new Button().setText(label.getString()).setActive(false)
                        .layout(layout -> layout.heightPercent(100)));
            }
        }
    }
}
