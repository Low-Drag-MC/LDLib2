package com.lowdragmc.lowdraglib.gui.ui.style;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true, fluent = true)
public class Style {
    public final UIElement holder;
    @Getter
    @Setter
    private boolean drawBackgroundWhenHover = true;
    @Getter @Setter
    private IGuiTexture backgroundTexture = IGuiTexture.EMPTY;
    @Getter @Setter
    private IGuiTexture borderTexture = IGuiTexture.EMPTY;
    @Getter @Setter
    private IGuiTexture overlayTexture = IGuiTexture.EMPTY;
    @Getter @Setter
    private IGuiTexture hoverTexture = IGuiTexture.EMPTY;
    @Getter
    private int zIndex = 0;

    public Style(UIElement holder) {
        this.holder = holder;
    }

    public Style zIndex(int zIndex) {
        if (zIndex == this.zIndex) return this;
        this.zIndex = zIndex;
        if (holder.getParent() != null) {
            holder.getParent().clearSortedChildrenCache();
        }
        return this;
    }
}
