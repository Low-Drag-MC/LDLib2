package com.lowdragmc.lowdraglib.syncdata.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class BlockPosPayload extends ObjectTypedPayload<BlockPos> {

    @Override
    public void writePayload(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(payload);
    }

    @Override
    public void readPayload(RegistryFriendlyByteBuf buf) {
        payload = buf.readBlockPos();
    }

    @Override
    public Tag serializeNBT(HolderLookup.Provider provider) {
        return NbtUtils.writeBlockPos(payload);
    }

    @Override
    public void deserializeNBT(Tag tag, HolderLookup.Provider provider) {
        if (tag instanceof IntArrayTag ints) {
            payload = ints.size() == 3 ? new BlockPos(ints.get(0).getAsInt(), ints.get(1).getAsInt(), ints.get(2).getAsInt()) : null;
        } else {
            payload = BlockPos.ZERO;
        }
    }

    @Override
    public Object copyForManaged(Object value) {
        if (value instanceof BlockPos) {
            return new BlockPos((BlockPos) value);
        }
        return super.copyForManaged(value);
    }
}
