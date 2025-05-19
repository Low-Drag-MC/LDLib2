package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft.data;

import com.lowdragmc.lowdraglib.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.editor.configurator.StringConfigurator;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.CustomPortBehavior;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.CustomPortOutput;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.graphprocessor.data.NodePort;
import com.lowdragmc.lowdraglib.graphprocessor.data.PortData;
import com.lowdragmc.lowdraglib.graphprocessor.data.PortEdge;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import net.minecraft.nbt.*;

import java.util.Collections;
import java.util.List;

@LDLRegister(name = "compound reader", group = "graph_processor.node.minecraft.data", registry = "ldlib:graph_node")
public class CompoundReaderNode extends BaseNode {
    public enum Type {
        STRING,
        NUMBER,
        BOOL,
        COMPOUND,
        LIST,

    }
    @InputPort
    public CompoundTag tag;
    @InputPort
    public String key;
    @OutputPort
    public Object out;
    @Configurable(showName = false)
    public Type type = Type.STRING;
    @Persisted
    public String internalKey = "";

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        super.buildConfigurator(father);
        for (var port : getInputPorts()) {
            if (port.fieldName.equals("key")) {
                if (port.getEdges().isEmpty()) {
                    var stringConfigurator = new StringConfigurator("key", () -> internalKey, s -> internalKey = s, "", true);
                    father.addConfigurators(stringConfigurator);
                }
            }
        }
    }

    @Override
    public int getMinWidth() {
        return 120;
    }

    @Override
    public void process() {
        if (tag == null) {
            out = null;
            return;
        }
        var realKey = key;
        if (realKey == null) {
            realKey = internalKey;
        }
        if (type == Type.STRING) {
            out = tag.getString(realKey);
        } else if (type == Type.NUMBER) {
            out = tag.getFloat(realKey);
        } else if (type == Type.BOOL) {
            out = tag.getBoolean(realKey);
        } else if (type == Type.COMPOUND) {
            out = tag.getCompound(realKey);
        } else if (type == Type.LIST) {
            if (tag.get(realKey) instanceof ListTag listTag) {
                switch (listTag.getElementType()) {
                    case Tag.TAG_STRING -> out = listTag.stream().map(Tag::getAsString).toList();
                    case Tag.TAG_FLOAT -> out = listTag.stream().map(FloatTag.class::cast).map(FloatTag::getAsFloat).toList();
                    case Tag.TAG_DOUBLE -> out = listTag.stream().map(DoubleTag.class::cast).map(DoubleTag::getAsFloat).toList();
                    case Tag.TAG_INT -> out = listTag.stream().map(IntTag.class::cast).map(IntTag::getAsFloat).toList();
                    case Tag.TAG_LONG -> out = listTag.stream().map(LongTag.class::cast).map(LongTag::getAsFloat).toList();
                    case Tag.TAG_SHORT -> out = listTag.stream().map(ShortTag.class::cast).map(ShortTag::getAsFloat).toList();
                    case Tag.TAG_COMPOUND -> out = listTag.stream().map(CompoundTag.class::cast).toList();
                    case Tag.TAG_BYTE -> out = listTag.stream().map(ByteTag.class::cast).map(t -> t.getAsByte() != 0).toList();
                    case Tag.TAG_LIST -> out = listTag.stream().map(ListTag.class::cast).toList();
                    default -> out = Collections.emptyList();
                }
            }
        }
    }

    public Class<?> getDisplayType() {
        return switch (type) {
            case STRING -> String.class;
            case NUMBER -> Float.class;
            case BOOL -> Boolean.class;
            case COMPOUND -> CompoundTag.class;
            case LIST -> List.class;
        };
    }

    @ConfigSetter(field = "type")
    public void setType(Type type) {
        this.type = type;
        for (var outputPort : getOutputPorts()) {
            outputPort.portData.displayType = getDisplayType();
        }
    }

    @CustomPortBehavior(field = "out")
    public List<PortData> modifyOutPort(List<PortEdge> edges) {
        return List.of(new PortData()
                .displayName("out")
                .identifier("out")
                .acceptMultipleEdges(true)
                .displayType(getDisplayType()));
    }

    @CustomPortOutput(field = "out")
    public void pushOut(List<PortEdge> outputEdges, NodePort outputPort) {
        for (var edge : outputEdges) {
            edge.passThroughBuffer = out;
        }
    }



}
