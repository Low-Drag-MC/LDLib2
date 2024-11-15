package com.lowdragmc.lowdraglib.gui.widget.codeeditor;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class Document {
    @Getter
    private final List<String> lines = new ArrayList<>();

    public Document() {
        lines.add(""); // 初始添加一行空行
    }

    public void insertText(int line, int column, String text) {
        String currentLine = lines.get(line);
        String newLine = currentLine.substring(0, column) + text + currentLine.substring(column);
        lines.set(line, newLine);
    }

    public void deleteText(int line, int column, int length) {
        String currentLine = lines.get(line);
        if (column + length > currentLine.length()) {
            length = currentLine.length() - column;
        }
        String newLine = currentLine.substring(0, column) + currentLine.substring(column + length);
        lines.set(line, newLine);
    }

    public String getLine(int line) {
        return lines.get(line);
    }

    public int getLineCount() {
        return lines.size();
    }

    public void insertLine(int index, String text) {
        lines.add(index, text);
    }

    public void deleteLine(int index) {
        if (lines.size() > 1) {
            lines.remove(index);
        } else {
            lines.set(0, "");
        }
    }

    public void setLine(int index, String text) {
        lines.set(index, text);
    }
}

