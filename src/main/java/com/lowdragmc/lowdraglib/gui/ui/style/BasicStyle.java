package com.lowdragmc.lowdraglib.gui.ui.style;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.style.value.TextWrap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true, fluent = true)
public class BasicStyle extends Style {
    @Getter @Setter
    private boolean drawBackgroundWhenHover = true;
    @Getter @Setter
    private IGuiTexture backgroundTexture = IGuiTexture.EMPTY;
    @Getter @Setter
    private IGuiTexture borderTexture = IGuiTexture.EMPTY;
    @Getter
    private int zIndex = 0;

    public BasicStyle(UIElement holder) {
        super(holder);
    }

    public BasicStyle zIndex(int zIndex) {
        if (zIndex == this.zIndex) return this;
        this.zIndex = zIndex;
        if (holder.getParent() != null) {
            holder.getParent().clearSortedChildrenCache();
        }
        return this;
    }
}
