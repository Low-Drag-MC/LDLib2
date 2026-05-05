package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.utils.IHistoryStack;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldValueConfigurable;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class FieldValueInspector extends UIElement {
    public final Label fieldName = new Label();
    public final UIElement fieldConfigurator = new UIElement();
    @Nullable @Setter @Getter
    private IHistoryStack historyStack;

    public FieldValueInspector() {
        getLayout().flexDirection(FlexDirection.ROW).gapAll(2).flexGrow(1);

        fieldName.getLayout().height(14);
        fieldName.getTextStyle().textAlignVertical(Vertical.CENTER);
        fieldName.setText("");
        fieldName.setDisplay(false);
        fieldName.getTextStyle().adaptiveWidth(true);
        fieldConfigurator.getLayout().flexGrow(1).gapAll(2).minWidth(55);

        addChildren(fieldName, fieldConfigurator);
    }

    public void setFieldName(Component name) {
        fieldName.setText(name);
        fieldName.setDisplay(!Component.empty().equals(name));
    }

    public void loadValueField(IFieldValueConfigurable valueField) {
        // for constant port
        fieldConfigurator.clearAllChildren();
        var container = new ConfiguratorGroup();
        valueField.buildConfigurator(container);
        if (!container.getConfigurators().isEmpty()) {
            for (Configurator configurator : container.getConfigurators()) {
                fieldConfigurator.addChild(configurator);
                // record value changes into history so the editor's save/dirty state stays in sync
//                if (historyStack != null && valueField instanceof INBTSerializable<?> serializable) {
//                    configurator.addEventListener(Configurator.CHANGE_EVENT, e -> {
//                        if (e.target instanceof Configurator c) {
//                            var notifyName = c.getNotifyName();
//                            historyStack.recordSerializableObject(
//                                    notifyName.getString().isEmpty() ?
//                                            Component.literal(valueField.getConfigurableName()) : notifyName,
//                                    serializable, c);
//                        }
//                    });
//                }
            }
        }
        fieldConfigurator.setDisplay(!fieldConfigurator.getChildren().isEmpty());
    }
}
