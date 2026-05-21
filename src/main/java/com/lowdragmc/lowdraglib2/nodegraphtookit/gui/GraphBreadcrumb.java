package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * A simple horizontal breadcrumb: {@code root > subA > subB}. Each segment is a clickable
 * {@link Button} whose action pops the editor's subgraph stack to that level. The deepest segment
 * (current level) is rendered as a plain label.
 */
public class GraphBreadcrumb extends UIElement {

    private final List<Component> labels = new ArrayList<>();
    private IntConsumer onJump = level -> {};

    public GraphBreadcrumb() {
        addClass("__graph-breadcrumb__");
        Style.defaultPipeline(getLayout(), l -> l.flexDirection(FlexDirection.ROW).heightPercent(100));
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
                var segment = new Button();
                segment.setText(label.getString()).setOnClick(e -> onJump.accept(level));
                segment.addClass("__graph-breadcrumb_segment__");
                Style.defaultPipeline(segment.getLayout(), l -> l.heightPercent(100));
                addChild(segment);

                var separator = new UIElement();
                separator.addClass("__graph-breadcrumb_separator__");
                Style.defaultPipeline(separator.getLayout(), l -> l.width(9).heightPercent(100).justifyContent(AlignContent.CENTER));
                addChild(separator.addChild(new UIElement().addClass("__graph-breadcrumb_separator-icon__")
                        .layout(l -> l.width(9).aspectRatio(1))
                        .style(style -> Style.defaultPipeline(style, s -> s.backgroundTexture(Icons.RIGHT_ARROW_NO_BAR_S_LIGHT)))));
            } else {
                // deepest: not clickable
                var current = new Button();
                current.setText(label.getString()).setActive(false);
                current.addClass("__graph-breadcrumb_current__");
                Style.defaultPipeline(current.getLayout(), l -> l.heightPercent(100));
                addChild(current);
            }
        }
    }
}
