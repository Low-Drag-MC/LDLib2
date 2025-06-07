package com.lowdragmc.lowdraglib2.gui.widget.codeeditor;

import com.lowdragmc.lowdraglib2.gui.widget.codeeditor.language.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CodeEditor {
    @Setter
    private String indentString = "    "; // 4 个空格
    private final Document document;
    private final SyntaxParser syntaxParser;
    private final StyleManager styleManager;
    @Setter
    private Cursor cursor;
    private final FoldingManager foldingManager;
    private final List<StyledLine> styledLines;
    @Nullable
    private Selection selection;
    // runtime
    private List<StyledLine> visibleLinesCache;

    public CodeEditor() {
        document = new Document();
        syntaxParser = new SyntaxParser();
        cursor = new Cursor(0, 0);
        styledLines = new ArrayList<>();
        styleManager = new StyleManager();
        foldingManager = new FoldingManager();
        reparseAndStyle();
    }

    public void setLanguageDefinition(ILanguageDefinition languageDefinition) {
        if (languageDefinition != syntaxParser.getLanguageDefinition()) {
            syntaxParser.setLanguageDefinition(languageDefinition);
            reparseAndStyle();
        }
    }

    public void setCursor(int line, int column) {
        setCursor(new Cursor(line, column));
    }
    
    public void setCursorLine(int line) {
        setCursor(new Cursor(line, cursor.column()));
    }
    
    public void setCursorColumn(int column) {
        setCursor(new Cursor(cursor.line(), column));
    }

    /**
     * Insert text after the cursor
     */
    public void insertText(String text) {
        // 如果存在选区，先删除选中内容
        if (isSelectionValid()) {
            deleteSelection();
        }
        // 处理文本中的换行符
        String[] lines = text.split("\n", -1);
        if (lines.length == 1) {
            document.insertText(cursor.line(), cursor.column(), text);
            setCursorColumn(cursor.column() + text.length());
        } else {
            String currentLine = document.getLine(cursor.line());
            String beforeCursor = currentLine.substring(0, cursor.column());
            String afterCursor = currentLine.substring(cursor.column());

            // 设置当前行文本
            document.setLine(cursor.line(), beforeCursor + lines[0]);

            // 插入新行
            for (int i = 1; i < lines.length - 1; i++) {
                document.insertLine(cursor.line() + i, lines[i]);
            }

            // 最后一行
            int lastLineIndex = cursor.line() + lines.length - 1;
            document.insertLine(lastLineIndex, lines[lines.length - 1] + afterCursor);

            // 更新光标位置
            setCursorLine(lastLineIndex);
            setCursorColumn(lines[lines.length - 1].length());
        }
        reparseAndStyle();
    }

    /**
     * Delete selection text if it is not null.
     * <br>
     * Delete the character before the cursor if there is no selection.
     */
    public void deleteText() {
        if (isSelectionValid()) {
            deleteSelection();
        }  else if (cursor.column() > 0) {
            document.deleteText(cursor.line(), cursor.column() - 1, 1);
            setCursorColumn(cursor.column() - 1);
            reparseAndStyle();
        } else if (cursor.line() > 0) {
            int previousLineLength = document.getLine(cursor.line() - 1).length();
            String currentLineText = document.getLine(cursor.line());
            document.deleteLine(cursor.line());
            setCursorLine(cursor.line() - 1);
            setCursorColumn(previousLineLength);
            document.setLine(cursor.line(), document.getLine(cursor.line()) + currentLineText);
            reparseAndStyle();
        }
    }

    public void backspace() {
        if (isSelectionValid()) {
            deleteSelection();
        } else {
            String currentLine = document.getLine(cursor.line());

            if (cursor.column() > 0) {
                // 获取光标前的字符
                int col = cursor.column();
                String beforeCursor = currentLine.substring(0, col);

                // 检查是否位于缩进区域
                if (isInIndentation(beforeCursor)) {
                    int indentSize = getIndentSize(beforeCursor);
                    int newCol = Math.max(0, col - indentSize);
                    document.setLine(cursor.line(), beforeCursor.substring(0, newCol) + currentLine.substring(col));
                    setCursorColumn(newCol);
                } else {
                    // 正常删除一个字符
                    document.setLine(cursor.line(), beforeCursor.substring(0, col - 1) + currentLine.substring(col));
                    setCursorColumn(col - 1);
                }
            } else if (cursor.line() > 0) {
                // 光标在行首，需将当前行合并到上一行
                String previousLine = document.getLine(cursor.line() - 1);
                String currentLineText = document.getLine(cursor.line());
                document.setLine(cursor.line() - 1, previousLine + currentLineText);
                document.deleteLine(cursor.line());
                setCursorLine(cursor.line() - 1);
                setCursorColumn(previousLine.length());
            }

            reparseAndStyle();
        }
    }

    // 检查光标前的文本是否全部是空白字符
    private boolean isInIndentation(String beforeCursor) {
        for (char c : beforeCursor.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }

    // 获取缩进单位的大小（字符数）
    private int getIndentSize(String beforeCursor) {
        int indentUnitLength = indentString.length();
        int length = beforeCursor.length();
        int remainder = length % indentUnitLength;
        if (remainder == 0) {
            return indentUnitLength;
        } else {
            return remainder;
        }
    }

    public void deleteForwardText() {
        if (isSelectionValid()) {
            deleteSelection();
        }  else if (cursor.column() < document.getLine(cursor.line()).length()) {
            document.deleteText(cursor.line(), cursor.column(), 1);
            reparseAndStyle();
        } else if (cursor.line() < document.getLineCount() - 1) {
            String currentLineText = document.getLine(cursor.line());
            document.deleteLine(cursor.line());
            document.setLine(cursor.line(), currentLineText + document.getLine(cursor.line()));
            reparseAndStyle();
        }
    }

    /**
     * Enter to a new line.
     */
    public void enter() {
        String currentLine = document.getLine(cursor.line());
        String beforeCursor = currentLine.substring(0, cursor.column());
        String afterCursor = currentLine.substring(cursor.column());

        // 获取当前行的缩进
        String currentIndent = getIndentation(beforeCursor);

        // 处理自动缩进逻辑
        String additionalIndent = "";

        // 简单示例：如果当前行以 '{' 结尾，增加一级缩进
        if (shouldIncreaseIndentation(beforeCursor.trim())) {
            // 根据其他条件判断是否增加缩进
            additionalIndent = indentString;
        }

        // 设置当前行文本
        document.setLine(cursor.line(), beforeCursor);

        // 插入新行
        String newLineIndent = currentIndent + additionalIndent;
        document.insertLine(cursor.line() + 1, newLineIndent + afterCursor);

        // 更新光标位置
        setCursorLine(cursor.line() + 1);
        setCursorColumn(newLineIndent.length());

        reparseAndStyle();
    }

    // 获取给定文本的缩进（即前导空白字符）
    private String getIndentation(String text) {
        int i = 0;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return text.substring(0, i);
    }

    // 判断是否应当增加缩进
    private boolean shouldIncreaseIndentation(String trimmedLine) {
        // 可以根据编程语言的语法规则来实现
        // 例如，如果行以特定关键字结尾，则增加缩进
        return syntaxParser.getLanguageDefinition().shouldIncreaseIndentation(trimmedLine); // 针对 Python 的示例
    }

    public void moveCursorUp() {
        if (cursor.line() > 0) {
            setCursorLine(cursor.line() - 1);
            int lineLength = document.getLine(cursor.line()).length();
            setCursorColumn(Math.min(cursor.column(), lineLength));
        }
    }

    public void moveCursorDown() {
        if (cursor.line() < document.getLineCount() - 1) {
            setCursorLine(cursor.line() + 1);
            int lineLength = document.getLine(cursor.line()).length();
            setCursorColumn(Math.min(cursor.column(), lineLength));
        }
    }

    public void moveCursorLeft() {
        if (cursor.column() > 0) {
            setCursorColumn(cursor.column() - 1);
        } else if (cursor.line() > 0) {
            setCursorLine(cursor.line() - 1);
            setCursorColumn(document.getLine(cursor.line()).length());
        }
    }

    public void moveCursorRight() {
        if (cursor.column() < document.getLine(cursor.line()).length()) {
            setCursorColumn(cursor.column() + 1);
        } else if (cursor.line() < document.getLineCount() - 1) {
            setCursorLine(cursor.line() + 1);
            setCursorColumn(0);
        }
    }

    public void moveCursorStart() {
        setCursor(0, 0);
    }

    public void moveCursorEnd() {
        setCursor(document.getLineCount() - 1, document.getLine(document.getLineCount() - 1).length());
    }

    // 重新解析和更新样式
    private void reparseAndStyle() {
        visibleLinesCache = null;
        styledLines.clear();
        for (int i = 0; i < document.getLineCount(); i++) {
            String lineText = document.getLine(i);
            List<Token> tokens = syntaxParser.parseLine(lineText);
            var styledLine = new StyledLine(i, applyStyles(tokens));
            styledLines.add(styledLine);
        }
        foldingManager.updateFoldingRegions(document);
    }

    private List<StyledText> applyStyles(List<Token> tokens) {
        List<StyledText> styledTexts = new ArrayList<>();
        for (Token token : tokens) {
            var style = styleManager.getStyleForTokenType(token.getType());
            styledTexts.add(new StyledText(token.getText(), style));
        }
        return styledTexts;
    }

    public List<StyledLine> getVisibleStyledLines() {
        if (visibleLinesCache == null) {
            visibleLinesCache = new ArrayList<>();
            for (int i = 0; i < styledLines.size(); i++) {
                if (foldingManager.isLineVisible(i)) {
                    visibleLinesCache.add(styledLines.get(i));
                }
            }
        }
        return visibleLinesCache;
    }

    // 开始选区
    public void startSelection() {
        selection = new Selection(cursor, cursor);
        selection.setSelecting(true);
    }

    public void endSelection() {
        if (selection != null) {
            selection.setSelecting(false);
        }
    }

    public boolean isSelecting() {
        return selection != null && selection.isSelecting();
    }

    // 更新选区
    public void updateSelection() {
        if (selection != null && selection.isSelecting()) {
            selection.updateEnd(cursor);
        }
    }

    public boolean isSelectionValid() {
        if (selection == null || !selection.hasSelection()) {
            return false;
        }
        return isCursorValid(selection.getStart()) && isCursorValid(selection.getEnd());
    }

    // 取消选区
    public void clearSelection() {
        selection = null;
    }

    public void selectAll() {
        selection = new Selection(new Cursor(0, 0), new Cursor(document.getLineCount() - 1, document.getLine(document.getLineCount() - 1).length()));
    }

    // 删除选中内容
    public void deleteSelection() {
        if (isSelectionValid()) {
            int[] range = selection.getSelectionRange();
            deleteRangeInternal(range[0], range[1], range[2], range[3]);
            // 将光标移动到选区的起始位置
            setCursorLine(range[0]);
            setCursorColumn(range[1]);
            clearSelection();
            reparseAndStyle();
        }
    }

    // 删除指定范围内的文本
    private void deleteRangeInternal(int startLine, int startColumn, int endLine, int endColumn) {
        if (startLine == endLine) {
            String lineText = document.getLine(startLine);
            String newLine = lineText.substring(0, startColumn) + lineText.substring(endColumn);
            document.setLine(startLine, newLine);
        } else {
            // 删除起始行到结束行之间的文本
            String startLineText = document.getLine(startLine);
            String endLineText = document.getLine(endLine);
            String newStartLine = startLineText.substring(0, startColumn) + endLineText.substring(endColumn);
            document.setLine(startLine, newStartLine);

            // 删除中间的行
            for (int i = endLine; i > startLine; i--) {
                document.deleteLine(i);
            }
        }
    }

    // 复制选中内容
    public String copySelection() {
        if (isSelectionValid()) {
            int[] range = selection.getSelectionRange();
            return getTextInRangeInternal(range[0], range[1], range[2], range[3]);
        }
        return "";
    }

    // 剪切选中内容
    public String cutSelection() {
        String copiedText = copySelection();
        deleteSelection();
        return copiedText;
    }

    // 粘贴文本
    public void pasteText(String text) {
        if (isSelectionValid()) {
            deleteSelection();
        }
        insertText(text);
    }

    // 获取指定范围内的文本
    private String getTextInRangeInternal(int startLine, int startColumn, int endLine, int endColumn) {
        StringBuilder sb = new StringBuilder();
        if (startLine == endLine) {
            String lineText = document.getLine(startLine);
            sb.append(lineText.substring(startColumn, endColumn));
        } else {
            String lineText = document.getLine(startLine);
            sb.append(lineText.substring(startColumn));
            sb.append('\n');
            for (int i = startLine + 1; i < endLine; i++) {
                sb.append(document.getLine(i));
                sb.append('\n');
            }
            lineText = document.getLine(endLine);
            sb.append(lineText.substring(0, endColumn));
        }
        return sb.toString();
    }

    // 在选区位置插入文本（替换选中内容）
    public void replaceSelection(String text) {
        deleteSelection();
        insertText(text);
    }

    public List<String> getLines() {
        return new ArrayList<>(getDocument().getLines());
    }

    public void setLines(List<String> lines) {
        if (lines.equals(getLines())) return;
        getDocument().getLines().clear();
        for (String text : lines) {
            var splits = text.split("\n", -1);
            for (String split : splits) {
                getDocument().insertLine(getDocument().getLineCount(), split);
            }
        }
        if (getDocument().getLineCount() == 0) {
            getDocument().insertLine(0, "");
        }
        if (!isCursorValid(cursor)) {
            setCursor(0, 0);
        }
        reparseAndStyle();
    }

    private boolean isCursorValid(Cursor cursor) {
        return cursor.line() >= 0 && cursor.line() < document.getLineCount() && cursor.column() >= 0 && cursor.column() <= document.getLine(cursor.line()).length();
    }
}

