package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.util;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.accessors.EnumAccessor;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.VariableDeclarationCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.ModifierFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds the variable-declaration configurator group (name, default value, internal/external
 * subgraph use) for a {@link VariableDeclarationModelBase}. Shared by the Blackboard variable
 * property panel and the on-canvas variable node inspector so both edit variables identically.
 */
public final class VariableDeclarationConfigurableHelper {
    private VariableDeclarationConfigurableHelper() {}

    public static IConfigurable build(VariableDeclarationModelBase model, @Nullable GraphView view) {
        return IConfigurable.create(group -> {
            var rename = new StringConfigurator("graph.variable_name", model::getName,
                    model::setName, model.getName(), true);
            var defaultValue = new ConfiguratorGroup("graph.default_value").setCollapse(false);
            model.buildConfigurator(defaultValue);
            var subGraphConfigurator = new ConfiguratorSelectorConfigurator<>(
                    "graph.variable_type",
                    () -> model.getModifiers() == ModifierFlags.NONE ? VariableType.INTERNAL : VariableType.EXTERNAL,
                    type -> {
                        if (view == null) return;
                        view.dispatchCommand(new VariableDeclarationCommands.ChangeVariableModifiersCommand(
                                List.of(model),
                                type == VariableType.INTERNAL ? ModifierFlags.NONE : getDefaultSubgraphPortModifier(model)
                        ));
                    },
                    VariableType.INTERNAL,
                    true,
                    getVariableTypeCandidates(model),
                    VariableType::getSerializedName,
                    (type, configuratorGroup) -> {
                        if (type == VariableType.EXTERNAL) {
                            var portCandidates = getSubGraphPortCandidates(model);
                            configuratorGroup.addConfigurator(EnumAccessor.create(
                                    "graph.flow_direction",
                                    portCandidates,
                                    () -> getSelectedSubGraphPort(model, portCandidates),
                                    io -> {
                                        if (view == null) return;
                                        view.dispatchCommand(new VariableDeclarationCommands.ChangeVariableModifiersCommand(
                                                List.of(model),
                                                toModifier(io)
                                        ));
                                    },
                                    portCandidates.isEmpty() ? SubGraphPort.INPUT : portCandidates.get(0),
                                    true,
                                    SubGraphPort::getIcon
                            ));
                        }
                    }
            );
            group.addConfigurators(rename, defaultValue, subGraphConfigurator);
        });
    }

    private static List<VariableType> getVariableTypeCandidates(VariableDeclarationModelBase model) {
        if (getSubGraphPortCandidates(model).isEmpty()) {
            return List.of(VariableType.INTERNAL);
        }
        return Arrays.stream(VariableType.values()).toList();
    }

    private static List<SubGraphPort> getSubGraphPortCandidates(VariableDeclarationModelBase model) {
        var graphModel = model.getGraphModel();
        if (graphModel == null) return List.of();
        var supportedKinds = graphModel.getSupportedSubgraphVariableKinds();
        var candidates = new ArrayList<SubGraphPort>();
        if (supportedKinds.contains(VariableKind.INPUT)) candidates.add(SubGraphPort.INPUT);
        if (supportedKinds.contains(VariableKind.OUTPUT)) candidates.add(SubGraphPort.OUTPUT);
        return candidates;
    }

    private static ModifierFlags getDefaultSubgraphPortModifier(VariableDeclarationModelBase model) {
        var candidates = getSubGraphPortCandidates(model);
        if (candidates.isEmpty()) return ModifierFlags.NONE;
        return toModifier(candidates.get(0));
    }

    private static SubGraphPort getSelectedSubGraphPort(VariableDeclarationModelBase model, List<SubGraphPort> candidates) {
        var modifiers = model.getModifiers();
        var selected = modifiers.hasFlag(ModifierFlags.WRITE) && !modifiers.hasFlag(ModifierFlags.READ)
                ? SubGraphPort.OUTPUT : SubGraphPort.INPUT;
        if (candidates.contains(selected)) return selected;
        return candidates.isEmpty() ? SubGraphPort.INPUT : candidates.get(0);
    }

    private static ModifierFlags toModifier(SubGraphPort io) {
        return io == SubGraphPort.INPUT ? ModifierFlags.READ : ModifierFlags.WRITE;
    }

    private enum VariableType implements StringRepresentable {
        INTERNAL,
        EXTERNAL;

        @Override
        public String getSerializedName() {
            return this == INTERNAL ? "graph.variable_type.internal" : "graph.variable_type.external";
        }
    }

    private enum SubGraphPort implements StringRepresentable {
        INPUT(new TextTexture("I")),
        OUTPUT(new TextTexture("O"));

        @Getter
        public final IGuiTexture icon;

        SubGraphPort(IGuiTexture icon) {
            this.icon = icon;
        }

        @Override
        public String getSerializedName() {
            return this == INPUT ? "input" : "output";
        }
    }
}
