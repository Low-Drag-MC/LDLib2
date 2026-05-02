package com.lowdragmc.lowdraglib2.gui.sync;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEvent;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.networking.both.PacketModularUISync;
import com.lowdragmc.lowdraglib2.networking.both.PacketUIRPCEvent;
import com.lowdragmc.lowdraglib2.networking.both.PacketUIRPCEventReturn;
import com.lowdragmc.lowdraglib2.utils.ByteBufUtil;
import com.lowdragmc.lowdraglib2.utils.IdentityMap;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.function.Consumers;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class UISyncManager {
    public final ModularUI modularUI;
    // runtime
    private final IdentityMap<SyncValue<?>> syncValues = new IdentityMap<>();
    private final IdentityMap<RPCEvent> rpcEvents = new IdentityMap<>();

    private final AtomicInteger eventID = new AtomicInteger(0);
    @Getter
    private final Map<Integer, Consumer<?>> returnCallbacks = new HashMap<>();

    public UISyncManager(ModularUI modularUI) {
        this.modularUI = modularUI;
    }

    public UISyncManager registerSyncValue(SyncValue<?> syncValue) {
        syncValues.add(syncValue);
        return this;
    }

    public UISyncManager unregisterSyncValue(SyncValue<?> syncValue) {
        syncValues.remove(syncValue);
        return this;
    }

    public UISyncManager registerRPCEvent(RPCEvent rpcEvent) {
        rpcEvents.add(rpcEvent);
        return this;
    }

    public UISyncManager unregisterRPCEvent(RPCEvent rpcEvent) {
        rpcEvents.remove(rpcEvent);
        return this;
    }


    /// Sync Data Logic
    public final void tick() {
        if (modularUI.player == null) return;
        var toSync = new ArrayList<SyncValue<?>>();
        for (var value : syncValues.values()) {
            value.update();
            if (value.hasChanged()) {
                toSync.add(value);
            }
        }
        if (toSync.isEmpty()) return;
        var data = ByteBufUtil.writeCustomData(buf -> {
            writePack(buf, toSync);
        }, modularUI.player.level().registryAccess());
        if (modularUI.player.level().isClientSide) {
            PacketDistributor.sendToServer(new PacketModularUISync(data));
        } else if (modularUI.player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new PacketModularUISync(data));
        }
    }

    public void writeInitialData(RegistryFriendlyByteBuf buffer) {
        for (SyncValue<?> value : syncValues.values()) {
            value.update();
        }
        writePack(buffer, syncValues.values());
    }

    public void readInitialData(RegistryFriendlyByteBuf data) {
        handlePack(data);
        // clear changed flag
        for (var value : syncValues.values()) {
            value.update();
            if (value.hasChanged()) {
                value.clearChanged();
            }
        }
    }

    public void handleSyncPacket(RegistryFriendlyByteBuf data) {
        handlePack(data);
    }

    private void writePack(RegistryFriendlyByteBuf buf, Collection<SyncValue<?>> syncValues) {
        buf.writeVarInt(syncValues.size());
        for (var syncValue : syncValues) {
            buf.writeVarInt(this.syncValues.getID(syncValue));
            syncValue.writeSyncData(buf);
            syncValue.clearChanged();
        }
    }

    private void handlePack(RegistryFriendlyByteBuf buf) {
        var size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            var id = buf.readVarInt();
            var syncValue = syncValues.getValue(id);
            if (syncValue != null) {
                try {
                    syncValue.readSyncData(buf);
                } catch (Exception e) {
                    LDLib2.LOGGER.warn("Note: This is an unexpected behavior, may be attacks by {}", modularUI.player, e);
                }
            } else {
                LDLib2.LOGGER.warn("Received sync data for unknown sync value with id {}", id);
            }
        }
    }

    /// Sync Event Logic
    private int nextEventID() {
        return eventID.getAndIncrement();
    }

    public void sendEvent(RPCEvent event, Object... args) {
        sendEvent(event, Consumers.nop(), args);
    }

    public <T> void sendEvent(RPCEvent event, Consumer<T> responseCallback, Object... args) {
        var player = modularUI.player;
        if (player == null) return;
        if (!rpcEvents.contains(event)) {
            LDLib2.LOGGER.warn("No UI RPC event registered for name {}", event);
            return;
        }

        var response = event.hasReturn() && responseCallback != null;
        int requestID;
        if (response) {
            requestID = nextEventID();
            returnCallbacks.put(requestID, responseCallback);
        } else {
            requestID = 0;
        }
        var data = ByteBufUtil.writeCustomData(buf -> {
            buf.writeVarInt(rpcEvents.getID(event));
            buf.writeBoolean(response);
            buf.writeVarInt(requestID);
            event.writeParametersToBuffer(buf, args);
        }, player.level().registryAccess());
        if (player.level().isClientSide) {
            PacketDistributor.sendToServer(new PacketUIRPCEvent(data));
        } else if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new PacketUIRPCEvent(data));
        }
    }

    public void handEvent(RegistryFriendlyByteBuf buf) {
        var player = modularUI.player;
        if (player == null) return;

        var eventID = buf.readVarInt();
        var response = buf.readBoolean();
        var requestID = buf.readVarInt();
        var rpcEvent = rpcEvents.getValue(eventID);
        if (rpcEvent == null) {
            LDLib2.LOGGER.warn("No UI RPC event registered for event {}, maybe attack?", eventID);
            return;
        }
        Object[] args;
        try {
            args = rpcEvent.readParametersFromBuffer(buf);
        } catch (Exception e) {
            LDLib2.LOGGER.warn("Could not handle ui rpc event {}. it may be attacks.", rpcEvent, e);
            return;
        }
        var returnValue = rpcEvent.executor().apply(args);
        if (response && rpcEvent.hasReturn()) {
            var data = ByteBufUtil.writeCustomData(returnBuf -> {
                returnBuf.writeVarInt(eventID);
                returnBuf.writeVarInt(requestID);
                rpcEvent.writeReturnValueToBuffer(returnBuf, returnValue);
            }, player.level().registryAccess());
            if (player.level().isClientSide) {
                PacketDistributor.sendToServer(new PacketUIRPCEventReturn(data));
            } else if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new PacketUIRPCEventReturn(data));
            }
        }
    }

    public void handEventReturn(RegistryFriendlyByteBuf buf) {
        var eventID = buf.readVarInt();
        var responseID = buf.readVarInt();
        var rpcEvent = rpcEvents.getValue(eventID);
        if (rpcEvent == null) {
            LDLib2.LOGGER.warn("No UI RPC event registered for event {}, maybe attack?", eventID);
            return;
        }
        if (rpcEvent.hasReturn() && returnCallbacks.containsKey(responseID)) {
            Consumer callback = returnCallbacks.remove(responseID);
            callback.accept(rpcEvent.readReturnValueFromBuffer(buf));
        }
    }

}
