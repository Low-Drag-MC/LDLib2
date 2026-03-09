package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TreeListClientTextures {
    private static final TreeListDraggingOverlays.Provider PROVIDER = mode -> switch (mode) {
        case 0 -> GuiTexture.of((context, x, y, width, height) ->
                DrawerHelperClient.drawSolidRect(context, x, y - 1, width, 1, ColorPattern.T_WHITE.color));
        case 1 -> GuiTexture.of((context, x, y, width, height) ->
                DrawerHelperClient.drawSolidRect(context, x, y, width, height, ColorPattern.T_WHITE.color));
        case 2 -> GuiTexture.of((context, x, y, width, height) ->
                DrawerHelperClient.drawSolidRect(context, x, y + height, width, 1, ColorPattern.T_WHITE.color));
        default -> IGuiTexture.EMPTY;
    };

    private TreeListClientTextures() {}

    public static void init() {
        TreeListDraggingOverlays.register(PROVIDER);
    }
}
