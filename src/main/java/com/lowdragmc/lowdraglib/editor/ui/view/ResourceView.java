package com.lowdragmc.lowdraglib.editor.ui.view;

import com.lowdragmc.lowdraglib.editor.ui.View;

public class ResourceView extends View {

    public ResourceView() {
        super("editor.resources");
        this.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
        });
    }

}
