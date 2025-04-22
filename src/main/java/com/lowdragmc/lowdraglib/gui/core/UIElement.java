package com.lowdragmc.lowdraglib.gui.core;

import org.lwjgl.util.yoga.Yoga;

public abstract class UIElement {
    // 样式属性
    private UIStyle style = new UIStyle();

    // 布局相关
    private Rect computedBounds = Rect.of(0, 0, 0, 0);
    private boolean needsLayout = true;

    // 触发布局计算
    public void requestLayout() {
        if (needsLayout) return;
        needsLayout = true;
        //getParent().ifPresent(UIElement::requestLayout);
        //Yoga.
    }

    // 实际布局入口
    public void performLayout(Rect parentBounds) {
        if (!needsLayout) return;

        // 1. 计算自身尺寸
        computeSize(parentBounds);

        // 2. 选择布局引擎
        //LayoutEngine engine = UIManager.getLayoutEngine(this);
        //engine.layout(this);

        // 3. 标记布局完成
        needsLayout = false;
    }

    protected abstract void computeSize(Rect availableSpace);
}
