package com.lowdragmc.lowdraglib2.editor.ui.view;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor_outdated.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import lombok.Getter;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaGutter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class InspectorView extends View {
    public final ScrollerView scrollerView;
    // runtime
    @Getter
    @Nullable
    private IConfigurable inspectedConfigurable;
    @Nullable
    private Runnable onClose;

    public InspectorView() {
        super("editor.inspector", Icons.SETTINGS);
        this.scrollerView = new ScrollerView();
        this.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
        });
        scrollerView.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
        });
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
        return inspect(configurable, null, null);
    }

    /**
     * Inspect a configurable object and display its configurators.
     * @param configurable the configurable object to inspect
     * @param listener an optional listener that can be notified while making changes.
     */
    public ConfiguratorGroup inspect(IConfigurable configurable, @Nullable Consumer<Configurator> listener, @Nullable Runnable onClose) {
        clear();
        inspectedConfigurable = configurable;
        this.onClose = onClose;
        scrollerView.clearAllScrollViewChildren();
        var group = new ConfiguratorGroup("").setCanCollapse(false).setCollapse(false);
        if (listener != null) {
            group.addEventListener(Configurator.CHANGE_EVENT, e -> {
                if (e.target instanceof Configurator configurator) {
                    listener.accept(configurator);
                }
            });
        }
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
