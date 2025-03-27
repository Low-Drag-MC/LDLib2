package com.lowdragmc.lowdraglib.editor.ui.tool;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.editor.Icons;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.utils.AnnotationDetector;
import com.lowdragmc.lowdraglib.editor.ui.ToolPanel;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.WidgetTexture;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectableWidgetGroup;
import com.lowdragmc.lowdraglib.math.Position;
import com.lowdragmc.lowdraglib.math.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/10
 * @implNote WidgetToolBox
 */
public class WidgetToolBox extends DraggableScrollableWidgetGroup {
    public static class Default {
        public static List<Default> TABS = new ArrayList<>();
        public static final Default BASIC = registerTab("widget.basic", Icons.WIDGET_BASIC);
        public static final Default GROUP = registerTab("widget.group", Icons.WIDGET_GROUP);
        public static final Default CONTAINER = registerTab("widget.container", Icons.WIDGET_CONTAINER);
        public static final Default CUSTOM = registerTab("widget.custom", Icons.WIDGET_CUSTOM);

        public final String groupName;
        public final ResourceTexture icon;

        private Default(String groupName, ResourceTexture icon) {
            this.groupName = groupName;
            this.icon = icon;
            TABS.add(this);
        }

        @Deprecated
        public WidgetToolBox createToolBox() {
            return new WidgetToolBox(groupName, Size.of(ToolPanel.WIDTH, 100));
        }

        public WidgetToolBox createToolBox(Size size) {
            return new WidgetToolBox(groupName, size);
        }

        public static Default registerTab(String groupName, ResourceTexture icon) {
            return new Default(groupName, icon);
        }
    }

    public WidgetToolBox(String groupName, Size size) {
        super(0, 0, size.width, size.height);
        int yOffset = 3;
        setYScrollBarWidth(4).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(2).transform(-0.5f, 0));
        for (AnnotationDetector.Wrapper<LDLRegister, IConfigurableWidget> wrapper : AnnotationDetector.REGISTER_WIDGETS) {
            String group = wrapper.annotation().group().isEmpty() ? "widget.basic" : wrapper.annotation().group();
            if (group.equals(groupName)) {
                var widget = wrapper.creator().get();
                widget.initTemplate();
                widget.widget().setSelfPosition(Position.of(0, 0));
                SelectableWidgetGroup selectableWidgetGroup = new SelectableWidgetGroup(0, yOffset, size.width - 2, 50 + 14);
                selectableWidgetGroup.addWidget(new ImageWidget((size.width - 2 - 45) / 2, 17, 45, 30, new WidgetTexture(widget.widget())));
                selectableWidgetGroup.addWidget(new LabelWidget(3, 3, widget.getTranslateKey()));
                selectableWidgetGroup.setSelectedTexture(ColorPattern.T_GRAY.rectTexture());
                selectableWidgetGroup.setDraggingProvider(() -> {
                    final IConfigurableWidget configurableWidget = wrapper.creator().get();
                    configurableWidget.initTemplate();
                    return (IWidgetPanelDragging) () -> configurableWidget;
                }, (w, p) -> new WidgetTexture(w.get().widget()).setDragging(true));
                addWidget(selectableWidgetGroup);
                yOffset += 50 + 14 + 3;
            }
        }
    }

    public interface IWidgetPanelDragging extends Supplier<IConfigurableWidget> {
    }
}
