package com.lowdragmc.lowdraglib2.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib2.graphprocessor.nodes.utils.PrintNode;

import java.util.regex.Pattern;

@LDLRegister(name = "regex", group = "graph_processor.node.logic", registry = "ldlib2:graph_node")
public class RegexNode extends BaseNode {
    @InputPort
    public Object in;
    @InputPort
    public String regex;
    @OutputPort
    public boolean out;

    // runtime
    private Pattern pattern;


    @Override
    public void process() {
        if (in == null || regex == null) {
            out = false;
            return;
        }
        if (pattern == null) {
            pattern = Pattern.compile(regex);
        } else if (!pattern.pattern().equals(regex)) {
            pattern = Pattern.compile(regex);
        }
        out = pattern.matcher(PrintNode.format(in)).matches();
    }
}
