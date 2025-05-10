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
public class Style {
    public final UIElement holder;
    @Getter @Setter
    private boolean drawBackgroundWhenHover = true;
    @Getter @Setter
    private IGuiTexture backgroundTexture = IGuiTexture.EMPTY;
    @Getter @Setter
    private IGuiTexture borderTexture = IGuiTexture.EMPTY;
    @Getter
    private int zIndex = 0;
    /// text
    @Getter @Setter
    private Horizontal textAlignHorizontal = Horizontal.LEFT;
    @Getter @Setter
    private Vertical textAlignVertical = Vertical.TOP;
    @Getter @Setter
    private TextWrap textWrap = TextWrap.NONE;
    @Getter @Setter
    private float fontSize = 9;
    @Getter @Setter
    private float lineSpacing = 1;
    @Getter @Setter
    private int textColor = -1;
    @Getter @Setter
    private boolean textShadow = true;

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
