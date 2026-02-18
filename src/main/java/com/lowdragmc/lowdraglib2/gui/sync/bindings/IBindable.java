package com.lowdragmc.lowdraglib2.gui.sync.bindings;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;

public interface IBindable<T> extends IDataSource<T> {
    /**
     * Binds the given {@code binding} to the current {@link UIElement} instance.
     * This method associates a binding object to enable synchronization of data
     * between the {@link UIElement} and the given binding's data source.
     * If the provided binding is {@code null}, no action is performed and
     * the current instance is returned unchanged.
     *
     * @param binding the {@link IBinding} instance to bind to the current {@link UIElement}.
     *                May be {@code null}, in which case the method has no effect.
     * @return the current {@link UIElement} instance with the binding applied,
     *         or unchanged if the provided {@code binding} is {@code null}.
     */
    default UIElement bind(@Nullable IBinding<T> binding) {
        var self = (UIElement) this;
        if (binding == null) return self;
        if (binding.getRemoteDataSource() == IDataSource.empty()) {
            binding.setRemoteDataSource(this);
        }
        self.addSyncValue(binding.getSyncValue());
        return self;
    }

    /**
     * Unbinds the given {@code binding} from the current {@link UIElement} instance.
     * This method disassociates the specified binding, removes its synchronization
     * value from the element, and detaches any data source previously associated with the binding.
     *
     * @param binding the {@link IBinding} instance to unbind from the current {@link UIElement}.
     *                Must not be {@code null}.
     * @return the current {@link UIElement} instance after the binding has been unbound.
     */
    default UIElement unbind(@Nonnull IBinding<T> binding) {
        var self = (UIElement) this;
        if (binding.getRemoteDataSource() == this) {
            binding.setRemoteDataSource(IDataSource.empty());
        }
        self.removeSyncValue(binding.getSyncValue());
        return self;
    }
}
