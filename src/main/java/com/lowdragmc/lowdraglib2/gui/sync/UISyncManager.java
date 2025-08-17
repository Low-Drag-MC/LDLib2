package com.lowdragmc.lowdraglib2.gui.sync;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBinding;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IData;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataBindingHolder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEvent;
import com.lowdragmc.lowdraglib2.networking.both.PacketModularUISync;
import com.lowdragmc.lowdraglib2.utils.ByteBufUtil;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.function.Function;

public class UISyncManager {
    public final Player player;
    // runtime
    @Getter
    private final Map<String, IBinding<?>> bindings = new HashMap<>();
    @Getter
    private final Map<String, IData<?>> datas = new HashMap<>();
    @Getter
    private final Map<String, IBinding<?>> autoBindings = new HashMap<>();
    @Getter
    private final Map<String, RPCEvent> rpcEvents = new HashMap<>();
    private final Map<String, Function<Object[], Object>> rpcClientExecutor = new HashMap<>();
    private final Map<String, Function<Object[], Object>> rpcServerExecutor = new HashMap<>();

    public UISyncManager(Player player) {
        this.player = player;
    }

    public final boolean isRemote() {
        return player.level().isClientSide;
    }

    public UISyncManager addDataBindings(DataBindingBuilder<?>... dataBinding) {
        for (int i = 0; i < dataBinding.length; i++) {
            addDataBinding(dataBinding[i]);
        }
        return this;
    }

    public UISyncManager addDataBinding(DataBindingBuilder<?> dataBinding) {
        addDataBinding(dataBinding.build(isRemote()));
        return this;
    }

    public UISyncManager addDataBindings(IDataBindingHolder<?>... dataBindings) {
        for (var dataBinding : dataBindings) {
            addDataBinding(dataBinding);
        }

        return this;
    }
    public UISyncManager addDataBinding(IDataBindingHolder<?> dataBinding) {
        if (isRemote()) {
            var binding = dataBinding.getBinding();
            bindings.put(binding.name(), binding);
            if (binding.name().startsWith("@")) {
                autoBindings.put(binding.name(), binding);
            }
        } else {
            var data = dataBinding.getData();
            datas.put(data.name(), data);
        }
        return this;
    }

    public UISyncManager removeDataBinding(String uid) {
        this.bindings.remove(uid);
        this.autoBindings.remove(uid);
        this.datas.remove(uid);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> IBinding<T> getBinding(String name) {
        return (IBinding<T>) bindings.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> IData<T> getData(String name) {
        return (IData<T>) datas.get(name);
    }

    /// Sync Data Logic
    public final void tick() {
        if (isRemote()) {
            tickClient();
        } else {
            tickServer();
        }
    }

    private void tickClient() {
        var toSync = new ArrayList<String>();
        bindings.forEach((uid, binding) -> {
            binding.tickClient();
            if (binding.needC2SSync()) {
                toSync.add(uid);
            }
        });
        if (toSync.isEmpty()) return;
        var data = ByteBufUtil.writeCustomData(buf -> {
            buf.writeVarInt(toSync.size());
            for (var uid : toSync) {
                buf.writeUtf(uid);
                var binding = bindings.get(uid);
                binding.writeC2SSyncData(buf);
                binding.clearChanged();
            }
        }, player.level().registryAccess());
        PacketDistributor.sendToServer(new PacketModularUISync(data));
    }

    private void tickServer() {
        if (player instanceof ServerPlayer serverPlayer) {
            var toSync = new ArrayList<String>();
            datas.forEach((uid, data) -> {
                data.tickServer();
                if (data.needS2CSync()) {
                    toSync.add(uid);
                }
            });
            if (toSync.isEmpty()) return;
            var data = ByteBufUtil.writeCustomData(buf -> {
                buf.writeVarInt(toSync.size());
                for (var uid : toSync) {
                    buf.writeUtf(uid);
                    var d = datas.get(uid);
                    d.writeS2CSyncData(buf);
                    d.clearChanged();
                }
            }, player.level().registryAccess());
            PacketDistributor.sendToPlayer(serverPlayer, new PacketModularUISync(data));
        }
    }

    public void readInitialData(RegistryFriendlyByteBuf data) {
        if (!isRemote()) return;
        handleS2CPacket(data);
    }

    public void writeInitialData(RegistryFriendlyByteBuf buffer) {
        if (isRemote()) return;
        buffer.writeVarInt(datas.size());
        datas.forEach((uid, data) -> {
            buffer.writeUtf(uid);
            data.writeS2CSyncData(buffer);
        });
    }

    public void handleS2CPacket(RegistryFriendlyByteBuf data) {
        var size = data.readVarInt();
        for (int i = 0; i < size; i++) {
            var uid = data.readUtf();
            var dataBinding = bindings.get(uid);
            if (dataBinding != null) {
                if (!dataBinding.acceptS2C()) {
                    LDLib2.LOGGER.warn("Received S2C sync data for {} but it is not registered for C2S sync. Be aware of that it maybe an error or attack!!", dataBinding);
                }
                dataBinding.readS2CSyncData(data);
            }
        }
    }

    public void handleC2SPacket(RegistryFriendlyByteBuf buf) {
        var size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            var uid = buf.readUtf();
            var dataBinding = datas.get(uid);
            if (dataBinding != null) {
                if (!dataBinding.acceptC2S()) {
                    LDLib2.LOGGER.warn("Received C2S sync data for {} but it is not registered for S2C sync. Be aware of that it maybe an error or attack!!", dataBinding);
                }
                dataBinding.readC2SSyncData(buf);
            }
        }
    }

    /// Sync Event Logic
    public void sendEvent(String name, Object... args) {

    }

}
