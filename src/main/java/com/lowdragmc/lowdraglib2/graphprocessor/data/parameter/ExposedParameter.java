package com.lowdragmc.lowdraglib2.graphprocessor.data.parameter;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Accessors(chain = true)
public class ExposedParameter<T> implements IPersistedSerializable {
    public enum ParameterAccessor {
        Get,
        Set
    }
    public final java.lang.String identifier;
    public final Class<T> type;
    @Getter
    private T value;
    @Getter
    @Setter
    @Persisted
    private java.lang.String displayName;
    @Getter
    @Setter
    private List<java.lang.String> tips = new ArrayList<>();
    @Persisted
    @Setter
    @Getter
    private ParameterAccessor accessor = ParameterAccessor.Get;

    public ExposedParameter(java.lang.String identifier, Class<T> type) {
        this.identifier = identifier;
        this.type = type;
        this.displayName = identifier;
    }

    public void setValue(Object value) {
        if (type.isInstance(value)) {
            this.value = (T) value;
        } else {
            this.value = null;
        }
    }

    public static class Int extends ExposedParameter<Integer> {
        public Int(java.lang.String identifier) {
            super(identifier, Integer.class);
        }
    }

    public static class Float extends ExposedParameter<java.lang.Float> {
        public Float(java.lang.String identifier) {
            super(identifier, java.lang.Float.class);
        }
    }

    public static class Bool extends ExposedParameter<Boolean> {
        public Bool(java.lang.String identifier) {
            super(identifier, Boolean.class);
        }
    }

    public static class String extends ExposedParameter<java.lang.String> {
        public String(java.lang.String identifier) {
            super(identifier, java.lang.String.class);
        }
    }

    public static class XYZ extends ExposedParameter<org.joml.Vector3f> {
        public XYZ(java.lang.String identifier) {
            super(identifier, org.joml.Vector3f.class);
        }
    }

}
