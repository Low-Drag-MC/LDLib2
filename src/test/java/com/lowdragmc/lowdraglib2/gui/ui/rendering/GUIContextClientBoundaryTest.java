package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import net.neoforged.api.distmarker.OnlyIn;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GUIContextClientBoundaryTest {
    @Test
    void guiContextDoesNotUseMemberLevelOnlyInAnnotations() {
        var annotatedFields = Arrays.stream(GUIContext.class.getDeclaredFields())
                .map(Field::getName)
                .filter(name -> hasOnlyIn(findField(name)))
                .toList();
        var annotatedMethods = Arrays.stream(GUIContext.class.getDeclaredMethods())
                .map(Method::getName)
                .distinct()
                .filter(name -> Arrays.stream(GUIContext.class.getDeclaredMethods())
                        .filter(method -> method.getName().equals(name))
                        .anyMatch(this::hasOnlyIn))
                .toList();

        assertTrue(annotatedFields.isEmpty(), "Expected no member-level @OnlyIn fields, found: " + annotatedFields);
        assertTrue(annotatedMethods.isEmpty(), "Expected no member-level @OnlyIn methods, found: " + annotatedMethods);
    }

    private Field findField(String name) {
        try {
            return GUIContext.class.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    private boolean hasOnlyIn(Field field) {
        return field.isAnnotationPresent(OnlyIn.class);
    }

    private boolean hasOnlyIn(Method method) {
        return method.isAnnotationPresent(OnlyIn.class);
    }
}
