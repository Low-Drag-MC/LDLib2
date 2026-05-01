package com.lowdragmc.lowdraglib2.nodegraphtookit.api.type;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
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

        VOID = TypeHandleHelpers.fromType(Void.class);

        BOOL = TypeHandleHelpers.fromType(Boolean.class);
        TypeHandleHelpers.setCustomDefaultValue(BOOL, () -> false);
        CHAR = TypeHandleHelpers.fromType(Character.class);
        TypeHandleHelpers.setCustomDefaultValue(CHAR, () -> '\0');
        DOUBLE = TypeHandleHelpers.fromType(Double.class);
        TypeHandleHelpers.setCustomDefaultValue(DOUBLE, () -> 0.0);
        FLOAT = TypeHandleHelpers.fromType(Float.class);
        TypeHandleHelpers.setCustomDefaultValue(FLOAT, () -> 0.0f);
        INT = TypeHandleHelpers.fromType(Integer.class);
        TypeHandleHelpers.setCustomDefaultValue(INT, () -> 0);
        LONG = TypeHandleHelpers.fromType(Long.class);
        TypeHandleHelpers.setCustomDefaultValue(LONG, () -> 0L);
        STRING = TypeHandleHelpers.fromType(String.class);
        TypeHandleHelpers.setCustomDefaultValue(STRING, () -> "");

        OBJECT = TypeHandleHelpers.fromType(Object.class);

        // Custom sentinel types
        AUTOMATIC = TypeHandleHelpers.customType("AUTOMATIC", "Automatic");
        MISSING = TypeHandleHelpers.customType("MISSING_TYPE", null);

        UNKNOWN = TypeHandleHelpers.customType(Unknown.class, "UNKNOWN");
        EXECUTION_FLOW = TypeHandleHelpers.customType(ExecutionFlow.class, "EXECUTION_FLOW");
        SUBGRAPH = TypeHandleHelpers.customType(Subgraph.class, "SUBGRAPH");

        COLOR = TypeHandleHelpers.customType(Integer.class, "COLOR", "Color");
        TypeHandleHelpers.setCustomIcon(COLOR, Icons.COLOR);
        TypeHandleHelpers.setCustomConfigurable(COLOR, (valueConfigurable, typeHandle) ->
                IConfigurable.create(group -> group.addConfigurator(new ColorConfigurator("",
                        valueConfigurable::getValue, valueConfigurable::setValue, -1,
                        valueConfigurable.forceUpdate()))));
    }

    public static void init() {}
}
