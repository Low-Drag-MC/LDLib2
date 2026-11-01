package com.lowdragmc.lowdraglib2.syncdata.holder;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.async.AsyncThreadData;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntities;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The syncs an async period found, sent once it ends: the block entity updates of a chunk travel in one packet, and
 * nothing is encoded or built for chunks no player is watching.
 */
public final class SyncPacketBatch implements AsyncThreadData.PeriodTask {
    private static final int MAX_PACKET_BYTES = 256 * 1024;

    // chunks nobody watched when a period last looked at them; a chunk not listed counts as watched
    private static final Map<ServerLevel, LongSet> UNWATCHED = Collections.synchronizedMap(new WeakHashMap<>());

    private final Map<ServerLevel, Long2ObjectMap<List<Update>>> updates = new LinkedHashMap<>();
    private final Map<ServerLevel, LongSet> unwatched = new HashMap<>();

    /**
     * Queues the changes a sync found to go out with the rest of the current async period's.
     *
     * @return {@code false} if the calling thread is not running an async period, so the caller has to send them
     */
    public static boolean offer(ISyncMangedHolder holder, BitSet changed, byte[] data) {
        var chunk = holder.getTrackingPos();
        if (chunk == null) return false;
        var batch = AsyncThreadData.periodTask(SyncPacketBatch.class, SyncPacketBatch::new);
        if (batch == null) return false;
        batch.chunks(holder.getServerLevel()).computeIfAbsent(chunk.pack(), key -> new ArrayList<>())
                .add(new Update(holder, changed, data));
        return true;
    }

    /**
     * Whether a sync on the async thread may leave the changes it found for later, because nobody watched the holder's
     * chunk when a period last looked. They stay marked, so once someone watches they go out with the values the fields
     * have by then; until then the chunk gets looked at again at the end of every period that defers for it.
     *
     * @return {@code false} if the chunk may be watched or the thread is not running an async period, so the caller
     * has to send the changes
     */
    public static boolean deferUnwatched(ISyncMangedHolder holder) {
        var chunk = holder.getTrackingPos();
        if (chunk == null) return false;
        var batch = AsyncThreadData.periodTask(SyncPacketBatch.class, SyncPacketBatch::new);
        if (batch == null) return false;
        var level = holder.getServerLevel();
        if (!batch.unwatched(level).contains(chunk.pack())) return false;
        batch.chunks(level).computeIfAbsent(chunk.pack(), key -> new ArrayList<>());
        return true;
    }

    private Long2ObjectMap<List<Update>> chunks(ServerLevel level) {
        return updates.computeIfAbsent(level, key -> new Long2ObjectLinkedOpenHashMap<>());
    }

    private LongSet unwatched(ServerLevel level) {
        return unwatched.computeIfAbsent(level, key -> UNWATCHED.computeIfAbsent(key, k -> LongSets.synchronize(new LongOpenHashSet())));
    }

    @Override
    public void run(MinecraftServer server) {
        updates.forEach((level, chunks) -> {
            var unwatched = unwatched(level);
            for (var chunk : chunks.long2ObjectEntrySet()) {
                var receivers = new ArrayList<Receiver>();
                for (var player : level.getChunkSource().chunkMap.getPlayers(ChunkPos.unpack(chunk.getLongKey()), false)) {
                    // a fake player's connection has no channel to check or send through
                    if (!(player instanceof FakePlayer)) {
                        receivers.add(new PlayerReceiver(player));
                    }
                }
                if (receivers.isEmpty()) {
                    unwatched.add(chunk.getLongKey());
                } else {
                    unwatched.remove(chunk.getLongKey());
                    send(receivers, build(level, chunk.getValue()));
                }
            }
        });
    }

    @ApiStatus.Internal
    public record Update(ISyncMangedHolder holder, BitSet changed, byte[] data) {}

    /**
     * The packets one chunk's updates make: their entries for the players who take
     * {@link SPacketAutoSyncBlockEntities}, the same updates as single packets for the rest, and the custom payloads of
     * holders that build their own.
     */
    @ApiStatus.Internal
    public record ChunkPackets(List<SPacketAutoSyncBlockEntities.Entry> entries, List<ClientboundCustomPayloadPacket> singles,
                               List<ClientboundCustomPayloadPacket> custom) {}

    @ApiStatus.Internal
    public static ChunkPackets build(ServerLevel level, List<Update> updates) {
        var packets = new ChunkPackets(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        for (var update : updates) {
            if (update.holder instanceof BlockEntity blockEntity && blockEntity.isRemoved()) continue;
            try {
                var extra = new CompoundTag();
                update.holder.writeCustomSyncData(level.registryAccess(), extra);
                var payload = update.holder.createSyncPacket(update.changed, update.data, extra);
                if (payload.getClass() == SPacketAutoSyncBlockEntity.class) {
                    packets.entries.add(SPacketAutoSyncBlockEntities.Entry.of((SPacketAutoSyncBlockEntity) payload));
                    packets.singles.add(new ClientboundCustomPayloadPacket(payload));
                } else {
                    packets.custom.add(new ClientboundCustomPayloadPacket(payload));
                }
            } catch (Throwable e) {
                LDLib2.LOGGER.error("Failed to build the sync packet of {}", update.holder, e);
            }
        }
        return packets;
    }

    /**
     * Where {@link #send} delivers a chunk's packets: a watching player's connection.
     */
    @ApiStatus.Internal
    public interface Receiver {
        boolean hasChannel(CustomPacketPayload.Type<?> type);

        void send(Packet<?> packet);
    }

    private record PlayerReceiver(ServerPlayer player) implements Receiver {
        @Override
        public boolean hasChannel(CustomPacketPayload.Type<?> type) {
            return player.connection.hasChannel(type);
        }

        @Override
        public void send(Packet<?> packet) {
            player.connection.send(packet);
        }
    }

    @ApiStatus.Internal
    public static void send(List<? extends Receiver> receivers, ChunkPackets packets) {
        List<ClientboundCustomPayloadPacket> batched = null;
        for (var receiver : receivers) {
            try {
                if (!packets.entries.isEmpty() && receiver.hasChannel(SPacketAutoSyncBlockEntities.TYPE)) {
                    if (batched == null) {
                        batched = SPacketAutoSyncBlockEntities.pack(packets.entries, MAX_PACKET_BYTES).stream()
                                .map(ClientboundCustomPayloadPacket::new).toList();
                    }
                    batched.forEach(receiver::send);
                } else {
                    sendSupported(receiver, packets.singles);
                }
                sendSupported(receiver, packets.custom);
            } catch (Throwable e) {
                LDLib2.LOGGER.error("Failed to send sync packets to {}", receiver, e);
            }
        }
    }

    private static void sendSupported(Receiver receiver, List<ClientboundCustomPayloadPacket> packets) {
        for (var packet : packets) {
            if (receiver.hasChannel(packet.payload().type())) {
                receiver.send(packet);
            }
        }
    }
}
