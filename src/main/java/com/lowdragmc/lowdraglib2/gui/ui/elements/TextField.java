package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigFont;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.TextUtilities;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Tuple;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
@LDLRegister(name = "text_field", registry = "ldlib2:ui_element")
public class TextField extends BindableUIElement<String> {
    private record NumberStart(double value){}
    private record CursorStart(int value){}
    @Accessors(chain = true, fluent = true)
    public static class TextFieldStyle extends Style {
        @Getter
        @Setter
        @Configurable(name = "focusOverlay")
        private IGuiTexture focusOverlay = Sprites.RECT_RD_T_SOLID;
        @Getter @Setter
        @Configurable(name = "fontSize")
        private float fontSize = 9;
        @Getter @Setter
        @Configurable(name = "font")
        @ConfigFont
        private ResourceLocation font = net.minecraft.network.chat.Style.DEFAULT_FONT;
        @Getter @Setter
        @Configurable(name = "textColor")
        @ConfigColor
        private int textColor = -1;
        @Getter @Setter
        @Configurable(name = "errorColor")
        @ConfigColor
        private int errorColor = 0xffff0000;
        @Getter @Setter
        @Configurable(name = "cursorColor")
        @ConfigColor
        private int cursorColor = 0xffeeeeee;
        @Getter @Setter
        @Configurable(name = "textShadow")
        private boolean textShadow = true;
        @Getter @Setter
        @Configurable(name = "placeholder")
        private Component placeholder = Component.translatable("text_field.empty");

        public TextFieldStyle(UIElement holder) {
            super(holder);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void buildConfigurator(ConfiguratorGroup father) {
            super.buildConfigurator(father);
            if (holder instanceof TextField textField) {
                father.addEventListener(Configurator.CHANGE_EVENT, event -> textField.updateDisplayOffset());
            }
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
    @Getter
    @Configurable(name = "textFieldStyle", subConfigurable = true)
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
    @Configurable(name = "value")
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
        addEventListener(UIEvents.BLUR, this::onBlur);
    }

    public TextField textFieldStyle(Consumer<TextFieldStyle> style) {
        style.accept(textFieldStyle);
        onStyleChanged();
        updateDisplayOffset();
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        textFieldStyle.applyStyles(values);
        updateDisplayOffset();
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        updateDisplayOffset();
    }

    /// events
    protected void onDragSource(UIEvent event) {
        if (isNumberField()) {
            if (event.dragHandler.draggingObject instanceof NumberStart(double numberStart)) {
                if (Mth.abs(event.x - event.dragStartX) < 4) {
                    handleNumber(numberStart, false);
                } else {
                    var value = ((int)((event.x - event.dragStartX) / 4))
                            * (isShiftDown() ? wheelDur * 10 : wheelDur) + numberStart;
                    handleNumber(value, false);
                }
            }
        } else if (event.dragHandler.draggingObject instanceof CursorStart(int cursorStart)) {
            var cursor = getCursorUnderMouseX(event.x);
            if (cursor != -1) {
                setCursor(cursor);
                setSelection(cursorStart, cursorPos);
            }
        }
    }

    private boolean handleNumber(double value, boolean append) {
        String number = null;
        if (mode == Mode.NUMBER_INT) {
           try {
               if (numberInstance != null) {
                   number = numberInstance.format(append ? (Integer.parseInt(getRawText()) + (int) (value * (isShiftDown() ? 10 : 1))) : (int) value);
               } else {
                   number = String.valueOf(append ? (Integer.parseInt(getRawText()) + (int) (value * (isShiftDown() ? 10 : 1))) : (int) value);
               }
           } catch (NumberFormatException ignored) { }
        } else if (mode == Mode.NUMBER_LONG) {
            try {
                if (numberInstance != null) {
                    number = numberInstance.format(append ? (Long.parseLong(getRawText()) + (long) (value * (isShiftDown() ? 10 : 1))) : (long) value);
                } else {
                    number = String.valueOf(append ? (Long.parseLong(getRawText()) + (long) (value * (isShiftDown() ? 10 : 1))) : (long) value);
                }
            } catch (NumberFormatException ignored) { }
        } else if (mode == Mode.NUMBER_FLOAT) {
            try {
                if (numberInstance != null) {
                    number = numberInstance.format(append ? (Float.parseFloat(getRawText()) + value * (isShiftDown() ? 10 : 1)) : (float) value);
                } else {
                    number = String.valueOf(append ? (Float.parseFloat(getRawText()) + value * (isShiftDown() ? 10 : 1)) : (float) value);
                }
            } catch (NumberFormatException ignored) { }
        }  else if (mode == Mode.NUMBER_DOUBLE) {
            try {
                if (numberInstance != null) {
                    number = numberInstance.format(append ? (Double.parseDouble(getRawText()) + value * (isShiftDown() ? 10 : 1)) : value);
                } else {
                    number = String.valueOf(append ? (Double.parseDouble(getRawText()) + value * (isShiftDown() ? 10 : 1)) : value);
                }
            } catch (NumberFormatException ignored) { }
        } else if (mode == Mode.NUMBER_SHORT) {
            try {
                if (numberInstance != null) {
                    number = numberInstance.format(append ? (Short.parseShort(getRawText()) + (short) (value * (isShiftDown() ? 10 : 1))) : (short) value);
                } else {
                    number = String.valueOf(append ? (Short.parseShort(getRawText()) + (short) (value * (isShiftDown() ? 10 : 1))) : (short) value);
                }
            } catch (NumberFormatException ignored) { }
        } else if (mode == Mode.NUMBER_BYTE) {
            try {
                if (numberInstance != null) {
                    number = numberInstance.format(append ? (Byte.parseByte(getRawText()) + (byte) (value * (isShiftDown() ? 10 : 1))) : (byte) value);
                } else {
                    number = String.valueOf(append ? (Byte.parseByte(getRawText()) + (byte) (value * (isShiftDown() ? 10 : 1))) : (byte) value);
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
            if (handleNumber((event.deltaY > 0 ? 1 : -1) * wheelDur, true)) {
                event.stopPropagation();
            }
        }
    }

    protected void onBlur(UIEvent event) {
        // remove highlight if lose focus
        if (selectionStart != selectionEnd) {
            setSelection(cursorPos, cursorPos);
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
                    var startValue = 0d;
                    try {
                        startValue = Double.parseDouble(getRawText());
                    } catch (NumberFormatException ignored) {}
                    startDrag(new NumberStart(startValue), null);
                } else {
                    startDrag(new CursorStart(cursorPos), null);
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
                    ClipboardManager.INSTANCE.copyDirect(this.getHighlighted());
                } else if (Screen.isPaste(event.keyCode)) {
                    if (this.isEditable()) {
                        this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
                    }
                } else {
                    if (Screen.isCut(event.keyCode)) {
                        ClipboardManager.INSTANCE.copyDirect(this.getHighlighted());
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

    @ConfigSetter(field = "rawText")
    public TextField setText(String text) {
        return setText(text, true);
    }

    @Override
    public String getValue() {
        return text;
    }

    @Override
    public TextField setValue(@Nullable String value, boolean notify) {
        if (value == null) value = "";
        this.rawText = value;
        if (isNumberField() && numberInstance != null) {
            switch (mode) {
                case NUMBER_INT -> this.rawText = numberInstance.format(Integer.parseInt(value));
                case NUMBER_FLOAT -> this.rawText = numberInstance.format(Float.parseFloat(value));
                case NUMBER_DOUBLE -> this.rawText = numberInstance.format(Double.parseDouble(value));
                case NUMBER_BYTE ->  this.rawText = numberInstance.format(Byte.parseByte(value));
                case NUMBER_SHORT ->  this.rawText = numberInstance.format(Short.parseShort(value));
                case NUMBER_LONG ->  this.rawText = numberInstance.format(Long.parseLong(value));
            }
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

    public TextField setAnyString() {
        mode = Mode.STRING;
        setCharValidator(Predicates.alwaysTrue());
        setTextValidator(Predicates.alwaysTrue());
        style(style -> style.setTooltips(new String[0]));
        return this;
    }

    public TextField setCompoundTagOnly() {
        mode = Mode.COMPOUND_TAG;
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
        mode = Mode.RESOURCE_LOCATION;
        setCharValidator(chr -> chr == ':' || ResourceLocation.isValidNamespace(Character.toString(chr)) || ResourceLocation.isAllowedInResourceLocation(chr));
        setTextValidator(LDLib2::isValidResourceLocation);
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
        return setWheelDur(4, wheelDur);
    }

    public TextField setWheelDur(int digits, float wheelDur) {
        this.wheelDur = wheelDur;
        this.numberInstance = NumberFormat.getNumberInstance();
        numberInstance.setGroupingUsed(false);
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
        if (!LDLib2.isClient()) return;
        // Keep cursor inside viewport; prefer placing cursor at the right edge when scrolling
        var scale = textFieldStyle.fontSize / getFont().lineHeight;
        var cursorPosX = getFont().width(TextUtilities.withFont(rawText.substring(0, cursorPos), getTextFieldStyle().font())) * scale;
        var width = getContentWidth();
        float rightPad = 1f;

        // Cursor position relative to current viewport
        var rel = cursorPosX - displayOffset;

        if (rel > width - rightPad || rel < 0) {
            // Cursor is out of view: scroll so it sticks to the right edge (or clamp to 0 if not enough content)
            displayOffset = Math.max(cursorPosX - width + rightPad, 0);
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
        var length = getFont().width(TextUtilities.withFont(subText, getTextFieldStyle().font())) * scale;
        if (subText.length() >= rawText.length()) {
            return rawText.length();
        }
        var nextCharWidth = getFont().width(TextUtilities.withFont(rawText.substring(subText.length(), subText.length() + 1), getTextFieldStyle().font())) * scale;
        return (availableWidth - length) - nextCharWidth / 2f > 0 ? (subText.length() + 1) : subText.length();
    }


    /// rendering
    public Font getFont() {
        return Minecraft.getInstance().font;
    }

    public Tuple<FormattedCharSequence, Float> getFormattedLine() {
        if (formattedLineCache == null) {
            var font = getTextFieldStyle().font();
            var text = rawText.isEmpty() ? textFieldStyle.placeholder() : Component.literal(rawText);
            var textWithFont = font.equals(net.minecraft.network.chat.Style.DEFAULT_FONT) ? text : text.copy().withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(font));
            var lines = TextUtilities.computeFormattedLines(
                    getFont(),
                    textWithFont,
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
    public void drawBackgroundOverlay(GUIContext guiContext) {
        if (isChildHover() || isFocused()) {
            guiContext.drawTexture(getTextFieldStyle().focusOverlay(), getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        }
        super.drawBackgroundOverlay(guiContext);
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
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
        RenderSystem.depthMask(false);
        guiContext.pose.pushPose();
        guiContext.pose.translate(lineX, lineY, 0);
        guiContext.pose.scale(scale, scale, 1);
        guiContext.graphics.drawString(font, line, 0, 0, rawText.isEmpty() ?
                ColorPattern.LIGHT_GRAY.color : (isError ? textFieldStyle.errorColor : textFieldStyle.textColor),
                !rawText.isEmpty() && textFieldStyle.textShadow);
        guiContext.pose.popPose();
        RenderSystem.depthMask(true);

        // draw highlight
        if (isFocused() && selectionStart != selectionEnd) {
            var minX = font.width(TextUtilities.withFont(rawText.substring(0, selectionStart), getTextFieldStyle().font())) * scale - displayOffset;
            var maxX = font.width(TextUtilities.withFont(rawText.substring(0, selectionEnd), getTextFieldStyle().font())) * scale - displayOffset;
            DrawerHelper.drawSolidRect(guiContext.graphics,
                    RenderType.guiTextHighlight(),
                    x + minX,
                    lineY,
                    maxX - minX,
                    textFieldStyle.fontSize, -16776961);
        }
        // draw cursor
        var cursorPosX = font.width(TextUtilities.withFont(rawText.substring(0, cursorPos), getTextFieldStyle().font())) * scale;
        if (isFocused() && System.currentTimeMillis() % 1000 < 500) {
            DrawerHelper.drawSolidRect(guiContext.graphics,
                    x + cursorPosX - displayOffset,
                    lineY,
                    1,
                    textFieldStyle.fontSize,
                    textFieldStyle.cursorColor);
        }
    }
}
