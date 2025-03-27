package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import net.minecraft.world.level.Level;

@LDLRegister(name = "level info", group = "graph_processor.node.minecraft")
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
