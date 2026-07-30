package com.lowdragmc.lowdraglib2.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@UtilityClass
public final class TagUtils {

    /**
     * Reads a UUID stored in either supported form:
     * <ul>
     *   <li>a String — what {@code UUID#toString()} + {@code putString} write today;</li>
     *   <li>an int array — what the now-removed {@code CompoundTag#putUUID} wrote, so every save
     *       made before it disappeared (node graphs above all) still carries this form.</li>
     * </ul>
     * Reading the legacy form as a String silently yields the empty default and then throws in
     * {@code UUID.fromString}, which is why this probes instead of assuming one.
     *
     * @return the UUID, or empty when the key is absent or holds neither form
     */
    public static Optional<UUID> readUUID(CompoundTag compoundTag, String key) {
        var asString = compoundTag.getString(key);
        if (asString.isPresent() && !asString.get().isEmpty()) {
            try {
                return Optional.of(UUID.fromString(asString.get()));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        try {
            return compoundTag.read(key, UUIDUtil.CODEC);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static CompoundTag getOrCreateTag(CompoundTag compoundTag, String key) {
        if (!compoundTag.contains(key)) {
            compoundTag.put(key, new CompoundTag());
        }
        return compoundTag.getCompoundOrEmpty(key);
    }

    /**
     * check {@link TagUtils#setTagExtended(CompoundTag, String, Tag)}
     */
    public static Tag getTagExtended(CompoundTag compoundTag, String key) {
        return getTagExtended(compoundTag, key, false);
    }

    /**
     * check {@link TagUtils#setTagExtended(CompoundTag, String, Tag)}
     */
    public static Tag getTagExtended(CompoundTag compoundTag, String key, boolean create) {
        if (compoundTag == null) {
            if (create) {
                throw new NullPointerException("CompoundTag is null");
            }
            return null;
        }
        String[] keys = key.split("\\.");
        CompoundTag current = compoundTag;
        for (int i = 0; i < keys.length - 1; i++) {
            if (create) {
                current = getOrCreateTag(current, keys[i]);
            } else {
                if (!current.contains(keys[i])) {
                    return null;
                }
                current = current.getCompoundOrEmpty(keys[i]);
            }
        }
        return current.get(keys[keys.length - 1]);
    }

    public static <T extends Tag> T getTagExtended(CompoundTag compoundTag, String key, T defaultValue) {
        var tag = getTagExtended(compoundTag, key, false);
        if (tag == null) {
            return defaultValue;
        }
        return (T) tag;
    }

    /**
     * <pre>{@code
     * var compoundTag = {
     *     kk: {}
     * };
     * var key = "kk.bb.cc";
     * var tag = StringTag.of("value");
     * setTagExtended(compoundTag, key, tag);
     * compoundTag = {
     *     kk: {
     *         bb: {
     *             cc: "value"
     *         }
     *     }
     * }
     * }</pre>
     */
    public static void setTagExtended(CompoundTag compoundTag, String key, Tag tag) {
        String[] keys = key.split("\\.");
        CompoundTag current = compoundTag;
        for (int i = 0; i < keys.length - 1; i++) {
            current = getOrCreateTag(current, keys[i]);
        }
        current.put(keys[keys.length - 1], tag);
    }

    /**
     * remove duplicates tags.
     *
     * @param target    to clean up
     * @param reference reference target
     * @return cleaned result, if null - target is completely same as reference.
     */
    @Nullable
    public static <T extends Tag> T removeDuplicates(T target, T reference) {
        if (target.equals(reference)) return null;
        if (target instanceof CompoundTag targetTag && reference instanceof CompoundTag refTag) {
            for (var key : refTag.keySet()) {
                var tag2 = refTag.get(key);
                var tag1 = targetTag.get(key);
                if (tag1 != null && tag2 != null) {
                    var cleanTag = removeDuplicates(tag1, tag2);
                    if (cleanTag != null) {
                        targetTag.put(key, cleanTag);
                    } else {
                        targetTag.remove(key);
                    }
                }
            }
            if (targetTag.isEmpty()) {
                return null;
            }
        }
        return target;
    }

    /**
     * Compare two CompoundTags and return a new CompoundTag containing only the differences.
     * If a key exists in both tags but has different values, the value from tagA is included.
     * If a key exists only in tagA, it is included.
     * If a key exists only in tagB, it is not included.
     *
     * @param tagA first tag to compare
     * @param tagB second tag to compare
     * @return new CompoundTag with differences, or null if tags are identical
     */
    @Nullable
    public static CompoundTag getDifferences(CompoundTag tagA, CompoundTag tagB) {
        if (tagA == null || tagB == null) return tagA;
        if (tagA.equals(tagB)) return null;

        CompoundTag result = new CompoundTag();
        for (String key : tagA.keySet()) {
            Tag valueA = tagA.get(key);
            Tag valueB = tagB.get(key);
            assert valueA != null;
            if (valueB == null) {
                // Key exists only in tagA
                result.put(key, TagBuilder.compound().add("a", valueA).add("b", "missing").build());
            } else if (!valueA.equals(valueB)) {
                // Values are different
                if (valueA instanceof CompoundTag && valueB instanceof CompoundTag) {
                    // Recursively check nested tags
                    CompoundTag nestedDiff = getDifferences((CompoundTag) valueA, (CompoundTag) valueB);
                    if (nestedDiff != null) {
                        result.put(key, nestedDiff);
                    }
                } else if (valueA instanceof ListTag listTag && valueB instanceof ListTag listTag2 && listTag.size() == listTag2.size()) {
                    var resultList = new ListTag();
                    for (int i = 0; i < listTag.size(); i++) {
                        var itemA = listTag.get(i);
                        var itemB = listTag2.get(i);
                        if (itemA instanceof CompoundTag && itemB instanceof CompoundTag) {
                            var diff = getDifferences((CompoundTag) itemA, (CompoundTag) itemB);
                            resultList.add(Objects.requireNonNullElseGet(diff, CompoundTag::new));
                        } else if (!itemA.equals(itemB)) {
                            resultList.add(itemA);
                        }
                    }
                    if (!resultList.isEmpty()) {
                        result.put(key, resultList);
                    }
                } else {
                    result.put(key, TagBuilder.compound().add("a", valueA).add("b", valueB).build());
                }
            }
        }

        return result.isEmpty() ? null : result;
    }
}
