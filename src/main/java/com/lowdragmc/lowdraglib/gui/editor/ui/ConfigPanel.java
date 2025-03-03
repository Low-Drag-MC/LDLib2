package com.lowdragmc.lowdraglib.gui.editor.ui;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.configurator.Configurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote ConfigPanel
 */
public class ConfigPanel extends WidgetGroup {
    public static final int WIDTH = 252;
    public static class Tab {
        public static final Tab WIDGET = createTab(Icons.WIDGET_SETTING, Component.translatable("ldlib.gui.editor.config_panel.tabs.widget"));
        public static final Tab RESOURCE = createTab(Icons.RESOURCE_SETTING, Component.translatable("ldlib.gui.editor.config_panel.tabs.resource"));

        public final ResourceTexture icon;
        public final Component tooltip;
        public final Consumer<ConfiguratorGroup> configurable;

        private Tab(ResourceTexture icon, Component tooltip, Consumer<ConfiguratorGroup> configurable) {
            this.icon = icon;
            this.tooltip = tooltip;
            this.configurable= configurable;
        }

        public static Tab createTab(ResourceTexture icon, Component tooltip) {
            return createTab(icon, tooltip, father -> {});
        }

        public static Tab createTab(ResourceTexture icon, Component tooltip, Consumer<ConfiguratorGroup> configurable) {
            return new Tab(icon, tooltip, configurable);
        }
    }

    @Getter
    protected final Editor editor;
    @Getter
    protected final Map<Tab, IConfigurable> focus = new HashMap<>();
    protected final Map<Tab, DraggableScrollableWidgetGroup> configuratorGroup = new HashMap<>();
    protected final Map<Tab, List<Configurator>> configurators = new HashMap<>();

    protected ImageWidget tabBackground;
    protected TabContainer tabContainer;
    @Getter
    protected HsbColorWidget palette;

    public ConfigPanel(Editor editor, List<Tab> tabs) {
        super(editor.getSize().getWidth() - WIDTH, 0, WIDTH, editor.getSize().height);
        setClientSideWidget();
        this.editor = editor;
        setBackground(ColorPattern.BLACK.rectTexture());
        addWidget(new ImageWidget(0, 10, WIDTH, 10, new TextTexture("ldlib.gui.editor.configurator").setWidth(202)));
        addWidget(tabBackground = new ImageWidget(-20, 30, 20, 2 * 20, ColorPattern.BLACK.rectTexture().setLeftRadius(8)));
        addWidget(tabContainer = new TabContainer(0, 0, WIDTH, editor.getSize().height));
        reloadTabs(tabs);
    }

    public ConfigPanel(Editor editor) {
        this(editor, List.of(Tab.WIDGET, Tab.RESOURCE));
    }

    public void reloadTabs(List<Tab> tabs) {
        tabBackground.setSize(new Size(20, tabs.size() * 20));
        tabContainer.clearAllWidgets();
        configurators.clear();
        configuratorGroup.clear();
        int y = 34;
        for (Tab tab : tabs) {
            tabContainer.addTab((TabButton) new TabButton(-16, y, 12, 12).setTexture(
                            tab.icon,
                            tab.icon.copy().setColor(ColorPattern.T_GREEN.color)
                    ).setHoverTooltips(tab.tooltip),
                    configuratorGroup.computeIfAbsent(tab, key -> new DraggableScrollableWidgetGroup(0, 25, WIDTH, editor.getSize().height - 25)
                            .setYScrollBarWidth(2).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(1)))
            );
            configurators.put(tab, new ArrayList<>());
            y += 20;
        }
    }

    public void clearAllConfigurators() {
        for (var tab : this.configurators.keySet()) {
            clearAllConfigurators(tab);
        }
    }

    public void clearAllConfigurators(Tab tab) {
        this.focus.remove(tab);
        configuratorGroup.get(tab).clearAllWidgets();
        configurators.get(tab).clear();
    }

    public void openConfigurator(Tab tab, IConfigurable configurable) {
        switchTag(tab);
        if (Objects.equals(configurable, this.focus.get(tab))) return;
        clearAllConfigurators(tab);
        this.focus.put(tab, configurable);
        ConfiguratorGroup group = new ConfiguratorGroup("", false);
        tab.configurable.accept(group);
        configurable.buildConfigurator(group);
        for (Configurator configurator : group.getConfigurators()) {
            configurator.setConfigPanel(this, tab);
//            configurator.setConfiguratorContainer(() -> ConfigPanel.this.computeLayout(tab));
            configurator.init(WIDTH - 2);
            configurator.addListener(c -> onConfiguratorChangeUpdate(tab, c));
            this.configurators.get(tab).add(configurator);
            configuratorGroup.get(tab).addWidget(configurator);
        }
        computeLayout(tab);
        configuratorGroup.get(tab).setScrollYOffset(0);
    }

    public void onConfiguratorChangeUpdate(Tab tab, Configurator configurator) {
        // update history view
        editor.addAutoHistory(configurator.getName(), configurator);
    }

    public void switchTag(Tab tab) {
        tabContainer.switchTag(configuratorGroup.get(tab));
    }
    
    public void computeLayout(Tab tab) {
        int height = 0;
        for (Configurator configurator : configurators.get(tab)) {
            configurator.computeHeight();
            configurator.setSelfPosition(new Position(0, height - configuratorGroup.get(tab).getScrollYOffset()));
            height += configurator.getSize().height + 5;
        }
        configuratorGroup.get(tab).computeMax();
    }
}
