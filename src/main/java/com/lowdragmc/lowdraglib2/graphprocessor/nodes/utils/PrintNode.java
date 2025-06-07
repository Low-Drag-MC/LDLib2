package com.lowdragmc.lowdraglib2.graphprocessor.nodes.utils;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.WrapperConfigurator;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.widget.ImageWidget;
import org.joml.Vector3f;

import java.text.NumberFormat;

@LDLRegister(name = "print", group = "graph_processor.node.utils", registry = "ldlib2:graph_node")
public class PrintNode extends BaseNode {
    private static NumberFormat numberFormat = NumberFormat.getInstance();
    static {
        numberFormat.setMaximumFractionDigits(6);
        numberFormat.setMinimumFractionDigits(0);
    }

    @InputPort
    public Object in;

    @Override
    public int getMinWidth() {
        return 150;
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        var numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(2);
        father.addConfigurators(new WrapperConfigurator("", new ImageWidget(0, 0, 140, 15,
                new TextTexture("").setWidth(140).setType(TextTexture.TextType.ROLL_ALWAYS)
                        .setSupplier(() -> in == null ? "null" : format(in))))
                .setRemoveTitleBar(true));
    }

    public static String format(Object obj) {
        if (obj instanceof Number) {
            return numberFormat.format(obj);
        }
        if (obj instanceof Vector3f vec) {
            return vec.toString(numberFormat);
        }
        return obj.toString();
    }
}
