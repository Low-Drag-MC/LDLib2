package com.lowdragmc.lowdraglib2.graphprocessor.nodes.utils;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.WrapperConfigurator;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.trigger.LinearTriggerNode;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.widget.ImageWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;

@LDLRegister(name = "console log", group = "graph_processor.node.utils", registry = "ldlib2:graph_node")
public class ConsoleLogNode extends LinearTriggerNode {
    public static final Logger LOGGER = LoggerFactory.getLogger("NodeGraph");

    @InputPort
    public Object in;

    @Override
    public int getMinWidth() {
        return 150;
    }

    @Override
    protected void process() {
        super.process();
        LOGGER.info("Node Graph Process Log: " + (in == null ? "null" : PrintNode.format(in)));
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        var numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(2);
        father.addConfigurators(new WrapperConfigurator("", new ImageWidget(0, 0, 140, 15,
                new TextTexture("").setWidth(140).setType(TextTexture.TextType.ROLL_ALWAYS)
                        .setSupplier(() -> in == null ? "null" : PrintNode.format(in))))
                .setRemoveTitleBar(true));
    }
}
