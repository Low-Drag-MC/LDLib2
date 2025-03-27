package com.lowdragmc.lowdraglib.compass;

import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.math.Position;
import lombok.Getter;

/**
 * @author KilaBash
 * @date 2022/9/3
 * @implNote LayoutWidget
 */
public class LayoutPageWidget extends DraggableScrollableWidgetGroup {

    @Getter
    protected int pageWidth;
    protected int offset = 0;

    public LayoutPageWidget(int width, int height) {
        super(4, 4, width - 8, height - 4);
        setClientSideWidget();
        this.pageWidth = width - 8;
    }

    public LayoutPageWidget addOffsetSpace(int offset) {
        return addStreamWidget(new Widget(0, 0, pageWidth, offset));
    }

    public LayoutPageWidget addStreamWidget(Widget widget) {
        widget.setSelfPosition(Position.of((pageWidth - widget.getSize().width) / 2, offset));
        addWidget(widget);
        offset += widget.getSize().height;
        return this;
    }

}
