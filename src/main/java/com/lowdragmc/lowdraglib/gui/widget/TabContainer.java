package com.lowdragmc.lowdraglib.gui.widget;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.editor.configurator.WrapperConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.runtime.PersistedParser;
import com.lowdragmc.lowdraglib.gui.texture.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

@LDLRegister(name = "tab_group", group = "widget.group")
public class TabContainer extends WidgetGroup {
    public static final ResourceTexture TABS_LEFT = new ResourceTexture("ldlib:textures/gui/tabs_left.png");

    public final BiMap<TabButton, WidgetGroup> tabs = HashBiMap.create();
    public final WidgetGroup buttonGroup;
    public final WidgetGroup containerGroup;
    public WidgetGroup focus;
    public BiConsumer<WidgetGroup, WidgetGroup> onChanged;

    public TabContainer() {
        this(0, 0,40, 60);
    }

    @Override
    public void initTemplate() {
        setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);
        addTab(new TabButton(-32 + 4, 0, 32, 28).setTexture(
                        new GuiTextureGroup(TABS_LEFT.getSubTexture(0, 0, 0.5f, 1f / 3), new TextTexture("A")),
                        new GuiTextureGroup(TABS_LEFT.getSubTexture(0.5f, 0, 0.5f, 1f / 3), new TextTexture("A"))
                ),
                new WidgetGroup(0, 0, 0, 0));
        addTab(new TabButton(-32 + 4, 28, 32, 28).setTexture(
                        new GuiTextureGroup(TABS_LEFT.getSubTexture(0, 1f / 3, 0.5f, 1f / 3), new TextTexture("B")),
                        new GuiTextureGroup(TABS_LEFT.getSubTexture(0.5f, 1f / 3, 0.5f, 1f / 3), new TextTexture("B"))
                ),
                new WidgetGroup(0, 0, 0, 0));
    }

    public TabContainer(int x, int y, int width, int height) {
        super(x, y, width, height);
        buttonGroup = new WidgetGroup(0, 0, 0, 0);
        containerGroup = new WidgetGroup(0, 0, 0, 0);
        this.addWidget(containerGroup);
        this.addWidget(buttonGroup);
    }

    public TabContainer setOnChanged(BiConsumer<WidgetGroup, WidgetGroup> onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public void switchTag(WidgetGroup tabWidget) {
        if (focus == tabWidget) return;
        if (focus != null) {
            tabs.inverse().get(focus).setPressed(false);
            focus.setVisible(false);
            focus.setActive(false);
        }
        if (onChanged != null) {
            onChanged.accept(focus, tabWidget);
        }
        focus= tabWidget;
        Optional.ofNullable(tabs.inverse().get(tabWidget)).ifPresent(tab -> {
            tab.setPressed(true);
            tabWidget.setActive(true);
            tabWidget.setVisible(true);
        });
    }

    public void addTab(TabButton tabButton, WidgetGroup tabWidget) {
        tabButton.setContainer(this);
        tabs.put(tabButton, tabWidget);
        containerGroup.addWidget(tabWidget);
        buttonGroup.addWidget(tabButton);
        if (focus == null) {
            focus = tabWidget;
        }
        tabButton.setPressed(focus == tabWidget);
        tabWidget.setVisible(focus == tabWidget);
        tabWidget.setActive(focus == tabWidget);
    }

    public void removeTab(TabButton tabButton) {
        if (tabs.containsKey(tabButton)) {
            buttonGroup.removeWidget(tabButton);
            containerGroup.removeWidget(tabs.remove(tabButton));
        }
    }

    @Override
    public void clearAllWidgets() {
        tabs.clear();
        buttonGroup.clearAllWidgets();
        containerGroup.clearAllWidgets();
        focus = null;
    }

    @Override
    public @Nullable Widget getHoverElement(double mouseX, double mouseY) {
        var hovered =  super.getHoverElement(mouseX, mouseY);
        if (hovered instanceof WidgetGroup group && tabs.containsValue(group)) return this;
        return hovered;
    }

    @Override
    public void acceptWidget(IConfigurableWidget widget) {
        if (focus != null) {
            focus.addWidget(widget.widget());
        }
    }

    @Override
    protected void addWidgetsConfigurator(ConfiguratorGroup father) {
        var tabsGroup = new ArrayConfiguratorGroup<>("tabs", false, () -> new ArrayList<>(tabs.keySet()),
                (getter, setter) -> {
                    var tab = getter.get();
                    return new WrapperConfigurator(tab.id, new ImageWidget(0, 0, 50, 50, new WidgetTexture(tab)));
                }, true);
        tabsGroup.setAddDefault(() -> new TabButton(0, 0, 32, 28).setTexture(
                new GuiTextureGroup(TABS_LEFT.getSubTexture(0, 1f / 3, 0.5f, 1f / 3), new TextTexture("N")),
                new GuiTextureGroup(TABS_LEFT.getSubTexture(0.5f, 1f / 3, 0.5f, 1f / 3), new TextTexture("N"))
        ));
        tabsGroup.setOnAdd(tab -> addTab(tab, new WidgetGroup(0, 0, getSize().width, getSize().height)));
        tabsGroup.setOnRemove(tab -> {
            buttonGroup.removeWidget(tab);
            containerGroup.removeWidget(tabs.get(tab));
            if (focus == tabs.remove(tab)) {
                focus = null;
            }
        });

        var childrenGroup = new ArrayConfiguratorGroup<>("children", true, () -> focus == null ? new ArrayList<>() : focus.widgets,
                (getter, setter) -> {
                    var child = getter.get();
                    return new WrapperConfigurator(child.id, new ImageWidget(0, 0, 50, 50, new WidgetTexture(child)));
                }, true);
        childrenGroup.setCanAdd(false);
        childrenGroup.setOnRemove(child -> {
            if (focus != null) {
                focus.removeWidget(child);
            }
        });
        childrenGroup.setOnReorder((index, widget) -> {
            if (focus != null) {
                focus.removeWidget(widget);
                focus.addWidget(index, widget);
            }
        });
        father.addConfigurators(tabsGroup, childrenGroup);
    }

    @Override
    public CompoundTag serializeInnerNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        PersistedParser.serializeNBT(tag, getClass(), this, provider);
        var tabs = new ListTag();
        for (Map.Entry<TabButton, WidgetGroup> entry : this.tabs.entrySet()) {
            var button = entry.getKey();
            var group = entry.getValue();
            var tab = new CompoundTag();
            tab.put("button", button.serializeInnerNBT(provider));
            tab.put("group", group.serializeWrapper(provider));
            tabs.add(tab);
        }
        tag.put("tabs", tabs);
        return tag;
    }

    @Override
    public void deserializeInnerNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        clearAllWidgets();
        PersistedParser.deserializeNBT(nbt, new HashMap<>(), getClass(), this, provider);
        var tabs = nbt.getList("tabs", Tag.TAG_COMPOUND);
        for (Tag tag : tabs) {
            if (tag instanceof CompoundTag tab) {
                TabButton button = new TabButton();
                button.deserializeInnerNBT(provider, tab.getCompound("button"));
                var widget = IConfigurableWidget.deserializeWrapper(tab.getCompound("group"), provider);
                if (widget != null && widget.widget() instanceof WidgetGroup group) {
                    addTab(button, group);
                }
            }
        }
    }
}
