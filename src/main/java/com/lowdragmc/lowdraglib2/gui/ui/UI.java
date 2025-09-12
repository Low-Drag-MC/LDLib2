package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleSheet;
import com.lowdragmc.lowdraglib2.math.Size;
import lombok.Data;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.function.Function;

@Data(staticConstructor = "of")
public final class UI {
    public final UIElement rootElement;
    public final StyleSheet styleSheet;
    @Nullable
    public final Function<Size, Size> dynamicSize;

    public static UI of(UIElement rootElement, @Nullable Function<Size, Size> dynamicSize) {
        return of(rootElement, new StyleSheet(), dynamicSize);
    }

    public static UI of(UIElement rootElement) {
        return of(rootElement, null);
    }

    public static UI of() {
        return of(new UIElement());
    }

    public CompoundTag serialize(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        var rootTag = rootElement.serializeNBT(provider);
        tag.put("root", rootTag);
        return tag;
    }

    public static UI fromNbt(HolderLookup.Provider provider, CompoundTag tag) {
       var root = new UIElement();
       root.deserializeNBT(provider, tag.getCompound("root"));
       return UI.of(root);
    }

}
