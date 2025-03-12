package com.lowdragmc.lowdraglib.networking.both;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.networking.PacketIntLocation;
import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IRPCBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.lowdraglib.syncdata.rpc.RPCSender;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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

import java.util.Arrays;
import java.util.Objects;

/**
 * a packet that contains payload for managed fields
 */
@NoArgsConstructor
public class PacketRPCMethodPayload extends PacketIntLocation implements CustomPacketPayload {
    public static final ResourceLocation ID = LDLib.location("rpc_method_payload");
    public static final Type<PacketRPCMethodPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRPCMethodPayload> CODEC = StreamCodec.ofMember(PacketRPCMethodPayload::write, PacketRPCMethodPayload::decode);

    private BlockEntityType<?> blockEntityType;
    private ITypedPayload<?>[] payloads;

    private String methodName;
    private int managedId;

    public PacketRPCMethodPayload(int managedId, BlockEntityType<?> type, BlockPos pos, String methodName, ITypedPayload<?>[] payloads) {
        super(pos);
        this.managedId = managedId;
        blockEntityType = type;
        this.methodName = methodName;
        this.payloads = payloads;
    }

    public static PacketRPCMethodPayload of(IManaged managed, IRPCBlockEntity tile, String methodName, HolderLookup.Provider provider, Object... args) {
        var index = Arrays.stream(tile.getRootStorage().getManaged()).toList().indexOf(managed);
        if (index < 0) {
            throw new IllegalArgumentException("No such rpc managed: " + methodName);
        }
        var rpcMethod = tile.getRPCMethod(managed, methodName);
        if (rpcMethod == null) {
            throw new IllegalArgumentException("No such RPC method: " + methodName);
        }
        var payloads = rpcMethod.serializeArgs(args, provider);
        return new PacketRPCMethodPayload(index, tile.getBlockEntityType(), tile.getCurrentPos(), methodName, payloads);
    }

    public static void processPacket(@NotNull BlockEntity blockEntity, RPCSender sender, PacketRPCMethodPayload packet, HolderLookup.Provider provider) {
        if (blockEntity.getType() != packet.blockEntityType) {
            LDLib.LOGGER.warn("Block entity type mismatch in rpc payload packet!");
            return;
        }
        if (!(blockEntity instanceof IRPCBlockEntity tile)) {
            LDLib.LOGGER.error("Received managed payload packet for block entity that does not implement IRPCBlockEntity: " + blockEntity);
            return;
        }
        if (tile.getRootStorage().getManaged().length >= packet.managedId) {
            LDLib.LOGGER.error("Received managed couldn't be found in IRPCBlockEntity: " + blockEntity);
            return;
        }
        var rpcMethod = tile.getRPCMethod(tile.getRootStorage().getManaged()[packet.managedId], packet.methodName);
        if (rpcMethod == null) {
            LDLib.LOGGER.error("Cannot find RPC method: " + packet.methodName);
            return;
        }

        rpcMethod.invoke(tile, sender, packet.payloads, provider);

    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        super.write(buf);
        buf.writeVarInt(this.managedId);
        buf.writeResourceLocation(Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntityType)));
        buf.writeUtf(methodName);
        buf.writeVarInt(payloads.length);
        for (ITypedPayload<?> payload : payloads) {
            buf.writeByte(payload.getType());
            payload.writePayload(buf);
        }
    }

    public static PacketRPCMethodPayload decode(RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int managedId = buffer.readVarInt();

        BlockEntityType<?> blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(buffer.readResourceLocation());
        String methodName = buffer.readUtf();
        ITypedPayload<?>[] payloads = new ITypedPayload<?>[buffer.readVarInt()];
        for (int i = 0; i < payloads.length; i++) {
            byte id = buffer.readByte();
            var payload = TypedPayloadRegistries.create(id);
            payload.readPayload(buffer);
            payloads[i] = payload;
        }
        return new PacketRPCMethodPayload(managedId, blockEntityType, pos, methodName, payloads);
    }

    public static void execute(PacketRPCMethodPayload packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer) {
            executeServer(packet, context);
        } else {
            executeClient(packet, context);
        }
    }

    public static void executeClient(PacketRPCMethodPayload packet, IPayloadContext context) {
        if (context.player().level() == null) {
            return;
        }
        BlockEntity tile = context.player().level().getBlockEntity(packet.pos);
        if (tile == null) {
            return;
        }
        processPacket(tile, RPCSender.ofServer(), packet, context.player().registryAccess());
    }

    public static void executeServer(PacketRPCMethodPayload packet, IPayloadContext context) {
        var player = context.player();
        if (player == null) {
            LDLib.LOGGER.error("Received rpc payload packet from client with no player!");
            return;
        }
        var level = player.level();
        if (!level.isLoaded(packet.pos)) return;
        BlockEntity tile = level.getBlockEntity(packet.pos);
        if (tile == null) {
            return;
        }
        processPacket(tile, RPCSender.ofClient(player), packet, context.player().registryAccess());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
