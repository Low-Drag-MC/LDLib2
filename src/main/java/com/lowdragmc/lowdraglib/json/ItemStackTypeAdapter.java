package com.lowdragmc.lowdraglib.json;

import com.google.gson.*;
import com.lowdragmc.lowdraglib.Platform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Type;

public class ItemStackTypeAdapter implements JsonDeserializer<ItemStack>, JsonSerializer<ItemStack> {

    public static final ItemStackTypeAdapter INSTANCE = new ItemStackTypeAdapter();

    private ItemStackTypeAdapter() { }

    @Override
    public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            return ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, Platform.getFrozenRegistry()), TagParser.parseTag(json.getAsString())).result().orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.save(Platform.getFrozenRegistry(), new CompoundTag()).toString());
    }
}
