package com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.value;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import org.joml.Vector3f;

@LDLRegister(name = "xyz", group = "graph_processor.node.value")
public class Vector3Node extends BaseNode {
    @InputPort(name = "xyz")
    public Object in;
    @OutputPort(name = "xyz")
    public Vector3f out;
    @InputPort(name = "x")
    public Float inX;
    @InputPort(name = "y")
    public Float inY;
    @InputPort(name = "z")
    public Float inZ;
    @OutputPort(name = "x")
    public float outX;
    @OutputPort(name = "y")
    public float outY;
    @OutputPort(name = "z")
    public float outZ;

    @Configurable(showName = false)
    @NumberRange(range = {-Float.MAX_VALUE, Float.MAX_VALUE}, wheel = 1f)
    public Vector3f internalValue = new Vector3f();

    @Override
    public void process() {
        if (in == null) {
            out = internalValue;
        } else if (in instanceof Vector3f vector3f) {
            out = vector3f;
        } else {
            out = new Vector3f();
        }
        out = new Vector3f(
                inX == null ? out.x() : inX,
                inY == null ? out.y() : inY,
                inZ == null ? out.z() : inZ);
        outX = out.x();
        outY = out.y();
        outZ = out.z();
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
        super.buildConfigurator(father);
    }
}
