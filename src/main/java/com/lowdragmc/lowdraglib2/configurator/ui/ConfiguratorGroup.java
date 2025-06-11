package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.editor_outdated.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.widget.Widget;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaGutter;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class ConfiguratorGroup extends Configurator {
    public final UIElement folderIcon;
    public final UIElement configuratorContainer;
    @Setter
    protected boolean canCollapse = true;
    @Getter
    protected boolean isCollapse;
    @Getter
    protected List<Configurator> configurators = new ArrayList<>();

    public ConfiguratorGroup() {
        this("");
    }

    public ConfiguratorGroup(String name) {
        this(name, true);
    }

    public ConfiguratorGroup(String name, boolean isCollapse) {
        super(name);
        getLayout().setGap(YogaGutter.ALL, 0);

        configuratorContainer = new UIElement().layout(layout -> {
            layout.setMargin(YogaEdge.LEFT, 2);
            layout.setGap(YogaGutter.ALL, 1);
            layout.setPadding(YogaEdge.ALL, 5);
        }).style(style -> style.backgroundTexture(Sprites.BORDER));

        lineContainer.style(style -> style.backgroundTexture(Sprites.RECT_RD_SOLID))
                .layout(layout -> layout.setPadding(YogaEdge.ALL, 2))
                .addEventListener(UIEvents.MOUSE_DOWN, this::onLineContainerClick)
                .addChildAt(folderIcon = new UIElement().layout(layout -> {
                    layout.setMargin(YogaEdge.ALL, 3f);
                    layout.setWidth(8);
                    layout.setHeight(8);
                }).style(style -> style.backgroundTexture(Icons.RIGHT_ARROW_NO_BAR_S_LIGHT)), 0);

        addChild(configuratorContainer);

        setCollapse(isCollapse);
    }

    protected void onLineContainerClick(UIEvent event) {
        if (event.button == 0 && canCollapse) {
            setCollapse(!isCollapse);
            Widget.playButtonClickSound();
        }
    }

    public ConfiguratorGroup setCollapse(boolean collapse) {
        isCollapse = collapse;
        configuratorContainer.setDisplay(collapse ? YogaDisplay.NONE : YogaDisplay.FLEX);
        folderIcon.style(style -> style.backgroundTexture(collapse ? Icons.RIGHT_ARROW_NO_BAR_S_LIGHT : Icons.DOWN_ARROW_NO_BAR_S_LIGHT));
        return this;
    }

    public ConfiguratorGroup addConfiguratorAt(Configurator configurator, int index) {
        this.configurators.add(index, configurator);
        configuratorContainer.addChildAt(configurator, index);
        return this;
    }

    public ConfiguratorGroup addConfigurator(Configurator configurator) {
        this.configurators.add(configurator);
        configuratorContainer.addChild(configurator);
        return this;
    }

    public ConfiguratorGroup addConfigurators(Configurator... configurators) {
        for (var configurator : configurators) {
            addConfigurator(configurator);
        }
        return this;
    }

    public void removeConfigurator(Configurator configurator) {
        if (configurators.remove(configurator)) {
            configuratorContainer.removeChild(configurator);
        }
    }

    public void removeAllConfigurators() {
        for (Configurator configurator : configurators) {
            configuratorContainer.removeChild(configurator);
        }
        configurators.clear();
    }

}
