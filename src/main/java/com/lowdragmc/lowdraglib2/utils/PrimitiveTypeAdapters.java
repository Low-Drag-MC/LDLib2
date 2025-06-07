package com.lowdragmc.lowdraglib2.utils;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;

@LDLRegister(name = "primitive_converter", registry = "ldlib2:type_adapter")
public class PrimitiveTypeAdapters implements TypeAdapter.ITypeAdapter {

    @Override
    public void onRegister() {
        TypeAdapter.registerAdapter(boolean.class, Boolean.class, Boolean::valueOf);
        TypeAdapter.registerAdapter(Boolean.class, boolean.class, Boolean::booleanValue);
        TypeAdapter.registerAdapter(float.class, int.class, Float::intValue);
        TypeAdapter.registerAdapter(int.class, float.class, Integer::floatValue);
        TypeAdapter.registerAdapter(float.class, Float.class, o -> o);
        TypeAdapter.registerAdapter(int.class, Integer.class, o -> o);
        TypeAdapter.registerAdapter(int.class, Float.class, Integer::floatValue);
        TypeAdapter.registerAdapter(float.class, Integer.class, Float::intValue);
        TypeAdapter.registerAdapter(Float.class, float.class, o -> o == null ? 0 : o);
        TypeAdapter.registerAdapter(Integer.class, int.class, o -> o == null ? 0 : o);
        TypeAdapter.registerAdapter(Float.class, int.class, o -> o == null ? 0 : o.intValue());
        TypeAdapter.registerAdapter(Integer.class, float.class, o -> o == null ? 0 : o.floatValue());
        TypeAdapter.registerAdapter(Float.class, Integer.class, o -> o == null ? 0 : o.intValue());
        TypeAdapter.registerAdapter(Integer.class, Float.class, o -> o == null ? 0 : o.floatValue());

        TypeAdapter.registerAdapter(Boolean.class, int.class, o -> o == null ? 0 : o ? 1 : 0);
        TypeAdapter.registerAdapter(Boolean.class, Integer.class, o -> o == null ? 0 : o ? 1 : 0);
        TypeAdapter.registerAdapter(Boolean.class, float.class, o -> o == null ? 0 : o ? 1f : 0);
        TypeAdapter.registerAdapter(Boolean.class, Float.class, o -> o == null ? 0 : o ? 1f : 0);

        TypeAdapter.registerAdapter(boolean.class, int.class, o -> o == null ? 0 : o ? 1 : 0);
        TypeAdapter.registerAdapter(boolean.class, Integer.class, o -> o == null ? 0 : o ? 1 : 0);
        TypeAdapter.registerAdapter(boolean.class, float.class, o -> o == null ? 0 : o ? 1f : 0);
        TypeAdapter.registerAdapter(boolean.class, Float.class, o -> o == null ? 0 : o ? 1f : 0);

    }
}
