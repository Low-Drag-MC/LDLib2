package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
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
    @Configurable(name = "BasicStyle.drawBackgroundWhenHover")
    private boolean drawBackgroundWhenHover = true;
    @Getter @Setter
    @Configurable(name = "BasicStyle.backgroundTexture")
    private IGuiTexture backgroundTexture = IGuiTexture.EMPTY;
    @Getter @Setter
    @Configurable(name = "BasicStyle.borderTexture")
    private IGuiTexture borderTexture = IGuiTexture.EMPTY;
    @Getter @Setter
    @Configurable(name = "BasicStyle.overlayTexture")
    private IGuiTexture overlayTexture = IGuiTexture.EMPTY;
    @Getter @Setter
    @Configurable(name = "BasicStyle.tooltips")
    private List<Component> tooltips = new ArrayList<>();
    @Getter
    @Configurable(name = "BasicStyle.zIndex")
    @ConfigNumber(range = {Integer.MIN_VALUE, Integer.MAX_VALUE})
    private int zIndex = 0;
    @Getter
    @Configurable(name = "BasicStyle.transform2D", subConfigurable = true)
    private final Transform2D transform2D = new Transform2D();

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
