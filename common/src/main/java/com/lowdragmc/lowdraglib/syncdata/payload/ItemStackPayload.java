package com.lowdragmc.lowdraglib.syncdata.payload;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class ItemStackPayload extends ObjectTypedPayload<ItemStack> {

    @Override
    public void writePayload(FriendlyByteBuf buf) {
        buf.writeItem(payload);
    }

    @Override
    public void readPayload(FriendlyByteBuf buf) {
        payload = buf.readItem();
    }

    @Override
    public Tag serializeNBT() {
        return payload.save(new CompoundTag());
    }

    @Override
    public void deserializeNBT(Tag tag) {
        payload = ItemStack.of((CompoundTag) tag);
    }

    @Override
    public Object copyForManaged(Object value) {
        if (value instanceof ItemStack) {
            return ((ItemStack) value).copy();
        }
        return super.copyForManaged(value);
    }
}

