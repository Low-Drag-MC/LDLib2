package com.lowdragmc.lowdraglib.gui.widget.codeeditor;

public class FoldableRegion {
    private int startLine;
    private int endLine;
    private boolean isCollapsed;

    public FoldableRegion(int startLine, int endLine) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.isCollapsed = false;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public boolean isCollapsed() {
        return isCollapsed;
    }

    public void toggle() {
        isCollapsed = !isCollapsed;
    }
}



