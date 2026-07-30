package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.font.LDFonts;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UIElementRendererRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.data.Cursor;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.HistoryStack;
import com.lowdragmc.lowdraglib2.utils.TextUtilities;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.util.Tuple;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import org.lwjgl.glfw.GLFW;
import org.w3c.dom.Element;
import oshi.util.tuples.Pair;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "text-area", group = "basic", registry = "ldlib2:ui_element")
public class TextArea extends BindableUIElement<String[]> {
    // Internal helpers
    public record CursorDragStart(Cursor anchor) {}
    public record History(String[] lines, Cursor cursor) {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof History(String[] lines1, Cursor cursor1) &&
                    Arrays.deepEquals(lines1, lines) &&
                    Objects.equals(cursor1, cursor);
        }
    }

    @Configurable(name = "TextAreaStyle")
    public class TextAreaStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.SCROLLER_VIEW_MARGIN,
                PropertyRegistry.FONT,
                PropertyRegistry.FONT_SIZE,
                PropertyRegistry.TEXT_COLOR,
                PropertyRegistry.ERROR_COLOR,
                PropertyRegistry.CURSOR_COLOR,
                PropertyRegistry.TEXT_SHADOW,
                PropertyRegistry.PLACEHOLDER,
                PropertyRegistry.SCROLLER_VERTICAL_DISPLAY,
                PropertyRegistry.SCROLLER_HORIZONTAL_DISPLAY,
                PropertyRegistry.SCROLLER_VIEW_MODE,
                PropertyRegistry.LINE_SPACING,
                PropertyRegistry.FOCUS_OVERLAY,
        };
        public TextAreaStyle() {
            super(TextArea.this);
            setDefault(PropertyRegistry.FOCUS_OVERLAY, Sprites.RECT_RD_T_SOLID);
        }

        public static void init() {
            PropertyRegistry.SCROLLER_VIEW_MARGIN.addListener(TextAreaStyle::onPropertyChanged);
            PropertyRegistry.FONT_SIZE.addListener(TextAreaStyle::onPropertyChanged);
            PropertyRegistry.FONT.addListener(TextAreaStyle::onPropertyChanged);
            PropertyRegistry.LINE_SPACING.addListener(TextAreaStyle::onPropertyChanged);
            PropertyRegistry.SCROLLER_VIEW_MODE.addListener(TextAreaStyle::onPropertyChanged);
            PropertyRegistry.SCROLLER_HORIZONTAL_DISPLAY.addListener(TextAreaStyle::onPropertyChanged);
            PropertyRegistry.SCROLLER_VERTICAL_DISPLAY.addListener(TextAreaStyle::onPropertyChanged);
        }

        private static <T> void onPropertyChanged(UIElement element, Property<T> property, @Nullable T oldValue, @Nullable T newValue) {
            if (element instanceof TextArea textArea) {
                textArea.onTextAreaStyleChanged();
            }
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public float scrollerViewMargin() {
            return getValueSave(PropertyRegistry.SCROLLER_VIEW_MARGIN);
        }

        public TextAreaStyle scrollerViewStyle(float scrollerViewMargin) {
            set(PropertyRegistry.SCROLLER_VIEW_MARGIN, scrollerViewMargin);
            return this;
        }

        public Identifier font() {
            return getValueSave(PropertyRegistry.FONT);
        }

        public TextAreaStyle font(Identifier font) {
            set(PropertyRegistry.FONT, font);
            return this;
        }

        public float fontSize() {
            return getValueSave(PropertyRegistry.FONT_SIZE);
        }

        public TextAreaStyle fontSize(float fontSize) {
            set(PropertyRegistry.FONT_SIZE, fontSize);
            return this;
        }

        public int textColor() {
            return getValueSave(PropertyRegistry.TEXT_COLOR);
        }

        public TextAreaStyle textColor(int textColor) {
            set(PropertyRegistry.TEXT_COLOR, textColor);
            return this;
        }

        public int errorColor() {
            return getValueSave(PropertyRegistry.ERROR_COLOR);
        }

        public TextAreaStyle errorColor(int errorColor) {
            set(PropertyRegistry.ERROR_COLOR, errorColor);
            return this;
        }

        public int cursorColor() {
            return getValueSave(PropertyRegistry.CURSOR_COLOR);
        }

        public TextAreaStyle cursorColor(int cursorColor) {
            set(PropertyRegistry.CURSOR_COLOR, cursorColor);
            return this;
        }

        public boolean textShadow() {
            return getValueSave(PropertyRegistry.TEXT_SHADOW);
        }

        public TextAreaStyle textShadow(boolean textShadow) {
            set(PropertyRegistry.TEXT_SHADOW, textShadow);
            return this;
        }

        public Component placeholder() {
            return getValueSave(PropertyRegistry.PLACEHOLDER);
        }

        public TextAreaStyle placeholder(Component placeholder) {
            set(PropertyRegistry.PLACEHOLDER, placeholder);
            return this;
        }

        public ScrollDisplay horizontalScrollDisplay() {
            return getValueSave(PropertyRegistry.SCROLLER_HORIZONTAL_DISPLAY);
        }

        public TextAreaStyle horizontalScrollDisplay(ScrollDisplay horizontalScrollDisplay) {
            set(PropertyRegistry.SCROLLER_HORIZONTAL_DISPLAY, horizontalScrollDisplay);
            return this;
        }

        public ScrollDisplay verticalScrollDisplay() {
            return getValueSave(PropertyRegistry.SCROLLER_VERTICAL_DISPLAY);
        }

        public TextAreaStyle verticalScrollDisplay(ScrollDisplay verticalScrollDisplay) {
            set(PropertyRegistry.SCROLLER_VERTICAL_DISPLAY, verticalScrollDisplay);
            return this;
        }

        public ScrollerMode viewMode() {
            return getValueSave(PropertyRegistry.SCROLLER_VIEW_MODE);
        }

        public TextAreaStyle viewMode(ScrollerMode viewMode) {
            set(PropertyRegistry.SCROLLER_VIEW_MODE, viewMode);
            return this;
        }

        public float lineSpacing() {
            return getValueSave(PropertyRegistry.LINE_SPACING);
        }

        public TextAreaStyle lineSpacing(float lineSpacing) {
            set(PropertyRegistry.LINE_SPACING, lineSpacing);
            return this;
        }

        public IGuiTexture focusOverlay() {
            return getValueSave(PropertyRegistry.FOCUS_OVERLAY);
        }

        public TextAreaStyle focusOverlay(IGuiTexture focusOverlay) {
            set(PropertyRegistry.FOCUS_OVERLAY, focusOverlay);
            return this;
        }
    }

    public final Scroller horizontalScroller;
    public final Scroller verticalScroller;
    public final UIElement contentView;

    // Validation
    @Setter private Predicate<String[]> textValidator = Predicates.alwaysTrue();
    @Setter private Predicate<Character> charValidator = Predicates.alwaysTrue();

    // Style
    @Getter private final TextAreaStyle textAreaStyle = new TextAreaStyle();

    // Raw edit buffer (what user is editing right now)
    protected final List<String> lines = new ArrayList<>();
    @Configurable(name = "value")
    private String[] value = new String[0];

    // runtime
    @Getter
    private final HistoryStack<History> historyStack = new HistoryStack<>(100);
    @Getter private boolean isError = false;

    // Cursor and selection
    @Getter private int cursorLine = 0;
    @Getter private int cursorCol = 0;
    @Getter private int selStartLine = 0;
    @Getter private int selStartCol = 0;
    @Getter private int selEndLine = 0;
    @Getter private int selEndCol = 0;

    // Scroll offsets
    @Getter private float scrollY = 0f; // vertical pixels
    @Getter private float scrollX = 0f; // horizontal pixels

    //runtime
    private float lastWidth = -1;
    private float lastHeight = -1;

    public TextArea() {
        this.horizontalScroller = new Scroller.Horizontal().setRange(0, 1f).setClampNormalizedValue(this::horizontalClamp);
        this.verticalScroller = new Scroller.Vertical().setRange(0, 1f).setClampNormalizedValue(this::verticalClamp);
        this.horizontalScroller.addClass("__text-area_horizontal-scroller__");
        this.verticalScroller.addClass("__text-area_vertical-scroller__");

        // Default layout and look
        getLayout().height(60);

        this.contentView = new ContentView(this);
        this.contentView.addClass("__text-area_content-view__");
        this.contentView.layout(layout -> {
            layout.paddingAll(3);
            layout.flex(1);
            layout.heightPercent(100);
        });
        this.contentView.style(style -> style.backgroundTexture(Sprites.RECT_RD_SOLID));
        this.contentView.setOverflowVisible(false);
        this.contentView.addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
            updateScrollers();
            if (Float.isNaN(scrollX) || Float.isNaN(scrollY)) {
                ensureCursorVisible();
            }
        });

        setFocusable(true);

        // Event wiring
        addEventListener(UIEvents.CHAR_TYPED, this::onCharTyped);
        addEventListener(UIEvents.KEY_DOWN, this::onKeyDown);
        addEventListener(UIEvents.VALIDATE_COMMAND, this::onValidateCommand);
        addEventListener(UIEvents.EXECUTE_COMMAND, this::onExecuteCommand);
        this.contentView.addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        this.contentView.addEventListener(UIEvents.DOUBLE_CLICK, this::onDoubleClick);
        this.contentView.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSource);
        this.contentView.addEventListener(UIEvents.MOUSE_WHEEL, this::onMouseWheel);
        addEventListener(UIEvents.BLUR, this::onBlur);
        lines.add("");

        verticalScroller.setOnValueChanged(this::onVerticalScroll);
        horizontalScroller.setOnValueChanged(this::onHorizontalScroll);
        addChildren(new UIElement().layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.widthPercent(100);
            layout.flex(1);
        }).addChildren(contentView, verticalScroller), horizontalScroller);
        internalSetup();
    }

    public TextArea textAreaStyle(Consumer<TextAreaStyle> style) {
        style.accept(textAreaStyle);
        return this;
    }

    protected void onTextAreaStyleChanged() {
        ensureCursorVisible();
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        if (lastWidth != contentView.getContentWidth() || lastHeight != contentView.getContentHeight()) {
            ensureCursorVisible();
        }
    }

    public void pushHistory() {
        historyStack.record(new History(getValue(), cursorPos()));
    }

    protected void onValidateCommand(UIEvent event) {
        if ((CommandEvents.UNDO.equals(event.command) || CommandEvents.REDO.equals(event.command)) && isEditable()) {
            event.stopPropagation();
        }
    }

    protected void onExecuteCommand(UIEvent event) {
        if (isEditable()) {
            var current = getValue();
            if (historyStack.getCurrent() == null || !Arrays.deepEquals(historyStack.getCurrent().lines, current)) {
                historyStack.record(new History(current, cursorPos()));
            }
            if (CommandEvents.UNDO.equals(event.command)) {
                if (historyStack.undo()) {
                    var value = historyStack.getCurrent().lines;
                    var cursor = historyStack.getCurrent().cursor;
                    var previousScrollX = scrollX;
                    var previousScrollY = scrollY;
                    setValue(value);
                    this.scrollX = previousScrollX;
                    this.scrollY = previousScrollY;
                    setCursor(cursor.line(), cursor.col());
                    ensureCursorVisible();
                }
            } else if (CommandEvents.REDO.equals(event.command)) {
                if (historyStack.redo()) {
                    var value = historyStack.getCurrent().lines;
                    var cursor = historyStack.getCurrent().cursor;
                    var previousScrollX = scrollX;
                    var previousScrollY = scrollY;
                    setValue(value);
                    this.scrollX = previousScrollX;
                    this.scrollY = previousScrollY;
                    setCursor(cursor.line(), cursor.col());
                    ensureCursorVisible();
                }
            }
        }
    }

    protected void onHorizontalScroll(float value) {
        scrollX = (getMaxWidth() - contentView.getContentWidth()) * value;
        scrollX = Math.max(0, scrollX);
    }

    protected void onVerticalScroll(float value) {
        scrollY = (getMaxHeight() - contentView.getContentHeight()) * value;
        scrollY = Math.max(0, scrollY);
    }

    protected float horizontalClamp(float normalizedValue) {
        var containerWidth = getMaxWidth() - contentView.getContentWidth();
        var fontSize = textAreaStyle.fontSize();
        return Mth.clamp(Mth.abs(normalizedValue),
                fontSize / containerWidth,
                (fontSize + textAreaStyle.lineSpacing()) / containerWidth)
                * (normalizedValue > 0 ? 1 : -1);
    }

    protected float verticalClamp(float normalizedValue) {
        var containerHeight = getMaxHeight() - contentView.getContentHeight();
        var fontSize = textAreaStyle.fontSize();
        return Mth.clamp(Mth.abs(normalizedValue),
                fontSize / containerHeight,
                (fontSize + textAreaStyle.lineSpacing()) / containerHeight)
                * (normalizedValue > 0 ? 1 : -1);
    }

    protected void updateScrollers() {
        if (!LDLib2.isRemote()) return;
        var maxWidth = getMaxWidth();
        var maxHeight = getMaxHeight();
        var leftWidth = maxWidth - contentView.getContentWidth();
        var leftHeight = maxHeight - contentView.getContentHeight();
        var hP = leftWidth == 0 ? 0 : scrollX / leftWidth;
        hP = Mth.clamp(hP, 0, 1);
        var wP = leftHeight == 0 ? 0 : scrollY / leftHeight;
        wP = Mth.clamp(wP, 0, 1);
        horizontalScroller.setValue(hP);
        verticalScroller.setValue(wP);
        var mode = textAreaStyle.viewMode();
        if (mode == ScrollerMode.HORIZONTAL || mode == ScrollerMode.BOTH) {
            // cause we are using a flexbox, the width of the view container is not the same as the width of the view port
            // so we need to calculate the width ourselves
            var vp = Math.min(1, contentView.getContentWidth() / maxWidth);
            horizontalScroller.setScrollBarSize(vp * 100);
            horizontalScroller.setDisplay((textAreaStyle.horizontalScrollDisplay() == ScrollDisplay.AUTO && vp < 1) || textAreaStyle.horizontalScrollDisplay() == ScrollDisplay.ALWAYS);
        } else {
            horizontalScroller.setDisplay(false);
        }

        if (horizontalScroller.getTaffyStyle().style.display == TaffyDisplay.FLEX) {
            horizontalScroller.layout(layout -> {
                Style.importantPipeline(layout, l ->
                        l.marginRight(verticalScroller.isDisplayed() ? textAreaStyle.scrollerViewMargin() : 0));
            });
        }

        if (mode == ScrollerMode.VERTICAL || mode == ScrollerMode.BOTH) {
            var hp = Math.min(1, contentView.getContentHeight() / maxHeight);
            verticalScroller.setScrollBarSize(hp * 100);
            verticalScroller.setDisplay((textAreaStyle.verticalScrollDisplay() == ScrollDisplay.AUTO && hp < 1) || textAreaStyle.verticalScrollDisplay() == ScrollDisplay.ALWAYS);
        } else {
            verticalScroller.setDisplay(false);
        }
    }

    // Bindable value
    @Override
    public String[] getValue() {
        return Arrays.copyOf(value, value.length);
    }

    public List<String> getLines() {
        return List.of(value);
    }

    public TextArea setLinesResponder(Consumer<String[]> textResponder) {
        registerValueListener(textResponder);
        return this;
    }

    public TextArea setLines(List<String> lines) {
        return setValue(lines.toArray(new String[0]));
    }

    public TextArea setLines(String[] lines, boolean notify) {
        return setValue(lines, notify);
    }

    @ConfigSetter(field = "value")
    public TextArea setValue(@Nullable String[] value) {
        return setValue(value, true);
    }

    @Override
    public TextArea setValue(@Nullable String[] value, boolean notify) {
        lines.clear();
        var valueBuilder = new ArrayList<String>();
        if (value != null && value.length > 0) {
            for (String s : value) {
                lines.add(s == null ? "" : s);
                valueBuilder.add(s == null ? "" : s);
            }
        } else {
            lines.add("");
            valueBuilder.add("");
        }
        this.value = valueBuilder.toArray(new String[0]);
        // Reset cursor and selection at end
        cursorLine = 0;
        cursorCol = 0;
        selStartLine = selEndLine = cursorLine;
        selStartCol = selEndCol = cursorCol;
        scrollX = 0;
        scrollY = 0;
        updateScrollers();

        if (notify) {
            notifyListeners();
        }
        return this;
    }

    public float scale() {
        return LDLib2.isRemote() ? TextAreaClientSupport.scale(this) : 1f;
    }

    /**
     * The rendered content of the character range {@code [from, to)} on {@code line}, with the styling it is
     * actually drawn with. Measuring the width of this component keeps caret/selection positions aligned with
     * the rendered text, including style-dependent advances such as bold. Subclasses that render styled text
     * (e.g. syntax highlighting) should override this to reflect that styling.
     */
    protected Component styledLineComponent(int line, int from, int to) {
        var text = lines.get(line);
        from = Mth.clamp(from, 0, text.length());
        to = Mth.clamp(to, 0, text.length());
        return TextUtilities.withFont(text.substring(from, to), getTextAreaStyle().font());
    }

    public float lineHeight() {
        return textAreaStyle.fontSize() + textAreaStyle.lineSpacing();
    }

    public boolean hasSelection() {
        return !(selStartLine == selEndLine && selStartCol == selEndCol);
    }

    public Cursor cursorPos() {
        return new Cursor(cursorLine, cursorCol);
    }

    private static int comparePos(Cursor a, Cursor b) {
        if (a.line() != b.line()) return Integer.compare(a.line(), b.line());
        return Integer.compare(a.col(), b.col());
    }

    private Cursor selMin() {
        var a = new Cursor(selStartLine, selStartCol);
        var b = new Cursor(selEndLine, selEndCol);
        return comparePos(a, b) <= 0 ? a : b;
    }

    private Cursor selMax() {
        var a = new Cursor(selStartLine, selStartCol);
        var b = new Cursor(selEndLine, selEndCol);
        return comparePos(a, b) >= 0 ? a : b;
    }

    private float getMaxWidth() {
        return LDLib2.isRemote() ? TextAreaClientSupport.getMaxWidth(this) : 0f;
    }

    private float getMaxHeight() {
        var max = lines.size() * lineHeight();
        if (!lines.isEmpty()) {
            max = max - textAreaStyle.lineSpacing();
        }
        return max;
    }

    public void setCursor(int line, int col) {
        cursorLine = Mth.clamp(line, 0, lines.size() - 1);
        cursorCol = Mth.clamp(col, 0, lines.get(cursorLine).length());
        ensureCursorVisible();
    }

    public void setSelection(Cursor a, Cursor b) {
        selStartLine = a.line(); selStartCol = a.col();
        selEndLine = b.line(); selEndCol = b.col();
    }

    public void collapseSelectionToCursor() {
        selStartLine = selEndLine = cursorLine;
        selStartCol = selEndCol = cursorCol;
    }

    protected void ensureCursorVisible() {
        if (!LDLib2.isRemote()) return;
        var scroll = TextAreaClientSupport.computeVisibleScroll(this);
        scrollX = scroll.getA();
        scrollY = scroll.getB();
        updateScrollers();
        lastHeight = contentView.getContentHeight();
        lastWidth = contentView.getContentWidth();
    }

    protected void onBlur(UIEvent e) {
        if (hasSelection()) {
            collapseSelectionToCursor();
        }
    }

    protected void onMouseWheel(UIEvent event) {
        var mode = textAreaStyle.viewMode();
        if (event.deltaY != 0 && (mode == ScrollerMode.VERTICAL || mode == ScrollerMode.BOTH)) {
            verticalScroller.onScrollWheel(event);
        }
        if (event.deltaX != 0 && (mode == ScrollerMode.HORIZONTAL || mode == ScrollerMode.BOTH)) {
            horizontalScroller.onScrollWheel(event);
        } else if (event.deltaY != 0 && mode == ScrollerMode.HORIZONTAL) {
            horizontalScroller.onScrollWheel(event);
        }
        event.stopPropagation();
    }

    protected void onMouseDown(UIEvent event) {
        if (event.button == 0) {
            var localMouse = getLocalMouse(event.x, event.y);
            var pos = getCursorUnderMouse(localMouse.x, localMouse.y);
            setCursor(pos.line(), pos.col());
            if (isShiftDown()) {
                // Extend selection
                var startCursor = new Cursor(selStartLine, selStartCol);
                setSelection(startCursor, cursorPos());
                contentView.startDrag(new CursorDragStart(startCursor), null);
            } else {
                // Reset selection
                setSelection(cursorPos(), cursorPos());
                contentView.startDrag(new CursorDragStart(pos), null);
            }
            event.stopPropagation();
            focus();
        }
    }

    protected void onDoubleClick(UIEvent event) {
        if (event.button == 0) {
            var localMouse = getLocalMouse(event.x, event.y);
            var pos = getCursorUnderMouse(localMouse.x, localMouse.y);
            // select string
            var selected = selectWord(pos);
            if (!selected.getA().equals(selected.getB())) {
                setSelection(new Cursor(pos.line(), selected.getA()), new Cursor(pos.line(), selected.getB()));
            }
        }
    }

    /**
     * Selects a word based on the position of the given cursor in a line of text.
     * The method calculates the start and end positions of a contiguous sequence of alphanumeric characters,
     * treating it as a word. If the cursor is not positioned within an alphanumeric sequence,
     * the start and end positions will be the same as the cursor's position.
     *
     * @param cursor the {@code Cursor} object containing the line and column position in the text for word selection
     * @return a {@code Pair} object containing the start and end column indices of the selected word
     */
    protected Pair<Integer, Integer> selectWord(Cursor cursor) {
        var line = lines.get(cursor.line());
        var col = cursor.col();
        var start = col;
        var end = col;
        while (start > 0 && isWordChar(line.charAt(start - 1))) start--;
        while (end < line.length() && isWordChar(line.charAt(end))) end++;
        return new Pair<>(start, end);
    }

    protected boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    protected void onDragSource(UIEvent event) {
        if (event.dragHandler.draggingObject instanceof CursorDragStart(Cursor anchor)) {
            var localMouse = getLocalMouse(event.x, event.y);
            var pos = getCursorUnderMouse(localMouse.x, localMouse.y);
            setCursor(pos.line(), pos.col());
            setSelection(anchor, cursorPos());
        }
    }

    public Cursor getCursorUnderMouse(double mouseX, double mouseY) {
        if (!LDLib2.isRemote()) {
            return cursorPos();
        }
        return TextAreaClientSupport.getCursorUnderMouse(this, mouseX, mouseY);
    }

    protected void onCharTyped(UIEvent event) {
        if (!isEditable()) return;
        if (StringUtil.isAllowedChatCharacter(event.codePoint) && charValidator.test(event.codePoint)) {
            insertText(Character.toString(event.codePoint));
        }
    }

    protected void onKeyDown(UIEvent event) {
        switch (event.keyCode) {
            case GLFW.GLFW_KEY_ENTER -> {
                if (!isEditable()) return;
                insertNewLine();
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!isEditable()) return;
                deleteChars(-1);
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (!isEditable()) return;
                deleteChars(1);
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (event.isCtrlDown()) {
                    moveWord(-1);
                } else {
                    moveLeft();
                }
                updateSelectionAfterMove();
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (event.isCtrlDown()) {
                    moveWord(1);
                } else {
                    moveRight();
                }
                updateSelectionAfterMove();
            }
            case GLFW.GLFW_KEY_UP -> {
                moveUp();
                updateSelectionAfterMove();
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveDown();
                updateSelectionAfterMove();
            }
            case GLFW.GLFW_KEY_HOME -> {
                if (event.isCtrlDown()) {
                    setCursor(0, 0);
                } else {
                    setCursor(cursorLine, 0);
                }
                updateSelectionAfterMove();
            }
            case GLFW.GLFW_KEY_END -> {
                if (event.isCtrlDown()) {
                    int lastLine = Math.max(0, lines.size() - 1);
                    setCursor(lastLine, lines.get(lastLine).length());
                } else {
                    setCursor(cursorLine, lines.get(cursorLine).length());
                }
                updateSelectionAfterMove();
            }
            case GLFW.GLFW_KEY_PAGE_UP -> {
                page(-1);
                updateSelectionAfterMove();
            }
            case GLFW.GLFW_KEY_PAGE_DOWN -> {
                page(1);
                updateSelectionAfterMove();
            }
            default -> {
                if (isPrimaryShortcut(event) && event.keyCode == GLFW.GLFW_KEY_A) {
                    selectAll();
                } else if (isPrimaryShortcut(event) && event.keyCode == GLFW.GLFW_KEY_C) {
                    TextAreaClientSupport.copyHighlightedText(this);
                } else if (isPrimaryShortcut(event) && event.keyCode == GLFW.GLFW_KEY_V) {
                    if (!isEditable()) return;
                    insertText(TextAreaClientSupport.getClipboardText());
                } else if (isPrimaryShortcut(event) && event.keyCode == GLFW.GLFW_KEY_X) {
                    if (!isEditable()) return;
                    TextAreaClientSupport.copyHighlightedText(this);
                    insertText(""); // replace selection with empty
                }
            }
        }
    }

    private boolean isPrimaryShortcut(UIEvent event) {
        return event.isCtrlDown() || (event.modifiers & GLFW.GLFW_MOD_SUPER) != 0;
    }

    protected void updateSelectionAfterMove() {
        if (isShiftDown()) {
            setSelection(new Cursor(selStartLine, selStartCol), cursorPos());
        } else {
            collapseSelectionToCursor();
        }
    }

    protected void moveLeft() {
        if (cursorCol > 0) {
            setCursor(cursorLine, cursorCol - 1);
        } else if (cursorLine > 0) {
            var prev = lines.get(cursorLine - 1);
            int newCol = prev.length();
            setCursor(cursorLine - 1, newCol);
        }
    }

    protected void moveRight() {
        if (cursorCol < lines.get(cursorLine).length()) {
            setCursor(cursorLine, cursorCol + 1);
        } else if (cursorLine < lines.size() - 1) {
            setCursor(cursorLine + 1, 0);
        }
    }

    protected void moveUp() {
        if (cursorLine > 0) {
            int newLine = cursorLine - 1;
            int col = Math.min(cursorCol, lines.get(newLine).length());
            setCursor(newLine, col);
        }
    }

    protected void moveDown() {
        if (cursorLine < lines.size() - 1) {
            int newLine = cursorLine + 1;
            int col = Math.min(cursorCol, lines.get(newLine).length());
            setCursor(newLine, col);
        }
    }

    protected void moveWord(int dir) {
        var lineText = lines.get(cursorLine);
        int idx = cursorCol;
        if (dir < 0) {
            // move to previous word boundary
            while (idx > 0 && lineText.charAt(idx - 1) == ' ') idx--;
            while (idx > 0 && lineText.charAt(idx - 1) != ' ') idx--;
        } else {
            int n = lineText.length();
            while (idx < n && lineText.charAt(idx) != ' ') idx++;
            while (idx < n && lineText.charAt(idx) == ' ') idx++;
        }
        setCursor(cursorLine, idx);
    }

    protected void page(int direction) {
        float visibleLines = Math.max(1, (int) (contentView.getContentHeight() / lineHeight()));
        int newLine = Mth.clamp(cursorLine + (int) (direction * visibleLines), 0, lines.size() - 1);
        int col = Math.min(cursorCol, lines.get(newLine).length());
        setCursor(newLine, col);
    }

    protected void selectAll() {
        selStartLine = 0;
        selStartCol = 0;
        selEndLine = Math.max(0, lines.size() - 1);
        selEndCol = lines.get(selEndLine).length();
        setCursor(selEndLine, selEndCol);
    }

    protected void insertNewLine() {
        replaceSelectionWith("\n");
    }

    protected void deleteChars(int dir) {
        if (hasSelection()) {
            replaceSelectionWith("");
            return;
        }
        pushHistory();
        if (dir < 0) { // backspace
            if (cursorCol > 0) {
                var s = lines.get(cursorLine);
                lines.set(cursorLine, s.substring(0, cursorCol - 1) + s.substring(cursorCol));
                setCursor(cursorLine, cursorCol - 1);
            } else if (cursorLine > 0) {
                // merge with previous line
                var prev = lines.get(cursorLine - 1);
                var cur = lines.get(cursorLine);
                int newCol = prev.length();
                lines.set(cursorLine - 1, prev + cur);
                lines.remove(cursorLine);
                setCursor(cursorLine - 1, newCol);
            }
        } else { // delete
            var s = lines.get(cursorLine);
            if (cursorCol < s.length()) {
                lines.set(cursorLine, s.substring(0, cursorCol) + s.substring(cursorCol + 1));
            } else if (cursorLine < lines.size() - 1) {
                // merge with next line
                var next = lines.get(cursorLine + 1);
                lines.set(cursorLine, s + next);
                lines.remove(cursorLine + 1);
            }
        }
        onRawLinesUpdated();
    }

    protected void replaceSelectionWith(String text) {
        pushHistory();
        if (hasSelection()) {
            var start = selMin();
            var end = selMax();

            if (start.line() == end.line()) {
                var s = lines.get(start.line());
                String before = s.substring(0, start.col());
                String after = s.substring(end.col());
                List<String> incoming = splitLines(text);

                if (incoming.size() == 1) {
                    lines.set(start.line(), before + incoming.get(0) + after);
                    setCursor(start.line(), before.length() + incoming.get(0).length());
                } else {
                    String first = before + incoming.get(0);
                    String last = incoming.get(incoming.size() - 1) + after;
                    lines.set(start.line(), first);
                    // drop lines between start..end
                    for (int i = end.line(); i > start.line(); i--) {
                        lines.remove(i);
                    }
                    // insert middle lines
                    for (int i = 1; i < incoming.size() - 1; i++) {
                        lines.add(start.line() + i, incoming.get(i));
                    }
                    lines.add(start.line() + incoming.size() - 1, last);
                    setCursor(start.line() + incoming.size() - 1, incoming.get(incoming.size() - 1).length());
                }
            } else {
                // multi-line selection
                var startLine = lines.get(start.line());
                var endLine = lines.get(end.line());

                var from = Mth.clamp(start.col(), 0, startLine.length());
                var to = Mth.clamp(end.col(), 0, endLine.length());

                String before = startLine.substring(0, from);
                String after = endLine.substring(to);
                List<String> incoming = splitLines(text);

                // remove lines between start+1 .. end
                for (int i = end.line(); i > start.line(); i--) {
                    lines.remove(i);
                }

                if (incoming.size() == 1) {
                    lines.set(start.line(), before + incoming.get(0) + after);
                    setCursor(start.line(), before.length() + incoming.get(0).length());
                } else {
                    String first = before + incoming.get(0);
                    String last = incoming.get(incoming.size() - 1) + after;
                    lines.set(start.line(), first);
                    for (int i = 1; i < incoming.size() - 1; i++) {
                        lines.add(start.line() + i, incoming.get(i));
                    }
                    lines.add(start.line() + incoming.size() - 1, last);
                    setCursor(start.line() + incoming.size() - 1, incoming.get(incoming.size() - 1).length());
                }
            }
        } else {
            // No selection: simple insert or newline
            if (text.contains("\n") || text.contains("\r")) {
                var incoming = splitLines(text);
                var s = lines.get(cursorLine);
                String before = s.substring(0, cursorCol);
                String after = s.substring(cursorCol);
                if (incoming.size() == 1) {
                    lines.set(cursorLine, before + incoming.getFirst() + after);
                    setCursor(cursorLine, cursorCol + incoming.getFirst().length());
                } else {
                    String first = before + incoming.getFirst();
                    String last = incoming.getLast() + after;
                    lines.set(cursorLine, first);
                    for (int i = 1; i < incoming.size() - 1; i++) {
                        lines.add(cursorLine + i, incoming.get(i));
                    }
                    lines.add(cursorLine + incoming.size() - 1, last);
                    setCursor(cursorLine + incoming.size() - 1, incoming.get(incoming.size() - 1).length());
                }
            } else {
                var s = lines.get(cursorLine);
                lines.set(cursorLine, s.substring(0, cursorCol) + text + s.substring(cursorCol));
                setCursor(cursorLine, cursorCol + text.length());
            }
        }
        collapseSelectionToCursor();
        onRawLinesUpdated();
    }

    protected void insertText(@Nullable String text) {
        // Filter disallowed characters if needed
        if (text == null || text.isEmpty()) {
            // Replacing selection with empty still needs to notify
            if (hasSelection()) {
                replaceSelectionWith("");
            }
            return;
        }

        // For paste, we won't filter strictly per-char; Text validator will gate commit.
        replaceSelectionWith(text);
    }

    private List<String> splitLines(String s) {
        // Normalize CRLF -> LF, split on \n
        String normalized = s.replace("\r\n", "\n").replace('\r', '\n');
        String[] arr = normalized.split("\n", -1);
        List<String> list = new ArrayList<>(arr.length);
        for (String it : arr) list.add(it);
        return list;
    }

    /**
     * Called whenever raw 'lines' changed.
     * If passes validator, update valueLines and notify listeners.
     */
    protected void onRawLinesUpdated() {
        // Validate
        String[] candidate = lines.toArray(String[]::new);
        if (textValidator.test(candidate)) {
            isError = false;
            if (!equalsValue(candidate)) {
                value = new String[candidate.length];
                System.arraycopy(candidate, 0, value, 0, candidate.length);
                notifyListeners();
            }
        } else {
            isError = true;
        }
        ensureCursorVisible();
    }

    private boolean equalsValue(String[] candidate) {
        if (candidate.length != value.length) return false;
        for (int i = 0; i < candidate.length; i++) {
            if (!candidate[i].equals(value[i])) return false;
        }
        return true;
    }

    private String getHighlightedText() {
        if (!hasSelection()) return "";
        var start = selMin();
        var end = selMax();
        if (start.line() == end.line()) {
            var s = lines.get(start.line());
            return s.substring(start.col(), end.col());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(lines.get(start.line()).substring(start.col()));
        sb.append('\n');
        for (int i = start.line() + 1; i < end.line(); i++) {
            sb.append(lines.get(i)).append('\n');
        }
        sb.append(lines.get(end.line()), 0, end.col());
        return sb.toString();
    }

    String getClipboardSelectionText() {
        return getHighlightedText();
    }

    public boolean isEditable() {
        return isActive() && isVisible() && isFocused() && isDisplayed();
    }

    /// Rendering hooks (overridable by subclasses such as CodeEditor)
    protected void drawContentLines(GUIContext context, Font font, float scale, float x, float y,
                                    int firstVisibleLine, int lastVisibleLine) {
        var textFont = textAreaStyle.font();
        for (int i = firstVisibleLine; i <= lastVisibleLine && i < lines.size(); i++) {
            float lineY = y + i * lineHeight() - scrollY;
            var text = lines.get(i);
            var drawX = x - scrollX;
            var textWithFont = Component.literal(text).withStyle(style -> style.withFont(new FontDescription.Resource(textFont)));

            context.pose.pushPose();
            context.pose.translate(drawX, lineY);
            context.pose.scale(scale, scale);
            LDFonts.drawText(context, 
                    font,
                    textWithFont,
                    0,
                    0,
                    isError ? textAreaStyle.errorColor() : textAreaStyle.textColor(),
                    textAreaStyle.textShadow()
            );
            context.pose.popPose();
        }
        if (isContentEmpty()) {
            drawPlaceHolder(context, font, scale, x, y);
        }
    }

    protected boolean isContentEmpty() {
        return lines.size() == 1 && lines.getFirst().isEmpty();
    }

    protected final void drawPlaceHolder(GUIContext context, Font font, float scale, float x, float y) {
        context.pose.pushPose();
        context.pose.translate(x, y);
        context.pose.scale(scale, scale);
        LDFonts.drawText(context, 
                font,
                textAreaStyle.placeholder(),
                0,
                0,
                ColorPattern.LIGHT_GRAY.color,
                false
        );
        context.pose.popPose();
    }

    /// Editor + Xml
    @Override
    public void loadXml(Element element) {
        var lines = XmlUtils.getContent(element, true);
        if (!lines.isEmpty()) {
            setValue(lines.split("\n"));
        }
        super.loadXml(element);
    }

    @Override
    protected void parseXmlChildElement(Element childElement) {
        // not able to add children for text
    }

    public static class ContentView extends UIElement {
        private final TextArea owner;

        public ContentView(TextArea owner) {
            this.owner = owner;
        }

        TextArea owner() {
            return owner;
        }
    }

    static final class TextAreaClientSupport {
        private TextAreaClientSupport() {
        }

        static void copyHighlightedText(TextArea area) {
            ClipboardManager.INSTANCE.copyDirect(area.getClipboardSelectionText());
        }

        static String getClipboardText() {
            var pasted = ClipboardManager.INSTANCE.paste();
            return pasted instanceof String text ? text : "";
        }

        static float scale(TextArea area) {
            return area.getTextAreaStyle().fontSize() / LDFonts.font().lineHeight;
        }

        static float getMaxWidth(TextArea area) {
            var font = LDFonts.font();
            var scale = scale(area);
            var max = 0f;
            for (String line : area.lines) {
                max = Math.max(font.getSplitter().stringWidth(TextUtilities.withFont(line, area.getTextAreaStyle().font())) * scale, max);
            }
            return max;
        }

        static Tuple<Float, Float> computeVisibleScroll(TextArea area) {
            var width = area.contentView.getContentWidth();
            var height = area.contentView.getContentHeight();
            if (width == 0 || height == 0) {
                return new Tuple<>(area.getScrollX(), area.getScrollY());
            }

            var font = LDFonts.font();
            var scale = scale(area);
            var currentLine = area.lines.get(area.getCursorLine());
            float cursorX = font.getSplitter().stringWidth(TextUtilities.withFont(currentLine.substring(0, area.getCursorCol()), area.getTextAreaStyle().font())) * scale;
            float lineTop = area.getCursorLine() * area.lineHeight();
            float lineBottom = lineTop + area.getTextAreaStyle().fontSize();

            float scrollX = area.getScrollX();
            float scrollY = area.getScrollY();
            float rightPad = 1f;
            float preferredScrollX = Math.max(0, cursorX - width + rightPad);

            if (cursorX - scrollX > width - rightPad || cursorX - scrollX < 0) {
                scrollX = preferredScrollX;
            }

            if (lineBottom - scrollY > height) {
                scrollY = lineBottom - height;
            } else if (lineTop - scrollY < 0) {
                scrollY = Math.max(lineTop, 0);
            }

            float contentTotalHeight = Math.max(area.lineHeight(), area.lines.size() * area.lineHeight());
            scrollY = Mth.clamp(Float.isNaN(scrollY) ? 0 : scrollY, 0, Math.max(0, contentTotalHeight - height));
            scrollX = Math.max(0, Float.isNaN(scrollX) ? 0 : scrollX);
            return new Tuple<>(scrollX, scrollY);
        }

        static Cursor getCursorUnderMouse(TextArea area, double mouseX, double mouseY) {
            var x = area.contentView.getContentX();
            var y = area.contentView.getContentY();
            var scale = scale(area);
            var font = LDFonts.font();

            var relY = (float) (mouseY - y + area.getScrollY()) - 2;
            int line = Mth.clamp((int) Math.floor(relY / area.lineHeight()), 0, Math.max(0, area.lines.size() - 1));
            var lineText = area.lines.get(line);
            var relX = (float) (mouseX - x + area.getScrollX());

            // Measure against the styled line so caret hit-testing matches the rendered (possibly bold/highlighted) text.
            var styledLine = area.styledLineComponent(line, 0, lineText.length());
            var subWithFont = font.substrByWidth(styledLine, (int) (relX / scale));
            float fullLength = font.getSplitter().stringWidth(styledLine) * scale;
            float subLength = font.getSplitter().stringWidth(subWithFont) * scale;
            int col;
            if (subLength >= fullLength) {
                col = lineText.length();
            } else {
                var subLen = subWithFont.getString().length();
                float nextCharWidth = font.getSplitter().stringWidth(area.styledLineComponent(line, 0, subLen + 1)) * scale - subLength;
                col = (relX - subLength) - nextCharWidth / 2f > 0 ? subLen + 1 : subLen;
            }
            return new Cursor(line, Mth.clamp(col, 0, lineText.length()));
        }
    }

    @LDLRegisterClient(name = "text_area", registry = "ldlib2:ui_element_renderer")
    public static final class TextAreaRenderer extends DelegatingUIElementRenderer<TextArea, TextAreaRenderer> {
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

        public static void drawContentViewElement(ContentView contentView, GUIContext context) {
            drawContentView(contentView.owner(), context);
        }

        public static void drawDefaultContentView(TextArea area, GUIContext context) {
            UIElementRendererRegistry.defaultRenderer().drawBackgroundAdditional(area, context);
            var x = area.contentView.getContentX();
            var y = area.contentView.getContentY();
            var height = area.contentView.getContentHeight();

            Font font = LDFonts.font();
            var scale = area.scale();

            int firstVisibleLine = (int) Math.floor(area.getScrollY() / area.lineHeight());
            int maxVisibleLines = (int) Math.ceil(height / area.lineHeight()) + 1;
            int lastVisibleLine = Mth.clamp(firstVisibleLine + maxVisibleLines, 0, Math.max(area.lines.size() - 1, 0));

            area.drawContentLines(context, font, scale, x, y, firstVisibleLine, lastVisibleLine);
            drawSelection(area, context, font, scale, x, y, firstVisibleLine, lastVisibleLine);
            drawCursor(area, context, font, scale, x, y);
        }

        private static void drawSelection(TextArea area, GUIContext context, Font font, float scale, float x, float y, int firstVisibleLine, int lastVisibleLine) {
            if (area.isFocused() && area.hasSelection()) {
                var start = selectionMin(area);
                var end = selectionMax(area);
                var highlightColor = -16776961;
                var maxWidth = getMaxWidthLocal(area, font);

                for (int line = start.line(); line <= end.line(); line++) {
                    if (line < firstVisibleLine || line > lastVisibleLine) continue;

                    String text = area.lines.get(line);
                    int from = (line == start.line()) ? start.col() : 0;
                    int to = (line == end.line()) ? end.col() : text.length();

                    from = Mth.clamp(from, 0, text.length());
                    to = Mth.clamp(to, 0, text.length());

                    float minX = font.getSplitter().stringWidth(area.styledLineComponent(line, 0, from)) * scale - area.getScrollX();
                    float maxX;
                    if (line == end.line()) {
                        if (from == to) continue;
                        maxX = font.getSplitter().stringWidth(area.styledLineComponent(line, 0, to)) * scale - area.getScrollX();
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

        private static void drawCursor(TextArea area, GUIContext context, Font font, float scale, float x, float y) {
            if (area.isVisible() && area.isFocused() && area.isDisplayed() && (!area.isActive() || System.currentTimeMillis() % 1000 < 500)) {
                float cursorPosX = font.getSplitter().stringWidth(area.styledLineComponent(area.getCursorLine(), 0, area.getCursorCol())) * scale;
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

        private static float getMaxWidthLocal(TextArea area, Font font) {
            var scale = area.scale();
            var max = 0f;
            for (String line : area.lines) {
                max = Math.max(font.getSplitter().stringWidth(TextUtilities.withFont(line, area.getTextAreaStyle().font())) * scale, max);
            }
            return max;
        }
    }

    @LDLRegisterClient(name = "text_area_content_view", registry = "ldlib2:ui_element_renderer")
    public static final class TextAreaContentViewRenderer extends DelegatingUIElementRenderer<ContentView, TextAreaContentViewRenderer> {
        @Override
        public Class<ContentView> type() {
            return ContentView.class;
        }

        @Override
        public void drawBackgroundAdditional(ContentView element, IGUIContext context) {
            if (!(context instanceof GUIContext guiContext)) {
                drawParentBackgroundAdditional(element, context);
                return;
            }
            TextAreaRenderer.drawContentViewElement(element, guiContext);
        }
    }
}
