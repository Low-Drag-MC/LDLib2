package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;

import java.util.Map;

public class Style implements IConfigurable, IPersistedSerializable {
    public final UIElement holder;

    public Style(UIElement holder) {
        this.holder = holder;
        this.holder._addStyleInternal(this);
    }

    public void applyStyles(Map<String, StyleValue<?>> values) {
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IConfigurable.super.buildConfigurator(father);
        father.addEventListener(Configurator.CHANGE_EVENT, event -> holder.onStyleChanged());
    }
}
