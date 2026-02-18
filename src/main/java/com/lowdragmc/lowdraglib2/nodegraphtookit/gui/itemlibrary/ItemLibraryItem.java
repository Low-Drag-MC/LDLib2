package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;

@Accessors(chain = true)
@ToString(onlyExplicitlyIncluded = true)
public class ItemLibraryItem {
    @Getter @Setter
    protected String path = "";
    @Getter @Setter
    protected IGuiTexture icon = IGuiTexture.EMPTY;
    @Getter @Setter
    protected Component displayName = Component.empty();
    @Getter @Setter
    @ToString.Include
    protected String searchableName;
    @Getter @Setter
    @ToString.Include
    protected IItemLibraryData data;
}
