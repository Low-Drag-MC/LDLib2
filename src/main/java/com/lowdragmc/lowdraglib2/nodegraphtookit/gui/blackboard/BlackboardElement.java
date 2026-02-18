package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.blackboard;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;


public class BlackboardElement extends ModelElement {
    @Getter @Setter(AccessLevel.PROTECTED)
    private Blackboard blackboard;

    @Override
    protected void onSelectionChanged() {
        if (blackboard != null) {
            blackboard.onSelectionChanged();
        }
    }
}
