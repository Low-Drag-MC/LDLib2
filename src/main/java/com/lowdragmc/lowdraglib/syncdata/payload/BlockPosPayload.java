package com.lowdragmc.lowdraglib.syncdata.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

public class BlockPosPayload extends ObjectTypedPayload<BlockPos> {

    @Override
    public void writePayload(FriendlyByteBuf buf) {
        buf.writeBlockPos(payload);
    }

    @Override
    public void readPayload(FriendlyByteBuf buf) {
        payload = buf.readBlockPos();
    }

    @Override
    public Tag serializeNBT() {
        return NbtUtils.writeBlockPos(payload);
    }

    @Override
    public void deserializeNBT(Tag tag) {
        payload = NbtUtils.readBlockPos((CompoundTag) tag);
    }

    @Override
    public Object copyForManaged(Object value) {
        if (value instanceof BlockPos) {
            return new BlockPos((BlockPos) value);
        }
        return super.copyForManaged(value);
    }
}
