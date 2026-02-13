package com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public class GhostWireModel extends WireModel implements IGhostWireModel {
    @Getter @Setter
    private Vector2f fromWorldPoint = new Vector2f();
    @Getter @Setter
    private Vector2f toWorldPoint = new Vector2f();

    @Getter @Setter
    private PortModel fromPort;
    @Getter @Setter
    private PortModel toPort;


    @Override
    public void setPorts(PortModel toPortModel, PortModel fromPortModel) {
        setFromPort(fromPortModel);
        setToPort(toPortModel);
    }

    @Override
    public void setPort(WireSide side, @Nullable PortModel value) {
        if (side == WireSide.FROM) setFromPort(value);
        else setToPort(value);
    }

    @Override
    public Pair<AddMissingPortResult, AddMissingPortResult> addMissingPorts() {
        return Pair.of(new AddMissingPortResult(PortMigrationResult.NONE, null),
                new AddMissingPortResult(PortMigrationResult.NONE, null));
    }
}
