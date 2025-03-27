package com.lowdragmc.lowdraglib.editor.configurator;

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

/**
 * @author KilaBash
 * @date 2022/12/6
 * @implNote IConfigurableWidgetGroup
 */
public interface IConfigurableWidgetGroup extends IConfigurableWidget {

    @Override
    default WidgetGroup widget() {
        return (WidgetGroup) this;
    }

    /**
     * Whether widget given be accepted via dragging, paste
     */
    default boolean canWidgetAccepted(IConfigurableWidget widget) {
        return false;
    }

    /**
     * Accept given widget
     */
    default void acceptWidget(IConfigurableWidget widget) {

    }

    /**
     * Child dragged out,cut
     */
    default void onWidgetRemoved(IConfigurableWidget widget) {

    }

}
