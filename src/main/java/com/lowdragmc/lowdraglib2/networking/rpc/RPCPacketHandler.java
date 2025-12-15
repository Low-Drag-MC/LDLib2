package com.lowdragmc.lowdraglib2.networking.rpc;

import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;

public interface RPCPacketHandler {
    RPCPacketHandler EMPTY = new RPCPacketHandler() {
        @Override
        public byte[] args2Bytes(Object... args) {
            return new byte[0];
        }

        @Override
        public Object[] bytes2Args(byte[] data) {
            return new Object[0];
        }

        @Override
        public void handler(RPCSender sender, Object... args) {

        }
    };

    byte[] args2Bytes(Object... args);

    Object[] bytes2Args(byte[] data);

    void handler(RPCSender sender, Object... args);
}
