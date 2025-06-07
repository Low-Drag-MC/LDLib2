package com.lowdragmc.lowdraglib2.gui.widget.codeeditor;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Selection {
    private Cursor start;
    private Cursor end;
    @Setter
    private boolean isSelecting = false;

    public Selection(Cursor start, Cursor end) {
        this.start = start;
        this.end = end;
    }


    public void updateEnd(Cursor newEnd) {
        this.end = newEnd;
    }

    public boolean hasSelection() {
        return (start != null && end != null) && !start.equals(end);
    }

    public int[] getSelectionRange() {
        int startLine = Math.min(start.line(), end.line());
        int endLine = Math.max(start.line(), end.line());
        int startColumn = start.line() == startLine ? start.column() : end.column();
        int endColumn = end.line() == endLine ? end.column() : start.column();
        if (startLine == endLine) {
            startColumn = Math.min(start.column(), end.column());
            endColumn = Math.max(start.column(), end.column());
        }
        return new int[]{startLine, startColumn, endLine, endColumn};
    }

    public void clear() {
        this.start = new Cursor(end.line(), end.column());
    }

}
