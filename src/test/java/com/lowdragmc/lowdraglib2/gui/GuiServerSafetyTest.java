package com.lowdragmc.lowdraglib2.gui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
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
        assertNoMemberLevelOnlyIn(ModularUI.class);
    }

    @Test
    void modularUiDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(ModularUI.class);
    }

    @Test
    void uiElementDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(UIElement.class);
    }

    @Test
    void uiElementDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(UIElement.class);
    }

    @Test
    void textElementDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(TextElement.class);
    }

    private void assertNoMemberLevelOnlyIn(Class<?> type) {
        List<String> annotatedFields = Arrays.stream(type.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(OnlyIn.class))
                .map(Field::getName)
                .toList();
        List<String> annotatedMethods = Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(OnlyIn.class))
                .map(Method::getName)
                .distinct()
                .toList();

        assertTrue(annotatedFields.isEmpty(), "Expected no member-level @OnlyIn fields on " + type.getSimpleName() + ", found: " + annotatedFields);
        assertTrue(annotatedMethods.isEmpty(), "Expected no member-level @OnlyIn methods on " + type.getSimpleName() + ", found: " + annotatedMethods);
    }

    private void assertNoClientTypeExposure(Class<?> type) {
        List<String> clientFields = Arrays.stream(type.getDeclaredFields())
                .filter(field -> isClientType(field.getType()))
                .map(field -> field.getName() + ":" + field.getType().getName())
                .toList();
        List<String> clientMethods = Arrays.stream(type.getDeclaredMethods())
                .filter(this::hasClientTypeInSignature)
                .map(this::describeMethod)
                .distinct()
                .toList();
        List<String> clientNestedClasses = Arrays.stream(type.getDeclaredClasses())
                .filter(this::isClientType)
                .map(Class::getName)
                .toList();

        assertTrue(clientFields.isEmpty(), "Expected no client-only field types on " + type.getSimpleName() + ", found: " + clientFields);
        assertTrue(clientMethods.isEmpty(), "Expected no client-only method signatures on " + type.getSimpleName() + ", found: " + clientMethods);
        assertTrue(clientNestedClasses.isEmpty(), "Expected no client-only nested classes on " + type.getSimpleName() + ", found: " + clientNestedClasses);
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
                || type.isAnnotationPresent(OnlyIn.class)
                || name.contains(".gui.holder.")
                || name.equals("com.lowdragmc.lowdraglib2.gui.ui.ModularUI$ModularUIWidget");
    }

    private String describeMethod(Method method) {
        return method.getName() + Arrays.toString(method.getParameterTypes()) + "->" + method.getReturnType().getName();
    }
}
