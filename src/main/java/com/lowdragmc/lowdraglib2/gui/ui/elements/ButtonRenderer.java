package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "button", registry = "ldlib2:ui_element_renderer")
public final class ButtonRenderer extends DelegatingUIElementRenderer<Button, ButtonRenderer> {
    @Override
    public Class<Button> type() {
        return Button.class;
    }

    @Override
    public void drawBackgroundAdditional(Button button, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundAdditional(button, context);
            return;
        }
        drawBackgroundAdditional(button, guiContext);
    }

    static void drawBackgroundAdditional(Button button, GUIContext context) {
        var texture = button.isActive() ? switch (button.getState()) {
            case DEFAULT -> button.getButtonStyle().baseTexture();
            case HOVERED -> button.getButtonStyle().hoverTexture();
            case PRESSED -> button.getButtonStyle().pressedTexture();
        } : button.getButtonStyle().baseTexture();
        context.drawTexture(texture, button.getPositionX(), button.getPositionY(), button.getSizeWidth(), button.getSizeHeight());
    }
}
