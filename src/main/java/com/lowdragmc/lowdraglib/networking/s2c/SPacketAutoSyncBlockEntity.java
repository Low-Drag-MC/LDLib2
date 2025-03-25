package com.lowdragmc.lowdraglib.networking.s2c;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.networking.PacketIntLocation;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib.utils.ByteBufUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * a packet that contains payload for managed fields
 */
public class SPacketAutoSyncBlockEntity extends PacketIntLocation {
    public static final ResourceLocation ID = LDLib.location("auto_sync_block_entity");
    public static final Type<SPacketAutoSyncBlockEntity> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SPacketAutoSyncBlockEntity> CODEC = StreamCodec.ofMember(SPacketAutoSyncBlockEntity::write, SPacketAutoSyncBlockEntity::decode);

    private final BlockEntityType<?> blockEntityType;
    private final BitSet changed;
    private final byte[] data;
    private final CompoundTag extra;

    private SPacketAutoSyncBlockEntity(BlockEntityType<?> type, BlockPos pos, BitSet changed, byte[] data, CompoundTag extra) {
        super(pos);
        blockEntityType = type;
        this.changed = changed;
        this.data = data;
        this.extra = extra;
    }

    public static SPacketAutoSyncBlockEntity of(IAutoSyncBlockEntity tile, boolean force) {
        var changed = new BitSet();
        var syncedFields = tile.getRootStorage().getSyncFields();
        var data = ByteBufUtil.writeCustomData(buffer -> {
            for (int i = 0; i < syncedFields.length; i++) {
                var field = syncedFields[i];
                if (force || field.isSyncDirty()) {
                    changed.set(i);
                    field.readSyncToStream(buffer);
                    field.clearSyncDirty();
                }
            }
        }, tile.getSelf().getLevel().registryAccess());
        var extra = new CompoundTag();
        tile.writeCustomSyncData(extra);
        return new SPacketAutoSyncBlockEntity(tile.getBlockEntityType(), tile.getCurrentPos(), changed, data, extra);
    }

    public static void processPacket(@NotNull IAutoSyncBlockEntity blockEntity, SPacketAutoSyncBlockEntity packet) {
        if (blockEntity.getSelf().getType() != packet.blockEntityType) {
            LDLib.LOGGER.warn("Block entity type mismatch in managed payload packet!");
            return;
        }
        ByteBufUtil.readCustomData(packet.data, buffer -> {
            var storage = blockEntity.getRootStorage();
            var syncedFields = storage.getSyncFields();
            for (int i = 0; i < syncedFields.length; i++) {
                if (packet.changed.get(i)) {
                    var field = syncedFields[i];
                    var key = field.getKey();
                    if (storage.hasSyncListener(key)) {
                        var postStream = storage.notifyFieldUpdate(key, field.readRaw());
                        field.writeSyncFromStream(buffer);
                        postStream.forEach(consumer -> consumer.accept(field.readRaw()));
                    } else {
                        field.writeSyncFromStream(buffer);
                    }
                }
            }
        }, blockEntity.getSelf().getLevel().registryAccess());
        blockEntity.readCustomSyncData(packet.extra);
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        super.write(buf);
        buf.writeResourceLocation(Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntityType)));
        buf.writeByteArray(changed.toByteArray());
        buf.writeByteArray(data);
        buf.writeNbt(extra);
    }

    public static SPacketAutoSyncBlockEntity decode(RegistryFriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        var blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(buffer.readResourceLocation());
        var changed = BitSet.valueOf(buffer.readByteArray());
        var data = buffer.readByteArray();
        var extra = buffer.readNbt();
        return new SPacketAutoSyncBlockEntity(blockEntityType, pos, changed, data, extra);
    }

    public static void execute(SPacketAutoSyncBlockEntity packet, IPayloadContext context) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            if (level.getBlockEntity(packet.pos) instanceof IAutoSyncBlockEntity autoSyncBlockEntity) {
                processPacket(autoSyncBlockEntity, packet);
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
