package com.lowdragmc.lowdraglib.gui.widget.codeeditor.language;

import com.lowdragmc.lowdraglib.gui.widget.codeeditor.Document;
import com.lowdragmc.lowdraglib.gui.widget.codeeditor.FoldableRegion;

import java.util.ArrayList;
import java.util.List;

public class FoldingManager {
    private List<FoldableRegion> regions;

    public FoldingManager() {
        regions = new ArrayList<>();
    }

    // 更新折叠区域（简单示例，实际应根据语法树）
    public void updateFoldingRegions(Document document) {
        regions.clear();
        for (int i = 0; i < document.getLineCount(); i++) {
            String line = document.getLine(i).trim();
            if (line.endsWith("{")) {
                int startLine = i;
                int endLine = findMatchingBrace(document, i);
                if (endLine > startLine) {
                    regions.add(new FoldableRegion(startLine, endLine));
                    i = endLine; // 跳过已处理的区域
                }
            }
        }
    }

    // 查找匹配的右括号
    private int findMatchingBrace(Document document, int startLine) {
        int braceCount = 0;
        for (int i = startLine; i < document.getLineCount(); i++) {
            String line = document.getLine(i);
            for (char c : line.toCharArray()) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        return i;
                    }
                }
            }
        }
        return startLine; // 未找到匹配，返回起始行
    }

    // 切换折叠状态
    public void toggleFold(int line) {
        for (FoldableRegion region : regions) {
            if (region.getStartLine() == line) {
                region.toggle();
                break;
            }
        }
    }

    // 判断行是否可见
    public boolean isLineVisible(int line) {
        for (FoldableRegion region : regions) {
            if (region.isCollapsed() && line > region.getStartLine() && line <= region.getEndLine()) {
                return false;
            }
        }
        return true;
    }

    public List<FoldableRegion> getRegions() {
        return regions;
    }
}
