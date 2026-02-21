package com.lowdragmc.lowdraglib2.gui.sync.rpc;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public record RPCEmitter(RPCEvent event, Supplier<@Nullable ModularUI> modularProvider) {
    /**
     * Sends an event with the provided arguments to the associated {@link ModularUI#syncManager}.
     *
     * @param args the arguments to be sent with the event. The number and types of arguments
     *             should match the requirements of the {@link RPCEvent}.
     * @return {@code true} if the event is successfully sent; {@code false} if the {@link ModularUI} instance is not available.
     */
    public boolean send(Object... args) {
        var mui = modularProvider.get();
        if (mui == null) return false;
        mui.syncManager.sendEvent(event, args);
        return true;
    }

    /**
     * Sends an event with the provided arguments and a callback to handle the response.
     * Utilizes the associated {@link ModularUI#syncManager} to dispatch the event.
     *
     * @param <T>       the type of the response object expected from the event.
     * @param callback  a callback function to handle the response from the event. Can be {@code null} if no response is expected.
     * @param args      the arguments to be sent with the event. The number and types of arguments
     *                  should match the requirements of the {@link RPCEvent}.
     * @return {@code true} if the event is successfully sent; {@code false} if the {@link ModularUI} instance is not available.
     */
    public <T> boolean send(Consumer<T> callback, Object... args) {
        var mui = modularProvider.get();
        if (mui == null) return false;
        mui.syncManager.sendEvent(event, callback, args);
        return true;
    }
}
