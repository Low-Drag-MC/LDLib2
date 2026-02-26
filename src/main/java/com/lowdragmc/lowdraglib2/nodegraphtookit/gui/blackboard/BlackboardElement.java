package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.blackboard;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector4f;


public class BlackboardElement extends ModelElement {
    @Getter @Setter(AccessLevel.PROTECTED)
    private Blackboard blackboard;

    @Override
    protected void onSelectionChanged() {
        if (blackboard != null) {
            blackboard.onSelectionChanged();
        }
    }

    @Override
    public boolean canBeRegionSelected(Vector4f region) {
        return false;
    }
}
