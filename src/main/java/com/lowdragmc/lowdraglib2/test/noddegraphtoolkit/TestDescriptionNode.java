package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Test node for the item library:
 * <li>three distinctly named {@code Float} inputs, so dragging a {@code Float} wire onto empty canvas
 * offers three connectable ports to expand and pick from.</li>
 * <li>a {@link #createDescriptionUI()} long enough to scroll, to exercise the description side panel.</li>
 */
@NodeAttribute(name = "test_description", group = "test", graphTypes = {TestGraph.class})
public class TestDescriptionNode extends Node {

    @Override
    public Component getDisplayName() {
        return Component.literal("Description Test");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        super.onDefinePorts(context);
        context.addInputPort("alpha", Float.class);
        context.addInputPort("beta", Float.class);
        context.addInputPort("gamma", Float.class);
        context.addOutputPort("out", Float.class);
    }

    @Override
    @Nullable
    public UIElement createDescriptionUI() {
        var container = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.gapAll(3);
        });
        container.addChildren(
                UIElementProvider.iconText(Node::getNodeIcon, Node::getDisplayName)
                        .apply(this).layout(layout -> layout.widthPercent(100)),
                descriptionText("This node exists to test the item library. It has three float inputs, " +
                        "so dropping a float wire on an empty canvas lets you expand this entry and " +
                        "choose which one of alpha / beta / gamma the new wire connects to."),
                descriptionText("Right click the entry (or click its arrow) to expand the ports, then " +
                        "confirm one with a double click or Enter. Confirming this row instead keeps " +
                        "the old behaviour and wires to the first compatible port."),
                descriptionText("This paragraph only exists to make the description tall enough that " +
                        "the panel has to scroll, which is the last thing worth checking here.")
        );
        return container;
    }

    private static Label descriptionText(String text) {
        return (Label) new Label()
                .textStyle(style -> style.textWrap(TextWrap.WRAP).adaptiveHeight(true).fontSize(6))
                .setText(Component.literal(text))
                .layout(layout -> layout.widthPercent(100));
    }
}
