package com.lowdragmc.lowdraglib2.gui.ui.bindings;

import com.lowdragmc.lowdraglib2.gui.ui.bindings.sync.SyncStrategy;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public interface IBinding<T> extends IDataSource<T>, IObserver<T> {
    /**
     * Get the strategy for synchronizing data from server to client.
     */
    SyncStrategy getS2CStrategy();

    /**
     * Get the strategy for synchronizing data from client to server.
     */
    SyncStrategy getC2SStrategy();

    /**
     * Get the stream codec for this binding.
     * This codec is used to serialize and deserialize the data in network packets.
     *
     * @return the stream codec for this binding
     */
    StreamCodec<? super RegistryFriendlyByteBuf, T> getStreamCodec();
}
