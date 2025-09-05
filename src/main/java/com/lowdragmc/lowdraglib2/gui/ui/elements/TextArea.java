package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigFont;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.TextUtilities;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaOverflow;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@LDLRegister(name = "text_area", registry = "ldlib2:ui_element")
public class TextArea extends BindableUIElement<String[]> {
    // Internal helpers
    public record Cursor(int line, int col) {}
    public record CursorDragStart(Cursor anchor) {}

    @Accessors(chain = true, fluent = true)
    public static class TextAreaStyle extends Style {
        @Getter @Setter
        @Configurable(name = "fontSize")
        private float fontSize = 9f;
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
        private int errorColor = 0xffff0000;
        @Getter @Setter
        @Configurable(name = "cursorColor")
        private int cursorColor = 0xffeeeeee;
        @Getter @Setter
        @Configurable(name = "textShadow")
        private boolean textShadow = true;
        @Getter @Setter
        @Configurable(name = "placeholder")
        private Component placeholder = Component.translatable("text_field.empty");
        @Getter @Setter
        @Configurable(name = "verticalScrollDisplay")
        private ScrollDisplay verticalScrollDisplay = ScrollDisplay.AUTO;
        @Getter @Setter
        @Configurable(name = "horizontalScrollDisplay")
        private ScrollDisplay horizontalScrollDisplay = ScrollDisplay.AUTO;
        @Getter @Setter
        @Configurable(name = "mode")
        private ScrollerMode mode = ScrollerMode.BOTH;

        @Getter @Setter
        private float lineSpacing = 1f; // extra pixels between lines

        // Focus overlay
        @Getter @Setter
        private IGuiTexture focusOverlay = Sprites.RECT_RD_T_SOLID;

        public TextAreaStyle(UIElement holder) {
            super(holder);
        }
    }

    public final Scroller horizontalScroller;
    public final Scroller verticalScroller;
    public final UIElement contentView;

    // Validation
    @Setter private Predicate<String[]> textValidator = Predicates.alwaysTrue();
    @Setter private Predicate<Character> charValidator = Predicates.alwaysTrue();

    // Style
    @Configurable(name = "textAreaStyle", subConfigurable = true)
    @Getter private final TextAreaStyle textAreaStyle = new TextAreaStyle(this);

    // Raw edit buffer (what user is editing right now)
    private final List<String> lines = new ArrayList<>();
    // Last accepted valid value (used for getValue/notify)
    private final List<String> valueLines = new ArrayList<>();

    // runtime
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

    public TextArea() {
        this.horizontalScroller = new Scroller.Horizontal().setRange(0, 1f).setClampNormalizedValue(this::horizontalClamp);
        this.verticalScroller = new Scroller.Vertical().setRange(0, 1f).setClampNormalizedValue(this::verticalClamp);

        // Default layout and look
        getLayout().setHeight(60);

        this.contentView = new UIElement() {
            @Override
            public void drawBackgroundAdditional(GUIContext guiContext) {
                drawContentView(guiContext);
            }
        };
        this.contentView.layout(layout -> {
            layout.setPadding(YogaEdge.ALL, 2);
            layout.setFlex(1);
            layout.setHeightPercent(100);
        });
        this.contentView.style(style -> style.backgroundTexture(Sprites.RECT_RD_SOLID));
        this.contentView.setOverflow(YogaOverflow.HIDDEN);
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
        this.contentView.addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        this.contentView.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSource);
        this.contentView.addEventListener(UIEvents.MOUSE_WHEEL, this::onMouseWheel);
        addEventListener(UIEvents.BLUR, this::onBlur);
        lines.add("");

        verticalScroller.setOnValueChanged(this::onVerticalScroll);
        horizontalScroller.setOnValueChanged(this::onHorizontalScroll);
        addChildren(new UIElement().layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWidthPercent(100);
            layout.setFlex(1);
        }).addChildren(contentView, verticalScroller), horizontalScroller);
        markAllChildrenAsInternal();
    }

    public TextArea textAreaStyle(Consumer<TextAreaStyle> style) {
        style.accept(textAreaStyle);
        onStyleChanged();
        ensureCursorVisible();
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        textAreaStyle.applyStyles(values);
        ensureCursorVisible();
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
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
        return Mth.clamp(Mth.abs(normalizedValue),
                textAreaStyle.fontSize / containerWidth,
                (textAreaStyle.fontSize + textAreaStyle.lineSpacing) / containerWidth)
                * (normalizedValue > 0 ? 1 : -1);
    }

    protected float verticalClamp(float normalizedValue) {
        var containerHeight = getMaxHeight() - contentView.getContentHeight();
        return Mth.clamp(Mth.abs(normalizedValue),
                textAreaStyle.fontSize / containerHeight,
                (textAreaStyle.fontSize + textAreaStyle.lineSpacing) / containerHeight)
                * (normalizedValue > 0 ? 1 : -1);
    }

    private void updateScrollers() {
        var maxWidth = getMaxWidth();
        var maxHeight = getMaxHeight();
        var hP = scrollX / (maxWidth - contentView.getContentWidth());
        hP = Mth.clamp(hP, 0, 1);
        var wP = scrollY / (maxHeight - contentView.getContentHeight());
        wP = Mth.clamp(wP, 0, 1);
        horizontalScroller.setValue(hP);
        verticalScroller.setValue(wP);

        if (textAreaStyle.mode == ScrollerMode.HORIZONTAL || textAreaStyle.mode == ScrollerMode.BOTH) {
            // cause we are using a flexbox, the width of the view container is not the same as the width of the view port
            // so we need to calculate the width ourselves
            var vp = Math.min(1, contentView.getContentWidth() / maxWidth);
            horizontalScroller.setScrollBarSize(vp * 100);
            if ((textAreaStyle.horizontalScrollDisplay == ScrollDisplay.AUTO && vp < 1) || textAreaStyle.horizontalScrollDisplay == ScrollDisplay.ALWAYS) {
                horizontalScroller.setDisplay(YogaDisplay.FLEX);

            } else {
                horizontalScroller.setDisplay(YogaDisplay.NONE);
            }
        } else {
            horizontalScroller.setDisplay(YogaDisplay.NONE);
        }

        if (textAreaStyle.mode == ScrollerMode.VERTICAL || textAreaStyle.mode == ScrollerMode.BOTH) {
            var hp = Math.min(1, contentView.getContentHeight() / maxHeight);
            verticalScroller.setScrollBarSize(hp * 100);
            if ((textAreaStyle.verticalScrollDisplay == ScrollDisplay.AUTO && hp < 1) || textAreaStyle.verticalScrollDisplay == ScrollDisplay.ALWAYS) {
                verticalScroller.setDisplay(YogaDisplay.FLEX);
            } else {
                verticalScroller.setDisplay(YogaDisplay.NONE);
            }
        } else {
            verticalScroller.setDisplay(YogaDisplay.NONE);
        }
    }

    // Bindable value
    @Override
    public String[] getValue() {
        return valueLines.toArray(String[]::new);
    }

    public TextArea setLines(List<String> lines) {
        return setValue(lines.toArray(new String[0]));
    }

    public TextArea setLines(String[] lines, boolean notify) {
        return setValue(lines, notify);
    }

    public TextArea setValue(@Nullable String[] value) {
        return setValue(value, true);
    }

    @Override
    public TextArea setValue(@Nullable String[] value, boolean notify) {
        lines.clear();
        valueLines.clear();
        if (value != null && value.length > 0) {
            for (String s : value) {
                lines.add(s == null ? "" : s);
                valueLines.add(s == null ? "" : s);
            }
        } else {
            lines.add("");
            valueLines.add("");
        }
        // Reset cursor and selection at end
        cursorLine = Math.max(0, lines.size() - 1);
        cursorCol = lines.get(cursorLine).length();
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

    // Editing helpers
    private Font getFont() {
        return Minecraft.getInstance().font;
    }

    private float scale() {
        return textAreaStyle.fontSize() / getFont().lineHeight;
    }

    private float lineHeight() {
        return textAreaStyle.fontSize() + textAreaStyle.lineSpacing();
    }

    public boolean hasSelection() {
        return !(selStartLine == selEndLine && selStartCol == selEndCol);
    }

    public Cursor cursorPos() {
        return new Cursor(cursorLine, cursorCol);
    }

    private static int comparePos(Cursor a, Cursor b) {
        if (a.line != b.line) return Integer.compare(a.line, b.line);
        return Integer.compare(a.col, b.col);
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
        var font = getFont();
        var s = scale();
        var max = 0f;
        for (String line : lines) {
            max = Math.max(font.width(TextUtilities.withFont(line, getTextAreaStyle().font())) * s, max);
        }
        return max;
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
        selStartLine = a.line; selStartCol = a.col;
        selEndLine = b.line; selEndCol = b.col;
    }

    public void collapseSelectionToCursor() {
        selStartLine = selEndLine = cursorLine;
        selStartCol = selEndCol = cursorCol;
    }

    private void ensureCursorVisible() {
        if (!LDLib2.isClient()) return;
        var width = contentView.getContentWidth();
        var height = contentView.getContentHeight();

        var font = getFont();
        var s = scale();
        var currentLine = lines.get(cursorLine);

        // Compute cursor pixel positions
        float cursorX = font.width(TextUtilities.withFont(currentLine.substring(0, cursorCol), getTextAreaStyle().font())) * s;
        float lineTop = cursorLine * lineHeight();
        float lineBottom = lineTop + textAreaStyle.fontSize();

        // Horizontal: prefer cursor on the right edge of viewport
        float rightPad = 1f; // keep the same visual padding as before
        float preferredScrollX = Math.max(0, cursorX - width + rightPad);

        // Only adjust when cursor is actually out of view
        if (cursorX - scrollX > width - rightPad || cursorX - scrollX < 0) {
            // Try to place the cursor at the right edge first;
            // if there's not enough content on the left, preferredScrollX will clamp to 0.
            scrollX = preferredScrollX;
        }

        // Vertical
        if (lineBottom - scrollY > height) {
            scrollY = lineBottom - height;
        } else if (lineTop - scrollY < 0) {
            scrollY = Math.max(lineTop, 0);
        }

        // Clamp vertical scroll to content size
        float contentTotalHeight = Math.max(lineHeight(), lines.size() * lineHeight());
        scrollY = Mth.clamp(Float.isNaN(scrollY) ? 0 : scrollY, 0, Math.max(0, contentTotalHeight - height));
        // Clamp horizontal scroll
        scrollX = Math.max(0, Float.isNaN(scrollX) ? 0 : scrollX);
        updateScrollers();
    }

    private void onBlur(UIEvent e) {
        if (hasSelection()) {
            collapseSelectionToCursor();
        }
    }

    private void onMouseWheel(UIEvent event) {
        if (event.deltaY != 0 && (textAreaStyle.mode == ScrollerMode.VERTICAL || textAreaStyle.mode == ScrollerMode.BOTH)) {
            verticalScroller.onScrollWheel(event);
        }
        if (event.deltaX != 0 && (textAreaStyle.mode == ScrollerMode.HORIZONTAL || textAreaStyle.mode == ScrollerMode.BOTH)) {
            horizontalScroller.onScrollWheel(event);
        } else if (event.deltaY != 0 && textAreaStyle.mode == ScrollerMode.HORIZONTAL) {
            horizontalScroller.onScrollWheel(event);
        }
        event.stopPropagation();
    }

    private void onMouseDown(UIEvent event) {
        if (event.button == 0 && isMouseOver(event.x, event.y)) {
            var pos = getCursorUnderMouse(event.x, event.y);
            setCursor(pos.line, pos.col);
            if (isShiftDown()) {
                // Extend selection
                setSelection(new Cursor(selStartLine, selStartCol), cursorPos());
            } else {
                // Reset selection
                setSelection(cursorPos(), cursorPos());
            }
            contentView.startDrag(new CursorDragStart(pos), null);
            event.stopPropagation();
            focus();
        }
    }

    private void onDragSource(UIEvent event) {
        if (event.dragHandler.draggingObject instanceof CursorDragStart(Cursor anchor)) {
            var pos = getCursorUnderMouse(event.x, event.y);
            setCursor(pos.line, pos.col);
            setSelection(anchor, cursorPos());
        }
    }

    public Cursor getCursorUnderMouse(double mouseX, double mouseY) {
        var x = getContentX();
        var y = getContentY();
        var s = scale();
        var font = getFont();

        // Determine line
        var relY = (float) (mouseY - y + scrollY);
        int line = Mth.clamp((int) Math.floor(relY / lineHeight()), 0, Math.max(0, lines.size() - 1));

        // Determine column by measuring width
        var lineText = lines.get(line);
        var relX = (float) (mouseX - x + scrollX);

        // Estimate column using font width and substring fitting
        var sub = font.plainSubstrByWidth(lineText, (int) (relX / s));
        float length = font.width(TextUtilities.withFont(sub, getTextAreaStyle().font())) * s;
        int col;
        if (sub.length() >= lineText.length()) {
            col = lineText.length();
        } else {
            float nextCharWidth = font.width(TextUtilities.withFont(lineText.substring(sub.length(), sub.length() + 1), getTextAreaStyle().font())) * s;
            col = (relX - length) - nextCharWidth / 2f > 0 ? sub.length() + 1 : sub.length();
        }
        col = Mth.clamp(col, 0, lineText.length());
        return new Cursor(line, col);
    }

    private void onCharTyped(UIEvent event) {
        if (!isEditable()) return;
        if (StringUtil.isAllowedChatCharacter(event.codePoint) && charValidator.test(event.codePoint)) {
            insertText(Character.toString(event.codePoint));
        }
    }

    private void onKeyDown(UIEvent event) {
        if (!isEditable()) return;

        switch (event.keyCode) {
            case GLFW.GLFW_KEY_ENTER -> {
                insertNewLine();
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                deleteChars(-1);
            }
            case GLFW.GLFW_KEY_DELETE -> {
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
                if (Screen.isSelectAll(event.keyCode)) {
                    selectAll();
                } else if (Screen.isCopy(event.keyCode)) {
                    ClipboardManager.INSTANCE.copyDirect(getHighlightedText());
                } else if (Screen.isPaste(event.keyCode)) {
                    insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
                } else if (Screen.isCut(event.keyCode)) {
                    ClipboardManager.INSTANCE.copyDirect(getHighlightedText());
                    insertText(""); // replace selection with empty
                }
            }
        }
    }

    private void updateSelectionAfterMove() {
        if (isShiftDown()) {
            setSelection(new Cursor(selStartLine, selStartCol), cursorPos());
        } else {
            collapseSelectionToCursor();
        }
    }

    private void moveLeft() {
        if (cursorCol > 0) {
            setCursor(cursorLine, cursorCol - 1);
        } else if (cursorLine > 0) {
            var prev = lines.get(cursorLine - 1);
            int newCol = prev.length();
            setCursor(cursorLine - 1, newCol);
        }
    }

    private void moveRight() {
        if (cursorCol < lines.get(cursorLine).length()) {
            setCursor(cursorLine, cursorCol + 1);
        } else if (cursorLine < lines.size() - 1) {
            setCursor(cursorLine + 1, 0);
        }
    }

    private void moveUp() {
        if (cursorLine > 0) {
            int newLine = cursorLine - 1;
            int col = Math.min(cursorCol, lines.get(newLine).length());
            setCursor(newLine, col);
        }
    }

    private void moveDown() {
        if (cursorLine < lines.size() - 1) {
            int newLine = cursorLine + 1;
            int col = Math.min(cursorCol, lines.get(newLine).length());
            setCursor(newLine, col);
        }
    }

    private void moveWord(int dir) {
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

    private void page(int direction) {
        float visibleLines = Math.max(1, (int) (contentView.getContentHeight() / lineHeight()));
        int newLine = Mth.clamp(cursorLine + (int) (direction * visibleLines), 0, lines.size() - 1);
        int col = Math.min(cursorCol, lines.get(newLine).length());
        setCursor(newLine, col);
    }

    private void selectAll() {
        selStartLine = 0;
        selStartCol = 0;
        selEndLine = Math.max(0, lines.size() - 1);
        selEndCol = lines.get(selEndLine).length();
        setCursor(selEndLine, selEndCol);
    }

    private void insertNewLine() {
        replaceSelectionWith("\n");
    }

    private void deleteChars(int dir) {
        if (hasSelection()) {
            replaceSelectionWith("");
            return;
        }
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

    private void replaceSelectionWith(String text) {
        if (hasSelection()) {
            var start = selMin();
            var end = selMax();

            if (start.line == end.line) {
                var s = lines.get(start.line);
                String before = s.substring(0, start.col);
                String after = s.substring(end.col);
                List<String> incoming = splitLines(text);

                if (incoming.size() == 1) {
                    lines.set(start.line, before + incoming.get(0) + after);
                    setCursor(start.line, before.length() + incoming.get(0).length());
                } else {
                    String first = before + incoming.get(0);
                    String last = incoming.get(incoming.size() - 1) + after;
                    lines.set(start.line, first);
                    // drop lines between start..end
                    for (int i = end.line; i > start.line; i--) {
                        lines.remove(i);
                    }
                    // insert middle lines
                    for (int i = 1; i < incoming.size() - 1; i++) {
                        lines.add(start.line + i, incoming.get(i));
                    }
                    lines.add(start.line + incoming.size() - 1, last);
                    setCursor(start.line + incoming.size() - 1, incoming.get(incoming.size() - 1).length());
                }
            } else {
                // multi-line selection
                var startLine = lines.get(start.line);
                var endLine = lines.get(end.line);
                String before = startLine.substring(0, start.col);
                String after = endLine.substring(end.col);
                List<String> incoming = splitLines(text);

                // remove lines between start+1 .. end
                for (int i = end.line; i > start.line; i--) {
                    lines.remove(i);
                }

                if (incoming.size() == 1) {
                    lines.set(start.line, before + incoming.get(0) + after);
                    setCursor(start.line, before.length() + incoming.get(0).length());
                } else {
                    String first = before + incoming.get(0);
                    String last = incoming.get(incoming.size() - 1) + after;
                    lines.set(start.line, first);
                    for (int i = 1; i < incoming.size() - 1; i++) {
                        lines.add(start.line + i, incoming.get(i));
                    }
                    lines.add(start.line + incoming.size() - 1, last);
                    setCursor(start.line + incoming.size() - 1, incoming.get(incoming.size() - 1).length());
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
                    lines.set(cursorLine, before + incoming.get(0) + after);
                    setCursor(cursorLine, cursorCol + incoming.get(0).length());
                } else {
                    String first = before + incoming.get(0);
                    String last = incoming.get(incoming.size() - 1) + after;
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

    private void insertText(String text) {
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
    private void onRawLinesUpdated() {
        // Validate
        String[] candidate = lines.toArray(String[]::new);
        if (textValidator.test(candidate)) {
            isError = false;
            if (!equalsValue(candidate)) {
                valueLines.clear();
                for (String s : candidate) valueLines.add(s);
                notifyListeners();
            }
        } else {
            isError = true;
        }
        ensureCursorVisible();
    }

    private boolean equalsValue(String[] candidate) {
        if (candidate.length != valueLines.size()) return false;
        for (int i = 0; i < candidate.length; i++) {
            if (!candidate[i].equals(valueLines.get(i))) return false;
        }
        return true;
    }

    private String getHighlightedText() {
        if (!hasSelection()) return "";
        var start = selMin();
        var end = selMax();
        if (start.line == end.line) {
            var s = lines.get(start.line);
            return s.substring(start.col, end.col);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(lines.get(start.line).substring(start.col));
        sb.append('\n');
        for (int i = start.line + 1; i < end.line; i++) {
            sb.append(lines.get(i)).append('\n');
        }
        sb.append(lines.get(end.line), 0, end.col);
        return sb.toString();
    }

    public boolean isEditable() {
        return isActive() && isVisible() && isFocused() && isDisplayed();
    }

    // Rendering
    @Override
    public void drawBackgroundOverlay(GUIContext guiContext) {
        if (contentView.isChildHover() || isFocused()) {
            guiContext.drawTexture(textAreaStyle.focusOverlay(),
                    contentView.getPositionX(), contentView.getPositionY(),
                    contentView.getSizeWidth(), contentView.getSizeHeight());
        }
        super.drawBackgroundOverlay(guiContext);
    }

    public void drawContentView(GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        var x = contentView.getContentX();
        var y = contentView.getContentY();
        var width = contentView.getContentWidth();
        var height = contentView.getContentHeight();

        var font = getFont();
        var s = scale();

        // Draw lines of text
        float totalHeight = Math.max(lineHeight(), lines.size() * lineHeight());
        int firstVisibleLine = (int) Math.floor(scrollY / lineHeight());
        int maxVisibleLines = (int) Math.ceil(height / lineHeight()) + 1;
        int lastVisibleLine = Mth.clamp(firstVisibleLine + maxVisibleLines, 0, Math.max(lines.size() - 1, 0));

        // Text
        for (int i = firstVisibleLine; i <= lastVisibleLine && i < lines.size(); i++) {
            float lineY = y + i * lineHeight() - scrollY;
            var text = lines.get(i);
            var drawX = x - scrollX;

            guiContext.pose.pushPose();
            guiContext.pose.translate(drawX, lineY, 0);
            guiContext.pose.scale(s, s, 1);
            guiContext.graphics.drawString(
                    font,
                    text,
                    0,
                    0,
                    isError ? textAreaStyle.errorColor() : textAreaStyle.textColor(),
                    textAreaStyle.textShadow()
            );
            guiContext.pose.popPose();
        }

        // Placeholder
        if (lines.size() == 1 && lines.getFirst().isEmpty()) {
            guiContext.pose.pushPose();
            guiContext.pose.translate(x, y, 0);
            guiContext.pose.scale(s, s, 1);
            guiContext.graphics.drawString(
                    font,
                    textAreaStyle.placeholder(),
                    0,
                    0,
                    ColorPattern.LIGHT_GRAY.color,
                    false
            );
            guiContext.pose.popPose();
        }

        // Selection highlight
        if (isFocused() && hasSelection()) {
            var start = selMin();
            var end = selMax();
            var highlightColor = -16776961; // same as TextField
            var maxWidth = getMaxWidth();

            guiContext.graphics.drawManaged(() -> {
                for (int line = start.line; line <= end.line; line++) {
                    if (line < firstVisibleLine || line > lastVisibleLine) continue;

                    String text = lines.get(line);
                    int from = (line == start.line) ? start.col : 0;
                    int to = (line == end.line) ? end.col : text.length();

                    float minX = font.width(TextUtilities.withFont(text.substring(0, from), getTextAreaStyle().font())) * s - scrollX;
                    float maxX;
                    if (line == end.line) {
                        if (from == to) continue;
                        maxX = font.width(TextUtilities.withFont(text.substring(0, to), getTextAreaStyle().font())) * s - scrollX;
                    } else {
                        maxX = maxWidth * s - scrollX;
                    }
                    float lineY = y + line * lineHeight() - scrollY;

                    DrawerHelper.drawSolidRect(
                            guiContext.graphics,
                            RenderType.guiTextHighlight(),
                            x + minX,
                            lineY,
                            maxX - minX,
                            textAreaStyle.fontSize(),
                            highlightColor
                    );
                }
            });
        }

        // Cursor
        if (isFocused() && System.currentTimeMillis() % 1000 < 500) {
            var current = lines.get(cursorLine);
            float cursorPosX = font.width(TextUtilities.withFont(current.substring(0, cursorCol), getTextAreaStyle().font())) * s;
            float cursorY = y + cursorLine * lineHeight() - scrollY;
            DrawerHelper.drawSolidRect(
                    guiContext.graphics,
                    x + cursorPosX - scrollX,
                    cursorY,
                    1,
                    textAreaStyle.fontSize(),
                    textAreaStyle.cursorColor()
            );
        }
    }
}