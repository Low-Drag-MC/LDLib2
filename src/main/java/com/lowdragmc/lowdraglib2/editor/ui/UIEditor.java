package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.editor.resource.*;
import org.appliedenergistics.yoga.YogaDisplay;

public class UIEditor extends Editor {

    public UIEditor() {
        this.leftWindow.setDisplay(YogaDisplay.NONE);
        this.leftWindow.getParentWindow().splitStyle(style -> style.percentage(0).minPercentage(0f).maxPercentage(0));
        this.resourceView.loadResources(Resources.of(
                UIResource.INSTANCE,
                ColorsResource.INSTANCE,
                TexturesResource.INSTANCE
        ));
    }

}
