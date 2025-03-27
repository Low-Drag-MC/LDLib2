package com.lowdragmc.lowdraglib.editor.configurator;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.data.Resources;
import com.lowdragmc.lowdraglib.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.editor.data.resource.TexturesResource;
import com.lowdragmc.lowdraglib.utils.AnnotationDetector;
import com.lowdragmc.lowdraglib.utils.PersistedParser;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.UIResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import javax.annotation.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/6
 * @implNote IConfigurableWidget
 */
public interface IConfigurableWidget extends IConfigurable {

    Function<String, AnnotationDetector.Wrapper<LDLRegister, IConfigurableWidget>> CACHE = Util.memoize(type -> {
        for (var wrapper : AnnotationDetector.REGISTER_WIDGETS) {
            if (wrapper.annotation().name().equals(type)) {
                return wrapper;
            }
        }
        return null;
    });

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

    @SuppressWarnings("unchecked")
    static CompoundTag serializeNBT(IConfigurableWidget widget, Resources resources, boolean isProject, HolderLookup.Provider provider) {
        return serializeNBT(widget, (Resource<IGuiTexture>) resources.resources.get(TexturesResource.RESOURCE_NAME), isProject, provider);
    }

    static CompoundTag serializeNBT(IConfigurableWidget widget, Resource<IGuiTexture> resources, boolean isProject, HolderLookup.Provider provider) {
        UIResourceTexture.setCurrentResource(resources, isProject);
        CompoundTag tag = widget.serializeInnerNBT(provider);
        UIResourceTexture.clearCurrentResource();
        return tag;
    }

    @SuppressWarnings("unchecked")
    static void deserializeNBT(IConfigurableWidget widget, CompoundTag tag, Resources resources, boolean isProject, HolderLookup.Provider provider) {
        deserializeNBT(widget, tag, (Resource<IGuiTexture>) resources.resources.get(TexturesResource.RESOURCE_NAME), isProject, provider);
    }

    static void deserializeNBT(IConfigurableWidget widget, CompoundTag tag, Resource<IGuiTexture> resources, boolean isProject, HolderLookup.Provider provider) {
        UIResourceTexture.setCurrentResource(resources, isProject);
        widget.deserializeInnerNBT(provider, tag);
        UIResourceTexture.clearCurrentResource();
    }

    default CompoundTag serializeInnerNBT(HolderLookup.Provider provider) {
        return PersistedParser.serializeNBT(this, provider);
    }

    default void deserializeInnerNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        PersistedParser.deserializeNBT(nbt,this, provider);
    }

    default CompoundTag serializeWrapper(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        tag.putString("type", name());
        tag.put("data", serializeInnerNBT(provider));
        return tag;
    }

    @Nullable
    static IConfigurableWidget deserializeWrapper(CompoundTag tag, HolderLookup.Provider provider) {
        String type = tag.getString("type");
        var wrapper = CACHE.apply(type);
        if (wrapper != null) {
            var child = wrapper.creator().get();
            child.deserializeInnerNBT(provider, tag.getCompound("data"));
            return child;
        }
        return null;
    }

    // ******* setter ********//


    @FunctionalInterface
    interface IIdProvider extends Supplier<String> {

    }
}
