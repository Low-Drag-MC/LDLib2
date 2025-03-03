package com.lowdragmc.lowdraglib.syncdata.accessor;

import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.NbtTagPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.joml.Quaternionf;

public class QuaternionfAccessor extends CustomObjectAccessor<Quaternionf>{

    public QuaternionfAccessor() {
        super(Quaternionf.class, true);
    }

    @Override
    public ITypedPayload<?> serialize(AccessorOp op, Quaternionf value, HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("x", value.x);
        tag.putFloat("y", value.y);
        tag.putFloat("z", value.z);
        tag.putFloat("w", value.w);
        return NbtTagPayload.of(tag);
    }

    @Override
    public Quaternionf deserialize(AccessorOp op, ITypedPayload<?> payload, HolderLookup.Provider provider) {
        if (payload instanceof NbtTagPayload nbtTagPayload) {
            var tag = (CompoundTag)nbtTagPayload.getPayload();
            return new Quaternionf(tag.getFloat("x"), tag.getFloat("y"), tag.getFloat("z"), tag.getFloat("w"));
        }
        return null;
    }
}
