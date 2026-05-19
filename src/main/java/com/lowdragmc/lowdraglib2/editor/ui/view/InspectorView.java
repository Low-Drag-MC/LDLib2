package com.lowdragmc.lowdraglib2.editor.ui.view;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Inspector;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class InspectorView extends View {
    public final Inspector inspector;
    public final Editor editor;

    public InspectorView(Editor editor) {
        super("editor.view.inspector", Icons.SETTINGS);
        this.editor = editor;
        this.inspector = new Inspector();
        this.inspector.layout(layout -> {
            layout.heightPercent(100);
            layout.widthPercent(100);
        });
        this.inspector.setHistoryStack(editor.getHistoryView());
        addChild(inspector);
    }

    public void clear() {
        inspector.clear();
    }

    public ConfiguratorGroup inspect(IConfigurable configurable) {
        return inspect(configurable, null);
    }

    public ConfiguratorGroup inspect(IConfigurable configurable, @Nullable Consumer<Configurator> listener) {
        return inspect(configurable, listener, null);
    }

    public ConfiguratorGroup inspect(IConfigurable configurable, @Nullable Consumer<Configurator> listener, @Nullable Runnable onClose) {
        return inspect(configurable, listener, onClose, null);
    }

    public <T extends IConfigurable> ConfiguratorGroup inspect(T configurable, @Nullable Consumer<Configurator> listener, @Nullable Runnable onClose, @Nullable Runnable historyAction) {
        return inspector.inspect(configurable, listener, onClose, historyAction);
    }

}
