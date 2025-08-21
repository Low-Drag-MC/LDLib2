package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBindable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataConsumer;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IObserver;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataProvider;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Label extends TextElement implements IBindable<Component>, IDataConsumer<Component> {
    protected final Map<IDataProvider<Component>, ISubscription> dataSources = new LinkedHashMap<>();

    public Label() {
        getLayout().setHeight(9);
        this.setText("Label");
    }

    @Override
    public Label bindDataSource(IDataProvider<Component> dataSource) {
        this.dataSources.put(dataSource, dataSource.registerListener(this::setText, true));
        return this;
    }

    @Override
    public Label unbindDataSource(IDataProvider<Component> dataSource) {
        var removed = this.dataSources.remove(dataSource);
        if (removed != null) {
            removed.unsubscribe();
        }
        return this;
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
