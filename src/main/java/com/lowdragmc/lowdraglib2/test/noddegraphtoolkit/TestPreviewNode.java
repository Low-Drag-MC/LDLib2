package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodePreviewContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * A custom node that opts into a preview panel via {@link #hasNodePreview()}, and demonstrates the
 * preview API: it reads its own {@code in} input value from the model, shows it live, renders a
 * colour swatch derived from the value, and reports whether a live graph view is present. Type a
 * value into the {@code in} port's field and the preview updates (driven by
 * {@link #onUpdateNodePreview}).
 */
@NodeAttribute(name = "test_preview", group = "test", graphTypes = {TestGraph.class})
public class TestPreviewNode extends Node {
    static final float PREVIEW_SIZE = 100f;

    // Live UI refs, rebuilt whenever the panel is (re)built.
    private Label valueLabel;
    private Label contextLabel;
    private UIElement swatch;

    @Override
    public Component getDisplayName() {
        return Component.literal("Preview");
    }

    @Override
    public boolean hasNodePreview() {
        return true;
    }

    @Override
    public float getNodeWidth() {
        return PREVIEW_SIZE;
    }

    @Override
    public void onDefinePorts(com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext context) {
        super.onDefinePorts(context);
        context.addInputPort("in", Float.class).withDefaultValue(0f).build();
        context.addOutputPort("out", Float.class).build();
    }

    @Override
    public void onBuildNodePreview(NodePreviewContext context) {
        var container = context.container();
        Style.defaultPipeline(container.getLayout(), l -> l
                .flexDirection(FlexDirection.COLUMN)
                .alignItems(AlignItems.STRETCH)
                .gapAll(2)
                .minWidth(90));

        // The preview needs an explicit block size; percentage width + aspect ratio does not
        // provide intrinsic height for the preview element's parent layout.
        swatch = new UIElement();
        swatch.getLayoutStyle().width(PREVIEW_SIZE).height(PREVIEW_SIZE);
        container.addChild(swatch);

        // Static title — demonstrates building arbitrary UI into the container.
        container.addChild(new Label().setValue(Component.literal("Preview of " + getDisplayName().getString())));

        // Live value read from the node model (context.nodeModel()).
        valueLabel = new Label();
        container.addChild(valueLabel);

        // Shows the rest of the context wiring: whether we're in a live editor.
        contextLabel = new Label();
        container.addChild(contextLabel);

        refresh(context);
    }

    @Override
    public void onUpdateNodePreview(NodePreviewContext context) {
        refresh(context);
    }

    private void refresh(NodePreviewContext context) {
        float value = readInputFloat(context);

        if (valueLabel != null) {
            valueLabel.setValue(Component.literal(String.format("in = %.2f", value)));
        }
        if (swatch != null) {
            int g = Mth.clamp((int) value, 0, 255);
            int color = 0xFF000000 | (g << 16) | (g << 8) | g;
            swatch.style(s -> s.background(new ColorRectTexture(color)));
        }
        if (contextLabel != null) {
            contextLabel.setValue(Component.literal("editor: " + (context.graphView() != null ? "live" : "headless")));
        }
    }

    /** Reads the current {@code in} input constant value from the node model. */
    private float readInputFloat(NodePreviewContext context) {
        if (context.nodeModel() instanceof NodeModel nm) {
            var constant = nm.getInputConstantsById().get("in");
            if (constant != null && constant.getValue() instanceof Float f) {
                return f;
            }
        }
        return 0f;
    }
}
