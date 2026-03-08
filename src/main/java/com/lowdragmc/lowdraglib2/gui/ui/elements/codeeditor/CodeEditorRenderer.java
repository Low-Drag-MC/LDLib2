package com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor;

import com.lowdragmc.lowdraglib2.gui.ui.elements.TextAreaRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CodeEditorRenderer {
    private CodeEditorRenderer() {
    }

    public static void drawLines(CodeEditor editor, GUIContext context, Font font, float scale, float x, float y, int firstVisibleLine, int lastVisibleLine) {
        for (int i = firstVisibleLine; i <= lastVisibleLine && i < editor.getStyledLines().size(); i++) {
            float lineY = y + i * editor.lineHeight() - editor.getScrollY();
            StyledLine styledLine = editor.getStyledLines().get(i);

            float drawX = x - editor.getScrollX();

            for (StyledText styledText : styledLine.text()) {
                var textComponent = Component.literal(styledText.text())
                        .withStyle(style -> style.withFont(new FontDescription.Resource(editor.getTextAreaStyle().font())))
                        .withStyle(styledText.style());

                context.pose.pushPose();
                context.pose.translate(drawX, lineY);
                context.pose.scale(scale, scale);
                context.graphics.drawString(
                        font,
                        textComponent,
                        0,
                        0,
                        -1,
                        editor.getTextAreaStyle().textShadow()
                );
                context.pose.popPose();

                drawX += font.getSplitter().stringWidth(textComponent) * scale;
            }
        }

        if (editor.getStyledLines().isEmpty() || (editor.getStyledLines().size() == 1 && editor.getStyledLines().getFirst().text().isEmpty())) {
            TextAreaRenderer.drawPlaceHolder(editor, context, font, scale, x, y);
        }
    }
}
