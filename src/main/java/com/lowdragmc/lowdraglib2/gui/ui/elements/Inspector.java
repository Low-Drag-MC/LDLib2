package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.utils.IHistoryStack;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "inspector", group = "misc", registry = "ldlib2:ui_element")
public class Inspector extends UIElement {
    public final ScrollerView scrollerView;
    @Nullable @Setter @Getter
    private IHistoryStack historyStack;

    // runtime
    @Getter
    @Nullable
    private IConfigurable inspectedConfigurable;
    @Nullable
    private Runnable onClose;

    public Inspector() {
        this.scrollerView = new ScrollerView();
        this.scrollerView.setId("_inspector_scroller-view_");
        scrollerView.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });
        scrollerView.viewPort.layout(layout -> {
            layout.paddingAll(1);
        }).style(style -> style.backgroundTexture(IGuiTexture.EMPTY));;
        scrollerView.viewContainer.layout(layout -> {
            layout.gapAll(1);
        });
        addChild(scrollerView);
        internalSetup();
    }

    public void clear() {
        if (inspectedConfigurable != null) {
            if (this.onClose != null) {
                this.onClose.run();
            }
            scrollerView.clearAllScrollViewChildren();
        }
        inspectedConfigurable = null;
        onClose = null;
    }

    public ConfiguratorGroup inspect(IConfigurable configurable) {
        return inspect(configurable, null);
    }

    public ConfiguratorGroup inspect(IConfigurable configurable, @Nullable Consumer<Configurator> listener) {
        return inspect(configurable, listener, null);
    }

    public ConfiguratorGroup inspect(IConfigurable configurable, @Nullable Consumer<Configurator> listener, @Nullable Runnable onClose) {
        return inspect(configurable, listener, onClose, null);
    }

    /**
     * Inspects a configurable instance and generates a configurable group for editor interaction.
     * This method allows observing changes in the configurators, managing history actions,
     * and handling closure of the inspection.
     *
     * <p>History recording is delegated to {@link IConfigurable#createHistoryRecorder()}; if it
     * returns {@code null} no history entries are pushed for this configurable.
     *
     * <p>When switching from a previously inspected configurable, group expansion/collapse state
     * is preserved across matching paths (groups with identical name path and {@code canCollapse}).
     *
     * @param <T>           the type of the configurable instance, which must extend {@link IConfigurable}
     * @param configurable  the configurable instance to inspect
     * @param listener      an optional {@link Consumer} that is triggered whenever a configurator's value changes,
     *                      providing the changed configurator as its argument
     * @param onClose       an optional {@link Runnable} that is executed when the inspection session is closed
     * @param historyAction an optional callback invoked when an undo/redo restores this configurable
     * @return a {@link ConfiguratorGroup} representing the configurable instance's structure and properties
     */
    public <T extends IConfigurable> ConfiguratorGroup inspect(T configurable, @Nullable Consumer<Configurator> listener, @Nullable Runnable onClose, @Nullable Runnable historyAction) {
        Map<List<String>, Boolean> savedStates = inspectedConfigurable != null ? captureGroupStates() : Map.of();
        clear();
        this.inspectedConfigurable = configurable;
        this.onClose = onClose;
        var group = inspectInternal(configurable);
        if (!savedStates.isEmpty()) {
            restoreGroupStates(group, savedStates);
        }

        var recorder = configurable.createHistoryRecorder();
        group.addEventListener(Configurator.CHANGE_EVENT, e -> {
            if (e.target instanceof Configurator configurator) {
                if (listener != null) {
                    listener.accept(configurator);
                }
                if (historyStack != null && recorder != null) {
                    var notifyName = configurator.getNotifyName();
                    var handle = recorder.record(historyStack,
                            notifyName.getString().isEmpty() ? Component.literal(configurable.getConfigurableName()) : notifyName,
                            configurator);
                    if (historyAction != null) {
                        handle.setOnExecute(historyAction).setOnUndo(historyAction);
                    }
                }
            }
        });

        if (historyStack != null && recorder != null) {
            recorder.record(historyStack,
                            Component.translatable("editor.inspector.history", configurable.getConfigurableName()),
                            configurable)
                    .setOnExecute(() -> {
                        clear();
                        scrollerView.addScrollViewChild(group);
                        inspectedConfigurable = configurable;
                        this.onClose = onClose;
                    })
                    .setOnUndo(this::clear);
        }
        return group;
    }

    private <T extends IConfigurable> ConfiguratorGroup inspectInternal(T configurable) {
        var group = new ConfiguratorGroup("").setCanCollapse(false).setCollapse(false);
        group.lineContainer.setDisplay(false);
        group.configuratorContainer.layout(layout -> {
            layout.marginLeft(0);
            layout.paddingAll(0);
        }).style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        configurable.buildConfigurator(group);
        scrollerView.addScrollViewChild(group);
        return group;
    }

    private Map<List<String>, Boolean> captureGroupStates() {
        Map<List<String>, Boolean> states = new HashMap<>();
        for (UIElement child : scrollerView.viewContainer.getChildren()) {
            if (child instanceof ConfiguratorGroup root) {
                captureRecursive(root, new ArrayDeque<>(), states);
            }
        }
        return states;
    }

    private void captureRecursive(ConfiguratorGroup group, Deque<String> path, Map<List<String>, Boolean> states) {
        boolean pushed = !path.isEmpty() || group.isCanCollapse();
        if (group.isCanCollapse() && !path.isEmpty()) {
            states.put(new ArrayList<>(path), group.isCollapse());
        }
        for (Configurator c : group.getConfigurators()) {
            if (c instanceof ConfiguratorGroup sub) {
                path.addLast(sub.getLabel().getString());
                captureRecursive(sub, path, states);
                path.removeLast();
            }
        }
    }

    private void restoreGroupStates(ConfiguratorGroup root, Map<List<String>, Boolean> states) {
        restoreRecursive(root, new ArrayDeque<>(), states);
    }

    private void restoreRecursive(ConfiguratorGroup group, Deque<String> path, Map<List<String>, Boolean> states) {
        if (group.isCanCollapse() && !path.isEmpty()) {
            Boolean saved = states.get(new ArrayList<>(path));
            if (saved != null) {
                group.setCollapse(saved);
            }
        }
        for (Configurator c : group.getConfigurators()) {
            if (c instanceof ConfiguratorGroup sub) {
                path.addLast(sub.getLabel().getString());
                restoreRecursive(sub, path, states);
                path.removeLast();
            }
        }
    }
}
