package com.lowdragmc.lowdraglib2.syncdata;

import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.jetbrains.annotations.NotNull;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;

/**
 * Class with this interface can serialize and deserialize itself by detecting fields with
 * {@link Persisted} and {@link Configurable} annotation.
 * <br>
 * <br>
 * It will use {@link PersistedParser} to serialize and deserialize. Don't override methods of {@link #serialize(ValueOutput)} and {@link #deserialize(ValueInput)}. unless you know what you are doing.
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
public interface IPersistedSerializable extends ValueIOSerializable {

    /**
     * This method is invoked before the serialization process begins.
     * It allows preparation or cleanup to be performed prior to the
     * serialization of fields annotated with {@link Persisted} and {@link Configurable}.
     *
     * The method can be overridden in implementing classes to execute custom logic
     * before serialization steps, ensuring any necessary adjustments
     * or preconditions are handled.
     *
     * This is the initial step in the serialization process as outlined:
     * 1. {@code beforeSerialize()} is called.
     * 2. Fields marked with serialization annotations are serialized.
     * 3. {@link #serializeAdditionalNBT(HolderLookup.Provider)} is called.
     * 4. {@link #afterSerialize()} is called.
     *
     * This default implementation provides no behavior and may be safely overridden.
     */
    default void beforeSerialize() {

    }

    @Override
    default void serialize(@NotNull ValueOutput output) {
        PersistedParser.serialize(this, output);
    }

    @Override
    default void deserialize(@NotNull ValueInput input) {
        PersistedParser.deserialize(this, input);
    }

    /**
     * Writes the data of this object to the provided {@link ByteBuf}.
     * This method uses {@link PersistedParser#writeBuff(ByteBuf, Object)}
     * to perform the writing operation.
     *
     * @param buf The {@link ByteBuf} to which the object's data will be written.
     */
    default void writeToBuff(ByteBuf buf) {
        PersistedParser.writeBuff(buf, this);
    }

    /**
     * Serializes additional NBT data for this object.
     * This method is typically used to store supplementary information that is not handled
     * by the default serialization process. The default implementation returns an empty {@link EndTag}.
     *
     * @param provider The {@link HolderLookup.Provider} used to resolve any necessary
     *                 context or dependencies during the serialization of additional NBT data.
     * @return A {@link Tag} containing the serialized additional NBT data.
     */
    default Tag serializeAdditionalNBT(HolderLookup.@NotNull Provider provider) {
        return EndTag.INSTANCE;
    }

    /**
     * This method is called after the serialization process is completed.
     * It allows for any final adjustments, cleanup, or actions to be taken
     * after the object has been serialized.
     */
    default void afterSerialize() {

    }

    /**
     * This method is invoked before the deserialization process begins.
     * It provides an opportunity to execute any preparatory steps or
     * cleanup necessary prior to processing fields annotated with
     * {@link Persisted} and {@link Configurable}.
     */
    default void beforeDeserialize() {

    }

    /**
     * Reads the state of this object from the provided {@link ByteBuf}.
     * This method relies on {@link PersistedParser#readBuff(ByteBuf, Object)}
     * to handle the deserialization process.
     *
     * @param buf The {@link ByteBuf} instance containing the data to be read and
     *            used to update the state of this object.
     */
    default void readFromBuff(ByteBuf buf) {
        PersistedParser.readBuff(buf, this);
    }

    /**
     * Deserializes additional NBT (Named Binary Tag) data for the implementing object.
     * This method is used to restore the supplementary state of an object
     * from a previously serialized {@link Tag}.
     *
     * @param tag      The {@link Tag} instance containing additional NBT data
     *                 to be deserialized into the object.
     * @param provider The {@link HolderLookup.Provider} providing contextual
     *                 information or dependencies necessary for deserialization.
     */
    default void deserializeAdditionalNBT(Tag tag, HolderLookup.@NotNull Provider provider) {

    }

    /**
     * This method is invoked after the deserialization process is completed.
     * It allows implementing classes to perform any additional actions,
     * cleanup, or adjustments required after the object's state has been restored.
     *
     * The default implementation provides no behavior and can be safely overridden
     * in implementing classes to execute custom post-deserialization logic if necessary.
     */
    default void afterDeserialize() {

    }
}
