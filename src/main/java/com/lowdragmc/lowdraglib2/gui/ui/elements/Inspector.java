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
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaGutter;
import org.jetbrains.annotations.Nullable;

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
                    var notifyName = configurator.getNotifyName();
                    var recordHistory = historyStack.recordSerializableObject(notifyName.getString().isEmpty() ?
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
            historyStack.recordSerializableObject(Component.translatable("editor.inspector.history", configurable.getConfigurableName()), serializable, configurable)
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
        group.lineContainer.setDisplay(false);
        group.configuratorContainer.layout(layout -> {
            layout.setMargin(YogaEdge.LEFT, 0);
            layout.setPadding(YogaEdge.ALL, 0);
        }).style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        configurable.buildConfigurator(group);
        scrollerView.addScrollViewChild(group);
        return group;
    }
}
