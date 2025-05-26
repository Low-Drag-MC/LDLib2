package com.lowdragmc.lowdraglib.gui.ui.style;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.style.value.TextWrap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Accessors(chain = true, fluent = true)
public class BasicStyle extends Style {
    @Getter @Setter
    private boolean drawBackgroundWhenHover = true;
    @Getter @Setter
    private IGuiTexture backgroundTexture = IGuiTexture.EMPTY;
    @Getter @Setter
    private IGuiTexture borderTexture = IGuiTexture.EMPTY;
    @Getter @Setter
    private IGuiTexture overlayTexture = IGuiTexture.EMPTY;
    @Getter @Setter
    private List<Component> tooltips = new ArrayList<>();
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

    public BasicStyle setTooltips(Component... tooltips) {
        this.tooltips.clear();
        this.tooltips.addAll(Arrays.asList(tooltips));
        return this;
    }

    public BasicStyle appendTooltips(Component... tooltips) {
        this.tooltips.addAll(Arrays.asList(tooltips));
        return this;
    }

    public BasicStyle setTooltips(String... tooltips) {
        this.tooltips.clear();
        this.tooltips.addAll(Arrays.stream(tooltips).map(Component::translatable).toList());
        return this;
    }

    public BasicStyle appendTooltipsString(String... tooltips) {
        this.tooltips.addAll(Arrays.stream(tooltips).map(Component::translatable).toList());
        return this;
    }

    public BasicStyle copyFrom(BasicStyle other) {
        this.drawBackgroundWhenHover = other.drawBackgroundWhenHover;
        this.backgroundTexture = other.backgroundTexture;
        this.borderTexture = other.borderTexture;
        this.tooltips = new ArrayList<>(other.tooltips);
        this.zIndex = other.zIndex;
        return this;
    }
}
