package com.lowdragmc.lowdraglib2.utils;

import com.lowdragmc.lowdraglib2.LDLib2;
import lombok.experimental.UtilityClass;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.*;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

@UtilityClass
public final class ReflectionUtils {

    public static Class<?> getRawType(Type type, Class<?> fallback) {
        var rawType = getRawType(type);
        return rawType != null ? rawType : fallback;
    }

    public static Class<?> getRawType(Type type) {
        return switch (type) {
            case Class<?> aClass -> aClass;
            case GenericArrayType genericArrayType -> getRawType(genericArrayType.getGenericComponentType());
            case ParameterizedType parameterizedType -> getRawType(parameterizedType.getRawType());
            case null, default -> null;
        };
    }

    public static <A extends Annotation> void findAnnotationClasses(Class<A> annotationClass,
                                                                    @Nullable Predicate<Map<String, Object>> annotationPredicate,
                                                                    Consumer<Class<?>> consumer,
                                                                    Runnable onFinished) {
        org.objectweb.asm.Type annotationType = org.objectweb.asm.Type.getType(annotationClass);
        for (ModFileScanData data : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData annotation : data.getAnnotations()) {
                if (annotationType.equals(annotation.annotationType()) && annotation.targetType() == ElementType.TYPE) {
                    if (annotationPredicate == null || annotationPredicate.test(annotation.annotationData())) {
                        try {
                            consumer.accept(Class.forName(annotation.memberName(), false, ReflectionUtils.class.getClassLoader()));
                        } catch (Throwable throwable) {
                            LDLib2.LOGGER.error("Failed to load class for notation: {}", annotation.memberName(), throwable);
                        }
                    }
                }
            }
        }
        onFinished.run();
    }

    public static <A extends Annotation> void findAnnotationStaticField(Class<A> annotationClass,
                                                                        @Nullable Predicate<Map<String, Object>> annotationPredicate,
                                                                        BiConsumer<Field, Object> consumer,
                                                                        Runnable onFinished) {
        org.objectweb.asm.Type annotationType = org.objectweb.asm.Type.getType(annotationClass);
        for (ModFileScanData data : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData annotation : data.getAnnotations()) {
                if (annotationType.equals(annotation.annotationType()) && annotation.targetType() == ElementType.FIELD) {
                    if (annotationPredicate == null || annotationPredicate.test(annotation.annotationData())) {
                        var clazz = annotation.clazz();
                        var fieldName = annotation.memberName();
                        try {
                            var field = Class.forName(annotation.clazz().getClassName()).getDeclaredField(fieldName);
                            if (Modifier.isStatic(field.getModifiers())) {
                                consumer.accept(field, field.get(null));
                            } else {
                                LDLib2.LOGGER.error("Field is not static for notation: {} in {}", fieldName, clazz);
                            }
                        } catch (Throwable throwable) {
                            LDLib2.LOGGER.error("Failed to load static field for notation: {} in {}", fieldName, clazz, throwable);
                        }
                    }
                }
            }
        }
        onFinished.run();
    }

    public static <A extends Annotation> void findAnnotationStaticMethod(Class<A> annotationClass,
                                                                         @Nullable Predicate<Map<String, Object>> annotationPredicate,
                                                                         Consumer<Method> consumer,
                                                                         Runnable onFinished) {
        org.objectweb.asm.Type annotationType = org.objectweb.asm.Type.getType(annotationClass);
        for (ModFileScanData data : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData annotation : data.getAnnotations()) {
                if (annotationType.equals(annotation.annotationType()) && annotation.targetType() == ElementType.METHOD) {
                    if (annotationPredicate == null || annotationPredicate.test(annotation.annotationData())) {
                        var clazz = annotation.clazz();
                        var methodFullDesc = annotation.memberName();
                        var methodName = methodFullDesc.substring(0, methodFullDesc.indexOf('('));
                        var methodDesc = methodFullDesc.substring(methodFullDesc.indexOf('('));
                        try {
                            for (var method : Class.forName(annotation.clazz().getClassName()).getDeclaredMethods()) {
                                if (method.getName().equals(methodName) &&
                                        methodDesc.equals(org.objectweb.asm.Type.getMethodDescriptor(method))) {
                                    if (Modifier.isStatic(method.getModifiers())) {
                                        consumer.accept(method);
                                    } else {
                                        LDLib2.LOGGER.error("Method is not static for notation: {} in {}", methodDesc, clazz);
                                    }
                                }
                            }
                        } catch (Throwable throwable) {
                            LDLib2.LOGGER.error("Failed to load static method for notation: {} in {}", methodDesc, clazz, throwable);
                        }
                    }
                }
            }
        }
        onFinished.run();
    }
}
