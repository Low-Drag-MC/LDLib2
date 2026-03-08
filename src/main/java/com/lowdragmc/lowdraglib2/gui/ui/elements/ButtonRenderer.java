package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ButtonRenderer {
    private ButtonRenderer() {
    }

    public static void drawBackgroundAdditional(Button button, GUIContext context) {
        var texture = button.isActive() ? switch (button.getState()) {
            case DEFAULT -> button.getButtonStyle().baseTexture();
            case HOVERED -> button.getButtonStyle().hoverTexture();
            case PRESSED -> button.getButtonStyle().pressedTexture();
        } : button.getButtonStyle().baseTexture();
        context.drawTexture(texture, button.getPositionX(), button.getPositionY(), button.getSizeWidth(), button.getSizeHeight());
    }
}
