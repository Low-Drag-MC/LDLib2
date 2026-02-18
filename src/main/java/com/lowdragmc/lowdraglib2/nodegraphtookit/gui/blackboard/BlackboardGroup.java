package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.blackboard;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.GroupModelBase;
import dev.vfyjxf.taffy.style.FlexDirection;

public class BlackboardGroup extends BlackboardElement {
    public final UIElement icon;
    public final Label label;

    public BlackboardGroup(GroupModelBase groupModel) {
        setModel(groupModel);
        getLayout().flex(1).flexDirection(FlexDirection.ROW).gapAll(2).height(10);
        icon = new UIElement().layout(layout -> {
            layout.setAspectRatio(1);
            layout.heightPercent(100);
        }).style(style -> style.backgroundTexture(Icons.FOLDER));
        label = new Label();
        label.textStyle(style -> style.textWrap(TextWrap.HOVER_ROLL).textAlignVertical(Vertical.CENTER))
                .setText(groupModel.getName()).layout(layout -> {
                    layout.heightPercent(100);
                    layout.flex(1);
                }).setOverflowVisible(false);
    }

    @Override
    protected void buildUI() {
        addChildren(icon, label);
        internalSetup();
    }

    @Override
    public GroupModelBase getModel() {
        return (GroupModelBase) super.getModel();
    }
}
