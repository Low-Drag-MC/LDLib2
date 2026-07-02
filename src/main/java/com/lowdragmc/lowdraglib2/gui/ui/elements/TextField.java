package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.math.Range;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue;
import com.lowdragmc.lowdraglib2.utils.HistoryStack;
import com.lowdragmc.lowdraglib2.utils.TextUtilities;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
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
import org.appliedenergistics.yoga.*;
import org.lwjgl.glfw.GLFW;
import org.w3c.dom.Element;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.text.NumberFormat;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "text-field", group = "basic", registry = "ldlib2:ui_element")
public class TextField extends BindableUIElement<String> {
    private record NumberStart(double value){}
    private record CursorStart(int value){}
    @Configurable(name = "TextFieldStyle")
    public class TextFieldStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.FOCUS_OVERLAY,
                PropertyRegistry.FONT,
                PropertyRegistry.FONT_SIZE,
                PropertyRegistry.TEXT_COLOR,
                PropertyRegistry.ERROR_COLOR,
                PropertyRegistry.CURSOR_COLOR,
                PropertyRegistry.TEXT_SHADOW,
                PropertyRegistry.PLACEHOLDER,
        };

        public TextFieldStyle() {
            super(TextField.this);
            setDefault(PropertyRegistry.FOCUS_OVERLAY, Sprites.RECT_RD_T_SOLID);
        }

        public static void init() {
            PropertyRegistry.FONT_SIZE.addListener(TextFieldStyle::onPropertyChanged);
            PropertyRegistry.FONT.addListener(TextFieldStyle::onPropertyChanged);
        }

        private static <T> void onPropertyChanged(UIElement element, Property<T> property, @Nullable T oldValue, @Nullable T newValue) {
            if (element instanceof TextField textField) {
                textField.onTextFieldStyleChanged();
            }
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public ResourceLocation font() {
            return getValueSave(PropertyRegistry.FONT);
        }

        public TextFieldStyle font(ResourceLocation font) {
            set(PropertyRegistry.FONT, font);
            return this;
        }

        public float fontSize() {
            return getValueSave(PropertyRegistry.FONT_SIZE);
        }

        public TextFieldStyle fontSize(float fontSize) {
            set(PropertyRegistry.FONT_SIZE, fontSize);
            return this;
        }

        public int textColor() {
            return getValueSave(PropertyRegistry.TEXT_COLOR);
        }

        public TextFieldStyle textColor(int textColor) {
            set(PropertyRegistry.TEXT_COLOR, textColor);
            return this;
        }

        public int errorColor() {
            return getValueSave(PropertyRegistry.ERROR_COLOR);
        }

        public TextFieldStyle errorColor(int errorColor) {
            set(PropertyRegistry.ERROR_COLOR, errorColor);
            return this;
        }

        public int cursorColor() {
            return getValueSave(PropertyRegistry.CURSOR_COLOR);
        }

        public TextFieldStyle cursorColor(int cursorColor) {
            set(PropertyRegistry.CURSOR_COLOR, cursorColor);
            return this;
        }

        public boolean textShadow() {
            return getValueSave(PropertyRegistry.TEXT_SHADOW);
        }

        public TextFieldStyle textShadow(boolean textShadow) {
            set(PropertyRegistry.TEXT_SHADOW, textShadow);
            return this;
        }

        public Component placeholder() {
            return getValueSave(PropertyRegistry.PLACEHOLDER);
        }

        public TextFieldStyle placeholder(Component placeholder) {
            set(PropertyRegistry.PLACEHOLDER, placeholder);
            return this;
        }

        public IGuiTexture focusOverlay() {
            return getValueSave(PropertyRegistry.FOCUS_OVERLAY);
        }

        public TextFieldStyle focusOverlay(IGuiTexture focusOverlay) {
            set(PropertyRegistry.FOCUS_OVERLAY, focusOverlay);
            return this;
        }
    }
    public enum Mode {
        INTERNAL,
        STRING,
        COMPOUND_TAG,
        RESOURCE_LOCATION,
        NUMBER_LONG,
        NUMBER_INT,
        NUMBER_FLOAT,
        NUMBER_DOUBLE,
        NUMBER_SHORT,
        NUMBER_BYTE
        ;

        public boolean isNumber() {
            return this == Mode.NUMBER_LONG || this == Mode.NUMBER_INT || this == Mode.NUMBER_FLOAT || this == Mode.NUMBER_DOUBLE || this == Mode.NUMBER_SHORT || this == Mode.NUMBER_BYTE;
        }

        @Nullable
        public ConfigNumber.Type getNumberType() {
            return switch (this) {
                case NUMBER_LONG -> ConfigNumber.Type.LONG;
                case NUMBER_INT -> ConfigNumber.Type.INTEGER;
                case NUMBER_FLOAT -> ConfigNumber.Type.FLOAT;
                case NUMBER_DOUBLE -> ConfigNumber.Type.DOUBLE;
                case NUMBER_SHORT -> ConfigNumber.Type.SHORT;
                case NUMBER_BYTE -> ConfigNumber.Type.BYTE;
                default -> null;
            };
        }
    }

    @Setter
    private Predicate<String> textValidator = Predicates.alwaysTrue();
    @Setter
    private Predicate<Character> charValidator = Predicates.alwaysTrue();
    @Getter
    private String text = "";
    @Getter
    private final TextFieldStyle textFieldStyle = new TextFieldStyle();
    @Getter
    private float wheelDur;
    private NumberFormat numberInstance;

    // editor support
    @Configurable(name = "EditorMode")
    @ConfigSelector(subConfiguratorBuilder = "editorModeSubConfigurator")
    private Mode editorMode = Mode.INTERNAL;
    @Persisted
    private String editorRegexValidator = "";
    @Persisted
    private Range editorRange = Range.of(0, 100);

    // runtime
    @Getter
    private final HistoryStack<String> historyStack = new HistoryStack<>(100);
    @Getter
    private Mode mode = Mode.STRING;
    @Getter
    private boolean isError = false;
    @Getter
    @Configurable(name = "value")
    private String rawText = "";
    @Getter @Setter
    @Nullable
    private Function<String, Component> formatter = null;
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
        getLayout().height(14);
        getLayout().paddingAll(2);
        getStyle().backgroundTexture(Sprites.RECT_RD_SOLID);
        setOverflowVisible(false);
        setFocusable(true);
        addEventListener(UIEvents.CHAR_TYPED, this::onCharTyped);
        addEventListener(UIEvents.KEY_DOWN, this::onKeyDown);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSource);
        addEventListener(UIEvents.MOUSE_WHEEL, this::onMouseWheel);
        addEventListener(UIEvents.BLUR, this::onBlur);
        addEventListener(UIEvents.VALIDATE_COMMAND, this::onValidateCommand);
        addEventListener(UIEvents.EXECUTE_COMMAND, this::onExecuteCommand);
        internalSetup();
    }

    public TextField textFieldStyle(Consumer<TextFieldStyle> style) {
        style.accept(textFieldStyle);
        return this;
    }

    protected void onTextFieldStyleChanged() {
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
                var localMouse = getLocalMouse(event.x, event.y);
                var localStart = getLocalMouse(event.dragStartX, event.dragStartY);
                if (Mth.abs(localMouse.x - localStart.x) < 4) {
                    handleNumber(numberStart, false);
                } else {
                    var value = ((int)((localMouse.x - localStart.x) / 4))
                            * (isShiftDown() ? wheelDur * 10 : wheelDur) + numberStart;
                    handleNumber(value, false);
                }
            }
        } else if (event.dragHandler.draggingObject instanceof CursorStart(int cursorStart)) {
            var cursor = getCursorUnderMouseX(getLocalMouse(event.x, event.y).x);
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
                    number = numberInstance.format(append ? (LocalizedNumberText.parseFloat(getRawText()) + value * (isShiftDown() ? 10 : 1)) : (float) value);
                } else {
                    number = String.valueOf(append ? (LocalizedNumberText.parseFloat(getRawText()) + value * (isShiftDown() ? 10 : 1)) : (float) value);
                }
            } catch (NumberFormatException ignored) { }
        }  else if (mode == Mode.NUMBER_DOUBLE) {
            try {
                if (numberInstance != null) {
                    number = numberInstance.format(append ? (LocalizedNumberText.parseDouble(getRawText()) + value * (isShiftDown() ? 10 : 1)) : value);
                } else {
                    number = String.valueOf(append ? (LocalizedNumberText.parseDouble(getRawText()) + value * (isShiftDown() ? 10 : 1)) : value);
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
            historyStack.record(getRawText());
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

    protected void onValidateCommand(UIEvent event) {
        if ((CommandEvents.UNDO.equals(event.command) || CommandEvents.REDO.equals(event.command)) && isEditable()) {
            event.stopPropagation();
        }
    }

    protected void onExecuteCommand(UIEvent event) {
        if (isEditable()) {
            var current = getRawText();
            if (!Objects.deepEquals(historyStack.getCurrent(), current)) {
                historyStack.record(current);
            }
            if (CommandEvents.UNDO.equals(event.command)) {
                if (historyStack.undo()) {
                    setRawText(historyStack.getCurrent());
                }
            } else if (CommandEvents.REDO.equals(event.command)) {
                if (historyStack.redo()) {
                    setRawText(historyStack.getCurrent());
                }
            }
        }
    }

    protected boolean isNumberField() {
        return mode == Mode.NUMBER_INT || mode == Mode.NUMBER_LONG || mode == Mode.NUMBER_FLOAT || mode == Mode.NUMBER_DOUBLE || mode == Mode.NUMBER_SHORT || mode == Mode.NUMBER_BYTE;
    }

    protected void onMouseDown(UIEvent event) {
        if (event.button == 0) {
            var cursor = getCursorUnderMouseX(getLocalMouse(event.x, event.y).x);
            if (cursor != -1) {
                setCursor(cursor);
                if (isShiftDown()) {
                    setSelection(selectionStart, cursorPos);
                } else {
                    setSelection(cursorPos, cursorPos);
                }
                if (isNumberField()) {
                    var startValue = 0d;
                    try {
                        startValue = LocalizedNumberText.parseDouble(getRawText());
                    } catch (NumberFormatException ignored) {}
                    startDrag(new NumberStart(startValue), null);
                } else {
                    startDrag(new CursorStart(selectionStart), null);
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
                    setSelection(selectionStart, cursorPos);
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
                    setSelection(selectionStart, cursorPos);
                } else {
                    setSelection(cursorPos, cursorPos);
                }
            }
            case GLFW.GLFW_KEY_HOME -> {
                setCursor(0);
                if (isShiftDown()) {
                    setSelection(selectionStart, cursorPos);
                } else {
                    setSelection(cursorPos, cursorPos);
                }
            }
            case GLFW.GLFW_KEY_END -> {
                setCursor(rawText.length());
                if (isShiftDown()) {
                    setSelection(selectionStart, cursorPos);
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
        var textValue = value;
        this.rawText = value;
        if (isNumberField() && numberInstance != null && !value.isEmpty()) {
            try {
                switch (mode) {
                    case NUMBER_INT -> this.rawText = numberInstance.format(Integer.parseInt(value));
                    case NUMBER_FLOAT -> {
                        var parsed = LocalizedNumberText.parseFloat(value);
                        this.rawText = numberInstance.format(parsed);
                        textValue = LocalizedNumberText.normalizeFloat(value);
                    }
                    case NUMBER_DOUBLE -> {
                        var parsed = LocalizedNumberText.parseDouble(value);
                        this.rawText = numberInstance.format(parsed);
                        textValue = LocalizedNumberText.normalizeDouble(value);
                    }
                    case NUMBER_BYTE ->  this.rawText = numberInstance.format(Byte.parseByte(value));
                    case NUMBER_SHORT ->  this.rawText = numberInstance.format(Short.parseShort(value));
                    case NUMBER_LONG ->  this.rawText = numberInstance.format(Long.parseLong(value));
                }
            } catch (Exception e) {
                this.rawText = "";
            }
        }
        if (!this.text.equals(textValue)) {
            this.text = textValue;
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

    public TextField setTextRegexValidator(String regex) {
        try {
            var pattern = Pattern.compile(regex);
            return setTextValidator(s -> pattern.matcher(s).matches());
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to compile regex{} for text-field: ", regex);
            return setTextValidator(Predicates.alwaysFalse());
        }
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
        style(style -> style.tooltips(new String[0]));
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
        style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.compound_tag")));
        return this;
    }

    public TextField setResourceLocationOnly() {
        mode = Mode.RESOURCE_LOCATION;
        setCharValidator(chr -> chr == ':' || ResourceLocation.isValidNamespace(Character.toString(chr)) || ResourceLocation.isAllowedInResourceLocation(chr));
        setTextValidator(LDLib2::isValidResourceLocation);
        style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.resourcelocation")));
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
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.3")));
        } else if (minValue == Long.MIN_VALUE) {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.2", maxValue)));
        } else if (maxValue == Long.MAX_VALUE) {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.1", minValue)));
        } else {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue)));
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
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.3")));
        } else if (minValue == Integer.MIN_VALUE) {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.2", maxValue)));
        } else if (maxValue == Integer.MAX_VALUE) {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.1", minValue)));
        } else {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue)));
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
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.3")));
        } else if (minValue == Byte.MIN_VALUE) {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.2", maxValue)));
        } else if (maxValue == Byte.MAX_VALUE) {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.1", minValue)));
        } else {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue)));
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
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.3")));
        } else if (minValue == Short.MIN_VALUE) {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.2", maxValue)));
        } else if (maxValue == Short.MAX_VALUE) {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.1", minValue)));
        } else {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue)));
        }
        return setWheelDur(1);
    }

    public TextField setNumbersOnlyFloat(float minValue, float maxValue) {
        mode = Mode.NUMBER_FLOAT;
        setTextValidator(s -> {
            try {
                float value = LocalizedNumberText.parseFloat(s);
                if (minValue <= value && value <= maxValue) return true;
            } catch (NumberFormatException ignored) { }
            return false;
        });
        setCharValidator(LocalizedNumberText::isFloatingPointCharacter);
        if (minValue == -Float.MAX_VALUE && maxValue == Float.MAX_VALUE) {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.3")));
        } else if (minValue == -Float.MAX_VALUE) {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.2", maxValue)));
        } else if (maxValue == Float.MAX_VALUE) {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.1", minValue)));
        } else {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue)));
        }
        return setWheelDur(0.1f);
    }

    public TextField setNumbersOnlyDouble(double minValue, double maxValue) {
        mode = Mode.NUMBER_DOUBLE;
        setTextValidator(s -> {
            try {
                var value = LocalizedNumberText.parseDouble(s);
                if (minValue <= value && value <= maxValue) return true;
            } catch (NumberFormatException ignored) { }
            return false;
        });
        setCharValidator(LocalizedNumberText::isFloatingPointCharacter);
        if (minValue == -Double.MAX_VALUE && maxValue == Double.MAX_VALUE) {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.3")));
        } else if (minValue == -Double.MAX_VALUE) {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.2", maxValue)));
        } else if (maxValue == Double.MAX_VALUE) {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.1", minValue)));
        } else {
            style(style -> style.tooltips(Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue)));
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
        historyStack.record(getRawText());
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
        this.selectionStart = Mth.clamp(start, 0, this.rawText.length());
        this.selectionEnd = Mth.clamp(end, 0, this.rawText.length());
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
        var scale = textFieldStyle.fontSize() / getFont().lineHeight;
        var cursorPosX = getFont().getSplitter().stringWidth(TextUtilities.truncateStyled(getStyledLine(), cursorPos)) * scale;
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
        historyStack.record(getRawText());
        if (selectionStart != selectionEnd) {
            var min = Math.min(selectionStart, selectionEnd);
            var max = Math.max(selectionStart, selectionEnd);
            rawText = rawText.substring(0, min) + rawText.substring(max);
            cursorPos = min;
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
            var textValue = normalizeRawTextValue();
            if (!text.equals(textValue)) {
                text = textValue;
                notifyListeners();
            }
        } else {
            isError = true;
        }
    }

    private String normalizeRawTextValue() {
        try {
            return switch (mode) {
                case NUMBER_FLOAT -> LocalizedNumberText.normalizeFloat(rawText);
                case NUMBER_DOUBLE -> LocalizedNumberText.normalizeDouble(rawText);
                default -> rawText;
            };
        } catch (NumberFormatException ignored) {
            return rawText;
        }
    }

    /**
     * Gets the cursor position under the mouse.
     * @return The cursor position, -1 if not found.
     */
    public int getCursorUnderMouseX(double mouseX) {
        var x = getContentX();
        var font = getFont();

        var scale = textFieldStyle.fontSize() / font.lineHeight;
        // Mouse offset in rendered pixels. substrByWidth() expects unscaled/natural font pixels, so divide by
        // scale for it; the half-character comparison below stays in rendered pixels (like subLength).
        var mouseOffset = (float) (mouseX - x + displayOffset);

        var styledLine = getStyledLine();
        var subWithFont = font.substrByWidth(styledLine, (int) (mouseOffset / scale));
        float fullLength = font.getSplitter().stringWidth(styledLine) * scale;
        float subLength = font.getSplitter().stringWidth(subWithFont) * scale;
        int col;
        if (subLength >= fullLength) {
            col = rawText.length();
        } else {
            var subLen = subWithFont.getString().length();
            float nextCharWidth = font.getSplitter().stringWidth(TextUtilities.truncateStyled(styledLine, subLen + 1)) * scale - subLength;
            col = (mouseOffset - subLength) - nextCharWidth / 2f > 0 ? subLen + 1 : subLen;
        }
        return Mth.clamp(col, 0, rawText.length());
    }

    /**
     * The rendered content of the text (formatter applied + font), used to measure caret/selection positions so
     * that they stay aligned with what {@link #drawBackgroundAdditional} actually draws (including bold styling).
     */
    @OnlyIn(Dist.CLIENT)
    public Component getStyledLine() {
        var formattedText = formatter == null ? Component.literal(rawText) : formatter.apply(rawText);
        return TextUtilities.withFont(formattedText, getTextFieldStyle().font());
    }


    /// rendering
    @OnlyIn(Dist.CLIENT)
    public Font getFont() {
        return Minecraft.getInstance().font;
    }

    public Tuple<FormattedCharSequence, Float> getFormattedLine() {
        if (formattedLineCache == null) {
            var font = getTextFieldStyle().font();
            var formattedText = rawText.isEmpty() ?
                    textFieldStyle.placeholder() :
                    (formatter == null ? Component.literal(rawText) : formatter.apply(rawText));
            var textWithFont = font.equals(net.minecraft.network.chat.Style.DEFAULT_FONT) ? formattedText : formattedText.copy().withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(font));
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
        if (isSelfOrChildHover() || isFocused()) {
            guiContext.drawTexture(getTextFieldStyle().focusOverlay(), getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        }
        super.drawBackgroundOverlay(guiContext);
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        var x = getContentX();
        var y = getContentY();
        var height = getContentHeight();
        var formattedLine = getFormattedLine();
        var font = getFont();
        var fontSize = textFieldStyle.fontSize();
        var styledLine = getStyledLine();
        var scale = fontSize / font.lineHeight;

        var lineY = y + (height - fontSize) / 2;
        var line = formattedLine.getA();
        var lineX = x - displayOffset;

        // draw the text line
        RenderSystem.depthMask(false);
        guiContext.pose.pushPose();
        guiContext.pose.translate(lineX, lineY, 0);
        guiContext.pose.scale(scale, scale, 1);
        guiContext.graphics.drawString(font, line, 0, 0, rawText.isEmpty() ?
                ColorPattern.LIGHT_GRAY.color : (isError ? textFieldStyle.errorColor() : textFieldStyle.textColor()),
                !rawText.isEmpty() && textFieldStyle.textShadow());
        guiContext.pose.popPose();
        RenderSystem.depthMask(true);

        // draw highlight
        if (isFocused() && selectionStart != selectionEnd) {
            var min = Math.min(selectionStart, selectionEnd);
            var max = Math.max(selectionStart, selectionEnd);
            var minX = font.getSplitter().stringWidth(TextUtilities.truncateStyled(styledLine, min)) * scale - displayOffset;
            var maxX = font.getSplitter().stringWidth(TextUtilities.truncateStyled(styledLine, max)) * scale - displayOffset;
            DrawerHelper.drawSolidRect(guiContext.graphics,
                    RenderType.guiTextHighlight(),
                    x + minX,
                    lineY,
                    maxX - minX,
                    fontSize, -16776961);
        }
        // draw cursor
        var cursorPosX = font.getSplitter().stringWidth(TextUtilities.truncateStyled(styledLine, cursorPos)) * scale;
        if (isFocused() && System.currentTimeMillis() % 1000 < 500) {
            DrawerHelper.drawSolidRect(guiContext.graphics,
                    x + cursorPosX - displayOffset,
                    lineY,
                    1,
                    fontSize,
                    textFieldStyle.cursorColor());
        }
    }

    /// Editor + Xml
    @Override
    public void beforeDeserialize() {
        super.beforeDeserialize();
        this.editorMode = Mode.INTERNAL;
        this.editorRegexValidator = "";
    }

    @SkipPersistedValue(field = "editorMode")
    private boolean skipEditorMode(Mode mode) {
        return mode == Mode.INTERNAL;
    }

    @SkipPersistedValue(field = "editorRegexValidator")
    private boolean skipEditorRegexValidator(String regex) {
        return regex.isEmpty( ) || editorMode != Mode.STRING;
    }

    @SkipPersistedValue(field = "editorRange")
    private boolean skipEditorRange(Range range) {
        return !editorMode.isNumber();
    }

    @Override
    public void afterDeserialize() {
        super.afterDeserialize();
        if (editorMode == Mode.INTERNAL) return;
        if (editorMode == Mode.STRING) {
            // for string
            if (editorRegexValidator.isEmpty()) {
                setTextValidator(Predicates.alwaysTrue());
            } else {
                setTextRegexValidator(editorRegexValidator);
            }
        } else {
            // for others
            switch (editorMode) {
                case RESOURCE_LOCATION -> setResourceLocationOnly();
                case COMPOUND_TAG -> setCompoundTagOnly();
                case NUMBER_INT -> setNumbersOnlyInt(editorRange.getMin().intValue(), editorRange.getMax().intValue());
                case NUMBER_SHORT -> setNumbersOnlyShort(editorRange.getMin().shortValue(), editorRange.getMax().shortValue());
                case NUMBER_FLOAT -> setNumbersOnlyFloat(editorRange.getMin().floatValue(), editorRange.getMax().floatValue());
                case NUMBER_DOUBLE -> setNumbersOnlyDouble(editorRange.getMin().doubleValue(), editorRange.getMax().doubleValue());
                case NUMBER_LONG -> setNumbersOnlyLong(editorRange.getMin().longValue(), editorRange.getMax().longValue());
                case NUMBER_BYTE -> setNumbersOnlyByte(editorRange.getMin().byteValue(), editorRange.getMax().byteValue());
                default -> throw new IllegalStateException("Unexpected value: " + editorMode);
            }
        }
    }

    private void editorModeSubConfigurator(Mode value, ConfiguratorGroup group) {
        if (value == Mode.STRING) {
            group.addConfigurator(new StringConfigurator("EditorRegValidator",
                    () -> this.editorRegexValidator,
                    reg -> this.editorRegexValidator = reg,
                    "", true).setTips("EditorRegValidator.tips"));
        } else if (value.isNumber()) {
            var type = value.getNumberType();
            if (type == null) return;
            var configurator = new Configurator("EditorRange");
            NumberConfigurator min, max;

            configurator.inlineContainer.addChildren(
                    min = new NumberConfigurator("min", () -> editorRange.getMin(),
                            v -> editorRange = Range.of(v.floatValue(), editorRange.getMax()), 0, true),
                    max = new NumberConfigurator("max", () ->editorRange.getMax(),
                            v -> editorRange = Range.of(editorRange.getMin(), v.floatValue()), 0, true)
            ).layout(layout -> {
                layout.gapAll(2);
                layout.marginLeft(2);
                layout.flexDirection(FlexDirection.ROW);
                layout.wrap(FlexWrap.WRAP);
            });
            min.layout(layout -> {
                layout.flex(1);
                layout.minWidth(40);
                layout.height(14);
            });
            max.layout(layout -> {
                layout.flex(1);
                layout.minWidth(40);
                layout.height(14);
            });
            if (type.min != null && type.max != null && type.wheel != null) {
                min.setRange(type.min, type.max).setWheel(type.wheel).setType(type);
                max.setRange(type.min, type.max).setWheel(type.wheel).setType(type);
            }
            group.addConfigurators(configurator);
        }
    }

    @Override
    public void loadXml(Element element) {
        // mode
        if (element.hasAttribute("mode")) {
            editorMode = XmlUtils.getAsEnum(element, "mode", Mode.class, editorMode);
        }
        // regex validator
        if (element.hasAttribute("regex-validator")) {
            editorRegexValidator = element.getAttribute("regex-validator");
        }
        // value
        if (element.hasAttribute("value")) {
            setText(element.getAttribute("value"));
        }
        super.loadXml(element);
        afterDeserialize();
    }
}
