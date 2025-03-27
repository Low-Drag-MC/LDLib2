package com.lowdragmc.lowdraglib.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.graphprocessor.nodes.utils.PrintNode;

import java.util.regex.Pattern;

@LDLRegister(name = "regex", group = "graph_processor.node.logic")
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
