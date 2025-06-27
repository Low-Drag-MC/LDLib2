package com.lowdragmc.lowdraglib2.editor.ui.view;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor_outdated.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaGutter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class InspectorView extends View {
    public final ScrollerView scrollerView;
    public final Editor editor;
    // runtime
    @Getter
    @Nullable
    private IConfigurable inspectedConfigurable;
    @Nullable
    private Runnable onClose;

    public InspectorView(Editor editor) {
        super("editor.inspector", Icons.SETTINGS);
        this.editor = editor;
        this.scrollerView = new ScrollerView();
        scrollerView.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
        });
        scrollerView.viewPort.layout(layout -> {
            layout.setPadding(YogaEdge.ALL, 1);
        }).style(style -> style.backgroundTexture(IGuiTexture.EMPTY));;
        scrollerView.viewContainer.layout(layout -> {
            layout.setGap(YogaGutter.ALL, 1);
        });
        addChild(scrollerView);
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
     * @param <T>           the type of the configurable instance, which must extend {@link IConfigurable}
     * @param configurable  the configurable instance to inspect
     * @param listener      an optional {@link Consumer} that is triggered whenever a configurator's value changes,
     *                      providing the changed configurator as its argument
     * @param onClose       an optional {@link Runnable} that is executed when the inspection session is closed
     * @param historyAction an optional {@link Consumer} for handling undo/redo operations during history actions,
     *                      receiving configurable instances when executed
     * @return a {@link ConfiguratorGroup} representing the configurable instance's structure and properties
     */
    public <T extends IConfigurable> ConfiguratorGroup inspect(T configurable, @Nullable Consumer<Configurator> listener, @Nullable Runnable onClose, @Nullable Consumer<T> historyAction) {
        clear();
        this.inspectedConfigurable = configurable;
        this.onClose = onClose;
        var group = inspectInternal(configurable);
        group.addEventListener(Configurator.CHANGE_EVENT, e -> {
            if (e.target instanceof Configurator configurator) {
                if (listener != null) {
                    listener.accept(configurator);
                }
                if (configurable instanceof INBTSerializable<?> serializable) {
                    var top = editor.historyView.getCurrentHistory();
                    if (top != null && top.source() == configurator) return;
                    var notifyName = configurator.getNotifyName();
                    var recordHistory = editor.historyView.recordSerializableObject(notifyName.getString().isEmpty() ?
                                    Component.literal(configurable.getConfigurableName()) : notifyName,
                            serializable, configurator);
                    if (historyAction != null) {
                        recordHistory.setOnExecute(value -> historyAction.accept((T) value));
                        recordHistory.setOnUndo(value -> historyAction.accept((T) value));
                    }
                }
            }
        });

        if (configurable instanceof INBTSerializable<?> serializable) {
            editor.historyView.recordSerializableObject(Component.translatable("editor.inspector.history", configurable.getConfigurableName()), serializable)
                    .setOnExecute(value -> {
                        clear();
                        scrollerView.addScrollViewChild(group);
                        inspectedConfigurable = configurable;
                        this.onClose = onClose;
                    })
                    .setOnUndo(value -> clear());
        }
        return group;
    }

    private <T extends IConfigurable> ConfiguratorGroup inspectInternal(T configurable) {
        var group = new ConfiguratorGroup("").setCanCollapse(false).setCollapse(false);
        group.lineContainer.setDisplay(YogaDisplay.NONE);
        group.configuratorContainer.layout(layout -> {
            layout.setMargin(YogaEdge.LEFT, 0);
            layout.setPadding(YogaEdge.ALL, 0);
        }).style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        configurable.buildConfigurator(group);
        scrollerView.addScrollViewChild(group);
        return group;
    }
}
