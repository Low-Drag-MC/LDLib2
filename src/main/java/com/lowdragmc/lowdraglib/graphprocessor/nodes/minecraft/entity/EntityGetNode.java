package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft.entity;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

@LDLRegister(name = "get entities", group = "graph_processor.node.minecraft.entity", registry = "ldlib:graph_node")
public class EntityGetNode extends BaseNode {
    @InputPort
    public Level level;
    @InputPort
    public Vector3f from;
    @InputPort
    public Vector3f to;
    @InputPort(name = "entity type", tips = "The type of entity to get. If null, all entities will be returned.")
    public EntityType entityType;
    @OutputPort
    public List<Entity> entities = new ArrayList<>();

    @Override
    public void process() {
        if (entities == null) {
            entities = new ArrayList<>();
        }
        entities.clear();
        if (level != null && from != null && to != null) {
            var area = new AABB(from.x, from.y, from.z, to.x, to.y, to.z);
            entities.addAll(level.getEntities((Entity) null, area, e -> e.getType() == entityType));
        }
    }
}
