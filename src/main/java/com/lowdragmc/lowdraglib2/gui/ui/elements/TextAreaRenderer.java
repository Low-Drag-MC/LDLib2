package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.data.Cursor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditorRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.utils.TextUtilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "text_area", registry = "ldlib2:ui_element_renderer")
public final class TextAreaRenderer extends DelegatingUIElementRenderer<TextArea, TextAreaRenderer> {
    @Override
    public Class<TextArea> type() {
        return TextArea.class;
    }

    @Override
    public void drawBackgroundOverlay(TextArea area, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundOverlay(area, context);
            return;
        }
        if (area.contentView.isSelfOrChildHover() || area.isFocused()) {
            guiContext.drawTexture(area.getTextAreaStyle().focusOverlay(),
                    area.contentView.getPositionX(), area.contentView.getPositionY(),
                    area.contentView.getSizeWidth(), area.contentView.getSizeHeight());
        }
        drawParentBackgroundOverlay(area, context);
    }

    static void drawContentView(TextArea area, GUIContext context) {
        drawDefaultContentView(area, context);
    }

    public static void drawContentViewElement(TextArea.ContentView contentView, GUIContext context) {
        drawContentView(contentView.owner(), context);
    }

    public static void drawDefaultContentView(TextArea area, GUIContext context) {
        com.lowdragmc.lowdraglib2.gui.ui.UIElementRendererRegistry.defaultRenderer().drawBackgroundAdditional(area, context);
        var x = area.contentView.getContentX();
        var y = area.contentView.getContentY();
        var height = area.contentView.getContentHeight();

        Font font = Minecraft.getInstance().font;
        var textFont = area.getTextAreaStyle().font();
        var scale = area.scale();

        int firstVisibleLine = (int) Math.floor(area.getScrollY() / area.lineHeight());
        int maxVisibleLines = (int) Math.ceil(height / area.lineHeight()) + 1;
        int lastVisibleLine = Mth.clamp(firstVisibleLine + maxVisibleLines, 0, Math.max(area.lines.size() - 1, 0));

        if (area instanceof CodeEditor editor) {
            CodeEditorRenderer.drawLines(editor, context, font, scale, x, y, firstVisibleLine, lastVisibleLine);
        } else {
            drawLines(area, context, font, textFont, scale, x, y, firstVisibleLine, lastVisibleLine);
        }
        drawSelection(area, context, font, textFont, scale, x, y, firstVisibleLine, lastVisibleLine);
        drawCursor(area, context, font, textFont, scale, x, y);
    }

    private static void drawLines(TextArea area, GUIContext context, Font font, Identifier textFont,
                                  float scale, float x, float y, int firstVisibleLine, int lastVisibleLine) {
        for (int i = firstVisibleLine; i <= lastVisibleLine && i < area.lines.size(); i++) {
            float lineY = y + i * area.lineHeight() - area.getScrollY();
            var text = area.lines.get(i);
            var drawX = x - area.getScrollX();
            var textWithFont = Component.literal(text).withStyle(style -> style.withFont(new FontDescription.Resource(textFont)));

            context.pose.pushPose();
            context.pose.translate(drawX, lineY);
            context.pose.scale(scale, scale);
            context.graphics.text(
                    font,
                    textWithFont,
                    0,
                    0,
                    area.isError() ? area.getTextAreaStyle().errorColor() : area.getTextAreaStyle().textColor(),
                    area.getTextAreaStyle().textShadow()
            );
            context.pose.popPose();
        }

        if (area.lines.size() == 1 && area.lines.getFirst().isEmpty()) {
            drawPlaceHolder(area, context, font, scale, x, y);
        }
    }

    public static void drawPlaceHolder(TextArea area, GUIContext context, Font font, float scale, float x, float y) {
        context.pose.pushPose();
        context.pose.translate(x, y);
        context.pose.scale(scale, scale);
        context.graphics.text(
                font,
                area.getTextAreaStyle().placeholder(),
                0,
                0,
                ColorPattern.LIGHT_GRAY.color,
                false
        );
        context.pose.popPose();
    }

    private static void drawSelection(TextArea area, GUIContext context, Font font, Identifier textFont, float scale, float x, float y, int firstVisibleLine, int lastVisibleLine) {
        if (area.isFocused() && area.hasSelection()) {
            var start = selectionMin(area);
            var end = selectionMax(area);
            var highlightColor = -16776961;
            var maxWidth = getMaxWidth(area, font);

            for (int line = start.line(); line <= end.line(); line++) {
                if (line < firstVisibleLine || line > lastVisibleLine) continue;

                String text = area.lines.get(line);
                int from = (line == start.line()) ? start.col() : 0;
                int to = (line == end.line()) ? end.col() : text.length();

                from = Mth.clamp(from, 0, text.length());
                to = Mth.clamp(to, 0, text.length());

                float minX = font.getSplitter().stringWidth(TextUtilities.withFont(text.substring(0, from), textFont)) * scale - area.getScrollX();
                float maxX;
                if (line == end.line()) {
                    if (from == to) continue;
                    maxX = font.getSplitter().stringWidth(TextUtilities.withFont(text.substring(0, to), textFont)) * scale - area.getScrollX();
                } else {
                    maxX = maxWidth;
                }
                float lineY = y + line * area.lineHeight() - area.getScrollY();

                DrawerHelperClient.drawSolidRect(
                        context,
                        RenderPipelines.GUI_TEXT_HIGHLIGHT,
                        x + minX,
                        lineY,
                        maxX - minX,
                        area.getTextAreaStyle().fontSize(),
                        highlightColor);
            }
        }
    }

    private static void drawCursor(TextArea area, GUIContext context, Font font, Identifier textFont, float scale, float x, float y) {
        if (area.isVisible() && area.isFocused() && area.isDisplayed() && (!area.isActive() || System.currentTimeMillis() % 1000 < 500)) {
            var current = area.lines.get(area.getCursorLine());
            float cursorPosX = font.getSplitter().stringWidth(TextUtilities.withFont(current.substring(0, area.getCursorCol()), textFont)) * scale;
            float cursorY = y + area.getCursorLine() * area.lineHeight() - area.getScrollY();
            DrawerHelperClient.drawSolidRect(
                    context,
                    x + cursorPosX - area.getScrollX(),
                    cursorY,
                    1,
                    area.getTextAreaStyle().fontSize(),
                    area.getTextAreaStyle().cursorColor()
            );
        }
    }

    private static Cursor selectionMin(TextArea area) {
        var a = new Cursor(area.getSelStartLine(), area.getSelStartCol());
        var b = new Cursor(area.getSelEndLine(), area.getSelEndCol());
        return comparePos(a, b) <= 0 ? a : b;
    }

    private static Cursor selectionMax(TextArea area) {
        var a = new Cursor(area.getSelStartLine(), area.getSelStartCol());
        var b = new Cursor(area.getSelEndLine(), area.getSelEndCol());
        return comparePos(a, b) >= 0 ? a : b;
    }

    private static int comparePos(Cursor a, Cursor b) {
        if (a.line() != b.line()) return Integer.compare(a.line(), b.line());
        return Integer.compare(a.col(), b.col());
    }

    private static float getMaxWidth(TextArea area, Font font) {
        var scale = area.scale();
        var max = 0f;
        for (String line : area.lines) {
            max = Math.max(font.getSplitter().stringWidth(TextUtilities.withFont(line, area.getTextAreaStyle().font())) * scale, max);
        }
        return max;
    }
}
