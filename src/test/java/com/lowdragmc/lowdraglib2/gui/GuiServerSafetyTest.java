package com.lowdragmc.lowdraglib2.gui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.neoforged.api.distmarker.OnlyIn;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiServerSafetyTest {
    @Test
    void modularUiDoesNotUseMemberLevelOnlyInAnnotations() {
        List<String> annotatedFields = Arrays.stream(ModularUI.class.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(OnlyIn.class))
                .map(Field::getName)
                .toList();
        List<String> annotatedMethods = Arrays.stream(ModularUI.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(OnlyIn.class))
                .map(Method::getName)
                .distinct()
                .toList();

        assertTrue(annotatedFields.isEmpty(), "Expected no member-level @OnlyIn fields, found: " + annotatedFields);
        assertTrue(annotatedMethods.isEmpty(), "Expected no member-level @OnlyIn methods, found: " + annotatedMethods);
    }

    @Test
    void modularUiDoesNotExposeClientTypesInSharedApi() {
        List<String> clientFields = Arrays.stream(ModularUI.class.getDeclaredFields())
                .filter(field -> isClientType(field.getType()))
                .map(field -> field.getName() + ":" + field.getType().getName())
                .toList();
        List<String> clientMethods = Arrays.stream(ModularUI.class.getDeclaredMethods())
                .filter(this::hasClientTypeInSignature)
                .map(this::describeMethod)
                .distinct()
                .toList();
        List<String> clientNestedClasses = Arrays.stream(ModularUI.class.getDeclaredClasses())
                .filter(this::isClientType)
                .map(Class::getName)
                .toList();

        assertTrue(clientFields.isEmpty(), "Expected no client-only field types on ModularUI, found: " + clientFields);
        assertTrue(clientMethods.isEmpty(), "Expected no client-only method signatures on ModularUI, found: " + clientMethods);
        assertTrue(clientNestedClasses.isEmpty(), "Expected no client-only nested classes on ModularUI, found: " + clientNestedClasses);
    }

    private boolean hasClientTypeInSignature(Method method) {
        if (isClientType(method.getReturnType())) {
            return true;
        }
        return Arrays.stream(method.getParameterTypes()).anyMatch(this::isClientType);
    }

    private boolean isClientType(Class<?> type) {
        String name = type.getName();
        return name.startsWith("net.minecraft.client.")
                || name.contains(".gui.holder.")
                || name.equals("com.lowdragmc.lowdraglib2.gui.ui.ModularUI$ModularUIWidget");
    }

    private String describeMethod(Method method) {
        return method.getName() + Arrays.toString(method.getParameterTypes()) + "->" + method.getReturnType().getName();
    }
}
