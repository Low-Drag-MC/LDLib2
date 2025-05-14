package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.style.Style;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.utils.TextUtilities;
import com.sun.jna.platform.win32.GL;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Tuple;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaOverflow;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class TextField extends UIElement {
    @Accessors(chain = true, fluent = true)
    public static class TextFieldStyle extends Style {
        @Getter
        @Setter
        private IGuiTexture focusTexture = Sprites.RECT_RD_T_SOLID;
        @Getter @Setter
        private float fontSize = 9;
        @Getter @Setter
        private int textColor = -1;
        @Getter @Setter
        private int errorColor = 0xffff0000;
        @Getter @Setter
        private int cursorColor = 0xffeeeeee;
        @Getter @Setter
        private boolean textShadow = true;

        public TextFieldStyle(UIElement holder) {
            super(holder);
        }
    }
    @Getter @Setter
    private Predicate<String> textValidator = Predicates.alwaysTrue();
    @Getter @Setter
    private String rawText = "DDDaaaaffffDFAEF";
    @Getter @Setter
    private String placeholder = "input here...";
    @Getter
    private final TextFieldStyle textFieldStyle = new TextFieldStyle(this);
    // runtime
    @Getter
    private int cursorPos;
    @Getter
    private int selectionStart;
    @Getter
    private int selectionEnd;
    @Getter
    private float displayOffset;
    /**
     * The formatted text to be displayed in the line and its width.
     */
    @Nullable
    private Tuple<FormattedCharSequence, Float> formattedLineCache = null;

    public TextField() {
        getLayout().setHeight(14);
        getLayout().setPadding(YogaEdge.ALL, 2);
        getStyle().backgroundTexture(Sprites.RECT_RD_SOLID);
        getLayoutNode().setOverflow(YogaOverflow.HIDDEN);
        setFocusable(true);
        addEventListener(UIEvents.CHAR_TYPED, this::onCharTyped);
        addEventListener(UIEvents.KEY_DOWN, this::onKeyDown);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSource);
    }

    protected void onDragSource(UIEvent event) {
        if (event.dragHandler.draggingObject instanceof Integer start) {
            var cursor = getCursorUnderMouseX(event.x);
            if (cursor != -1) {
                setCursor(cursor);
                setSelection(start, cursorPos);
            }
        }
    }

    protected void onMouseDown(UIEvent event) {
        if (event.button == 0 && isMouseOver(event.x, event.y)) {
            var cursor = getCursorUnderMouseX(event.x);
            if (cursor != -1) {
                setCursor(cursor);
                setSelection(cursorPos, cursorPos);
                startDrag(cursorPos, null);
            }
        }
    }

    protected void onKeyDown(UIEvent event) {
        switch (event.keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (isEditable()) {
                    deleteText(-1);
                }
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (isEditable()) {
                    deleteText(1);
                }
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (event.isCtrlDown()) {
                    setCursor(getWordPosition(-1));
                } else {
                    setCursor(getCursorPos(-1));
                }
                if (isShiftDown()) {
                    if (cursorPos > selectionStart) {
                        setSelection(selectionStart, cursorPos);
                    } else {
                        setSelection(cursorPos, selectionEnd);
                    }
                } else {
                    setSelection(cursorPos, cursorPos);
                }
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (event.isCtrlDown()) {
                    setCursor(getWordPosition(1));
                } else {
                    setCursor(getCursorPos(1));
                }
                if (isShiftDown()) {
                    if (cursorPos < selectionEnd) {
                        setSelection(cursorPos, selectionEnd);
                    } else {
                        setSelection(selectionStart, cursorPos);
                    }
                } else {
                    setSelection(cursorPos, cursorPos);
                }
            }
            case GLFW.GLFW_KEY_HOME -> {
                setCursor(0);
                if (isShiftDown()) {
                    if (cursorPos > selectionStart) {
                        setSelection(selectionStart, cursorPos);
                    } else {
                        setSelection(cursorPos, selectionEnd);
                    }
                } else {
                    setSelection(cursorPos, cursorPos);
                }
            }
            case GLFW.GLFW_KEY_END -> {
                setCursor(rawText.length());
                if (isShiftDown()) {
                    if (cursorPos < selectionEnd) {
                        setSelection(cursorPos, selectionEnd);
                    } else {
                        setSelection(selectionStart, cursorPos);
                    }
                } else {
                    setSelection(cursorPos, cursorPos);
                }
            }
            default -> {
                if (Screen.isSelectAll(event.keyCode)) {
                    setCursor(rawText.length());
                    setSelection(0, rawText.length());
                } else if (Screen.isCopy(event.keyCode)) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                } else if (Screen.isPaste(event.keyCode)) {
                    if (this.isEditable()) {
                        this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
                    }
                } else {
                    if (Screen.isCut(event.keyCode)) {
                        Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                        if (this.isEditable()) {
                            this.insertText("");
                        }
                    }
                }
            }
        }
    }

    public String getHighlighted() {
        if (selectionStart != selectionEnd) {
            return rawText.substring(Math.min(selectionStart, selectionEnd), Math.max(selectionStart, selectionEnd));
        }
        return "";
    }

    protected void onCharTyped(UIEvent event) {
        if (!isEditable()) return;
        if (StringUtil.isAllowedChatCharacter(event.codePoint)) {
            this.insertText(Character.toString(event.codePoint));
        }
    }

    public boolean isEditable() {
        return isActive() && isVisible() && isFocused() && isDisplayed();
    }

    private void deleteText(int count) {
        if (count == 0) {
            return;
        }
        if (Screen.hasControlDown()) {
            this.deleteWords(count);
        } else {
            this.deleteChars(count);
        }
    }

    public void setCursor(int pos) {
        this.cursorPos = Mth.clamp(pos, 0, this.rawText.length());
        updateDisplayOffset();
    }

    public void setSelection(int start, int end) {
        var min = Math.min(start, end);
        var max = Math.max(start, end);
        this.selectionStart = Mth.clamp(min, 0, this.rawText.length());
        this.selectionEnd = Mth.clamp(max, 0, this.rawText.length());
    }

    /**
     * Deletes the given number of words from the current cursor's position, unless there is currently a selection, in which case the selection is deleted instead.
     */
    public void deleteWords(int num) {
        if (!this.rawText.isEmpty()) {
            if (this.selectionStart != this.selectionEnd) {
                this.insertText("");
            } else {
                this.deleteCharsToPos(this.getWordPosition(num));
            }
        }
    }

    /**
     * Gets the starting index of the word at the specified number of words away from the cursor position.
     */
    public int getWordPosition(int numWords) {
        return this.getWordPosition(numWords, getCursorPos());
    }

    /**
     * Gets the starting index of the word at a distance of the specified number of words away from the given position.
     */
    private int getWordPosition(int numWords, int pos) {
        return this.getWordPosition(numWords, pos, true);
    }

    /**
     * Like getNthWordFromPos (which wraps this), but adds option for skipping consecutive spaces
     */
    private int getWordPosition(int numWords, int pos, boolean skipConsecutiveSpaces) {
        int i = pos;
        boolean flag = numWords < 0;
        int j = Math.abs(numWords);

        for (int k = 0; k < j; k++) {
            if (!flag) {
                int l = this.rawText.length();
                i = this.rawText.indexOf(32, i);
                if (i == -1) {
                    i = l;
                } else {
                    while (skipConsecutiveSpaces && i < l && this.rawText.charAt(i) == ' ') {
                        i++;
                    }
                }
            } else {
                while (skipConsecutiveSpaces && i > 0 && this.rawText.charAt(i - 1) == ' ') {
                    i--;
                }

                while (i > 0 && this.rawText.charAt(i - 1) != ' ') {
                    i--;
                }
            }
        }

        return i;
    }

    private int getCursorPos(int delta) {
        return Util.offsetByCodepoints(this.rawText, this.cursorPos, delta);
    }

    /**
     * Deletes the given number of characters from the current cursor's position, unless there is currently a selection, in which case the selection is deleted instead.
     */
    public void deleteChars(int num) {
        this.deleteCharsToPos(this.getCursorPos(num));
    }

    private void updateDisplayOffset() {
        // make sure the cursor is in the display area
        var scale = textFieldStyle.fontSize / getFont().lineHeight;
        var cursorPosX = getFont().width(rawText.substring(0, cursorPos)) * scale;
        var width = getContentWidth();
        if ((cursorPosX - displayOffset) > width - 1) {
            displayOffset = Math.max(cursorPosX - width + 1, 0);
        } else if ((cursorPosX - displayOffset) < 0) {
            displayOffset = Math.max(cursorPosX, 0);
        }
    }

    public void deleteCharsToPos(int pos) {
        if (!this.rawText.isEmpty()) {
            if (this.selectionStart != this.selectionEnd) {
                this.insertText("");
            } else {
                int i = Math.min(pos, this.cursorPos);
                int j = Math.max(pos, this.cursorPos);
                if (i != j) {
                    rawText = new StringBuilder(this.rawText).delete(i, j).toString();
                    cursorPos = i;
                    formattedLineCache = null;
                    onRawTextUpdate();
                }
            }
        }
    }

    /**
     * Adds the given text after the cursor, or replaces the currently selected text if there is a selection.
     */
    public void insertText(String textToWrite) {
        if (selectionStart != selectionEnd) {
            rawText = rawText.substring(0, selectionStart) + rawText.substring(selectionEnd);
            cursorPos = selectionStart;
        }
        rawText = rawText.substring(0, cursorPos) + textToWrite + rawText.substring(cursorPos);
        cursorPos += textToWrite.length();
        selectionStart = cursorPos;
        selectionEnd = cursorPos;
        formattedLineCache = null;
        onRawTextUpdate();
    }

    /**
     * It should be called when the raw text is changed. we will check text validator and notify the change.
     */
    protected void onRawTextUpdate() {
        updateDisplayOffset();
        if (textValidator.test(rawText)) {

        }
    }

    /**
     * Gets the cursor position under the mouse.
     * @return The cursor position, -1 if not found.
     */
    public int getCursorUnderMouseX(double mouseX) {
        var x = getContentX();

        var scale = textFieldStyle.fontSize / getFont().lineHeight;
        var availableWidth = ((mouseX - x + displayOffset) * scale);
        var subText = getFont().plainSubstrByWidth(rawText, (int) availableWidth);
        var length = getFont().width(subText) * scale;
        if (subText.length() >= rawText.length()) {
            return rawText.length();
        }
        var nextCharWidth = getFont().width(rawText.substring(subText.length(), subText.length() + 1)) * scale;
        return (availableWidth - length) - nextCharWidth / 2f > 0 ? (subText.length() + 1) : subText.length();
    }


    /// rendering
    public Font getFont() {
        return Minecraft.getInstance().font;
    }

    public Tuple<FormattedCharSequence, Float> getFormattedLine() {
        if (formattedLineCache == null) {
            var lines = TextUtilities.computeFormattedLines(
                    getFont(),
                    Component.literal(rawText),
                    getTextFieldStyle().fontSize(),
                    Float.MAX_VALUE
            );
            if (lines.isEmpty()) {
                formattedLineCache = new Tuple<>(FormattedCharSequence.EMPTY, 0f);
            } else {
                formattedLineCache = lines.getFirst();
            }
        }
        return formattedLineCache;
    }

    @Override
    public void drawBackgroundOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackgroundOverlay(graphics, mouseX, mouseY, partialTicks);
        if (isHover() || isFocused()) {
            getTextFieldStyle().focusTexture().draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), partialTicks);
        }
    }

    @Override
    public void drawBackgroundAdditional(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var x = getContentX();
        var y = getContentY();
        var width = getContentWidth();
        var height = getContentHeight();
        var formattedLine = getFormattedLine();
        var font = getFont();
        var scale = textFieldStyle.fontSize / font.lineHeight;

        var lineY = y + (height - textFieldStyle.fontSize) / 2;
        var line = formattedLine.getA();
        var lineX = x - displayOffset;

        // draw the text line
        graphics.pose().pushPose();
        graphics.pose().translate(lineX, lineY, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, line, 0, 0, textFieldStyle.textColor, textFieldStyle.textShadow);
        graphics.pose().popPose();

        // draw highlight
        if (selectionStart != selectionEnd) {
            var minX = font.width(rawText.substring(0, selectionStart)) * scale - displayOffset;
            var maxX = font.width(rawText.substring(0, selectionEnd)) * scale - displayOffset;
            graphics.fill(RenderType.guiTextHighlight(),
                    (int) (x + minX),
                    (int) lineY,
                    (int) (x + maxX),
                    (int) (lineY + textFieldStyle.fontSize), -16776961);
        }
        // draw cursor
        var cursorPosX = font.width(rawText.substring(0, cursorPos)) * scale;
        if (isFocused() && System.currentTimeMillis() % 1000 < 500) {
            graphics.fill(
                    (int) (x + cursorPosX - displayOffset),
                    (int) lineY,
                    (int) (x + cursorPosX - displayOffset + 1),
                    (int) (lineY + textFieldStyle.fontSize),
                    textFieldStyle.cursorColor);
        }
    }
}
