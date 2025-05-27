package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.BindableUIElement;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.style.Style;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.utils.TextUtilities;
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
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Tuple;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaOverflow;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.text.NumberFormat;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class TextField extends BindableUIElement<String> {
    @Accessors(chain = true, fluent = true)
    public static class TextFieldStyle extends Style {
        @Getter
        @Setter
        private IGuiTexture focusOverlay = Sprites.RECT_RD_T_SOLID;
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
    public enum Mode {
        STRING,
        COMPOUND_TAG,
        RESOURCE_LOCATION,
        NUMBER_LONG,
        NUMBER_INT,
        NUMBER_FLOAT,
        NUMBER_DOUBLE,
        NUMBER_SHORT,
        NUMBER_BYTE,
    }
    @Setter
    private Predicate<String> textValidator = Predicates.alwaysTrue();
    @Setter
    private Predicate<Character> charValidator = Predicates.alwaysTrue();
    @Getter
    private String text = "";
    @Getter @Setter
    private String placeholder = "empty";
    @Getter
    private final TextFieldStyle textFieldStyle = new TextFieldStyle(this);
    @Getter
    private float wheelDur;
    private NumberFormat numberInstance;
    // runtime
    @Getter
    private Mode mode = Mode.STRING;
    @Getter
    private boolean isError = false;
    @Getter
    private String rawText = "";
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
        addEventListener(UIEvents.MOUSE_WHEEL, this::onMouseWheel);
    }

    public TextField textFieldStyle(Consumer<TextFieldStyle> style) {
        style.accept(textFieldStyle);
        onStyleChanged();
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        textFieldStyle.applyStyles(values);
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        updateDisplayOffset();
    }

    /// events
    protected void onDragSource(UIEvent event) {
        if (isNumberField()) {
            var value = event.x - event.dragStartX > 4 ? wheelDur : event.x - event.dragStartX < -4 ? -wheelDur : 0;
            if (value != 0) {
                handleNumber(value);
            }
        } else if (event.dragHandler.draggingObject instanceof Integer start) {
            var cursor = getCursorUnderMouseX(event.x);
            if (cursor != -1) {
                setCursor(cursor);
                setSelection(start, cursorPos);
            }
        }
    }

    private boolean handleNumber(double value) {
        String number = null;
        if (mode == Mode.NUMBER_INT) {
           try {
               if (numberInstance != null) {
                   number = numberInstance.format(Integer.parseInt(getRawText()) + (int) (value * (isShiftDown() ? 10 : 1)));
               } else {
                   number = String.valueOf(Integer.parseInt(getRawText()) + (int) (value * (isShiftDown() ? 10 : 1)));
               }
           } catch (NumberFormatException ignored) { }
        } else if (mode == Mode.NUMBER_LONG) {
            try {
                if (numberInstance != null) {
                    number = numberInstance.format(Long.parseLong(getRawText()) + (long) (value * (isShiftDown() ? 10 : 1)));
                } else {
                    number = String.valueOf(Long.parseLong(getRawText()) + (long) (value * (isShiftDown() ? 10 : 1)));
                }
            } catch (NumberFormatException ignored) { }
        } else if (mode == Mode.NUMBER_FLOAT) {
            try {
                if (numberInstance != null) {
                    number = numberInstance.format(Float.parseFloat(getRawText()) + value * (isShiftDown() ? 10 : 1));
                } else {
                    number = String.valueOf(Float.parseFloat(getRawText()) + value * (isShiftDown() ? 10 : 1));
                }
            } catch (NumberFormatException ignored) { }
        }  else if (mode == Mode.NUMBER_DOUBLE) {
            try {
                if (numberInstance != null) {
                    number = numberInstance.format(Double.parseDouble(getRawText()) + value * (isShiftDown() ? 10 : 1));
                } else {
                    number = String.valueOf(Double.parseDouble(getRawText()) + value * (isShiftDown() ? 10 : 1));
                }
            } catch (NumberFormatException ignored) { }
        } else if (mode == Mode.NUMBER_SHORT) {
            try {
                if (numberInstance != null) {
                    number = numberInstance.format(Short.parseShort(getRawText()) + (short) (value * (isShiftDown() ? 10 : 1)));
                } else {
                    number = String.valueOf(Short.parseShort(getRawText()) + (short) (value * (isShiftDown() ? 10 : 1)));
                }
            } catch (NumberFormatException ignored) { }
        } else if (mode == Mode.NUMBER_BYTE) {
            try {
                if (numberInstance != null) {
                    number = numberInstance.format(Byte.parseByte(getRawText()) + (byte) (value * (isShiftDown() ? 10 : 1)));
                } else {
                    number = String.valueOf(Byte.parseByte(getRawText()) + (byte) (value * (isShiftDown() ? 10 : 1)));
                }
            } catch (NumberFormatException ignored) { }
        }
        if (number != null) {
            setRawText(number);
            return true;
        }
        return false;
    }

    protected void onMouseWheel(UIEvent event) {
        if (isEditable()) {
            if (handleNumber((event.deltaY > 0 ? 1 : -1) * wheelDur)) {
                event.stopPropagation();
            }
        }
    }

    protected boolean isNumberField() {
        return mode == Mode.NUMBER_INT || mode == Mode.NUMBER_LONG || mode == Mode.NUMBER_FLOAT || mode == Mode.NUMBER_DOUBLE || mode == Mode.NUMBER_SHORT || mode == Mode.NUMBER_BYTE;
    }

    protected void onMouseDown(UIEvent event) {
        if (event.button == 0 && isMouseOver(event.x, event.y)) {
            var cursor = getCursorUnderMouseX(event.x);
            if (cursor != -1) {
                var currentCursor = cursorPos;
                setCursor(cursor);
                if (isShiftDown()) {
                    setSelection(currentCursor, cursorPos);
                } else {
                    setSelection(cursorPos, cursorPos);
                }
                if (isNumberField()) {
                    startDrag(null, null);
                } else {
                    startDrag(cursorPos, null);
                }
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

    /// logic
    public TextField setText(String text, boolean notify) {
        return setValue(text, notify);
    }

    public TextField setText(String text) {
        return setText(text, true);
    }

    @Override
    public String getValue() {
        return text;
    }

    @Override
    public TextField setValue(String value, boolean notify) {
        this.rawText = value;
        if (isNumberField() && numberInstance != null) {
            this.rawText = numberInstance.format(Double.parseDouble(value));
        }
        if (!this.text.equals(value)) {
            this.text = value;
            if (notify) {
                notifyListeners();
            }
        }
        this.cursorPos = rawText.length();
        this.selectionStart = cursorPos;
        this.selectionEnd = cursorPos;
        this.formattedLineCache = null;
        updateDisplayOffset();
        return this;
    }

    public TextField setTextResponder(Consumer<String> textResponder) {
        registerValueListener(textResponder);
        return this;
    }

    protected TextField setRawText(String text) {
        this.rawText = text;
        this.cursorPos = text.length();
        this.selectionStart = cursorPos;
        this.selectionEnd = cursorPos;
        this.formattedLineCache = null;
        onRawTextUpdate();
        return this;
    }

    public TextField setCompoundTagOnly() {
        setTextValidator(s -> {
            try {
                TagParser.parseTag(s);
                return true;
            } catch (Exception ignored) { }
            return false;
        });
        style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.compound_tag")));
        return this;
    }

    public TextField setResourceLocationOnly() {
        setCharValidator(chr -> chr == ':' || ResourceLocation.isValidNamespace(Character.toString(chr)) || ResourceLocation.isAllowedInResourceLocation(chr));
        setTextValidator(LDLib::isValidResourceLocation);
        style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.resourcelocation")));
        return this;
    }

    public TextField setNumbersOnlyLong(long minValue, long maxValue) {
        mode = Mode.NUMBER_LONG;
        setTextValidator(s -> {
            try {
                long value = Long.parseLong(s);
                if (minValue <= value && value <= maxValue) return true;
            } catch (NumberFormatException ignored) { }
            return false;
        });
        setCharValidator(chr -> Character.isDigit(chr) || chr == '-' || chr == '+');
        if (minValue == Long.MIN_VALUE && maxValue == Long.MAX_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.3")));
        } else if (minValue == Long.MIN_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.2", maxValue)));
        } else if (maxValue == Long.MAX_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.1", minValue)));
        } else {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue)));
        }
        return setWheelDur(1);
    }

    public TextField setNumbersOnlyInt(int minValue, int maxValue) {
        mode = Mode.NUMBER_INT;
        setTextValidator(s -> {
            try {
                int value = Integer.parseInt(s);
                if (minValue <= value && value <= maxValue) return true;
            } catch (NumberFormatException ignored) { }
            return false;
        });
        setCharValidator(chr -> Character.isDigit(chr) || chr == '-' || chr == '+');
        if (minValue == Integer.MIN_VALUE && maxValue == Integer.MAX_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.3")));
        } else if (minValue == Integer.MIN_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.2", maxValue)));
        } else if (maxValue == Integer.MAX_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.1", minValue)));
        } else {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue)));
        }
        return setWheelDur(1);
    }

    public TextField setNumbersOnlyByte(byte minValue, byte maxValue) {
        mode = Mode.NUMBER_BYTE;
        setTextValidator(s -> {
            try {
                int value = Byte.parseByte(s);
                if (minValue <= value && value <= maxValue) return true;
            } catch (NumberFormatException ignored) { }
            return false;
        });
        setCharValidator(chr -> Character.isDigit(chr) || chr == '-' || chr == '+');
        if (minValue == Byte.MIN_VALUE && maxValue == Byte.MAX_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.3")));
        } else if (minValue == Byte.MIN_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.2", maxValue)));
        } else if (maxValue == Byte.MAX_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.1", minValue)));
        } else {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue)));
        }
        return setWheelDur(1);
    }

    public TextField setNumbersOnlyShort(short minValue, short maxValue) {
        mode = Mode.NUMBER_SHORT;
        setTextValidator(s -> {
            try {
                int value = Short.parseShort(s);
                if (minValue <= value && value <= maxValue) return true;
            } catch (NumberFormatException ignored) { }
            return false;
        });
        setCharValidator(chr -> Character.isDigit(chr) || chr == '-' || chr == '+');
        if (minValue == Short.MIN_VALUE && maxValue == Short.MAX_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.3")));
        } else if (minValue == Short.MIN_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.2", maxValue)));
        } else if (maxValue == Short.MAX_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.1", minValue)));
        } else {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue)));
        }
        return setWheelDur(1);
    }

    public TextField setNumbersOnlyFloat(float minValue, float maxValue) {
        mode = Mode.NUMBER_FLOAT;
        setTextValidator(s -> {
            try {
                float value = Float.parseFloat(s);
                if (minValue <= value && value <= maxValue) return true;
            } catch (NumberFormatException ignored) { }
            return false;
        });
        setCharValidator(chr -> chr == '.' || Character.isDigit(chr) || chr == '-' || chr == '+');
        if (minValue == -Float.MAX_VALUE && maxValue == Float.MAX_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.3")));
        } else if (minValue == -Float.MAX_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.2", maxValue)));
        } else if (maxValue == Float.MAX_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.1", minValue)));
        } else {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue)));
        }
        return setWheelDur(0.1f);
    }

    public TextField setNumbersOnlyDouble(double minValue, double maxValue) {
        mode = Mode.NUMBER_DOUBLE;
        setTextValidator(s -> {
            try {
                var value = Double.parseDouble(s);
                if (minValue <= value && value <= maxValue) return true;
            } catch (NumberFormatException ignored) { }
            return false;
        });
        setCharValidator(chr -> chr == '.' || Character.isDigit(chr) || chr == '-' || chr == '+');
        if (minValue == -Double.MAX_VALUE && maxValue == Double.MAX_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.3")));
        } else if (minValue == -Double.MAX_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.2", maxValue)));
        } else if (maxValue == Double.MAX_VALUE) {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.1", minValue)));
        } else {
            style(style -> style.setTooltips(Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue)));
        }
        return setWheelDur(0.1f);
    }

    public TextField setWheelDur(float wheelDur) {
        this.wheelDur = wheelDur;
        this.numberInstance = NumberFormat.getNumberInstance();
        numberInstance.setMaximumFractionDigits(4);
        return this;
    }

    public TextField setWheelDur(int digits, float wheelDur) {
        this.wheelDur = wheelDur;
        this.numberInstance = NumberFormat.getNumberInstance();
        numberInstance.setMaximumFractionDigits(digits);
        return this;
    }

    public String getHighlighted() {
        if (selectionStart != selectionEnd) {
            return rawText.substring(Math.min(selectionStart, selectionEnd), Math.max(selectionStart, selectionEnd));
        }
        return "";
    }

    protected void onCharTyped(UIEvent event) {
        if (!isEditable()) return;
        if (StringUtil.isAllowedChatCharacter(event.codePoint) && charValidator.test(event.codePoint)) {
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
        if (width -1 > cursorPosX) {
            displayOffset = 0;
        } else if ((cursorPosX - displayOffset) > width - 1) {
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
            isError = false;
            if (!text.equals(rawText)) {
                text = rawText;
                notifyListeners();
            }
        } else {
            isError = true;
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
                    Component.literal(rawText.isEmpty() ? placeholder : rawText),
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
        if (isChildHover() || isFocused()) {
            getTextFieldStyle().focusOverlay().draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), partialTicks);
        }
        super.drawBackgroundOverlay(graphics, mouseX, mouseY, partialTicks);
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
        graphics.drawString(font, line, 0, 0, rawText.isEmpty() ?
                ColorPattern.LIGHT_GRAY.color : (isError ? textFieldStyle.errorColor : textFieldStyle.textColor),
                !rawText.isEmpty() && textFieldStyle.textShadow);
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
