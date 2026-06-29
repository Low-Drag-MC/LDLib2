package com.lowdragmc.lowdraglib2.test.syncdata;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.syncdata.IManaged;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RPCMethod;
import com.lowdragmc.lowdraglib2.syncdata.holder.IRPCManagedHolder;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.MultiManagedStorage;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LDLib2.MOD_ID)
public class RPCManagedHolderTest {

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void handleRPCPacketInvokesManagedInstanceSelectedByPacketIndex(GameTestHelper helper) {
        var holder = new TestRPCManagedHolder();
        var first = new TestManaged();
        var second = new TestManaged();
        holder.attach(first);
        holder.attach(second);

        var data = holder.parseArgs2Bytes(second, "recordRpc", "payload");
        holder.handleRPCPacket(RPCSender.ofServer(), data);

        if (first.value != null) {
            helper.fail("RPC invoked the wrong managed instance");
        }
        if (!"payload".equals(second.value)) {
            helper.fail("RPC did not invoke the managed instance selected by packet index");
        }

        helper.succeed();
    }

    private static final class TestRPCManagedHolder implements IRPCManagedHolder {
        private final MultiManagedStorage rootStorage = new MultiManagedStorage();

        void attach(IManaged managed) {
            rootStorage.attach(managed.getSyncStorage());
        }

        @Override
        public CustomPacketPayload createRPCPacket(byte[] data) {
            return null;
        }

        @Override
        public ServerLevel getServerLevel() {
            return null;
        }

        @Override
        public ChunkPos getTrackingPos() {
            return null;
        }

        @Override
        public IManagedStorage getRootStorage() {
            return rootStorage;
        }
    }

    private static final class TestManaged implements IManaged {
        private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
        private String value;

        @RPCMethod
        public void recordRpc(String value) {
            this.value = value;
        }

        @Override
        public IManagedStorage getSyncStorage() {
            return syncStorage;
        }

        @Override
        public void notifyPersistence() {
        }
    }
}
