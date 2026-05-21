package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.util;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.IResizeWidth;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.ElementRenameColorCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IHasElementColor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IHasName;
import dev.vfyjxf.taffy.style.FlexDirection;

/**
 * Builds the rename + color configurator group for a graph element model. Used by the various
 * GraphElement subclasses to populate the {@code GraphInspector} when a single element is
 * selected. Both fields are gated on the relevant capability + interface and dispatch through
 * undoable commands.
 */
public final class RenameColorConfigurableHelper {
    private RenameColorConfigurableHelper() {}

    public static IConfigurable build(GraphElementModel model, GraphView view) {
        return IConfigurable.create(group -> {
            if (model.isRenamable() && model instanceof IHasName named) {
                group.addConfigurator(new StringConfigurator(
                        "graph.name",
                        named::getName,
                        newName -> {
                            if (view != null) {
                                view.dispatchCommand(new ElementRenameColorCommands.RenameElementCommand(model, newName));
                            } else {
                                named.setName(newName);
                            }
                        },
                        named.getName(),
                        true));
            }
            if (model.isColorable() && model instanceof IHasElementColor colored) {
                var colorConfigurator = new ColorConfigurator(
                        "graph.color",
                        colored::getElementColor,
                        newColor -> {
                            if (view != null) {
                                view.dispatchCommand(new ElementRenameColorCommands.SetElementColorCommand(model, newColor));
                            } else {
                                colored.setColor(newColor);
                            }
                        },
                        colored.getDefaultColor(),
                        true);
                // Append a "reset to default" button to the color row. The configurator framework
                // doesn't ship a built-in reset affordance, so we tack on a small icon button next
                // to the color swatch. Active only while the model carries a user-set color —
                // otherwise the button would be a no-op.
                var resetBtn = new Button().noText()
                        .setOnClick(e -> {
                            if (!colored.hasUserColor()) return;
                            if (view != null) {
                                view.dispatchCommand(new ElementRenameColorCommands.ResetElementColorCommand(model));
                            } else {
                                colored.resetColor();
                            }
                        })
                        .layout(layout -> layout.width(14).height(14))
                        .style(style -> style.tooltips("graph.color.reset"))
                        .addChild(new UIElement()
                                .layout(layout -> {
                                    layout.heightPercent(100);
                                    layout.setAspectRatio(1);
                                })
                                .addClass("__white_icon__")
                                .style(style -> style.backgroundTexture(Icons.REPLAY)));
                resetBtn.setActive(colored.hasUserColor());
                colorConfigurator.inlineContainer.getLayout().flexDirection(FlexDirection.ROW);
                colorConfigurator.colorPreview.getLayout().flex(1);
                colorConfigurator.inlineContainer.addChild(resetBtn);
                group.addConfigurator(colorConfigurator);
            }
            if (model.isResizable() && model instanceof IResizeWidth resizable) {
                group.addConfigurator(new NumberConfigurator(
                        "graph.min_width",
                        resizable::getMinWidth,
                        v -> resizable.setMinWidth(v.floatValue()),
                        0f,
                        true)
                        .setRange(0, 2000)
                        .setWheel(1)
                        .setType(ConfigNumber.Type.FLOAT));
            }
        });
    }
}
