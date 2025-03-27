package com.lowdragmc.lowdraglib.compass;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import lombok.Getter;

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2023/7/28
 * @implNote ICompassUIConfig
 */
public interface ICompassUIConfig {

    static ICompassUIConfig getDefault() {
        return new ICompassUIConfig() {
            @Getter
            public IGuiTexture sectionBackground = IGuiTexture.EMPTY;
            @Getter
            public IGuiTexture listViewBackground = ResourceBorderTexture.BORDERED_BACKGROUND_INVERSE;
            @Getter
            public IGuiTexture listItemBackground = ResourceBorderTexture.BUTTON_COMMON;
            @Getter
            public IGuiTexture listItemSelectedBackground = ResourceBorderTexture.BUTTON_COMMON.copy().setColor(0xff337f7f);
            @Getter
            public IGuiTexture nodeBackground = ResourceBorderTexture.BUTTON_COMMON;
            @Getter
            public IGuiTexture nodeHoverBackground = ResourceBorderTexture.BUTTON_COMMON.copy().setColor(0xff337f7f);
        };
    }

    @Nonnull
    IGuiTexture getSectionBackground();

    @Nonnull
    IGuiTexture getListViewBackground();

    @Nonnull
    IGuiTexture getListItemBackground();

    @Nonnull
    IGuiTexture getListItemSelectedBackground();

    @Nonnull
    IGuiTexture getNodeBackground();

    @Nonnull
    IGuiTexture getNodeHoverBackground();

}
