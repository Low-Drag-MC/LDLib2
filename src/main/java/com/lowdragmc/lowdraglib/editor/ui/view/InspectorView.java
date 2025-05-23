package com.lowdragmc.lowdraglib.editor.ui.view;

import com.lowdragmc.lowdraglib.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.editor.ui.View;
import com.lowdragmc.lowdraglib.gui.ui.elements.ScrollerView;
import org.appliedenergistics.yoga.YogaGutter;

import java.util.ArrayList;

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

    public void inspect(IConfigurable configurable) {
        scrollerView.clearAllScrollViewChildren();
        var group = new ConfiguratorGroup("");
        configurable.buildConfigurator(group);
        var configurators = new ArrayList<>(group.getConfigurators());
        group.removeAllConfigurators();
        for (var configurator : configurators) {
            scrollerView.addScrollViewChild(configurator);
        }
    }
}
