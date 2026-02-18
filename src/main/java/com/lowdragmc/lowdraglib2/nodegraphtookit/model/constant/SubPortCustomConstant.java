package com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SubPortCustomConstant extends Constant {
    public final PortModel port;
    @Setter
    private Supplier<Object> getter;
    @Setter
    private Consumer<Object> setter;
    @Getter @Setter
    private Object defaultValue;


    public SubPortCustomConstant(PortModel portModel, Supplier<Object> getter, Consumer<Object> setter) {
        setOwner(portModel);
        this.port = portModel;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public Object getValue() {
        return getter.get();
    }

    @Override
    public void setValue(Object value) {
        setter.accept(value);
    }

    @Override
    public Type getType() {
        return port.getDataType();
    }

    @Override
    public Constant copy() {
        var copy = new SubPortCustomConstant(port, getter, setter);
        copy.defaultValue = defaultValue;
        copy.init(typeHandle);
        return copy;
    }
}
