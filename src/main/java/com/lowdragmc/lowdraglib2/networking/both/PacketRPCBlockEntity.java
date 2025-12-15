package com.lowdragmc.lowdraglib2.networking.both;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.networking.PacketIntLocation;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.IRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * a packet that contains payload for managed fields
 */
@NoArgsConstructor
public class PacketRPCBlockEntity extends PacketIntLocation implements CustomPacketPayload {
    public static final ResourceLocation ID = LDLib2.id("rpc_method_payload");
    public static final Type<PacketRPCBlockEntity> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRPCBlockEntity> CODEC = StreamCodec.ofMember(PacketRPCBlockEntity::write, PacketRPCBlockEntity::decode);

    private BlockEntityType<?> blockEntityType;

    private byte[] data;

    public PacketRPCBlockEntity(BlockEntityType<?> type, BlockPos pos, byte[] data) {
        super(pos);
        blockEntityType = type;
        this.data = data;
    }

    public static PacketRPCBlockEntity of(IRPCBlockEntity tile, byte[] data) {
        return new PacketRPCBlockEntity(tile.getSelf().getType(), tile.getSelf().getBlockPos(), data);
    }

    public static void processPacket(@NotNull BlockEntity blockEntity, RPCSender sender, PacketRPCBlockEntity packet, IPayloadContext context) {
        if (blockEntity.getType() != packet.blockEntityType) {
            LDLib2.LOGGER.warn("Block entity type mismatch in rpc payload packet!");
            return;
        }
        if (!(blockEntity instanceof IRPCBlockEntity tile)) {
            LDLib2.LOGGER.error("Received managed payload packet for block entity that does not implement IRPCBlockEntity: " + blockEntity);
            return;
        }
        tile.handleRPCPacket(sender, packet.data);
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        super.write(buf);
        buf.writeResourceLocation(Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntityType)));
        buf.writeByteArray(data);
    }

    public static PacketRPCBlockEntity decode(RegistryFriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        var blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(buffer.readResourceLocation());
        var data = buffer.readByteArray();
        return new PacketRPCBlockEntity(blockEntityType, pos, data);
    }

    public static void execute(PacketRPCBlockEntity packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer) {
            executeServer(packet, context);
        } else {
            executeClient(packet, context);
        }
    }

    public static void executeClient(PacketRPCBlockEntity packet, IPayloadContext context) {
        if (context.player().level() == null) {
            return;
        }
        BlockEntity tile = context.player().level().getBlockEntity(packet.pos);
        if (tile == null) {
            return;
        }
        processPacket(tile, RPCSender.ofServer(), packet, context);
    }

    public static void executeServer(PacketRPCBlockEntity packet, IPayloadContext context) {
        var player = context.player();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LDLib2.LOGGER.error("Received rpc payload packet from client with no server player!");
            return;
        }
        var level = player.level();
        if (!level.isLoaded(packet.pos)) return;
        BlockEntity tile = level.getBlockEntity(packet.pos);
        if (tile == null) {
            return;
        }
        processPacket(tile, RPCSender.ofClient(serverPlayer), packet, context);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
