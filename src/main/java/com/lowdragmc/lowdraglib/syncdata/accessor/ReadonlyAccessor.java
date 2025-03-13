package com.lowdragmc.lowdraglib.syncdata.accessor;

import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries;
import com.lowdragmc.lowdraglib.syncdata.managed.IRef;
import com.lowdragmc.lowdraglib.syncdata.managed.ReadOnlyManagedRef;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.NbtTagPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.PrimitiveTypedPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public abstract class ReadonlyAccessor implements IAccessor {

    private byte defaultType = -1;

    @Override
    public byte getDefaultType() {
        return defaultType;
    }

    @Override
    public void setDefaultType(byte defaultType) {
        this.defaultType = defaultType;
    }

    public abstract ITypedPayload<?> readFromReadonlyField(AccessorOp op, Object obj, HolderLookup.Provider provider);

    public abstract void writeToReadonlyField(AccessorOp op, Object obj, ITypedPayload<?> payload, HolderLookup.Provider provider);

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public ITypedPayload<?> readField(AccessorOp op, IRef field, HolderLookup.Provider provider) {
        var obj = field.readRaw();
        if (field instanceof ReadOnlyManagedRef managedRef) {
            if (obj == null) {
                return PrimitiveTypedPayload.NullPayload.ofNull();
            } else {
                var tag = new CompoundTag();
                tag.put("uid", managedRef.getReadOnlyField().serializeUid(obj));

                var payloadTag = new CompoundTag();
                var payload = readFromReadonlyField(op, obj, provider);
                payloadTag.putByte("t", payload.getType());
                var data = payload.serializeNBT(provider);
                if (data != null) {
                    payloadTag.put("d", data);
                }
                tag.put("payload", payloadTag);
                return NbtTagPayload.of(tag);
            }
        } else if (obj == null) {
            throw new IllegalArgumentException("readonly field %s has a null reference".formatted(field.getKey()));
        }
        return readFromReadonlyField(op, obj, provider);
    }

    @Override
    public void writeField(AccessorOp op, IRef field, ITypedPayload<?> payload, HolderLookup.Provider provider) {
        var obj = field.readRaw();
        if (field instanceof ReadOnlyManagedRef managedRef) {
            var readOnlyField = managedRef.getReadOnlyField();
            if(payload instanceof PrimitiveTypedPayload<?> primitive && primitive.isNull()) {
                readOnlyField.set(null);
            } else if (payload instanceof NbtTagPayload nbtTagPayload && nbtTagPayload.getPayload() instanceof CompoundTag tag) {
                var uid = tag.getCompound("uid");
                if (obj == null || !readOnlyField.serializeUid(obj).equals(uid)) { // need to update obj
                    obj = managedRef.getReadOnlyField().deserializeUid(uid);
                    readOnlyField.set(obj);
                }
                var payloadTag = tag.getCompound("payload");
                byte id = payloadTag.getByte("t");
                var p = TypedPayloadRegistries.create(id);
                p.deserializeNBT(payloadTag.get("d"), provider);
                writeToReadonlyField(op, obj, p, provider);
            }
            return;
        } else if (obj == null) {
            throw new IllegalArgumentException("readonly field %s has a null reference".formatted(field));
        }
        writeToReadonlyField(op, obj, payload, provider);
    }

}
