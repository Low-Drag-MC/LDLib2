package com.lowdragmc.lowdraglib2.syncdata.ref;

import com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib2.syncdata.var.IReadOnlyManagedVar;
import com.lowdragmc.lowdraglib2.syncdata.var.ReadOnlyVar;
import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.mojang.serialization.DynamicOps;
import lombok.Getter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;

import javax.annotation.Nullable;

public abstract class ReadOnlyManagedRef<TYPE> extends Ref<TYPE> {
    @Getter
    private final ReadOnlyVar<TYPE> readOnlyVar;
    protected @Nullable Tag oldUid;

    protected ReadOnlyManagedRef(ReadOnlyVar<TYPE> readOnlyVar, ManagedKey key, IAccessor<TYPE> accessor) {
        super(key, accessor);
        this.readOnlyVar = readOnlyVar;
    }

    /**
     * Check if the var is a read-only managed var. If it is, the instance of the value can be changed internal via {@link IReadOnlyManagedVar}.
     */
    public boolean isReadOnlyManaged() {
        return getReadOnlyVar().isReadOnlyManaged();
    }

    @Override
    public TYPE readRaw() {
        return getReadOnlyVar().value();
    }

    @Override
    public final void update() {
        if (isReadOnlyManaged()) {
            readOnlyManagedUpdate();
        } else {
            readOnlyUpdate();
        }
    }

    /**
     * Update the value of the read-only var while the var is not a read-only managed var.
     */
    public abstract void readOnlyUpdate();

    public void readOnlyManagedUpdate() {
        var newValue = readRaw();
        if ((oldUid == null && newValue != null) || (oldUid != null && newValue == null)) {
            markAsDirty();
        }
        if (newValue != null) {
            var field = getReadOnlyVar();
            var managedVar = field.getManagedVar();
            assert managedVar != null;
            var newUid = managedVar.serializeUid(newValue);
            if (newUid.equals(oldUid)) {
                if (managedVar.hasDirtyChecker()) {
                    if (managedVar.checkIsDirty()) {
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

    @Override
    public final void readSyncToStream(RegistryFriendlyByteBuf buffer) {
        if (isReadOnlyManaged()) {
            assert getReadOnlyVar().getManagedVar() != null;
            var value = readRaw();
            if (value == null) {
                buffer.writeBoolean(true);
            } else {
                buffer.writeBoolean(false);
                buffer.writeNbt(getReadOnlyVar().getManagedVar().serializeUid(value));
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
            var field = getReadOnlyVar();
            assert field.getManagedVar() != null;
            if (buffer.readBoolean()) {
                field.set(null);
            } else {
                var uid = buffer.readNbt();
                var value = readRaw();
                var managedVar = field.getManagedVar();
                if (value == null || !managedVar.serializeUid(value).equals(uid)) {
                    value = managedVar.deserializeUid(uid);
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
    public final <T> T readInitialSync(DynamicOps<T> op) {
        if (isReadOnlyManaged()) {
            var value = readRaw();
            if (value == null) {
                return LDLibExtraCodecs.createStringNull(op);
            }
            var field = getReadOnlyVar();
            assert field.getManagedVar() != null;
            return op.mapBuilder()
                    .add("uid", NbtOps.INSTANCE.convertTo(op, field.getManagedVar().serializeUid(value)))
                    .add("payload", readReadOnlySync(op))
                    .build(op.empty()).getOrThrow();
        } else {
            return readReadOnlySync(op);
        }
    }

    public  <T> T readReadOnlySync(DynamicOps<T> op) {
        return super.readInitialSync(op);
    }

    @Override
    public final <T> void writeInitialSync(DynamicOps<T> op, T payload) {
        if (isReadOnlyManaged()) {
            var field = getReadOnlyVar();
            assert field.getManagedVar() != null;
            if (LDLibExtraCodecs.isEmptyOrStringNull(op, payload)) {
                field.set(null);
            } else {
                var uid = op.get(payload, "uid").result().map(data -> op.convertTo(NbtOps.INSTANCE, data)).orElseThrow();
                var value = readRaw();
                var managedVar = field.getManagedVar();
                if (value == null || !managedVar.serializeUid(value).equals(uid)) {
                    value = managedVar.deserializeUid(uid);
                    field.set(value);
                }
                writeReadOnlySync(op, op.get(payload, "payload").result().orElse(op.empty()));
            }
        } else {
            writeReadOnlySync(op, payload);
        }
    }

    public <T> void writeReadOnlySync(DynamicOps<T> op, T payload) {
        super.writeInitialSync(op, payload);
    }

    @Override
    public final <T> T readPersisted(DynamicOps<T> op) {
        if (isReadOnlyManaged()) {
            var field = getReadOnlyVar();
            assert field.getManagedVar() != null;
            var value = readRaw();
            if (value == null) {
                return LDLibExtraCodecs.createStringNull(op);
            }
            return op.mapBuilder()
                    .add("uid", NbtOps.INSTANCE.convertTo(op, field.getManagedVar().serializeUid(value)))
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
            var field = getReadOnlyVar();
            assert field.getManagedVar() != null;
            if (LDLibExtraCodecs.isEmptyOrStringNull(op, payload)) {
                field.set(null);
            } else {
                var uid = op.get(payload, "uid").result().map(data -> op.convertTo(NbtOps.INSTANCE, data)).orElseThrow();
                var value = readRaw();
                var managedVar = field.getManagedVar();
                if (value == null || !managedVar.serializeUid(value).equals(uid)) {
                    value = managedVar.deserializeUid(uid);
                    field.set(value);
                }
                writeReadOnlyPersisted(op, op.get(payload, "payload").result().orElse(op.empty()));
            }
        } else {
            writeReadOnlyPersisted(op, payload);
        }
    }

    public  <T> void writeReadOnlyPersisted(DynamicOps<T> op, T payload) {
        super.writePersisted(op, payload);
    }
}
