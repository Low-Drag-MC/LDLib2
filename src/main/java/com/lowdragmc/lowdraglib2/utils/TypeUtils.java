package com.lowdragmc.lowdraglib2.utils;

import lombok.experimental.UtilityClass;

import java.lang.reflect.*;
import java.util.*;

@UtilityClass
public final class TypeUtils {

    public static boolean isAssignableFrom(Type target, Type source) {
        if (target.equals(source)) {
            return true;
        }

        // Class vs Class
        if (target instanceof Class<?> tc && source instanceof Class<?> sc) {
            return isClassAssignable(tc, sc);
        }

        // ParameterizedType vs ParameterizedType
        if (target instanceof ParameterizedType tp &&
            source instanceof ParameterizedType sp) {
            return isParameterizedAssignable(tp, sp);
        }

        // ParameterizedType vs Class
        if (target instanceof ParameterizedType tp &&
            source instanceof Class<?> sc) {
            return isClassAssignable(
                    (Class<?>) tp.getRawType(), sc);
        }

        // Class vs ParameterizedType
        if (target instanceof Class<?> tc &&
            source instanceof ParameterizedType sp) {
            return isClassAssignable(tc,
                    (Class<?>) sp.getRawType());
        }

        return false;
    }

    // ===========================
    // Class assignable
    // ===========================
    private static boolean isClassAssignable(Class<?> target, Class<?> source) {
        // Handle boxing
        if (target.isPrimitive()) {
            target = primitiveToWrapper(target);
        }
        if (source.isPrimitive()) {
            source = primitiveToWrapper(source);
        }

        return target.isAssignableFrom(source);
    }

    // ===========================
    // ParameterizedType assignable
    // ===========================
    private static boolean isParameterizedAssignable(
            ParameterizedType target,
            ParameterizedType source) {

        Class<?> targetRaw = (Class<?>) target.getRawType();
        Class<?> sourceRaw = (Class<?>) source.getRawType();

        if (!targetRaw.isAssignableFrom(sourceRaw)) {
            return false;
        }

        Type[] targetArgs = target.getActualTypeArguments();
        Type[] sourceArgs = source.getActualTypeArguments();

        if (targetArgs.length != sourceArgs.length) {
            return false;
        }

        for (int i = 0; i < targetArgs.length; i++) {
            if (!isTypeArgumentAssignable(
                    targetArgs[i],
                    sourceArgs[i])) {
                return false;
            }
        }

        return true;
    }

    // ===========================
    // Generic argument rules
    // ===========================

    private static boolean isTypeArgumentAssignable(
            Type target,
            Type source) {

        // Exact match
        if (target.equals(source)) {
            return true;
        }

        // Wildcard: <? extends X>
        if (target instanceof WildcardType wt) {
            for (Type upper : wt.getUpperBounds()) {
                if (!isAssignableFrom(upper, source)) {
                    return false;
                }
            }
            return true;
        }

        // Normal recursive check
        return isAssignableFrom(target, source);
    }

    // ===========================
    // Primitive → Wrapper
    // ===========================
    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS =
            Map.of(
                    boolean.class, Boolean.class,
                    byte.class, Byte.class,
                    short.class, Short.class,
                    int.class, Integer.class,
                    long.class, Long.class,
                    float.class, Float.class,
                    double.class, Double.class,
                    char.class, Character.class
            );

    private static Class<?> primitiveToWrapper(Class<?> c) {
        return PRIMITIVE_WRAPPERS.getOrDefault(c, c);
    }
}
