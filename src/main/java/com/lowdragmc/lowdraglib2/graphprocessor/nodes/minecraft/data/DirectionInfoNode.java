package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.data;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

@LDLRegister(name = "direction info", group = "graph_processor.node.minecraft.data", registry = "ldlib2:graph_node")
public class DirectionInfoNode extends BaseNode {
    @InputPort
    public Direction in;
    @OutputPort
    public int ordinal = 0;
    @OutputPort
    public Vector3f xyz;
    @OutputPort
    public Direction clockwise;
    @OutputPort
    public Direction opposite;

    @Override
    public void process() {
        if (in != null) {
            ordinal = in.ordinal();
            xyz = new Vector3f(in.getStepX(), in.getStepY(), in.getStepZ());
            if (in.getAxis() != Direction.Axis.Y) {
                clockwise = in.getClockWise();
            }
            opposite = in.getOpposite();
        }
    }

}
