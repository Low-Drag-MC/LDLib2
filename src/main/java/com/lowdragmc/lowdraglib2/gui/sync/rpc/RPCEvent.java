package com.lowdragmc.lowdraglib2.gui.sync.rpc;

import com.lowdragmc.lowdraglib2.syncdata.SyncValueHolder;
import net.minecraft.network.RegistryFriendlyByteBuf;

import org.jetbrains.annotations.Nullable;
import java.util.function.Function;

public record RPCEvent(
        SyncValueHolder[] argHolders,
        @Nullable SyncValueHolder returnHolder,
        Function<Object[], Object> executor
) {

    private void checkArgs(Object[] args) {
        if (args.length != argHolders.length) {
            throw new IllegalArgumentException("Wrong number of parameters for RPC call, expected %d but got %d".formatted(argHolders.length, args.length));
        }
    }

    public void writeParametersToBuffer(RegistryFriendlyByteBuf buffer, Object[] args) {
        checkArgs(args);
        for (int i = 0; i < args.length; i++) {
            var argHolder = argHolders[i];
            argHolder.setValue(args[i]);
            argHolder.ref.update();
            argHolder.ref.readSyncToStream(buffer);
        }
    }

    public Object[] readParametersFromBuffer(RegistryFriendlyByteBuf buffer) {
        var args = new Object[argHolders.length];
        for (int i = 0; i < argHolders.length; i++) {
            var argHolder = argHolders[i];
            argHolder.ref.writeSyncFromStream(buffer);
            args[i] = argHolder.getValue();
        }
        return args;
    }

    public void writeReturnValueToBuffer(RegistryFriendlyByteBuf buffer, Object returnValue) {
        if (returnHolder != null) {
            returnHolder.setValue(returnValue);
            returnHolder.ref.update();
            returnHolder.ref.readSyncToStream(buffer);
        }
    }

    public Object readReturnValueFromBuffer(RegistryFriendlyByteBuf buffer) {
        if (returnHolder != null) {
            returnHolder.ref.writeSyncFromStream(buffer);
            return returnHolder.getValue();
        }
        return null;
    }

    public boolean hasReturn() {
        return returnHolder != null;
    }
}
