package com.lowdragmc.lowdraglib.syncdata.managed;

import com.lowdragmc.lowdraglib.syncdata.accessor.IReadOnlyAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;

/**
 * ReadonlyRef represents a reference to a nonnull value, the value is readonly and the instance won't change.
 *  <br>
 *  It will store the old value mark to compare with the new value mark every update.
 *  Please implement {@link IMarkFunction} for the accessor.
 *  If the {@link IMarkFunction} is not implemented, it will use codec to store the mark in a type of {@link com.mojang.serialization.JavaOps}
 */
@SuppressWarnings("unchecked")
public class ReadonlyRef<TYPE> extends Ref {
    private final ReadOnlyDirectField<TYPE> field;
    private @Nullable Object oldValueMark;
    private @Nullable CompoundTag oldUid;

    public ReadonlyRef(ReadOnlyDirectField<TYPE> field, ManagedKey key, IReadOnlyAccessor<TYPE> accessor) {
        super(key, accessor);
        this.field = field;
        var value = field.value();
        if (value != null) {
            if (!isReadOnlyManaged() || !field.hasDirtyMethod()) {
                this.oldValueMark = accessor instanceof IMarkFunction markFunction ?
                        markFunction.obtainManagedMark(value) :
                        accessor.readReadOnlyValue(JavaOps.INSTANCE, value);
            }
            if (isReadOnlyManaged()) {
                oldUid = field.serializeUid(value);
            }
        } else {
            if (!isReadOnlyManaged()) {
                throw new IllegalStateException("The read only value is null, it should not be null!");
            } else {
                oldValueMark = null;
                oldUid = null;
            }
        }
    }

    public boolean isReadOnlyManaged() {
        return getKey().isReadOnlyManaged();
    }

    @Override
    public IReadOnlyAccessor<TYPE> getAccessor() {
        return (IReadOnlyAccessor<TYPE>) super.getAccessor();
    }

    @Override
    public @Nullable TYPE readRaw() {
        return field.value();
    }

    @Override
    public final void update() {
        if (isReadOnlyManaged()) {
            readOnlyManagedUpdate();
        } else {
            readOnlyUpdate();
        }
    }

    public void readOnlyManagedUpdate() {
        var newValue = readRaw();
        if ((oldUid == null && newValue != null) || (oldUid != null && newValue == null)) {
            markAsDirty();
        }
        if (newValue != null) {
            var newUid = field.serializeUid(newValue);
            if (newUid.equals(oldUid)) {
                if (field.hasDirtyMethod()) {
                    if (field.isDirty(newValue)) {
                        markAsDirty();
                    }
                } else {
                    readOnlyUpdate();
                }
            } else {
                markAsDirty();
                oldUid = newUid;
            }
        } else {
            oldUid = null;
        }
    }

    public void readOnlyUpdate() {
        var value = readRaw();
        if (value == null) {
            throw new IllegalStateException("The read only value is null, it should not be null!");
        }
        var accessor = getAccessor();
        if (accessor instanceof IMarkFunction markFunction) {
            if (markFunction.areDifferent(oldValueMark, value)) {
                oldValueMark = markFunction.obtainManagedMark(value);
                markAsDirty();
            }
        } else {
            var newValueMark = accessor.readReadOnlyValue(JavaOps.INSTANCE, value);
            if (!oldValueMark.equals(newValueMark)) {
                oldValueMark = newValueMark;
                markAsDirty();
            }
        }
    }

    @Override
    public final void readSyncToStream(RegistryFriendlyByteBuf buffer) {
        if (isReadOnlyManaged()) {
            var value = readRaw();
            if (value == null) {
                buffer.writeBoolean(true);
            } else {
                buffer.writeBoolean(false);
                buffer.writeNbt(field.serializeUid(value));
                readReadOnlySyncToStream(buffer);
            }
        } else {
            readReadOnlySyncToStream(buffer);
        }
    }

    public void readReadOnlySyncToStream(RegistryFriendlyByteBuf buffer) {
        super.readSyncToStream(buffer);
    }

    @Override
    public final void writeSyncFromStream(RegistryFriendlyByteBuf buffer) {
        if (isReadOnlyManaged()) {
            if (buffer.readBoolean()) {
                field.set(null);
            } else {
                var uid = buffer.readNbt();
                var value = readRaw();
                if (value == null || !field.serializeUid(value).equals(uid)) {
                    value = (TYPE) field.deserializeUid(uid);
                    field.set(value);
                }
                writeReadOnlySyncFromStream(buffer);
            }
        } else {
            writeReadOnlySyncFromStream(buffer);
        }
    }

    public void writeReadOnlySyncFromStream(RegistryFriendlyByteBuf buffer) {
        super.writeSyncFromStream(buffer);
    }


    @Override
    public final <T> T readSync(DynamicOps<T> op) {
        if (isReadOnlyManaged()) {
            var value = readRaw();
            if (value == null) {
                return op.empty();
            }
            return op.mapBuilder()
                    .add("uid", NbtOps.INSTANCE.convertMap(op, field.serializeUid(value)))
                    .add("payload", readReadOnlySync(op))
                    .build(op.empty()).getOrThrow();
        } else {
            return readReadOnlySync(op);
        }
    }

    public <T> T readReadOnlySync(DynamicOps<T> op) {
        return super.readSync(op);
    }

    @Override
    public final <T> void writeSync(DynamicOps<T> op, T payload) {
        if (isReadOnlyManaged()) {
            if (payload == op.empty()) {
                field.set(null);
            } else {
                var uid = op.get(payload, "uid").result().map(data -> op.convertTo(NbtOps.INSTANCE, data)).map(CompoundTag.class::cast).orElseThrow();
                var value = readRaw();
                if (value == null || !field.serializeUid(value).equals(uid)) {
                    value = (TYPE) field.deserializeUid(uid);
                    field.set(value);
                }
                writeReadOnlySync(op, op.get(payload, "payload").result().orElse(op.empty()));
            }
        } else {
            writeReadOnlySync(op, payload);
        }
    }

    public <T> void writeReadOnlySync(DynamicOps<T> op, T payload) {
        super.writeSync(op, payload);
    }

    @Override
    public final <T> T readPersisted(DynamicOps<T> op) {
        if (isReadOnlyManaged()) {
            var value = readRaw();
            if (value == null) {
                return op.empty();
            }
            return op.mapBuilder()
                    .add("uid", NbtOps.INSTANCE.convertMap(op, field.serializeUid(value)))
                    .add("payload", readReadOnlyPersisted(op))
                    .build(op.empty()).getOrThrow();
        } else {
            return readReadOnlyPersisted(op);
        }
    }

    public <T> T readReadOnlyPersisted(DynamicOps<T> op) {
        return super.readPersisted(op);
    }

    @Override
    public final <T> void writePersisted(DynamicOps<T> op, T payload) {
        if (isReadOnlyManaged()) {
            if (payload == op.empty()) {
                field.set(null);
            } else {
                var uid = op.get(payload, "uid").result().map(data -> op.convertTo(NbtOps.INSTANCE, data)).map(CompoundTag.class::cast).orElseThrow();
                var value = readRaw();
                if (value == null || !field.serializeUid(value).equals(uid)) {
                    value = (TYPE) field.deserializeUid(uid);
                    field.set(value);
                }
                writeReadOnlyPersisted(op, op.get(payload, "payload").result().orElse(op.empty()));
            }
        } else {
            writeReadOnlyPersisted(op, payload);
        }
    }

    public <T> void writeReadOnlyPersisted(DynamicOps<T> op, T payload) {
        super.writePersisted(op, payload);
    }


}
