package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "search_component", registry = "ldlib2:ui_element_renderer")
public final class SearchComponentRenderer extends DelegatingUIElementRenderer<SearchComponent<?>, SearchComponentRenderer> {
    @Override
    public Class<SearchComponent<?>> type() {
        return (Class<SearchComponent<?>>) (Class<?>) SearchComponent.class;
    }

    @Override
    public void drawBackgroundOverlay(SearchComponent<?> searchComponent, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundOverlay(searchComponent, context);
            return;
        }
        if (searchComponent.isSelfOrChildHover() || searchComponent.textField.isFocused()) {
            guiContext.drawTexture(searchComponent.getSearchStyle().focusOverlay(),
                    searchComponent.getPositionX(), searchComponent.getPositionY(),
                    searchComponent.getSizeWidth(), searchComponent.getSizeHeight());
        }
        drawParentBackgroundOverlay(searchComponent, context);
    }
}
