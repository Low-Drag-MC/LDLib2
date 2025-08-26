package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaOverflow;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A multi-line editable text area.
 * Value type is String[], each item is a line.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class TextArea extends BindableUIElement<String[]> {
    // Internal helpers
    public record Cursor(int line, int col) {}
    public record CursorDragStart(Cursor anchor) {}

    @Accessors(chain = true, fluent = true)
    public static class TextAreaStyle extends Style {
        @Getter @Setter
        private float fontSize = 9f;
        @Getter @Setter
        private int textColor = -1;
        @Getter @Setter
        private int errorColor = 0xffff0000;
        @Getter @Setter
        private int cursorColor = 0xffeeeeee;
        @Getter @Setter
        private boolean textShadow = true;
        @Getter @Setter
        private Component placeholder = Component.translatable("text_field.empty");

        @Getter @Setter
        private float lineSpacing = 1f; // extra pixels between lines

        // Focus overlay
        @Getter @Setter
        private com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture focusOverlay = Sprites.RECT_RD_T_SOLID;

        public TextAreaStyle(UIElement holder) {
            super(holder);
        }
    }

    // Validation
    @Setter private Predicate<String[]> textValidator = Predicates.alwaysTrue();
    @Setter private Predicate<Character> charValidator = Predicates.alwaysTrue();

    // Style
    @Getter private final TextAreaStyle textAreaStyle = new TextAreaStyle(this);

    // Raw edit buffer (what user is editing right now)
    private final List<String> lines = new ArrayList<>();
    // Last accepted valid value (used for getValue/notify)
    private final List<String> valueLines = new ArrayList<>();
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
        // Default layout and look
        getLayout().setHeight(60);
        getLayout().setPadding(YogaEdge.ALL, 2);
        getStyle().backgroundTexture(Sprites.RECT_RD_SOLID);
        getLayoutNode().setOverflow(YogaOverflow.HIDDEN);

        setFocusable(true);

        // Event wiring
        addEventListener(UIEvents.CHAR_TYPED, this::onCharTyped);
        addEventListener(UIEvents.KEY_DOWN, this::onKeyDown);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSource);
        addEventListener(UIEvents.MOUSE_WHEEL, this::onMouseWheel);
        addEventListener(UIEvents.BLUR, this::onBlur);
        lines.add("");
    }

    public TextArea textAreaStyle(Consumer<TextAreaStyle> style) {
        style.accept(textAreaStyle);
        onStyleChanged();
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        textAreaStyle.applyStyles(values);
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        ensureCursorVisible();
    }

    // Bindable value
    @Override
    public String[] getValue() {
        return valueLines.toArray(String[]::new);
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

    private boolean hasSelection() {
        return !(selStartLine == selEndLine && selStartCol == selEndCol);
    }

    private Cursor cursorPos() {
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
        var width = getContentWidth();
        var height = getContentHeight();

        var font = getFont();
        var s = scale();
        var currentLine = lines.get(cursorLine);

        // Compute cursor pixel positions
        float cursorX = font.width(currentLine.substring(0, cursorCol)) * s;
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
        scrollY = Mth.clamp(scrollY, 0, Math.max(0, contentTotalHeight - height));
        // Clamp horizontal scroll
        scrollX = Math.max(0, scrollX);
    }

    private void onBlur(UIEvent e) {
        if (hasSelection()) {
            collapseSelectionToCursor();
        }
    }

    private void onMouseWheel(UIEvent event) {
        // Vertical scroll by lines
        float deltaLines = event.deltaY > 0 ? 3 : -3;
        scrollY = Mth.clamp(scrollY + deltaLines * lineHeight(), 0, Math.max(0, lines.size() * lineHeight() - getContentHeight()));
        event.stopPropagation();
    }

    private void onMouseDown(UIEvent event) {
        if (event.button == 0 && isMouseOver(event.x, event.y)) {
            var pos = getCursorUnderMouse(event.x, event.y);
            var old = cursorPos();
            setCursor(pos.line, pos.col);
            if (isShiftDown()) {
                // Extend selection
                setSelection(new Cursor(selStartLine, selStartCol), cursorPos());
            } else {
                // Reset selection
                setSelection(cursorPos(), cursorPos());
            }
            startDrag(new CursorDragStart(pos), null);
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
        float length = font.width(sub) * s;
        int col;
        if (sub.length() >= lineText.length()) {
            col = lineText.length();
        } else {
            float nextCharWidth = font.width(lineText.substring(sub.length(), sub.length() + 1)) * s;
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
        if (hasSelection()) {
            // collapse to selection start
            var start = selMin();
            setCursor(start.line, start.col);
            collapseSelectionToCursor();
            return;
        }
        if (cursorCol > 0) {
            setCursor(cursorLine, cursorCol - 1);
        } else if (cursorLine > 0) {
            setCursor(cursorLine - 1, lines.get(cursorLine - 1).length());
        }
    }

    private void moveRight() {
        if (hasSelection()) {
            var end = selMax();
            setCursor(end.line, end.col);
            collapseSelectionToCursor();
            return;
        }
        var lineText = lines.get(cursorLine);
        if (cursorCol < lineText.length()) {
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
        float visibleLines = Math.max(1, (int) (getContentHeight() / lineHeight()));
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
        if (isChildHover() || isFocused()) {
            guiContext.drawTexture(textAreaStyle.focusOverlay(), getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        }
        super.drawBackgroundOverlay(guiContext);
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        var x = getContentX();
        var y = getContentY();
        var width = getContentWidth();
        var height = getContentHeight();

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

            for (int line = start.line; line <= end.line; line++) {
                if (line < firstVisibleLine || line > lastVisibleLine) continue;

                String text = lines.get(line);
                int from = (line == start.line) ? start.col : 0;
                int to = (line == end.line) ? end.col : text.length();
                if (from == to) continue;

                float minX = font.width(text.substring(0, from)) * s - scrollX;
                float maxX = font.width(text.substring(0, to)) * s - scrollX;
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
        }

        // Cursor
        if (isFocused() && System.currentTimeMillis() % 1000 < 500) {
            var current = lines.get(cursorLine);
            float cursorPosX = font.width(current.substring(0, cursorCol)) * s;
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