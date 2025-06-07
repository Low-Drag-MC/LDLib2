package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.entity;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.trigger.LinearTriggerNode;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

@LDLRegister(name = "entity action", group = "graph_processor.node.minecraft.entity", registry = "ldlib2:graph_node")
public class EntityActionNode extends LinearTriggerNode {
    public enum Action {
        KILL,
        MOVE,
        FIRE;
    }

    @InputPort
    public Entity entity;

    @InputPort
    public Vector3f xyz;

    @Configurable
    public Action action = Action.KILL;

    @Override
    public void process() {
        if (entity != null && action != null) {
            switch (action) {
                case KILL -> entity.kill();
                case FIRE -> entity.igniteForSeconds(5);
                case MOVE -> {
                    if (xyz != null) {
                        entity.teleportTo(xyz.x, xyz.y, xyz.z);
                    }
                }
            }
        }
    }
}
