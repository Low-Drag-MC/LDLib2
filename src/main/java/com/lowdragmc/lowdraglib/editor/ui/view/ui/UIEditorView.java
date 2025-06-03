package com.lowdragmc.lowdraglib.editor.ui.view.ui;

import com.lowdragmc.lowdraglib.editor.ui.View;
import com.lowdragmc.lowdraglib.editor.ui.util.SplitView;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UI;
import com.lowdragmc.lowdraglib.gui.ui.elements.ScrollerView;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;

public class UIEditorView extends View {
    public final UI ui;
    public final UIHierarchyList hierarchyList;
    public final ScrollerView scrollerView = new ScrollerView();

    public UIEditorView(UI ui) {
        super("editor.ui.editor");
        this.ui = ui;
        this.hierarchyList = new UIHierarchyList(this);
        getLayout().setWidthPercent(100);
        getLayout().setHeightPercent(100);
        getLayout().setFlexDirection(YogaFlexDirection.ROW);

        hierarchyList.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setWidthPercent(100);
        });

        scrollerView.viewPort.layout(layout -> layout.setPadding(YogaEdge.ALL, 0))
                .style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        scrollerView.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setWidthPercent(100);
        });
        scrollerView.addScrollViewChild(ui.getRootElement());
        addChildren(new SplitView.Horizontal().setPercentage(20).left(hierarchyList).right(scrollerView));
    }

}
