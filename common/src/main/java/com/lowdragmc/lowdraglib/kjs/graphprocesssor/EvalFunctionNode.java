package com.lowdragmc.lowdraglib.kjs.graphprocesssor;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.CustomPortBehavior;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.CustomPortInput;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.NodePort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.PortData;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.PortEdge;
import dev.latvian.mods.kubejs.server.ServerScriptManager;
import dev.latvian.mods.rhino.Function;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@LDLRegister(name = "eval function", group = "graph_processor.node.kjs", modID = "kubejs")
public class EvalFunctionNode extends BaseNode {
    @InputPort
    public String code;
    @InputPort
    public List<Object> args;
    @OutputPort(name = "return")
    public Object result;
    @OutputPort(name = "return", tips = "return true if the node captures exception, and the result is the error log.")
    public boolean error;

    // runtime
    @Nullable
    private String codeCache;
    @Nullable
    private Function functionCache;

    @Override
    protected void process() {
        var manager = ServerScriptManager.getScriptManager();
        var context = manager.context;
        var scope = manager.topLevelScope;
        result = null;
        error = false;
        var function = getFunction();
        if (function != null) {
            try {
                result = function.call(context, scope, scope, args.toArray());
            } catch (Exception e) {
                error = true;
                result = e;
            }
        }
    }

    @Nullable
    public Function getFunction() {
        if (!Objects.equals(code, codeCache)) {
            codeCache = code;
            functionCache = null;
            if (codeCache != null) {
                try {
                    var manager = ServerScriptManager.getScriptManager();
                    var context = manager.context;
                    var scope = manager.topLevelScope;
                    if (context.evaluateString(scope, codeCache, name(), 1, null) instanceof Function function) {
                        functionCache = function;
                    }
                } catch (Exception e) {
                    LDLib.LOGGER.error("Failed to compile kjs function \n{}", codeCache, e);
                }
            }
        }
        return functionCache;
    }

    @CustomPortBehavior(field = "args")
    public List<PortData> inputPortBehavior(List<PortEdge> edges) {
        var ports = new ArrayList<PortData>();
        for (int i = 0; i < edges.size() + 1; i++) {
            var identifier = String.valueOf(i);
            if (i < edges.size()) {
                var edge = edges.get(i);
                edge.inputPortIdentifier = identifier;
            }
            ports.add(new PortData()
                    .displayName("arg " + i)
                    .identifier(identifier)
                    .displayType(Object.class));
        }
        if (args == null) {
            args = new ArrayList<>();
        }
        args.clear();
        while (args.size() + 1 < ports.size()) {
            args.add(null);
        }
        return ports;
    }

    // This function will be called once per port created from the `args` custom port function
    // will in parameter the list of the edges connected to this port
    @CustomPortInput(field = "args")
    public void pullArgs(List<PortEdge> inputEdges, NodePort inputPort) {
        if (inputEdges.isEmpty()) return;
        Object value = null;
        // we only find the first available edge
        for (PortEdge inputEdge : inputEdges) {
            if (inputEdge.passThroughBuffer != null) {
                value = inputEdge.passThroughBuffer;
                break;
            }
        }
        var index = inputPort.owner.getInputPorts().indexOf(inputPort) - 1;
        while (args.size() <= index) {
            args.add(value);
        }
        args.set(index, value);
    }

}
