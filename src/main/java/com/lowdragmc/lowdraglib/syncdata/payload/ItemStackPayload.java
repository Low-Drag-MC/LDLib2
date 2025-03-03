package com.lowdragmc.lowdraglib.syncdata.payload;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class ItemStackPayload extends ObjectTypedPayload<ItemStack> {

    @Override
    public void writePayload(RegistryFriendlyByteBuf buf) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload);
    }

    @Override
    public void readPayload(RegistryFriendlyByteBuf buf) {
        payload = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
    }

    @Override
    public Tag serializeNBT(HolderLookup.Provider provider) {
        return payload.save(provider, new CompoundTag());
    }

    @Override
    public void deserializeNBT(Tag tag, HolderLookup.Provider provider) {
        payload = ItemStack.parse(provider, tag).orElse(ItemStack.EMPTY);
    }

    @Override
    public Object copyForManaged(Object value) {
        if (value instanceof ItemStack) {
            return ((ItemStack) value).copy();
        }
        return super.copyForManaged(value);
    }
}

