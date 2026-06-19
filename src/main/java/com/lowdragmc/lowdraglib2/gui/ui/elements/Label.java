package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBindable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataConsumer;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataProvider;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import net.minecraft.MethodsReturnNonnullByDefault;
import com.lowdragmc.lowdraglib2.gui.util.ITickable;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
@LDLRegister(name = "label", group = "basic", registry = "ldlib2:ui_element")
public class Label extends TextElement implements IBindable<Component>, IDataConsumer<Component> {
    protected final Map<IDataProvider<Component>, ISubscription> dataSources = new LinkedHashMap<>();

    public Label() {
        getLayout().height(9);
        this.setText("Label");
        internalSetup();
    }

    @Override
    public Label bindDataSource(IDataProvider<Component> dataProvider) {
        UIEventListener tickableListener;
        if (dataProvider instanceof ITickable tickable) {
            tickableListener = e -> tickable.tick();
            addEventListener(UIEvents.TICK, tickableListener);
        } else {
            tickableListener = null;
        }
        var subscription = dataProvider.registerListener(this::setText, true);
        if (tickableListener != null) {
            subscription.andThen(() -> removeEventListener(UIEvents.TICK, tickableListener));
        }
        this.dataSources.put(dataProvider, subscription);
        return this;
    }

    @Override
    public Label unbindDataSource(IDataProvider<Component> dataProvider) {
        var removed = this.dataSources.remove(dataProvider);
        if (removed != null) {
            removed.unsubscribe();
        }
        return this;
    }

    @Override
    public Collection<IDataProvider<Component>> getBoundDataSources() {
        return dataSources.keySet();
    }

    @Override
    public Component getValue() {
        return getText();
    }

    @Override
    public Label setValue(@Nullable Component value) {
        if (value == null) value = Component.empty();
        return (Label) setText(value);
    }
}
