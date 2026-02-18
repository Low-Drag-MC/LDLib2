package com.lowdragmc.lowdraglib2.gui.sync.bindings.impl;

import com.lowdragmc.lowdraglib2.gui.sync.SyncValue;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBinding;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataSource;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.SyncStrategy;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SimpleBinding<T> implements IBinding<T> {
    public final boolean isRemote;
    @Getter
    public final SyncValue<T> syncValue;
    @Getter @Accessors(fluent = true)
    public final SyncStrategy c2sStrategy;
    @Getter @Accessors(fluent = true)
    public final SyncStrategy s2cStrategy;

    // runtime
    @Setter @Getter
    private IDataSource<T> serverDataSource = IDataSource.empty();
    @Setter @Getter
    private IDataSource<T> remoteDataSource = IDataSource.empty();

    public SimpleBinding(boolean isRemote, String name, Type type, @Nullable T initialValue, SyncStrategy c2sStrategy, SyncStrategy s2cStrategy) {
        this.isRemote = isRemote;
        this.c2sStrategy = c2sStrategy;
        this.s2cStrategy = s2cStrategy;
        this.syncValue = new SyncValue<>(name, type, initialValue);
        if (isRemote) {
            syncValue.setAcceptSync(acceptS2C());
            syncValue.setToSync(acceptC2S());
            syncValue.setSyncStrategy(c2sStrategy);
            registerListener(this::setRemoteValue);
            setValueProvider(this::getRemoteValue);
        } else {
            syncValue.setAcceptSync(acceptC2S());
            syncValue.setToSync(acceptS2C());
            syncValue.setSyncStrategy(s2cStrategy);
            registerListener(this::setServerValue);
            setValueProvider(this::getServerValue);
        }
    }

    public ISubscription registerListener(Consumer<T> listener) {
        return this.syncValue.addListener(listener);
    }

    public void setValueProvider(Supplier<T> dataValueProvider) {
        this.syncValue.setValueProvider(dataValueProvider);
    }

    private T getServerValue() {
        return serverDataSource.getValue();
    }

    private T getRemoteValue() {
        return remoteDataSource.getValue();
    }

    private void setServerValue(T value) {
        serverDataSource.setValue(value);
    }

    private void setRemoteValue(T value) {
        remoteDataSource.setValue(value);
    }
}
