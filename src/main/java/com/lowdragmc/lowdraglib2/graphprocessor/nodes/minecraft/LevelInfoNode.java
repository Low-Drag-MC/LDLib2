package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import net.minecraft.world.level.Level;

@LDLRegister(name = "level info", group = "graph_processor.node.minecraft", registry = "ldlib2:graph_node")
public class LevelInfoNode extends BaseNode {
    @InputPort
    public Level level;
    @OutputPort
    public int height;
    @OutputPort(name = "day time")
    public int dayTime;
    @OutputPort(name = "rain level")
    public float rainLevel;
    @OutputPort(name = "thunder level")
    public float thunderLevel;
    @OutputPort(name = "is day")
    public boolean isDay;

    @Override
    public void process() {
        if (level != null) {
            rainLevel = level.getRainLevel(0);
            thunderLevel = level.getThunderLevel(0);
            height = level.getHeight();
            dayTime = (int) level.getDayTime();
            isDay = level.isDay();
        }
    }
}
