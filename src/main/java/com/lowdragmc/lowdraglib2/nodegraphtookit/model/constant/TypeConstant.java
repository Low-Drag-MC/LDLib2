package com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.syncdata.SyncValueHolder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
     * Types we've already warned about lacking both an {@code AccessorRegistries} entry and a
     * {@link Constant#getCustomCodec() custom codec}. Used to keep the warn log to one line per
     * unique type rather than spamming once per port instance.
     *
     * <p>Package-private for tests that want to assert warn-once semantics.</p>
     */
    static final Set<Type> WARNED_UNSERIALIZABLE_TYPES = ConcurrentHashMap.newKeySet();

    /** Test hook — resets the warn-once cache. */
    public static void clearWarnedTypesForTesting() {
        WARNED_UNSERIALIZABLE_TYPES.clear();
    }

    /** Test hook — read-only view of the warn-once cache. */
    public static Set<Type> getWarnedUnserializableTypesForTesting() {
        return java.util.Collections.unmodifiableSet(WARNED_UNSERIALIZABLE_TYPES);
    }

    /**
     * Serializes a Constant to a CompoundTag.
     *
     * <p>Branch order:</p>
     * <ol>
     *   <li>{@code constant.serializationEnabled == false} → only the type identifier is written.</li>
     *   <li>{@code constant.customCodec != null} → encoded via the codec.</li>
     *   <li>Otherwise the legacy {@link SyncValueHolder} / {@code AccessorRegistries} path,
     *       gracefully skipping (with a one-time warn) when no accessor is registered.</li>
     * </ol>
     *
     * <p>The whole method is try/catched at the outer level so a corrupt accessor or codec can
     * never crash a graph save — at worst the affected value is dropped from the tag.</p>
     */
    public static CompoundTag serializeConstant(Constant constant, HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        try {
            if (constant.getTypeHandle() != null) {
                tag.putString("type", constant.getTypeHandle().getIdentification());
            }
            if (!constant.isSerializationEnabled()) return tag;
            var ops = provider.createSerializationContext(NbtOps.INSTANCE);
            if (constant.getValue() != null) {
                encodeField(constant, ops, constant.getValue(), "value").ifPresent(t -> tag.put("value", t));
            }
            if (constant instanceof TypeConstant tc && tc.defaultValue != null) {
                encodeField(constant, ops, tc.defaultValue, "defaultValue").ifPresent(t -> tag.put("defaultValue", t));
            }
        } catch (Exception e) {
            LDLib2.LOGGER.error("Unrecoverable error serializing constant of type {}", safeTypeName(constant), e);
        }
        return tag;
    }

    /**
     * Phase-1 deserializer: produces a fresh {@link TypeConstant} from a tag. The result already
     * carries the right {@code deserializeFailed} flag, so callers that don't need a builder-aware
     * second pass ({@link ConstantNodeModel}, {@link VariableDeclarationModel}, and ports/options
     * whose builder has no extra state) can act on the constant directly.
     *
     * <p>"No builder state" here means: no custom codec, no {@code withoutSerialization()}, no
     * builder-supplied default value. In that scenario Phase 1's accessor decode produces the
     * same value Phase 2 would, so {@link NodeModel#updateConstantForInput} skips Phase 2 to
     * avoid redundant work.</p>
     *
     * <p>Wrapped in try/catch so a corrupt tag never throws past this call.</p>
     */
    @Nullable
    public static Constant deserializeConstant(CompoundTag tag, HolderLookup.Provider provider) {
        try {
            if (!tag.contains("type")) return null;
            var typeId = tag.getString("type");
            var typeHandle = TypeHandle.create(typeId);

            var constant = new TypeConstant();
            constant.init(typeHandle);

            // Freshly-init'd constants have serializationEnabled=true and customCodec=null. The
            // codec / disabled-serialization scenarios only materialise after the builder's
            // initializationCallback runs in Phase 2.
            if (!constant.isSerializationEnabled()) return constant;
            decodeFieldsInto(constant, provider, tag);
            return constant;
        } catch (Exception e) {
            LDLib2.LOGGER.error("Unrecoverable error deserializing constant", e);
            return null;
        }
    }

    /**
     * Phase-2 deserializer: re-decodes value / defaultValue from {@code tag} <em>into the
     * existing</em> {@code target} constant — at which point the builder's codec and
     * {@code serializationEnabled} flag have already been installed by
     * {@code initializationCallback}.
     *
     * <p>Only called by the port/option pipeline when the builder actually has state worth
     * applying (custom codec, {@code withoutSerialization}, or a builder-supplied default). For
     * stateless ports Phase 1's result is final.</p>
     *
     * <p>The provider is looked up via {@link Platform#getFrozenRegistry()} (the project's
     * standard fallback) — there's no reason to cache one passed in from phase 1.</p>
     */
    public static void deserializeIntoConstant(Constant target, CompoundTag tag) {
        target.setDeserializeFailed(false);
        // Builder said this port doesn't persist — the saved tag's value (if any) is intentionally
        // ignored. The constant keeps the builder default set by initializationCallback. Not a
        // failure.
        if (!target.isSerializationEnabled()) return;
        HolderLookup.Provider provider;
        try {
            provider = Platform.getFrozenRegistry();
        } catch (Exception e) {
            LDLib2.LOGGER.error("Could not obtain registry provider for constant decode", e);
            target.setDeserializeFailed(true);
            return;
        }
        if (provider == null) {
            LDLib2.LOGGER.error("Null registry provider — cannot decode constant of type {}", safeTypeName(target));
            target.setDeserializeFailed(true);
            return;
        }
        decodeFieldsInto(target, provider, tag);
    }

    /**
     * Decodes the {@code value} / {@code defaultValue} entries of {@code tag} into the given
     * Constant, honoring whatever codec / accessor the constant currently has installed. Sets
     * {@code deserializeFailed=true} on the constant if any present entry couldn't be decoded.
     * Used by both Phase 1 (fresh constant) and Phase 2 (existing constant with builder state).
     */
    private static void decodeFieldsInto(Constant target, HolderLookup.Provider provider, CompoundTag tag) {
        DynamicOps<Tag> ops;
        try {
            ops = provider.createSerializationContext(NbtOps.INSTANCE);
        } catch (Exception e) {
            LDLib2.LOGGER.error("Could not build serialization context for constant decode", e);
            target.setDeserializeFailed(true);
            return;
        }

        boolean failed = false;
        if (tag.contains("value")) {
            var decoded = tryDecodeField(target, ops, tag.get("value"), "value");
            if (decoded.isPresent()) {
                try {
                    target.setValue(decoded.get());
                } catch (Exception e) {
                    LDLib2.LOGGER.error("Failed applying decoded value to constant of type {}", safeTypeName(target), e);
                    failed = true;
                }
            } else {
                failed = true;
                maybeWarnUnserializable(target);
            }
        }
        if (tag.contains("defaultValue") && target instanceof TypeConstant tc) {
            var decoded = tryDecodeField(target, ops, tag.get("defaultValue"), "defaultValue");
            if (decoded.isPresent()) {
                try {
                    tc.setDefaultValue(decoded.get());
                } catch (Exception e) {
                    LDLib2.LOGGER.error("Failed applying decoded defaultValue to constant of type {}", safeTypeName(target), e);
                    failed = true;
                }
            } else {
                failed = true;
                maybeWarnUnserializable(target);
            }
        }
        if (failed) target.setDeserializeFailed(true);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Optional<Tag> encodeField(Constant constant, DynamicOps<Tag> ops, Object value, String fieldName) {
        var codec = constant.getCustomCodec();
        if (codec != null) {
            try {
                return ((Codec<Object>) (Codec) codec).encodeStart(ops, value)
                        .resultOrPartial(err -> LDLib2.LOGGER.error("Failed to encode constant {} of type {}: {}", fieldName, safeTypeName(constant), err));
            } catch (Exception e) {
                LDLib2.LOGGER.error("Failed to encode constant {} of type {}", fieldName, safeTypeName(constant), e);
                return Optional.empty();
            }
        }
        if (AccessorRegistries.findByTypeOrNull(constant.getType()) == null) {
            warnUnserializableOnce(constant.getType());
            return Optional.empty();
        }
        try {
            var holder = new SyncValueHolder<>(fieldName, constant.getType(), value);
            var serialized = holder.ref.readPersisted(ops);
            return serialized instanceof Tag nbtTag ? Optional.of(nbtTag) : Optional.empty();
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to serialize constant {} of type {}", fieldName, safeTypeName(constant), e);
            return Optional.empty();
        }
    }

    /**
     * Quiet decode — does NOT warn or set failure flags. Used by both phase 1 (best-effort) and
     * phase 2; phase 2 inspects the empty-Optional to decide whether to mark the constant as
     * failed and whether to emit the warn-once.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Optional<Object> tryDecodeField(Constant constant, DynamicOps<Tag> ops, Tag tag, String fieldName) {
        if (tag == null) return Optional.empty();
        var codec = constant.getCustomCodec();
        if (codec != null) {
            try {
                return ((Codec<Object>) (Codec) codec).parse(ops, tag)
                        .resultOrPartial(err -> LDLib2.LOGGER.error("Failed to decode constant {} of type {}: {}", fieldName, safeTypeName(constant), err))
                        .map(v -> (Object) v);
            } catch (Exception e) {
                LDLib2.LOGGER.error("Failed to decode constant {} of type {}", fieldName, safeTypeName(constant), e);
                return Optional.empty();
            }
        }
        if (AccessorRegistries.findByTypeOrNull(constant.getType()) == null) {
            return Optional.empty();
        }
        try {
            var holder = new SyncValueHolder<>(fieldName, constant.getType(), null);
            holder.ref.writePersisted(ops, tag);
            return Optional.ofNullable(holder.getValue());
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to deserialize constant {} of type {}", fieldName, safeTypeName(constant), e);
            return Optional.empty();
        }
    }

    /** Warn-once iff the constant has no decoder available (no codec, no accessor). */
    private static void maybeWarnUnserializable(Constant constant) {
        if (constant.getCustomCodec() != null) return; // codec is set — failure is parse-error, not missing-decoder
        var type = constant.getType();
        if (type == null) return;
        if (AccessorRegistries.findByTypeOrNull(type) != null) return; // accessor exists — failure is parse-error
        warnUnserializableOnce(type);
    }

    private static void warnUnserializableOnce(Type type) {
        if (WARNED_UNSERIALIZABLE_TYPES.add(type)) {
            LDLib2.LOGGER.warn("No serializer registered for {} — port/option values of this type will not persist. Provide a Codec via withCodec() or disable serialization with withoutSerialization().", type.getTypeName());
        }
    }

    private static String safeTypeName(Constant constant) {
        var type = constant.getType();
        return type == null ? "<unknown>" : type.getTypeName();
    }
}
