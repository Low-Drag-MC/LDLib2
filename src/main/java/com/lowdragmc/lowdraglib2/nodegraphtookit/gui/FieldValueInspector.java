package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldValueConfigurable;
import dev.vfyjxf.taffy.style.FlexDirection;

public class FieldValueInspector extends UIElement {
    public final Label fieldName = new Label();
    public final UIElement fieldConfigurator = new UIElement();

    public FieldValueInspector() {
        getLayout().flexDirection(FlexDirection.ROW).gapAll(2).flexGrow(1);

        fieldName.getLayout().height(14);
        fieldName.getTextStyle().textAlignVertical(Vertical.CENTER);
        fieldName.setText("");
        fieldName.getTextStyle().adaptiveWidth(true);
        fieldConfigurator.getLayout().flexGrow(1).gapAll(2).minWidth(55);

        addChildren(fieldName, fieldConfigurator);
    }

    public void loadValueField(IFieldValueConfigurable valueField) {
        // for constant port
        fieldConfigurator.clearAllChildren();
        var container = new ConfiguratorGroup();
        valueField.buildConfigurator(container);
        if (!container.getConfigurators().isEmpty()) {
            for (Configurator configurator : container.getConfigurators()) {
                fieldConfigurator.addChild(configurator);
            }
        }
        fieldConfigurator.setDisplay(!fieldConfigurator.getChildren().isEmpty());
    }
}
