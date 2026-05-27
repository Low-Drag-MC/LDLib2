package com.lowdragmc.lowdraglib2.syncdata.accessor.maplike;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.IMarkFunction;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.IDirectAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.readonly.IReadOnlyAccessor;
import com.lowdragmc.lowdraglib2.syncdata.utils.TypeFabricator;
import com.lowdragmc.lowdraglib2.syncdata.var.ManagedHolderVar;
import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MapAccessor<K, V> implements
        IReadOnlyAccessor<Map<K, V>>,
        IMapLikeAccessor<K, V, Map<K, V>>,
        IMarkFunction<Map<K, V>, Object[]> {

    @Getter private final IAccessor<K> keyAccessor;
    @Getter private final Class<K> keyType;
    @Getter private final IAccessor<V> valueAccessor;
    @Getter private final Class<V> valueType;

    public MapAccessor(IAccessor<K> keyAccessor, Class<K> keyType,
                       IAccessor<V> valueAccessor, Class<V> valueType) {
        this.keyAccessor = keyAccessor;
        this.keyType = keyType;
        this.valueAccessor = valueAccessor;
        this.valueType = valueType;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean test(Class<?> type) {
        return Map.class.isAssignableFrom(type);
    }

    // ---------- DynamicOps serialization ----------
    // Wire format: flat list of [k0, v0, k1, v1, ...].

    @Override
    public <T> T readReadOnlyValue(DynamicOps<T> op, @NotNull Map<K, V> value) {
        Stream<T> entries = value.entrySet().stream().flatMap(entry -> Stream.of(
                serializeChild(op, keyAccessor, entry.getKey(), "key"),
                serializeChild(op, valueAccessor, entry.getValue(), "value")
        ));
        return op.createList(entries);
    }

    @Override
    public <T> void writeReadOnlyValue(DynamicOps<T> op, Map<K, V> value, T payload) {
        var list = op.getStream(payload).getOrThrow().toList();
        if ((list.size() & 1) != 0) {
            throw new IllegalArgumentException(
                    "Map payload must contain an even number of [k,v] elements, got " + list.size());
        }
        int pairs = list.size() / 2;

        boolean kDirect = keyAccessor instanceof IDirectAccessor;
        boolean vDirect = valueAccessor instanceof IDirectAccessor;
        if (!kDirect && !(keyAccessor instanceof IReadOnlyAccessor)) {
            throw new IllegalArgumentException("Key accessor not managed: " + keyAccessor);
        }
        if (!vDirect && !(valueAccessor instanceof IReadOnlyAccessor)) {
            throw new IllegalArgumentException("Value accessor not managed: " + valueAccessor);
        }

        if (kDirect && vDirect) {
            // Both direct: clear and rebuild from scratch.
            value.clear();
            for (int i = 0; i < pairs; i++) {
                K k = deserializeForCreate(op, keyAccessor, keyType, list.get(i * 2), "key");
                V v = deserializeForCreate(op, valueAccessor, valueType, list.get(i * 2 + 1), "value");
                value.put(k, v);
            }
            return;
        }

        @SuppressWarnings("unchecked")
        IReadOnlyAccessor<V> roVAccessor = vDirect ? null : (IReadOnlyAccessor<V>) valueAccessor;

        // K direct + V read-only: support auto-fabrication of fresh V instances when structure differs.
        // Existing V instances are mutated in-place when their K is still present (preserves identity);
        // entries whose K disappeared are dropped; entries whose K is new get a fabricated V.
        if (kDirect && !vDirect) {
            var payloadEntries = new LinkedHashMap<K, T>(pairs);
            for (int i = 0; i < pairs; i++) {
                K k = deserializeForCreate(op, keyAccessor, keyType, list.get(i * 2), "key");
                payloadEntries.put(k, list.get(i * 2 + 1));
            }
            value.keySet().removeIf(k -> !payloadEntries.containsKey(k));
            @SuppressWarnings("unchecked")
            Supplier<V> vFabricator = (Supplier<V>) TypeFabricator.fabricator(valueType);
            for (var entry : payloadEntries.entrySet()) {
                K k = entry.getKey();
                T vPayload = entry.getValue();
                if (LDLibExtraCodecs.isEmptyOrStringNull(op, vPayload)) {
                    throw new IllegalArgumentException("Null value in read-only-V map payload");
                }
                V existing = value.get(k);
                if (existing == null) {
                    if (vFabricator == null) {
                        throw new IllegalArgumentException("No existing entry for key " + k
                                + " in read-only-V map and value type " + valueType.getName()
                                + " has no accessible no-arg constructor and is not a known interface (List/Set/Map/Queue/Deque)."
                                + " Add a no-arg ctor or use @ReadOnlyManaged.");
                    }
                    existing = vFabricator.get();
                    value.put(k, existing);
                }
                roVAccessor.writeReadOnlyValue(op, existing, vPayload);
            }
            return;
        }

        // K read-only (with V direct or read-only): structure must already match (use @ReadOnlyManaged
        // on the field to rebuild structure when keys change). Match entries by serialized K.
        if (pairs != value.size()) {
            throw new IllegalArgumentException(
                    "Stream entry count " + pairs + " != map size " + value.size()
                            + " for read-only-K map; use @ReadOnlyManaged on the field"
                            + " to rebuild structure when keys change");
        }
        for (int i = 0; i < pairs; i++) {
            T kPayload = list.get(i * 2);
            T vPayload = list.get(i * 2 + 1);
            K matched = findExistingKey(op, value, kPayload);
            if (matched == null && !value.containsKey(null)) {
                throw new IllegalArgumentException("No existing entry for key payload " + kPayload);
            }
            if (vDirect) {
                // Direct V: replace value at this K.
                V newV = deserializeForCreate(op, valueAccessor, valueType, vPayload, "value");
                value.put(matched, newV);
            } else {
                V existing = value.get(matched);
                if (existing == null) {
                    throw new IllegalArgumentException("Existing value is null for key " + matched + " in read-only-V map");
                }
                if (LDLibExtraCodecs.isEmptyOrStringNull(op, vPayload)) {
                    throw new IllegalArgumentException("Null value in read-only-V map payload");
                }
                roVAccessor.writeReadOnlyValue(op, existing, vPayload);
            }
        }
    }

    // ---------- Network buffer serialization ----------

    @Override
    public void readReadOnlyValueToStream(RegistryFriendlyByteBuf buffer, @NotNull Map<K, V> value) {
        buffer.writeVarInt(value.size());
        for (var entry : value.entrySet()) {
            writeChildToStream(buffer, keyAccessor, entry.getKey(), "key");
            writeChildToStream(buffer, valueAccessor, entry.getValue(), "value");
        }
    }

    @Override
    public void writeReadOnlyValueFromStream(RegistryFriendlyByteBuf buffer, @NotNull Map<K, V> value) {
        int size = buffer.readVarInt();

        boolean kDirect = keyAccessor instanceof IDirectAccessor;
        boolean vDirect = valueAccessor instanceof IDirectAccessor;
        if (!kDirect && !(keyAccessor instanceof IReadOnlyAccessor)) {
            throw new IllegalArgumentException("Key accessor not managed: " + keyAccessor);
        }
        if (!vDirect && !(valueAccessor instanceof IReadOnlyAccessor)) {
            throw new IllegalArgumentException("Value accessor not managed: " + valueAccessor);
        }

        if (kDirect && vDirect) {
            value.clear();
            for (int i = 0; i < size; i++) {
                K k = readChildFromStreamForCreate(buffer, keyAccessor, keyType, "key");
                V v = readChildFromStreamForCreate(buffer, valueAccessor, valueType, "value");
                value.put(k, v);
            }
            return;
        }

        if (kDirect) {
            // K direct, V read-only: diff-and-rebuild with auto-fabrication.
            // Existing V instances whose K is still in the stream are mutated in place; entries
            // whose K disappeared are dropped; entries whose K is new get a fabricated V.
            @SuppressWarnings("unchecked")
            IReadOnlyAccessor<V> roVAccessor = (IReadOnlyAccessor<V>) valueAccessor;
            @SuppressWarnings("unchecked")
            Supplier<V> vFabricator = (Supplier<V>) TypeFabricator.fabricator(valueType);
            var seen = new HashSet<K>(size);
            for (int i = 0; i < size; i++) {
                K k = readChildFromStreamForCreate(buffer, keyAccessor, keyType, "key");
                seen.add(k);
                V existing = value.get(k);
                if (existing == null) {
                    if (vFabricator == null) {
                        throw new IllegalArgumentException("No existing entry for stream key " + k
                                + " in read-only-V map and value type " + valueType.getName()
                                + " has no accessible no-arg constructor and is not a known interface (List/Set/Map/Queue/Deque)."
                                + " Add a no-arg ctor or use @ReadOnlyManaged.");
                    }
                    existing = vFabricator.get();
                    value.put(k, existing);
                }
                roVAccessor.writeReadOnlyValueFromStream(buffer, existing);
            }
            value.keySet().removeIf(k -> !seen.contains(k));
            return;
        }

        if (size != value.size()) {
            throw new IllegalArgumentException(
                    "Stream entry count " + size + " != map size " + value.size()
                            + " for read-only-K map; use @ReadOnlyManaged on the field"
                            + " to rebuild structure when keys change");
        }

        // K read-only: byte-level lookup is impractical, so use order-based pairing.
        // Caller is responsible for deterministic iteration order (LinkedHashMap/TreeMap, or a
        // @ReadOnlyManaged deserializeUid that inserts keys in the same order both sides observe).
        @SuppressWarnings("unchecked")
        IReadOnlyAccessor<K> roKAccessor = (IReadOnlyAccessor<K>) keyAccessor;
        var iter = value.entrySet().iterator();
        for (int i = 0; i < size; i++) {
            var entry = iter.next();
            K existingK = entry.getKey();
            if (existingK == null) {
                throw new IllegalArgumentException("Null read-only K in map at position " + i);
            }
            // Mutate K in place from the stream payload so K's internal state stays in sync.
            roKAccessor.writeReadOnlyValueFromStream(buffer, existingK);
            if (vDirect) {
                V newV = readChildFromStreamForCreate(buffer, valueAccessor, valueType, "value");
                entry.setValue(newV);
            } else {
                V existingV = entry.getValue();
                if (existingV == null) {
                    throw new IllegalArgumentException("Null existing read-only V in map at position " + i);
                }
                @SuppressWarnings("unchecked")
                IReadOnlyAccessor<V> roVAccessor = (IReadOnlyAccessor<V>) valueAccessor;
                roVAccessor.writeReadOnlyValueFromStream(buffer, existingV);
            }
        }
    }

    // ---------- Mark / dirty tracking ----------

    @Override
    public Object[] obtainManagedMark(@NotNull Map<K, V> value) {
        var op = Platform.getFrozenRegistry().createSerializationContext(JavaOps.INSTANCE);
        var marks = new Object[value.size() * 2];
        int i = 0;
        for (var entry : value.entrySet()) {
            marks[i++] = obtainChildMark(op, keyAccessor, entry.getKey());
            marks[i++] = obtainChildMark(op, valueAccessor, entry.getValue());
        }
        return marks;
    }

    @Override
    public boolean areDifferent(Object[] managedMark, @NotNull Map<K, V> value) {
        if (managedMark.length != value.size() * 2) return true;
        var op = Platform.getFrozenRegistry().createSerializationContext(JavaOps.INSTANCE);
        int i = 0;
        for (var entry : value.entrySet()) {
            if (childMarkDiffers(op, keyAccessor, managedMark[i++], entry.getKey())) return true;
            if (childMarkDiffers(op, valueAccessor, managedMark[i++], entry.getValue())) return true;
        }
        return false;
    }

    // ---------- Helpers ----------

    private <T> T serializeChild(DynamicOps<T> op, IAccessor<?> accessor, Object obj, String role) {
        if (accessor instanceof IDirectAccessor directAccessor) {
            if (obj == null) return LDLibExtraCodecs.createStringNull(op);
            return (T) directAccessor.readDirectVar(op, ManagedHolderVar.of(obj));
        }
        if (accessor instanceof IReadOnlyAccessor readOnlyAccessor) {
            if (obj == null) {
                throw new IllegalArgumentException("Null " + role + " for read-only accessor in map");
            }
            return (T) readOnlyAccessor.readReadOnlyValue(op, obj);
        }
        throw new IllegalArgumentException("Child accessor not managed for map " + role + ": " + accessor);
    }

    private <T, X> X deserializeForCreate(DynamicOps<T> op, IAccessor<?> accessor, Class<?> type, T payload, String role) {
        if (accessor instanceof IDirectAccessor directAccessor) {
            if (LDLibExtraCodecs.isEmptyOrStringNull(op, payload)) return null;
            var holder = ManagedHolderVar.ofNull(type);
            directAccessor.writeDirectVar(op, holder, payload);
            return (X) holder.value();
        }
        throw new IllegalArgumentException("Cannot fabricate new instance for read-only " + role + " accessor: " + accessor);
    }

    private <T> K findExistingKey(DynamicOps<T> op, Map<K, V> value, T kPayload) {
        // Fast path for direct K: deserialize K and use map.get equality.
        if (keyAccessor instanceof IDirectAccessor directKAccessor) {
            if (LDLibExtraCodecs.isEmptyOrStringNull(op, kPayload)) {
                return value.containsKey(null) ? null : null;
            }
            var holder = ManagedHolderVar.ofNull(keyType);
            directKAccessor.writeDirectVar(op, holder, kPayload);
            K candidate = (K) holder.value();
            if (value.containsKey(candidate)) return candidate;
            // Fall through to serialized-form comparison if .equals doesn't match (rare but possible).
        }
        // Slow path for read-only K (or direct K that didn't match): compare serialized forms.
        for (var key : value.keySet()) {
            T existing = serializeChild(op, keyAccessor, key, "key");
            if (Objects.equals(existing, kPayload)) return key;
        }
        return null;
    }

    private void writeChildToStream(RegistryFriendlyByteBuf buffer, IAccessor<?> accessor, Object obj, String role) {
        if (accessor instanceof IDirectAccessor directAccessor) {
            if (obj == null) {
                buffer.writeBoolean(true);
                return;
            }
            buffer.writeBoolean(false);
            directAccessor.readDirectVarToStream(buffer, ManagedHolderVar.of(obj));
            return;
        }
        if (accessor instanceof IReadOnlyAccessor readOnlyAccessor) {
            if (obj == null) {
                throw new IllegalArgumentException("Null " + role + " for read-only accessor in map (stream)");
            }
            readOnlyAccessor.readReadOnlyValueToStream(buffer, obj);
            return;
        }
        throw new IllegalArgumentException("Child accessor not managed for map " + role + ": " + accessor);
    }

    private <X> X readChildFromStreamForCreate(RegistryFriendlyByteBuf buffer, IAccessor<?> accessor, Class<?> type, String role) {
        if (accessor instanceof IDirectAccessor directAccessor) {
            if (buffer.readBoolean()) return null;
            var holder = ManagedHolderVar.ofNull(type);
            directAccessor.writeDirectVarFromStream(buffer, holder);
            return (X) holder.value();
        }
        throw new IllegalArgumentException("Cannot fabricate new instance for read-only " + role + " accessor in stream: " + accessor);
    }

    private Object obtainChildMark(DynamicOps<?> op, IAccessor<?> accessor, Object obj) {
        if (obj == null) return null;
        return switch (accessor) {
            case IMarkFunction markFunction -> markFunction.obtainManagedMark(obj);
            case IDirectAccessor directAccessor -> directAccessor.readDirectVar((DynamicOps) op, ManagedHolderVar.of(obj));
            case IReadOnlyAccessor readOnlyAccessor -> readOnlyAccessor.readReadOnlyValue((DynamicOps) op, obj);
            case null, default -> throw new IllegalArgumentException("Unsupported child accessor: " + accessor);
        };
    }

    private boolean childMarkDiffers(DynamicOps<?> op, IAccessor<?> accessor, Object mark, Object obj) {
        if (obj == null) return mark != null;
        if (mark == null) return true;
        return switch (accessor) {
            case IMarkFunction markFunction -> markFunction.areDifferent(mark, obj);
            case IDirectAccessor directAccessor ->
                    !directAccessor.readDirectVar((DynamicOps) op, ManagedHolderVar.of(obj)).equals(mark);
            case IReadOnlyAccessor readOnlyAccessor ->
                    !readOnlyAccessor.readReadOnlyValue((DynamicOps) op, obj).equals(mark);
            case null, default -> throw new IllegalArgumentException("Unsupported child accessor: " + accessor);
        };
    }
}
