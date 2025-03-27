package com.lowdragmc.lowdraglib.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

@UtilityClass
public final class TagUtils {

    public static CompoundTag getOrCreateTag(CompoundTag compoundTag, String key) {
        if (!compoundTag.contains(key)) {
            compoundTag.put(key, new CompoundTag());
        }
        return compoundTag.getCompound(key);
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
        if(compoundTag == null) {
            if(create) {
                throw new NullPointerException("CompoundTag is null");
            }
            return null;
        }
        String[] keys = key.split("\\.");
        CompoundTag current = compoundTag;
        for (int i = 0; i < keys.length - 1; i++) {
            if(create) {
                current = getOrCreateTag(current, keys[i]);
            } else {
                if(!current.contains(keys[i])) {
                    return null;
                }
                current = current.getCompound(keys[i]);
            }
        }
        return current.get(keys[keys.length - 1]);
    }

    public static <T extends Tag> T getTagExtended(CompoundTag compoundTag, String key, T defaultValue) {
        var tag = getTagExtended(compoundTag, key, false);
        if(tag == null) {
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
     * @param tag to clean up
     * @param demo reference tag
     * @return cleaned result, if null - tag is completely same as demo.
     */
    @Nullable
    public static <T extends Tag> T removeDuplicates(T tag, T demo) {
        if (tag.equals(demo)) return null;
        if (tag instanceof CompoundTag compoundTag1 && demo instanceof CompoundTag compoundTag2) {
            for (var key : compoundTag2.getAllKeys()) {
                if (key.startsWith("_")) continue;
                var tag2 = compoundTag2.get(key);
                var tag1 = compoundTag1.get(key);
                if (tag1 != null && tag2 != null) {
                    var cleanTag = removeDuplicates(tag1, tag2);
                    if (cleanTag != null) {
                        compoundTag1.put(key, cleanTag);
                    } else {
                        compoundTag1.remove(key);
                    }
                }
            }
        }
        return tag;
    }
}
