package com.lowdragmc.lowdraglib.editor.ui.view;

import com.lowdragmc.lowdraglib.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.editor.ui.View;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.elements.ScrollerView;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaGutter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class InspectorView extends View {
    public final ScrollerView scrollerView;

    public InspectorView() {
        super("editor.inspector");
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

    public ConfiguratorGroup inspect(IConfigurable configurable) {
        return inspect(configurable, null);
    }

    /**
     * Inspect a configurable object and display its configurators.
     * @param configurable the configurable object to inspect
     * @param listener an optional listener that can be notified while making changes.
     */
    public ConfiguratorGroup inspect(IConfigurable configurable, @Nullable Consumer<Configurator> listener) {
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
