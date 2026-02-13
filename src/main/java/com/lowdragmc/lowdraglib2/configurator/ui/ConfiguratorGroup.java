package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.UISoundUtils;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class ConfiguratorGroup extends Configurator {
    public final UIElement folderIcon;
    public final UIElement configuratorContainer;
    @Getter @Setter
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
        getLayout().gapAll(0);
        addClass("__configurator-group__");

        configuratorContainer = new UIElement().layout(layout -> {
            layout.marginLeft(2);
            layout.gapAll(1);
            layout.paddingAll(5);
        }).style(style -> style.backgroundTexture(Sprites.BORDER)).addClass("__configurator-group_container__").moveInlineAsDefault();

        lineContainer.style(style -> style.backgroundTexture(Sprites.RECT_RD_SOLID))
                .layout(layout -> layout.paddingAll(2))
                .addEventListener(UIEvents.MOUSE_DOWN, this::onLineContainerClick)
                .addChildAt(folderIcon = new UIElement().layout(layout -> {
                    layout.marginAll(3f);
                    layout.width(8);
                    layout.height(8);
                }).style(style -> style.backgroundTexture(Icons.RIGHT_ARROW_NO_BAR_S_LIGHT))
                        .addClass("__configurator-group_folder-icon__")
                        .moveInlineAsDefault(), 0)
                .moveInlineAsDefault();

        addChild(configuratorContainer);

        setCollapse(isCollapse);
        moveInlineAsDefault();
    }

    public ConfiguratorGroup hideTitle() {
        this.lineContainer.setDisplay(false);
        return this;
    }

    protected void onLineContainerClick(UIEvent event) {
        if (event.button == 0 && canCollapse) {
            setCollapse(!isCollapse);
            UISoundUtils.playButtonClickSound();
        }
    }

    public ConfiguratorGroup setCollapse(boolean collapse) {
        isCollapse = collapse;
        configuratorContainer.setDisplay(!collapse);
        folderIcon.style(style -> style.backgroundTexture(collapse ? Icons.RIGHT_ARROW_NO_BAR_S_LIGHT : Icons.DOWN_ARROW_NO_BAR_S_LIGHT));
        return this;
    }

    public ConfiguratorGroup configuratorContainer(Consumer<UIElement> consumer) {
        consumer.accept(configuratorContainer);
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
