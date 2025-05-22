package com.lowdragmc.lowdraglib.graphprocessor.nodes.value;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor_outdated.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.editor_outdated.configurator.WrapperConfigurator;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.graphprocessor.nodes.utils.PrintNode;
import com.lowdragmc.lowdraglib.gui.widget.codeeditor.CodeEditorWidget;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

@LDLRegister(name = "string", group = "graph_processor.node.value", registry = "ldlib:graph_node")
public class StringNode extends BaseNode {
    @InputPort
    public Object in;
    @OutputPort
    public String out;

    @Getter
    @Persisted
    private String internalValue;

    @Override
    public void process() {
        if (in == null) {
            out = Objects.requireNonNullElse(internalValue, "");
        } else {
            out = PrintNode.format(in);
        }
    }

    @Override
    public int getMinWidth() {
        return 150;
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        for (var port : getInputPorts()) {
            if (port.fieldName.equals("in")) {
                if (!port.getEdges().isEmpty()) return;
            }
        }
        var codeEditor = new CodeEditorWidget(0, 0, getMinWidth(), 100);
        codeEditor.setLines(List.of(internalValue == null ? "" : internalValue));
        codeEditor.setOnTextChanged(lines -> internalValue = String.join("\n", lines));
        father.addConfigurators(new WrapperConfigurator(codeEditor));
    }
}
