package com.lowdragmc.lowdraglib2.networking.s2c;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * The {@link SPacketAutoSyncBlockEntity} updates of several block entities, sent as one packet.
 */
public record SPacketAutoSyncBlockEntities(List<Entry> entries) implements CustomPacketPayload {
    public static final Identifier ID = LDLib2.id("auto_sync_block_entities");
    public static final Type<SPacketAutoSyncBlockEntities> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SPacketAutoSyncBlockEntities> CODEC =
            Entry.CODEC.apply(ByteBufCodecs.list()).map(SPacketAutoSyncBlockEntities::new, SPacketAutoSyncBlockEntities::entries);

    public record Entry(BlockPos pos, BlockEntityType<?> type, BitSet changed, byte[] data, CompoundTag extra) {
        private static final StreamCodec<RegistryFriendlyByteBuf, BlockEntityType<?>> TYPE_CODEC = ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE);
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.of((buf, entry) -> entry.write(buf), Entry::read);

        public static Entry of(SPacketAutoSyncBlockEntity packet) {
            return new Entry(packet.getPos(), packet.getBlockEntityType(), packet.getChanged(), packet.getData(), packet.getExtra());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            TYPE_CODEC.encode(buf, type);
            buf.writeByteArray(changed.toByteArray());
            buf.writeByteArray(data);
            buf.writeBoolean(!extra.isEmpty());
            if (!extra.isEmpty()) {
                buf.writeNbt(extra);
            }
        }

        private static Entry read(RegistryFriendlyByteBuf buf) {
            var pos = buf.readBlockPos();
            var type = TYPE_CODEC.decode(buf);
            var changed = BitSet.valueOf(buf.readByteArray());
            var data = buf.readByteArray();
            var extra = buf.readBoolean() ? buf.readNbt() : null;
            return new Entry(pos, type, changed, data, extra == null ? new CompoundTag() : extra);
        }

        /**
         * An upper bound of what the entry adds to an encoded packet.
         */
        public int sizeBound() {
            return 32 + changed.size() / 8 + data.length + (extra.isEmpty() ? 0 : encodedSize(extra));
        }

        private static int encodedSize(CompoundTag tag) {
            var buf = Unpooled.buffer();
            try {
                new FriendlyByteBuf(buf).writeNbt(tag);
                return buf.readableBytes();
            } finally {
                buf.release();
            }
        }
    }

    /**
     * Packs entries in order into as few packets as possible with at most {@code maxBytes} of entries each. An entry
     * bigger than that gets a packet of its own.
     */
    public static List<SPacketAutoSyncBlockEntities> pack(List<Entry> entries, int maxBytes) {
        var packets = new ArrayList<SPacketAutoSyncBlockEntities>();
        var current = new ArrayList<Entry>();
        int size = 0;
        for (var entry : entries) {
            int entrySize = entry.sizeBound();
            if (!current.isEmpty() && size + entrySize > maxBytes) {
                packets.add(new SPacketAutoSyncBlockEntities(current));
                current = new ArrayList<>();
                size = 0;
            }
            current.add(entry);
            size += entrySize;
        }
        if (!current.isEmpty()) {
            packets.add(new SPacketAutoSyncBlockEntities(current));
        }
        return packets;
    }

    public static void execute(SPacketAutoSyncBlockEntities packet, IPayloadContext context) {
        var level = context.player().level();
        for (var entry : packet.entries) {
            if (!(level.getBlockEntity(entry.pos) instanceof ISyncBlockEntity blockEntity)) continue;
            if (blockEntity.getSelf().getType() != entry.type) {
                LDLib2.LOGGER.warn("Block entity type mismatch in managed payload packet!");
                continue;
            }
            try {
                blockEntity.handleSyncPacket(level.registryAccess(), entry.changed, entry.data, entry.extra);
            } catch (Throwable e) {
                LDLib2.LOGGER.error("Failed to apply the synced fields of the block entity at {}", entry.pos, e);
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
