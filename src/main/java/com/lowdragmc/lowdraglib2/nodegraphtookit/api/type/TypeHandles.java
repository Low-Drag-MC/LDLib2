package com.lowdragmc.lowdraglib2.nodegraphtookit.api.type;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldValueConfigurable;

public class TypeHandles {
    public static final class Unknown { private Unknown() {} }
    public static final class ExecutionFlow { private ExecutionFlow() {} }
    public static final class Subgraph { private Subgraph() {} }
    public static final class MissingPort { private MissingPort() {} }

    public static final TypeHandle AUTOMATIC;
    public static final TypeHandle MISSING;
    public static final TypeHandle UNKNOWN;
    public static final TypeHandle EXECUTION_FLOW;
    public static final TypeHandle SUBGRAPH;

    public static final TypeHandle MISSING_PORT;

    public static final TypeHandle BOOL;
    public static final TypeHandle VOID;
    public static final TypeHandle CHAR;
    public static final TypeHandle DOUBLE;
    public static final TypeHandle FLOAT;
    public static final TypeHandle INT;
    public static final TypeHandle LONG;
    public static final TypeHandle OBJECT;
    public static final TypeHandle STRING;

    public static final TypeHandle COLOR;

    static {
        // Normal type handles
        MISSING_PORT = TypeHandleHelpers.fromType(MissingPort.class);

        BOOL = TypeHandleHelpers.fromType(Boolean.class);
        VOID = TypeHandleHelpers.fromType(Void.class);
        CHAR = TypeHandleHelpers.fromType(Character.class);
        DOUBLE = TypeHandleHelpers.fromType(Double.class);
        FLOAT = TypeHandleHelpers.fromType(Float.class);
        INT = TypeHandleHelpers.fromType(Integer.class);
        LONG = TypeHandleHelpers.fromType(Long.class);

        OBJECT = TypeHandleHelpers.fromType(Object.class);
        STRING = TypeHandleHelpers.fromType(String.class);

        // Custom sentinel types
        AUTOMATIC = TypeHandleHelpers.customType("AUTOMATIC", "Automatic");
        MISSING = TypeHandleHelpers.customType("MISSING_TYPE", null);

        UNKNOWN = TypeHandleHelpers.customType(Unknown.class, "UNKNOWN");
        EXECUTION_FLOW = TypeHandleHelpers.customType(ExecutionFlow.class, "EXECUTION_FLOW");
        SUBGRAPH = TypeHandleHelpers.customType(Subgraph.class, "SUBGRAPH");

        COLOR = TypeHandleHelpers.customType(Integer.class, "COLOR", "Color");
        TypeHandleHelpers.setCustomConfigurable(COLOR, (valueConfigurable, typeHandle) ->
                IConfigurable.create(group -> group.addConfigurator(new ColorConfigurator("",
                        valueConfigurable::getValue, valueConfigurable::setValue, -1,
                        valueConfigurable.forceUpdate()))));
    }
}
