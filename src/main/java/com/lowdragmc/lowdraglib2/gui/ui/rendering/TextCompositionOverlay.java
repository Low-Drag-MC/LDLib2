package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.TextCursorUtils;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

/**
 * An input method's in-progress composition, drawn in a small box beside the caret of the text element
 * it is for — the pinyin being typed before a character is picked, say.
 *
 * <p>LDLib2's counterpart of vanilla's {@code IMEPreeditOverlay}, and deliberately the same to look at.
 * It differs in one thing: the candidate area is reported to whichever window is hosting the UI, which
 * need not be the game's — vanilla's reports to the game window unconditionally.
 *
 * <p>Drawn as a deferred element, on top of everything else in the frame, so a composition near the
 * edge of a clipped scroll view is not cut off with it. That also means it is positioned in screen gui
 * coordinates, which is why {@link #submit} transforms the caret out of the element's pose first.
 */
public final class TextCompositionOverlay implements Renderable {
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("widget/preedit");
    private static final Style FOCUSED_STYLE = Style.EMPTY.withUnderlined(true);
    private static final int SEPARATION_FROM_INPUT = 4;
    private static final int BORDER_OFFSET = 5;
    private static final int TEXT_COLOR = 0xFF000000;

    private final ModularUI modularUI;
    private final Font font;
    private final Component text;
    private final int textWidth;
    private final int caretOffset;
    private final int inputLeft;
    private final int inputTop;
    private final int inputHeight;
    private final long initTimeMs;

    private TextCompositionOverlay(ModularUI modularUI, PreeditEvent preedit, int inputLeft, int inputTop, int inputHeight) {
        this.modularUI = modularUI;
        this.font = Minecraft.getInstance().font;
        this.text = preedit.toFormattedText(FOCUSED_STYLE).withColor(TEXT_COLOR);
        this.textWidth = font.width(text);
        this.caretOffset = font.width(preedit.fullText().substring(0, preedit.caretPosition()));
        this.inputLeft = inputLeft;
        this.inputTop = inputTop;
        this.inputHeight = inputHeight;
        this.initTimeMs = Util.getMillis();
    }

    /**
     * Reports where a focused text element's caret is, and draws the composition there if one is in
     * progress. Called from the element's renderer every frame it is focused and editable.
     *
     * @param caretX      the caret's left edge, in the element's local coordinates
     * @param caretY      the caret's top edge, likewise
     * @param caretHeight the caret's height, likewise
     */
    public static void submit(ModularUI modularUI, GUIContext context, @Nullable Object preedit,
                              float caretX, float caretY, float caretHeight) {
        var top = context.pose.pose.transformPosition(caretX, caretY, new Vector2f());
        var bottom = context.pose.pose.transformPosition(caretX, caretY + caretHeight, new Vector2f());
        var height = Math.max(1, Math.round(bottom.y - top.y));
        if (preedit instanceof PreeditEvent composition) {
            context.graphics.setPreeditOverlay(new TextCompositionOverlay(modularUI, composition,
                    Math.round(top.x), Math.round(top.y), height));
        } else {
            // Nothing being composed yet: still tell the input method where the caret is, so its
            // candidate list opens beside it as soon as composing starts.
            ModularUIClientAccess.setTextInputArea(modularUI, top.x, top.y, top.x + 1, top.y + height);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        var left = inputLeft;
        var right = left + textWidth;
        if (right > graphics.guiWidth()) {
            left = graphics.guiWidth() - textWidth;
            right = left + textWidth;
        }
        var bottom = inputTop + inputHeight + SEPARATION_FROM_INPUT + font.lineHeight;
        if (bottom > graphics.guiHeight()) {
            bottom = inputTop - SEPARATION_FROM_INPUT;
        }
        var top = bottom - font.lineHeight;
        ModularUIClientAccess.setTextInputArea(modularUI, left, top, right, bottom);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, left - BORDER_OFFSET, top - BORDER_OFFSET,
                right - left + BORDER_OFFSET * 2, bottom - top + BORDER_OFFSET * 2);
        graphics.text(font, text, left, top, TEXT_COLOR, false);
        if (TextCursorUtils.isCursorVisible(Util.getMillis() - initTimeMs)) {
            TextCursorUtils.extractInsertCursor(graphics, left + caretOffset, top, TEXT_COLOR, font.lineHeight + 1);
        }
    }
}
