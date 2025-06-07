package com.lowdragmc.lowdraglib2.graphprocessor.data.parameter;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.CustomPortBehavior;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib2.graphprocessor.data.PortData;
import com.lowdragmc.lowdraglib2.graphprocessor.data.PortEdge;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

@LDLRegister(name = "parameter", group = "graph_processor.node.parameter", registry = "ldlib2:graph_node")
public class ParameterNode extends BaseNode {

    @InputPort
    public Object input;
    @OutputPort
    public Object output;
    /**
     * We serialize the GUID of the exposed parameter in the graph so we can retrieve the true ExposedParameter from the graph
     */
    @Persisted
    public String parameterIdentifier;
    @Nullable
    public ExposedParameter<?> parameter;

    public ParameterNode() {
        expanded = false;
    }

    @Override
    public void setExpanded(boolean expanded) {
    }

    @Override
    public String getDisplayName() {
        return parameter == null ? super.getDisplayName() : parameter.getDisplayName();
    }

    @Override
    public void setDisplayName(String displayName) {
        if (parameter != null) parameter.setDisplayName(displayName);
        else super.setDisplayName(displayName);
    }

    @Override
    protected void enable() {
        parameter = graph.getExposedParameterFromIdentifier(parameterIdentifier);
        if (parameter == null) {
            LDLib2.LOGGER.error("Property {} Can't be found !", parameterIdentifier);
            graph.removeNode(this);
            return;
        }
        output = parameter.getValue();
    }

    @CustomPortBehavior(field = "output")
    public List<PortData> getOutputPort(List<PortEdge> edges) {
        if (parameter != null && parameter.getAccessor() == ExposedParameter.ParameterAccessor.Get) {
            return List.of(new PortData()
                    .identifier("output")
                    .displayName("Value")
                    .displayType(parameter.type)
                    .tooltip(parameter.getTips())
                    .acceptMultipleEdges(true));
        }
        return Collections.emptyList();
    }

    @CustomPortBehavior(field = "input")
    public List<PortData> getInputPort(List<PortEdge> edges) {
        if (parameter != null && parameter.getAccessor() == ExposedParameter.ParameterAccessor.Set) {
            return List.of(new PortData()
                    .identifier("input")
                    .displayName("Value")
                    .displayType(parameter.type)
                    .tooltip(parameter.getTips()));
        }
        return Collections.emptyList();
    }

    @Override
    protected void process() {
        if (parameter == null) return;
        if (parameter.getAccessor() == ExposedParameter.ParameterAccessor.Get)
            output = parameter.getValue();
        else
            graph.updateExposedParameter(parameter.identifier, input);
    }

}


