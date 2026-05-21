package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.blackboard;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
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
        addClass("__blackboard-group__");
        Style.defaultPipeline(getLayout(), l -> l.flex(1).flexDirection(FlexDirection.ROW).gapAll(2).height(10));

        icon = new UIElement().addClass("__blackboard-group_icon__");
        Style.defaultPipeline(icon.getLayout(), l -> l.setAspectRatio(1).heightPercent(100));
        Style.defaultPipeline(icon.getStyle(), s -> s.backgroundTexture(Icons.FOLDER));

        label = new Label();
        label.addClass("__blackboard-group_label__");
        Style.defaultPipeline(label.getTextStyle(), s -> s.textWrap(TextWrap.HOVER_ROLL).textAlignVertical(Vertical.CENTER));
        label.setText(groupModel.getName());
        Style.defaultPipeline(label.getLayout(), l -> l.heightPercent(100).flex(1));
        Style.defaultPipeline(label.getStyle(), s -> s.overflowVisible(false));
    }

    @Override
    protected void buildUI() {
        addChildren(icon, label);
    }

    @Override
    public GroupModelBase getModel() {
        return (GroupModelBase) super.getModel();
    }
}
