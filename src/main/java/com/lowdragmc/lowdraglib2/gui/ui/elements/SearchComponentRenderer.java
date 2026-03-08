package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SearchComponentRenderer {
    private SearchComponentRenderer() {
    }

    public static void drawBackgroundOverlay(SearchComponent<?> searchComponent, GUIContext context) {
        if (searchComponent.isSelfOrChildHover() || searchComponent.textField.isFocused()) {
            context.drawTexture(searchComponent.getSearchStyle().focusOverlay(),
                    searchComponent.getPositionX(), searchComponent.getPositionY(),
                    searchComponent.getSizeWidth(), searchComponent.getSizeHeight());
        }
        searchComponent.drawSharedDefaultOverlay(context);
    }
}
