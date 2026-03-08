package com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor;

import com.lowdragmc.lowdraglib2.gui.LDLibFonts;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * A code editor with syntax highlighting support
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "code-editor", group = "misc", registry = "ldlib2:ui_element")
public class CodeEditor extends TextArea {
    private static final String CMT = "//";

    @Getter
    private final SyntaxParser syntaxParser = new SyntaxParser();
    @Getter @Setter
    private StyleManager styleManager = StyleManager.DEFAULT;

    // runtime
    private boolean needsReparsing = true;
    private final List<StyledLine> styledLines = new ArrayList<>();

    public CodeEditor() {
        getTextAreaStyle().setDefault(PropertyRegistry.FONT, LDLibFonts.JETBRAINS_MONO_BOLD);
        internalSetup();
    }

    /**
     * Set the language for syntax highlighting
     */
    public CodeEditor setLanguage(ILanguageDefinition language) {
        if (this.syntaxParser.getLanguageDefinition() != language) {
            this.syntaxParser.setLanguageDefinition(language);
            this.needsReparsing = true;
        }
        return this;
    }

    public ILanguageDefinition getLanguage() {
        return syntaxParser.getLanguageDefinition();
    }

    @Override
    public CodeEditor setValue(@Nullable String[] value, boolean notify) {
        super.setValue(value, notify);
        needsReparsing = true;
        return this;
    }

    @Override
    protected void onRawLinesUpdated() {
        super.onRawLinesUpdated();
        needsReparsing = true;
    }

    @Override
    protected void onKeyDown(UIEvent event) {
        if (isEditable()) {
            switch (event.keyCode) {
                case GLFW.GLFW_KEY_TAB -> insertText("  ");
                case GLFW.GLFW_KEY_SLASH -> {
                    if (isCtrlDown()) {
                        toggleCommentAtBol();
                    }
                }
                default -> super.onKeyDown(event);
            }
        } else {
            super.onKeyDown(event);
        }
    }

    @Override
    protected void insertNewLine() {
        var cursorPos = cursorPos();
        super.insertNewLine();
        var lastLine = lines.get(cursorPos.line());
        int spaces = 0;
        for (char c : lastLine.toCharArray()) {
            if (c == ' ') spaces++;
            else break;
        }
        if (spaces > 0) {
            insertText(" ".repeat(spaces));
        }
    }

    protected void toggleCommentAtBol() {
        pushHistory();

        if (!hasSelection()) {
            int line = getCursorLine();
            String s = lines.get(line);
            int delta = 0;

            if (s.startsWith(CMT)) {
                lines.set(line, s.substring(CMT.length()));
                delta = -CMT.length();
            } else {
                lines.set(line, CMT + s);
                delta = +CMT.length();
            }
            adjustCaretForBolDelta(line, delta);
            onRawLinesUpdated();
            return;
        }

        int a = getSelStartLine();
        int b = getSelEndLine();
        if (b < a) {
            int tmp = a;
            a = b;
            b = tmp;
        }

        boolean allCommented = true;
        for (int i = a; i <= b; i++) {
            String s = lines.get(i);
            if (!s.startsWith(CMT)) { allCommented = false; break; }
        }

        int caretLine  = getCursorLine();
        int caretDelta = 0;

        for (int i = a; i <= b; i++) {
            String s = lines.get(i);
            if (allCommented) {
                if (s.startsWith(CMT)) {
                    lines.set(i, s.substring(CMT.length()));
                    if (i == caretLine) caretDelta = -CMT.length();
                }
            } else {
                lines.set(i, CMT + s);
                if (i == caretLine) caretDelta = +CMT.length();
            }
        }

        if (caretDelta != 0) adjustCaretForBolDelta(caretLine, caretDelta);
        onRawLinesUpdated();
    }

    private void adjustCaretForBolDelta(int line, int delta) {
        if (getCursorLine() != line || delta == 0) return;
        int col = getCursorCol();
        if (col > 0) setCursor(line, Math.max(0, col + delta));
    }

    /**
     * Reparse all lines and update styling
     */
    private void reparseAndStyle() {
        if (!needsReparsing) return;

        styledLines.clear();
        String[] lines = getValue();

        for (int i = 0; i < lines.length; i++) {
            String lineText = lines[i];
            List<Token> tokens = syntaxParser.parseLine(lineText);
            List<StyledText> styledTexts = applyStyles(tokens);
            styledLines.add(new StyledLine(i, styledTexts));
        }

        needsReparsing = false;
    }

    /**
     * Apply styles to tokens
     */
    private List<StyledText> applyStyles(List<Token> tokens) {
        List<StyledText> result = new ArrayList<>();
        for (Token token : tokens) {
            Style style = styleManager.getStyleForTokenType(token.type());
            result.add(new StyledText(token.text(), style));
        }
        return result;
    }

    /**
     * Get styled lines for rendering
     */
    public List<StyledLine> getStyledLines() {
        if (needsReparsing) {
            reparseAndStyle();
        }
        return styledLines;
    }
}
