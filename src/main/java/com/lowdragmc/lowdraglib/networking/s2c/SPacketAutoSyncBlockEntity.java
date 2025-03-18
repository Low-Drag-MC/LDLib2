package com.lowdragmc.lowdraglib.networking.s2c;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.networking.PacketIntLocation;
import com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries;
import com.lowdragmc.lowdraglib.syncdata.accessor.IManagedObjectAccessor;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import io.netty.buffer.Unpooled;
import lombok.NoArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * a packet that contains payload for managed fields
 */
@NoArgsConstructor
public class SPacketAutoSyncBlockEntity extends PacketIntLocation {
    public static final ResourceLocation ID = LDLib.location("managed_payload");
    public static final Type<SPacketAutoSyncBlockEntity> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SPacketAutoSyncBlockEntity> CODEC = StreamCodec.ofMember(SPacketAutoSyncBlockEntity::write, SPacketAutoSyncBlockEntity::decode);

    private BlockEntityType<?> blockEntityType;
    private BitSet changed;
    private RegistryFriendlyByteBuf payloadBuf;
    private CompoundTag extra;

    private SPacketAutoSyncBlockEntity(BlockEntityType<?> type, BlockPos pos, BitSet changed, RegistryFriendlyByteBuf payloadBuf, CompoundTag extra) {
        super(pos);
        blockEntityType = type;
        this.changed = changed;
        this.payloadBuf = payloadBuf;
        this.extra = extra;
    }

    public static SPacketAutoSyncBlockEntity of(IAutoSyncBlockEntity tile, boolean force) {
        var changed = new BitSet();
        var syncedFields = tile.getRootStorage().getSyncFields();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), tile.getSelf().getLevel().registryAccess(), ConnectionType.NEOFORGE);
        for (int i = 0; i < syncedFields.length; i++) {
            var field = syncedFields[i];
            if (force || field.isSyncDirty()) {
                changed.set(i);
                field.readSyncToStream(buffer);
                field.clearSyncDirty();
            }
        }
        var extra = new CompoundTag();
        tile.writeCustomSyncData(extra);
        return new SPacketAutoSyncBlockEntity(tile.getBlockEntityType(), tile.getCurrentPos(), changed, buffer, extra);
    }

    public static void processPacket(@NotNull IAutoSyncBlockEntity blockEntity, SPacketAutoSyncBlockEntity packet) {
        if (blockEntity.getSelf().getType() != packet.blockEntityType) {
            LDLib.LOGGER.warn("Block entity type mismatch in managed payload packet!");
            return;
        }
        var storage = blockEntity.getRootStorage();
        var syncedFields = storage.getSyncFields();
        for (int i = 0; i < syncedFields.length; i++) {
            if (packet.changed.get(i)) {
                syncedFields[i].writeSyncFromStream(packet.payloadBuf);
            }
        }

        IManagedObjectAccessor.writeSyncedFields(storage, syncedFields, packet.changed, packet.payloads, blockEntity.getSelf().getLevel().registryAccess());
        blockEntity.readCustomSyncData(packet.extra);
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        super.write(buf);
        buf.writeResourceLocation(Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntityType)));
        buf.writeByteArray(changed.toByteArray());
        buf.writeVarInt(payloadBuf.readableBytes());
        buf.writeBytes(buf);
        buf.writeNbt(extra);
        buf.writeBytes(payloadBuf);
    }

    public static SPacketAutoSyncBlockEntity decode(RegistryFriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        var blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(buffer.readResourceLocation());
        var changed = BitSet.valueOf(buffer.readByteArray());
        var bufPayload = new RegistryFriendlyByteBuf(buffer.readBytes(buffer.readableBytes()), buffer.registryAccess());
        buffer.readBytes(bufPayload, buffer.readVarInt());
        var extra = buffer.readNbt();
        return new SPacketAutoSyncBlockEntity(blockEntityType, pos, changed, bufPayload, extra);
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
