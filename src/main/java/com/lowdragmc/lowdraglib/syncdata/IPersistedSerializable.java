package com.lowdragmc.lowdraglib.syncdata;

import com.lowdragmc.lowdraglib.utils.PersistedParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.configurator.annotation.Configurable;

/**
 * Class with this interface can serialize and deserialize itself by detecting fields with
 * {@link Persisted} and {@link Configurable} annotation.
 * <br>
 * <br>
 * It will use {@link PersistedParser} to serialize and deserialize. Don't override methods of {@link #serializeNBT(HolderLookup.Provider)} and {@link #deserializeNBT(HolderLookup.Provider, CompoundTag)}. unless you know what you are doing.
 * <br>
 * <br>
 * For additional serialization, you can override {@link #serializeAdditionalNBT(HolderLookup.Provider)}. and {@link #deserializeAdditionalNBT(Tag, HolderLookup.Provider)}.
 * <br>
 * <br>
 * The serialization process will be:
 * <ol>
 *     <li>Call {@link #beforeSerialize()}</li>
 *     <li>Serialize fields with annotation</li>
 *     <li>Call {@link #serializeAdditionalNBT(HolderLookup.Provider)}</li>
 *     <li>Call {@link #afterSerialize()}</li>
 * </ol>
 * The deserialization process will be:
 * <ol>
 *     <li>Call {@link #beforeDeserialize()}</li>
 *     <li>Deserialize fields with annotation</li>
 *     <li>Call {@link #deserializeAdditionalNBT(Tag, HolderLookup.Provider)}</li>
 *     <li>Call {@link #afterDeserialize()}</li>
 * </ol>
 */
public interface IPersistedSerializable extends INBTSerializable<CompoundTag> {

    default void beforeSerialize() {

    }

    @Override
    default CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        return PersistedParser.serializeNBT(this, provider);
    }

    default Tag serializeAdditionalNBT(HolderLookup.@NotNull Provider provider) {
        return EndTag.INSTANCE;
    }

    default void afterSerialize() {

    }

    default void beforeDeserialize() {

    }

    @Override
    default void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
        PersistedParser.deserializeNBT(tag, this, provider);
    }

    default void deserializeAdditionalNBT(Tag tag, HolderLookup.@NotNull Provider provider) {

    }

    default void afterDeserialize() {

    }
}
