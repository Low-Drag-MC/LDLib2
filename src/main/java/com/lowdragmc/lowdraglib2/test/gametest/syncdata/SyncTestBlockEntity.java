package com.lowdragmc.lowdraglib2.test.gametest.syncdata;

import com.lowdragmc.lowdraglib2.CommonProxy;
import com.lowdragmc.lowdraglib2.syncdata.annotation.*;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SyncTestBlockEntity extends BlockEntity implements ISyncPersistRPCBlockEntity {
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @DescSynced
    @ManagedCodec("anchorCodec")
    public BlockPos anchor = BlockPos.ZERO;
    @Persisted
    @DescSynced
    @ManagedCodec(value = "labelCodec", streamCodec = "labelStream")
    public String label = "";
    @Persisted
    @DescSynced
    @ManagedCodec("tagsCodec")
    public List<String> codecTags = new ArrayList<>();
    @Persisted
    @DescSynced
    @ManagedCodec("anchorCodec")
    @Nullable
    public BlockPos maybeAnchor;

    @Persisted
    @DescSynced
    @UpdateListener(methodName = "onCounterChanged")
    public int counter;
    @Persisted
    @DescSynced
    public ItemStack stack = ItemStack.EMPTY;
    @Persisted
    @DescSynced
    public List<BlockPos> positions = new ArrayList<>();
    @Persisted
    @DescSynced
    public Map<String, Integer> scores = new HashMap<>();
    @DescSynced
    @ConditionalSynced(methodName = "shouldSyncGated")
    public int gated;
    @DescSynced
    @LazyManaged
    public int lazyValue;
    @Persisted
    public int serverOnly;

    public final List<String> counterChanges = new ArrayList<>();
    public String lastRpc = "";

    public SyncTestBlockEntity(BlockPos pos, BlockState state) {
        super(CommonProxy.SYNC_TEST_BE_TYPE.get(), pos, state);
    }

    protected Codec<BlockPos> anchorCodec() {
        return Codec.LONG.xmap(BlockPos::of, BlockPos::asLong);
    }

    private static Codec<String> labelCodec() {
        return Codec.STRING;
    }

    // upper-cases on decode, so a test can tell a sync packet's label from the codec's
    private static StreamCodec<ByteBuf, String> labelStream() {
        return ByteBufCodecs.STRING_UTF8.map(s -> s.toUpperCase(Locale.ROOT), s -> s);
    }

    private static Codec<List<String>> tagsCodec() {
        return Codec.STRING.listOf().xmap(ArrayList::new, list -> list);
    }

    private void onCounterChanged(int oldValue, int newValue) {
        counterChanges.add(oldValue + "->" + newValue);
    }

    private boolean shouldSyncGated(int value) {
        return value > 0;
    }

    @RPCMethod
    public void rpcPing(RPCSender sender, String message) {
        var player = sender.asPlayer();
        lastRpc = (player == null ? "server" : player.getGameProfile().name()) + ":" + message;
    }

    public String describeSynced() {
        return "anchor=" + anchor.toShortString()
                + " tags=" + codecTags
                + " maybeAnchor=" + (maybeAnchor == null ? "null" : maybeAnchor.toShortString())
                + " counter=" + counter
                + " stack=" + BuiltInRegistries.ITEM.getKey(stack.getItem()) + "x" + stack.getCount()
                + " positions=" + positions.stream().map(BlockPos::toShortString).toList()
                + " scores=" + new TreeMap<>(scores);
    }
}
