package com.lowdragmc.lowdraglib2.gui.sync;

import com.lowdragmc.lowdraglib2.gui.bindings.IBinding;
import com.lowdragmc.lowdraglib2.gui.bindings.IDataBindingHolder;
import com.lowdragmc.lowdraglib2.networking.both.PacketModularUISync;
import com.lowdragmc.lowdraglib2.utils.ByteBufUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class UISyncManager {
    public final Player player;
    // runtime
    private final Map<String, IDataBindingHolder<?>> dataBindings = new HashMap<>();

    public UISyncManager(Player player) {
        this.player = player;
    }

    public final boolean isRemote() {
        return player.level().isClientSide;
    }

    public UISyncManager addDataBinding(String uid, IDataBindingHolder<?> binding) {
        this.dataBindings.put(uid, binding);
        return this;
    }

    public UISyncManager removeDataBinding(String uid) {
        this.dataBindings.remove(uid);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> IBinding<T> getBinding(String name) {
        return (IBinding<T>) dataBindings.get(name);
    }

    public final void tick() {
        if (isRemote()) {
            tickClient();
        } else {
            tickServer();
        }
    }

    private void tickClient() {
        var toSync = new ArrayList<String>();
        dataBindings.forEach((uid, dataBinding) -> {
            if (dataBinding.getBinding().needC2SSync()) {
                toSync.add(uid);
            }
        });
        if (toSync.isEmpty()) return;
        var data = ByteBufUtil.writeCustomData(buf -> {
            buf.writeVarInt(toSync.size());
            for (var uid : toSync) {
                buf.writeUtf(uid);
                var binding = dataBindings.get(uid).getBinding();
                binding.writeC2SSyncData(buf);
                binding.clearChanged();
            }
        }, player.level().registryAccess());
        PacketDistributor.sendToServer(new PacketModularUISync(data));
    }

    private void tickServer() {
        if (player instanceof ServerPlayer serverPlayer) {
            var toSync = new ArrayList<String>();
            dataBindings.forEach((uid, dataBinding) -> {
                dataBinding.getData().tickServer();
                if (dataBinding.getData().needS2CSync()) {
                    toSync.add(uid);
                }
            });
            if (toSync.isEmpty()) return;
            var data = ByteBufUtil.writeCustomData(buf -> {
                buf.writeVarInt(toSync.size());
                for (var uid : toSync) {
                    buf.writeUtf(uid);
                    var d = dataBindings.get(uid).getData();
                    d.writeS2CSyncData(buf);
                    d.clearChanged();
                }
            }, player.level().registryAccess());
            PacketDistributor.sendToPlayer(serverPlayer, new PacketModularUISync(data));
        }
    }

    public void readInitialData(RegistryFriendlyByteBuf data) {
        if (!isRemote()) return;
        var size = data.readVarInt();
        for (int i = 0; i < size; i++) {
            var uid = data.readUtf();
            var dataBinding = dataBindings.get(uid);
            if (dataBinding != null) {
                dataBinding.getBinding().readS2CSyncData(data);
            }
        }
    }

    public void writeInitialData(RegistryFriendlyByteBuf buffer) {
        if (isRemote()) return;
        buffer.writeVarInt(dataBindings.size());
        dataBindings.forEach((uid, dataBinding) -> {
            buffer.writeUtf(uid);
            dataBinding.getData().writeS2CSyncData(buffer);
        });
    }

    public void handleS2CPacket(RegistryFriendlyByteBuf buf) {
        var size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            var uid = buf.readUtf();
            var dataBinding = dataBindings.get(uid);
            if (dataBinding != null) {
                dataBinding.getBinding().readS2CSyncData(buf);
            }
        }
    }

    public void handleC2SPacket(RegistryFriendlyByteBuf buf) {
        var size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            var uid = buf.readUtf();
            var dataBinding = dataBindings.get(uid);
            if (dataBinding != null) {
                dataBinding.getData().readC2SSyncData(buf);
            }
        }
    }

}
