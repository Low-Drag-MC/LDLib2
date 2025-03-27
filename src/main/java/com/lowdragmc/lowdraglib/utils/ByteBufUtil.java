package com.lowdragmc.lowdraglib.utils;

import io.netty.buffer.Unpooled;
import lombok.experimental.UtilityClass;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.common.util.FriendlyByteBufUtil;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.function.Consumer;

@UtilityClass
public final class ByteBufUtil {
    /**
     * Writes custom data to a {@link RegistryFriendlyByteBuf}, then read it for consumer.
     *
     * @param data           Data to write.
     * @param dataWriter     The data reader.
     * @param registryAccess The registry access used by registry dependent writers on the buffer
     */
    public static void readCustomData(byte[] data, Consumer<RegistryFriendlyByteBuf> dataWriter, RegistryAccess registryAccess) {
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), registryAccess, ConnectionType.NEOFORGE);
        try {
            dataWriter.accept(buf);
        } finally {
            buf.release();
        }
    }

    /**
     * Writes custom data to a {@link RegistryFriendlyByteBuf}, then returns the written data as a byte array.
     * This implementation fix bytes array too big in the {@link FriendlyByteBufUtil#writeCustomData(Consumer, RegistryAccess)}.
     *
     * @param dataWriter     The data writer.
     * @param registryAccess The registry access used by registry dependent writers on the buffer
     * @return The written data.
     */
    public static byte[] writeCustomData(Consumer<RegistryFriendlyByteBuf> dataWriter, RegistryAccess registryAccess) {
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess, ConnectionType.NEOFORGE);
        try {
            dataWriter.accept(buf);
            buf.readerIndex(0);
            final byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return data;
        } finally {
            buf.release();
        }
    }
}
