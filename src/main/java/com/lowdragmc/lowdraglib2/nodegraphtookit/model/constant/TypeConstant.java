package com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.syncdata.SyncValueHolder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public class TypeConstant extends Constant {
    @Getter
    private Type type;
    @Getter
    @Nullable
    private Object value;
    @Nullable @Getter @Setter
    private Object defaultValue;

    public TypeConstant() {}

    @Override
    public void init(TypeHandle typeHandle) {
        super.init(typeHandle);
        this.type = TypeHandleHelpers.convertType(typeHandle.resolve());
    }

    @Override
    public void setValue(Object value) {
        this.value = value;
        if (owner != null) {
            var graphModel = owner.getGraphModel();
            if (graphModel != null) {
                graphModel.getCurrentGraphChangeDescription().addChangedModel(owner, ChangeHint.DATA);
                // If OwnerModel is a PortModel, the graph object will not be marked as dirty (since PortModels are not serialized).
                // Make sure the asset is marked as dirty so the changes to the Constant are saved.
                graphModel.setGraphObjectDirty();
            }
        }
    }

    @Override
    public TypeConstant copy() {
        var copy = new TypeConstant();
        copy.init(typeHandle);
        copy.value = value;
        copy.defaultValue = defaultValue;
        return copy;
    }

    // --- Static serialization helpers ---

    /**
     * Serializes a Constant to a CompoundTag.
     * Stores the type identification and the value using SyncValueHolder/IRef codec.
     */
    public static CompoundTag serializeConstant(Constant constant, HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        if (constant.getTypeHandle() != null) {
            tag.putString("type", constant.getTypeHandle().getIdentification());
        }
        if (constant.getValue() != null) {
            try {
                var holder = new SyncValueHolder<>("value", constant.getType(), constant.getValue());
                var ops = provider.createSerializationContext(NbtOps.INSTANCE);
                var serialized = holder.ref.readPersisted(ops);
                if (serialized instanceof Tag nbtTag) {
                    tag.put("value", nbtTag);
                }
            } catch (Exception e) {
                LDLib2.LOGGER.error("Failed to serialize constant value of type {}", constant.getType(), e);
            }
        }
        if (constant instanceof TypeConstant tc && tc.defaultValue != null) {
            try {
                var holder = new SyncValueHolder<>("defaultValue", constant.getType(), tc.defaultValue);
                var ops = provider.createSerializationContext(NbtOps.INSTANCE);
                var serialized = holder.ref.readPersisted(ops);
                if (serialized instanceof Tag nbtTag) {
                    tag.put("defaultValue", nbtTag);
                }
            } catch (Exception e) {
                LDLib2.LOGGER.error("Failed to serialize constant default value of type {}", constant.getType(), e);
            }
        }
        return tag;
    }

    /**
     * Deserializes a Constant from a CompoundTag.
     */
    @Nullable
    public static Constant deserializeConstant(CompoundTag tag, HolderLookup.Provider provider) {
        if (!tag.contains("type")) return null;
        var typeId = tag.getStringOr("type", "");
        var typeHandle = TypeHandle.create(typeId);

        var constant = new TypeConstant();
        constant.init(typeHandle);

        if (tag.contains("value")) {
            try {
                var holder = new SyncValueHolder<>("value", constant.getType(), null);
                var ops = provider.createSerializationContext(NbtOps.INSTANCE);
                holder.ref.writePersisted(ops, tag.get("value"));
                constant.value = holder.getValue();
            } catch (Exception e) {
                LDLib2.LOGGER.error("Failed to deserialize constant value of type {}", constant.getType(), e);
            }
        }
        if (tag.contains("defaultValue")) {
            try {
                var holder = new SyncValueHolder<>("defaultValue", constant.getType(), null);
                var ops = provider.createSerializationContext(NbtOps.INSTANCE);
                holder.ref.writePersisted(ops, tag.get("defaultValue"));
                constant.defaultValue = holder.getValue();
            } catch (Exception e) {
                LDLib2.LOGGER.error("Failed to deserialize constant default value of type {}", constant.getType(), e);
            }
        }
        return constant;
    }
}
