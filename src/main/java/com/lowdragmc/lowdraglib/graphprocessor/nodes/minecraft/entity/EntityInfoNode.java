package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft.entity;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

@LDLRegister(name = "entity info", group = "graph_processor.node.minecraft.entity", registry = "ldlib:graph_node")
public class EntityInfoNode extends BaseNode {
    @InputPort
    public Object in;
    @OutputPort(name = "entity type")
    public EntityType entityType;
    @OutputPort(name = "is alive")
    public boolean isAlive;
    @OutputPort
    public Vector3f xyz;
    @OutputPort
    public Level level;
    @OutputPort
    public CompoundTag tag;

    @Override
    public void process() {
        if (in instanceof Entity entity) {
            entityType = entity.getType();
            isAlive = entity.isAlive();
            xyz = new Vector3f((float) entity.getX(), (float) entity.getY(), (float) entity.getZ());
            level = entity.level();
            tag = entity.saveWithoutId(new CompoundTag());
        }
    }
}
