package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.utils.TextUtilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "text_field", registry = "ldlib2:ui_element_renderer")
public final class TextFieldRenderer extends DelegatingUIElementRenderer<TextField, TextFieldRenderer> {
    @Override
    public Class<TextField> type() {
        return TextField.class;
    }

    @Override
    public void drawBackgroundOverlay(TextField field, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundOverlay(field, context);
            return;
        }
        if (field.isSelfOrChildHover() || field.isFocused()) {
            guiContext.drawTexture(field.getTextFieldStyle().focusOverlay(), field.getPositionX(), field.getPositionY(), field.getSizeWidth(), field.getSizeHeight());
        }
        drawParentBackgroundOverlay(field, context);
    }

    @Override
    public void drawBackgroundAdditional(TextField field, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundAdditional(field, context);
            return;
        }
        drawBackgroundAdditional(field, guiContext);
    }

    static void drawBackgroundAdditional(TextField field, GUIContext context) {
        var x = field.getContentX();
        var y = field.getContentY();
        var height = field.getContentHeight();
        var formattedLine = field.getFormattedLine();
        Font font = Minecraft.getInstance().font;
        var fontSize = field.getTextFieldStyle().fontSize();
        var textFont = field.getTextFieldStyle().font();
        var scale = fontSize / font.lineHeight;

        var lineY = y + (height - fontSize) / 2;
        var line = formattedLine.getA();
        var lineX = x - field.getDisplayOffset();

        context.pose.pushPose();
        context.pose.translate(lineX, lineY);
        context.pose.scale(scale, scale);
        context.graphics.drawString(font, line, 0, 0, field.getRawText().isEmpty() ?
                ColorPattern.LIGHT_GRAY.color : (field.isError() ? field.getTextFieldStyle().errorColor() : field.getTextFieldStyle().textColor()),
                !field.getRawText().isEmpty() && field.getTextFieldStyle().textShadow());
        context.pose.popPose();

        if (field.isFocused() && field.getSelectionStart() != field.getSelectionEnd()) {
            var min = Math.min(field.getSelectionStart(), field.getSelectionEnd());
            var max = Math.max(field.getSelectionStart(), field.getSelectionEnd());
            var minX = font.getSplitter().stringWidth(TextUtilities.withFont(field.getRawText().substring(0, min), textFont)) * scale - field.getDisplayOffset();
            var maxX = font.getSplitter().stringWidth(TextUtilities.withFont(field.getRawText().substring(0, max), textFont)) * scale - field.getDisplayOffset();
            DrawerHelperClient.drawSolidRect(context,
                    RenderPipelines.GUI_TEXT_HIGHLIGHT,
                    x + minX,
                    lineY,
                    maxX - minX,
                    fontSize, -16776961);
        }

        var cursorPosX = font.getSplitter().stringWidth(TextUtilities.withFont(field.getRawText().substring(0, field.getCursorPos()), textFont)) * scale;
        if (field.isFocused() && System.currentTimeMillis() % 1000 < 500) {
            DrawerHelperClient.drawSolidRect(context,
                    x + cursorPosX - field.getDisplayOffset(),
                    lineY,
                    1,
                    fontSize,
                    field.getTextFieldStyle().cursorColor());
        }
    }
}
