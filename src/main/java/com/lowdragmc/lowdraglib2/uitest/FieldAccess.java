package com.lowdragmc.lowdraglib2.uitest;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * The reflective field escape hatch shared by {@link ServerContext} and {@link TestContext}. Tests
 * frequently need to observe or seed state on a class the test author does not own and cannot add
 * an accessor to; without this the alternative is either not testing it or polluting production
 * classes with test-only getters.
 */
final class FieldAccess {

    private FieldAccess() {
    }

    @SuppressWarnings("unchecked")
    static <T> T get(Object target, String fieldName) {
        try {
            return (T) find(target.getClass(), fieldName).get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read field '" + fieldName + "' on "
                    + target.getClass().getName(), e);
        }
    }

    static void set(Object target, String fieldName, @Nullable Object value) {
        try {
            find(target.getClass(), fieldName).set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot write field '" + fieldName + "' on "
                    + target.getClass().getName(), e);
        }
    }

    private static Field find(Class<?> type, String fieldName) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                var field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // keep walking up
            }
        }
        throw new NoSuchFieldException(fieldName + " not found on " + type.getName() + " or its supertypes");
    }
}
