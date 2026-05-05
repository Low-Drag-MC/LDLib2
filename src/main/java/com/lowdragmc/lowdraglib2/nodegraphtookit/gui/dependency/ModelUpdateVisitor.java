package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHintList;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode(callSuper = false)
public class ModelUpdateVisitor extends ElementUpdateVisitor {
    public static final ModelUpdateVisitor UNSPECIFIED = new ModelUpdateVisitor(ChangeHintList.UNSPECIFIED);
    public static final ModelUpdateVisitor LAYOUT = new ModelUpdateVisitor(ChangeHintList.LAYOUT);
    private @Nullable ChangeHintList changeHints;

    public ModelUpdateVisitor(@Nullable ChangeHintList changeHints) {
        this.changeHints = changeHints;
    }

    public void reset(ChangeHintList hints) {
        changeHints = hints;
    }

    @Override
    public void update(ModelElement view) {
        view.updateUIFromModel(this);
    }

    public boolean hasHint(ChangeHint hint) {
        if (changeHints == null) return false;
        return changeHints.hasChange(hint);
    }

    @Override
    public String toString() {
        return changeHints == null ? "" : changeHints.toString();
    }
}
