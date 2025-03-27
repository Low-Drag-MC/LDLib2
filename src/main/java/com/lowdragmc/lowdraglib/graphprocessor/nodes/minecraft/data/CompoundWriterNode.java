package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft.data;

import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.trigger.LinearTriggerNode;
import net.minecraft.nbt.*;

import java.util.List;

@LDLRegister(name = "compound writer", group = "graph_processor.node.minecraft.data")
public class CompoundWriterNode extends LinearTriggerNode {
    @InputPort
    public CompoundTag tag;
    @InputPort
    public String key;
    @InputPort
    public Object value;
    @OutputPort
    public CompoundTag out;
    @Configurable(name = "key")
    public String internalKey = "";

    @Override
    public void process() {
        if (tag == null) {
            return;
        }
        out = tag.copy();
        var realKey = key;
        if (realKey == null) {
            realKey = internalKey;
        }
        if (value instanceof String) {
            out.putString(realKey, (String) value);
        } else if (value instanceof Number number) {
            out.putFloat(realKey, number.floatValue());
        } else if (value instanceof Boolean) {
            out.putBoolean(realKey, (Boolean) value);
        } else if (value instanceof CompoundTag) {
            out.put(realKey, (CompoundTag) value);
        } else if (value instanceof List<?> list) {
            var listTag = new ListTag();
            dfsList(listTag, list);
            out.put(realKey, listTag);
        }
    }

    public void dfsList(ListTag listTag, List<?> list) {
        for (var element : list) {
            if (element instanceof String) {
                listTag.add(StringTag.valueOf((String) element));
            } else if (element instanceof Number number) {
                listTag.add(FloatTag.valueOf(number.floatValue()));
            } else if (element instanceof Boolean v) {
                listTag.add(ByteTag.valueOf(v));
            } else if (element instanceof CompoundTag) {
                listTag.add((CompoundTag) element);
            } else if (element instanceof List<?> subList) {
                var subListTag = new ListTag();
                dfsList(subListTag, subList);
                listTag.add(subListTag);
            }
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        for (var port : getInputPorts()) {
            if (port.fieldName.equals("key")) {
                if (!port.getEdges().isEmpty()) return;
            }
        }
        super.buildConfigurator(father);
    }
}
