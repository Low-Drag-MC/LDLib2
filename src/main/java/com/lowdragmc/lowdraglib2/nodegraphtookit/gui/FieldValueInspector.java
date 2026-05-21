package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.utils.IHistoryStack;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldValueConfigurable;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
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
        addClass("__field-value-inspector__");
        Style.defaultPipeline(getLayout(), l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)
                .gapAll(2).flexGrow(1));

        fieldName.addClass("__field-value-inspector_field-name__");
        Style.defaultPipeline(fieldName.getLayout(), l -> l.height(14));
        Style.defaultPipeline(fieldName.getTextStyle(), s -> s.textAlignVertical(Vertical.CENTER).adaptiveWidth(true));
        fieldName.setText("");
        // fieldName visibility is data-driven (empty name → hidden) — pin via IMPORTANT.
        Style.importantPipeline(fieldName.getLayout(), l -> l.display(TaffyDisplay.NONE));

        fieldConfigurator.addClass("__field-value-inspector_configurator__");
        Style.defaultPipeline(fieldConfigurator.getLayout(), l -> l.flexGrow(1).gapAll(2).minWidth(55));

        addChildren(fieldName, fieldConfigurator);
    }

    public void setFieldName(Component name) {
        fieldName.setText(name);
        var empty = Component.empty().equals(name);
        Style.importantPipeline(fieldName.getLayout(), l -> l.display(empty ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
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
        // Hide the configurator area when no child configurators were produced — data-driven.
        var empty = fieldConfigurator.getChildren().isEmpty();
        Style.importantPipeline(fieldConfigurator.getLayout(), l -> l.display(empty ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
    }
}
