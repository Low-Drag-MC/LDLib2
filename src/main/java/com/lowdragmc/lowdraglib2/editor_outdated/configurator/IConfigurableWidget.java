package com.lowdragmc.lowdraglib2.editor_outdated.configurator;

import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.widget.Widget;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

import javax.annotation.Nullable;

import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/6
 * @implNote IConfigurableWidget
 */
public interface IConfigurableWidget extends IConfigurable, IPersistedSerializable, ILDLRegister<IConfigurableWidget, Supplier<IConfigurableWidget>> {
    Codec<IConfigurableWidget> CODEC = LDLib2Registries.WIDGETS.optionalCodec().dispatch(ILDLRegister::getRegistryHolderOptional,
            optional -> optional.map(holder -> PersistedParser.createCodec(holder.value()).fieldOf("data"))
                    .orElseGet(LDLibExtraCodecs::errorDecoder));

    default Widget widget() {
        return (Widget) this;
    }

    default void initTemplate() {
    }

    default boolean canDragIn(Object dragging) {
        if (dragging instanceof IGuiTexture) {
            return true;
        } else if (dragging instanceof String) {
            return true;
        } else if (dragging instanceof IIdProvider) {
            return true;
        } else if (dragging instanceof Integer) {
            return true;
        }
        return false;
    }

    default boolean handleDragging(Object dragging) {
        if (dragging instanceof IGuiTexture guiTexture) {
            widget().setBackground(guiTexture);
            return true;
        } else if (dragging instanceof String string) {
            widget().setHoverTooltips(string);
            return true;
        } else if (dragging instanceof IIdProvider idProvider) {
            widget().setId(idProvider.get());
            return true;
        } else if (dragging instanceof Integer color) {
            widget().setBackground(new ColorRectTexture(color));
            return true;
        }
        return false;
    }

    default CompoundTag serializeWrapper() {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
    }

    @Nullable
    static IConfigurableWidget deserializeWrapper(CompoundTag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null);
    }

    // ******* setter ********//
    @FunctionalInterface
    interface IIdProvider extends Supplier<String> {

    }
}
